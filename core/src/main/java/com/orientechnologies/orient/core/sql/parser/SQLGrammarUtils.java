/*
 * Copyright 2013 Orient Technologies.
 * Copyright 2013 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.core.sql.parser;

import java.util.ArrayList;
import java.util.List;

import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.command.OCommandTraverse;
import com.orientechnologies.orient.core.sql.model.*;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.Iterator;
import java.util.LinkedHashMap;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.orientechnologies.orient.core.command.OCommandExecutor;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.sql.command.OCommandCustom;
import com.orientechnologies.orient.core.sql.method.OSQLMethod;
import com.orientechnologies.orient.core.sql.method.OSQLMethodFactory;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.command.OCommandRequestText;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.command.OCommandSelect;
import com.orientechnologies.orient.core.sql.functions.OSQLFunction;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionFactory;

import static com.orientechnologies.orient.core.sql.parser.OSQLParser.*;
import static com.orientechnologies.common.util.OClassLoaderHelper.lookupProviderWithOrientClassLoader;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.model.reflect.OExpressionClass;
import com.orientechnologies.orient.core.sql.model.reflect.OExpressionORID;
import com.orientechnologies.orient.core.sql.model.reflect.OExpressionRaw;
import com.orientechnologies.orient.core.sql.model.reflect.OExpressionSize;
import com.orientechnologies.orient.core.sql.model.reflect.OExpressionThis;
import com.orientechnologies.orient.core.sql.model.reflect.OExpressionType;
import com.orientechnologies.orient.core.sql.model.reflect.OExpressionVersion;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

/**
 * Utility class to convert from SQL to a list of arguments.
 * 
 * @author Johann Sorel (Geomatys)
 */
public final class SQLGrammarUtils {

  private static ClassLoader CLASSLOADER = SQLGrammarUtils.class.getClassLoader();
  
  private static final DateFormat DF1 = new SimpleDateFormat("yyyy-MM-dd");
  private static final SimpleDateFormat DF2 = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
  private static final SimpleDateFormat DF3 = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  
  private SQLGrammarUtils() {
  }

  public static OExpression parseExpression(String osql) throws OCommandSQLParsingException{
      final OSQLParser parser = getParser(osql);
      final SingleexpContext context = parser.singleexp();
      if(context != null){
          return visit(context.filter());
      }else{
          throw new OCommandSQLParsingException("Not a valid expression :"+osql);
      }
  }
  
  public static OCommandExecutor parse(String osql) throws OCommandSQLParsingException{
    final ParseTree tree = compileExpression(osql);
    return toOrient(tree);
  }
  
  public static CommandContext compileExpression(String osql) {
      final OSQLParser parser = getParser(osql);
      final CommandContext sentence = parser.command();
      return sentence;
  }

  private static OSQLParser getParser(String osql){
      //lexer splits input into tokens
      final CharStream input = new ANTLRInputStream(osql);
      final TokenStream tokens = new CommonTokenStream(new OSQLLexer(input));

      //parser generates abstract syntax tree
      final OSQLParser parser = new OSQLParser(tokens);
      return parser;
  }

  public static OCommandExecutor toOrient(ParseTree tree) throws OCommandSQLParsingException{
    return SQLGrammarUtils.visit((OSQLParser.CommandContext)tree);
  }
  
  public static String toString(ParseTree node){
        final StringBuilder sb = new StringBuilder();
        
        if(node instanceof ParserRuleContext){
          final ParserRuleContext prc = (ParserRuleContext) node;
          sb.append("[");
          sb.append(prc.start.getStartIndex());
          sb.append(",");
          sb.append(prc.stop.getStopIndex());
          sb.append("] ");
          sb.append(prc.getRuleIndex());          
        }else if(node instanceof TerminalNode){
          final TerminalNode tn = (TerminalNode) node;
          final Token tk = tn.getSymbol();
          sb.append("[");
          sb.append(tk.getStartIndex());
          sb.append(",");
          sb.append(tk.getStopIndex());
          sb.append("] ");
          sb.append(tn.getSymbol().getType());
        }
        sb.append(" : (").append(node.getClass().getSimpleName()).append(") \"").append(node.getText()).append("\"");

        //print childrens
        final int nbChild = node.getChildCount();
        if(nbChild>0){
            sb.append('\n');
            for(int i=0;i<nbChild;i++){
                if(i==nbChild-1){
                    sb.append("\u2514\u2500 ");
                }else{
                    sb.append("\u251C\u2500 ");
                }

                String sc = toString(node.getChild(i));
                String[] parts = sc.split("\n");
                sb.append(parts[0]).append('\n');
                for(int k=1;k<parts.length;k++){
                    if(i==nbChild-1){
                        sb.append(' ');
                    }else{
                        sb.append('\u2502');
                    }
                    sb.append("    ");
                    sb.append(parts[k]);
                    sb.append('\n');
                }
            }
        }

        return sb.toString();
    }
  
  public static Object evaluate(Object candidate){
    if(candidate instanceof OExpression){
      return ((OExpression)candidate).evaluate(null, null);
    }else{
      return candidate;
    }
  }
  
  public static Object convert(Object candidate, OProperty property){
    if(candidate == null) return null;
    candidate = evaluate(candidate);
    if(candidate == null) return null;
    
    final OType type = property.getType();
    for(Class c: type.getJavaTypes()){
      if(c.isInstance(candidate)){
        //type matchs
        return candidate;
      }
    }
    
    switch(property.getType()){
      case BINARY : 
        break;
      case BOOLEAN : 
        break;
      case BYTE : 
        if(candidate instanceof Number){
          candidate = ((Number)candidate).byteValue();
        }
        break;
      case CUSTOM : 
        break;
      case DATE : 
        break;
      case DATETIME : 
        break;
      case DECIMAL : 
        if(candidate instanceof Number){
          candidate = new BigDecimal(((Number)candidate).doubleValue());
        }
        break;
      case DOUBLE : 
        if(candidate instanceof Number){
          candidate = ((Number)candidate).doubleValue();
        }
        break;
      case EMBEDDED : 
        break;
      case EMBEDDEDLIST : 
        break;
      case EMBEDDEDMAP : 
        break;
      case EMBEDDEDSET : 
        break;
      case FLOAT : 
        if(candidate instanceof Number){
          candidate = ((Number)candidate).floatValue();
        }
        break;
      case INTEGER : 
        if(candidate instanceof Number){
          candidate = ((Number)candidate).intValue();
        }
        break;
      case LINK : 
        break;
      case LINKLIST : 
        break;
      case LINKMAP : 
        break;
      case LINKSET : 
        break;
      case LONG : 
        if(candidate instanceof Number){
          candidate = ((Number)candidate).longValue();
        }
        break;
      case SHORT : 
        if(candidate instanceof Number){
          candidate = ((Number)candidate).shortValue();
        }
        break;
      case STRING : 
        candidate = candidate.toString();
        break;
      case TRANSIENT : 
        break;
    }
    return candidate;
  }

  public static <T extends ParserRuleContext> T getCommand(
          final OCommandRequest iRequest, final Class<T> c) throws OCommandSQLParsingException{
    
    final String sql = ((OCommandRequestText) iRequest).getText();
    System.err.println("|||||||||||||||||||| "+ sql);
    final ParseTree tree = compileExpression(sql);
    //System.err.println(toString(tree));
    checkErrorNodes(tree);
    if(!(tree instanceof OSQLParser.CommandContext)){
      throw new OCommandSQLParsingException("Parse error, query is not a valid command.");
    }
    
    final Object commandTree = ((OSQLParser.CommandContext)tree).getChild(0);
    if(c.isInstance(commandTree)){
      return (T) commandTree;
    }else{
      throw new OCommandSQLParsingException("Unexpected command : "+c.getClass() +" was expecting a "+c.getSimpleName());
    }
  }
  
  private static void checkErrorNodes(ParseTree tree) throws OCommandSQLParsingException {
    if(tree instanceof ErrorNode){
      throw new OCommandSQLParsingException("Malformed command at : "+tree.getText());
    }
    for(int i=0,n=tree.getChildCount();i<n;i++){
      checkErrorNodes(tree.getChild(i));
    }
  }
  
  public static OCommandExecutor visit(CommandContext candidate) throws OCommandSQLParsingException {
    
    final OCommandExecutor command;
    final Object commandTree = candidate.getChild(0);
    if(commandTree instanceof OSQLParser.CommandUnknownedContext){
      command = visit((OSQLParser.CommandUnknownedContext)commandTree);
    }else{
      throw new OCommandSQLParsingException("Unknowned command " + candidate.getClass()+" "+candidate);
    }
    
    return command;
  }
    
  private static OCommandCustom visit(CommandUnknownedContext candidate) throws OCommandSQLParsingException {
    //variables
    final List<Object> elements = new ArrayList<Object>();
    
    final int nb = candidate.getChildCount();
    for(int i=0;i<nb;i++){
      final ParseTree child = candidate.getChild(i);
      elements.add(SQLGrammarUtils.visit(child));
    }
    
    return new OCommandCustom(elements);
  }
  
  public static Object visit(ParseTree candidate) throws OCommandSQLParsingException {
    if(candidate instanceof ExpressionContext){
      return visit((ExpressionContext)candidate);
    }else if(candidate instanceof ReferenceContext){
      return visitAsExpression((ReferenceContext) candidate);
    }else if(candidate instanceof CleanreferenceContext){
      return visitAsExpression((CleanreferenceContext) candidate);
    }else if(candidate instanceof ContextVariableContext){
        return visit((ContextVariableContext) candidate);
    }else if(candidate instanceof ExpressionContext){
      return visit((ExpressionContext)candidate);
    }else if(candidate instanceof LiteralContext){
      return visit((LiteralContext)candidate);
    }else if(candidate instanceof FunctionCallContext){
      return visit((FunctionCallContext)candidate);
    }else if(candidate instanceof OridContext){
      return visit((OridContext)candidate);
    }else if(candidate instanceof MapContext){
      return visit((MapContext)candidate);
    }else if(candidate instanceof CollectionContext){
      return visit((CollectionContext)candidate);
    }else if(candidate instanceof UnsetContext){
      return visit((UnsetContext)candidate);
    }else if(candidate instanceof FilterContext){
      return visit((FilterContext)candidate);
    }else if(candidate instanceof CommandContext){
        return visit((CommandContext)candidate);
    }else{
      throw new OCommandSQLParsingException("Unexpected parse tree element :"+candidate.getClass()+" "+candidate);
    }
  }
  
  public static OExpression visit(ExpressionContext candidate) throws OCommandSQLParsingException {
    final int nbChild = candidate.getChildCount();
    
    if(nbChild == 1){
      //can be a word, literal, functionCall, context variable
      if(candidate.OCLASS_ATTR() != null){
        return new OExpressionClass();
      }else if(candidate.ORID_ATTR() != null){
        return new OExpressionORID();
      }else if(candidate.OSIZE_ATTR() != null){
        return new OExpressionSize();
      }else if(candidate.OTHIS() != null){
        return new OExpressionThis();
      }else if(candidate.OTYPE_ATTR() != null){
        return new OExpressionType();
      }else if(candidate.OVERSION_ATTR() != null){
        return new OExpressionVersion();
      }else if(candidate.ORAW_ATTR() != null){
        return new OExpressionRaw();
      }else if(candidate.traverseAll() != null){
        return new OExpression.All();
      }else if(candidate.traverseAny() != null){
        return new OExpression.Any();
      }else if(candidate.sourceQuery()!= null){
        return new OLiteral(visit(candidate.sourceQuery()));
      }
        
      return (OExpression)visit(candidate.getChild(0));
    }else if(nbChild == 2){
      //can be a method call, pathcall
      final OExpression source = (OExpression)visit(candidate.getChild(0));
      final OExpression right = (OExpression)visit(candidate.getChild(1));
      if(right instanceof OSQLMethod){
        ((OSQLMethod)right).getArguments().add(0, source); //add the source as first argument.
      }else if(right instanceof OPath){
        ((OPath)right).getChildren().add(0, source); //add the source as first argument.
      }
      return right;
    }else if(nbChild == 3){
      //can be '(' exp ')'
      //can be exp (+|-|/|*|^) exp
      //can be exp . exp = path
      final ParseTree left = candidate.getChild(0);
      final ParseTree center = candidate.getChild(1);
      final ParseTree right = candidate.getChild(2);
      if(center instanceof TerminalNode){
        //(+|-|/|*|^) exp
        final String operator = center.getText();
        final OExpression leftExp = (OExpression)visit(left);
        final OExpression rightExp = (OExpression)visit(right);
        if("+".equals(operator)){
          return new OOperatorPlus(leftExp, rightExp);
        }else if("-".equals(operator)){
          return new OOperatorMinus(leftExp, rightExp);
        }else if("/".equals(operator)){
          return new OOperatorDivide(leftExp, rightExp);
        }else if("*".equals(operator)){
          return new OOperatorMultiply(leftExp, rightExp);
        }else if("%".equals(operator)){
          return new OOperatorModulo(leftExp, rightExp);
        }else if("^".equals(operator)){
          return new OOperatorPower(leftExp, rightExp);
        }else if(".".equals(operator)){
            if(rightExp instanceof OSQLMethod){
                ((OSQLMethod)rightExp).getArguments().add(0,leftExp);
                rightExp.setAlias(leftExp.getAlias());
                return rightExp;
            }else{
                return new OPath(leftExp, rightExp);
            }
        }else{
          throw new OCommandSQLParsingException("Unexpected operator "+operator);
        }
        
      }else{
        // '(' exp ')'
        return (OExpression)visit(center);
      }
    }else if(nbChild >= 4){
      // exp '[' filter(,filter)* ']'
      final OExpression source = visit((ExpressionContext)candidate.getChild(0));
      final OFiltered filter = new OFiltered(source);
      
      if(candidate.getChild(2) instanceof TerminalNode){
          filter.getChildren().add(new OLiteral(Integer.valueOf(candidate.getChild(2).toString())));
          filter.getChildren().add(new OLiteral(Integer.valueOf(candidate.getChild(4).toString())));
      }else{
        for(Object o : candidate.children){
            if(o instanceof FilterContext){
                filter.getChildren().add(visit((FilterContext)o));
            }
        }
      }
      return filter;
    }else{
      throw new OCommandSQLParsingException("Unexpected number of arguments");
    }
    
  }
  
  public static OName visitAsExpression(ReferenceContext candidate) throws OCommandSQLParsingException {
    return new OName(visitAsString(candidate));
  }
  
  public static String visitAsString(ReferenceContext candidate) throws OCommandSQLParsingException {
    if(candidate.WORD() != null){
      String txt = candidate.WORD().getText();
      if(txt.startsWith("\"") && txt.endsWith("\"")){
        txt = txt.substring(1, txt.length() - 1);
      }
      return txt;
    }else if(candidate.ESCWORD() != null){
      String txt = candidate.ESCWORD().getText();
      txt = txt.substring(1, txt.length() - 1);
      return txt;
    }else{
      String txt = candidate.keywords().getText();
      return txt;
    }
  }

  public static OName visitAsExpression(CleanreferenceContext candidate) throws OCommandSQLParsingException {
        return new OName(visitAsString(candidate));
    }

  public static String visitAsString(CleanreferenceContext candidate) throws OCommandSQLParsingException {
        if(candidate.WORD() != null){
            String txt = candidate.WORD().getText();
            if(txt.startsWith("\"") && txt.endsWith("\"")){
                txt = txt.substring(1, txt.length() - 1);
            }
            return txt;
        }else{
            String txt = candidate.ESCWORD().getText();
            txt = txt.substring(1, txt.length() - 1);
            return txt;
        }
    }

  public static Number visit(NumberContext candidate) throws OCommandSQLParsingException {
    if (candidate.INT() != null) {
      return Integer.valueOf(candidate.getText());
    } else {
      return Double.valueOf(candidate.getText());
    }
  }
  
  public static int visit(NintContext candidate) throws OCommandSQLParsingException {
    return Integer.valueOf(candidate.getText());
  }
  
  public static String visit(CwordContext candidate, final OCommandRequest iRequest) throws OCommandSQLParsingException {
    if(candidate.NULL() != null){
      return null;
    }else{
      //CWord can be anything, spaces count
      String text = ((OCommandRequestText) iRequest).getText();
      return text.substring(candidate.getStart().getStartIndex());
    }
  }

  public static OContextVariable visit(ContextVariableContext candidate) throws OCommandSQLParsingException {
    return new OContextVariable(candidate.WORD().getText());
  }
  
  public static OUnset visit(UnsetContext candidate) throws OCommandSQLParsingException {
    if(candidate.UNSET() == null){
      return new OUnset(visitAsString(candidate.cleanreference()));
    }else{
      return new OUnset();
    }
    
  }
  
  public static OLiteral visit(OridContext candidate) throws OCommandSQLParsingException {
    final ORecordId oid = new ORecordId(candidate.getText());
    return new OLiteral(oid);
  } 
  
  public static OCollection visit(CollectionContext candidate) throws OCommandSQLParsingException {
    final List col = new ArrayList();
    final List<ExpressionContext> values = candidate.expression();
    for (int i = 0, n = values.size(); i < n; i++) {
      col.add(visit(values.get(i)));
    }
    return new OCollection(col);
  }

  public static OMap visit(MapContext candidate) throws OCommandSQLParsingException {
    final LinkedHashMap map = new LinkedHashMap();
    for(MapEntryContext entry : candidate.mapEntry()){
      OExpression key = null;
      if(entry.literal() != null){
        key = visit(entry.literal());
      }else{
        key = new OLiteral(visitAsString(entry.cleanreference()));
      }
      map.put(key, visit(entry.expression()));
    }
    return new OMap(map);
  }
  
  public static OLiteral visit(LiteralContext candidate) throws OCommandSQLParsingException {
    if(candidate.TEXT() != null){
      final String txt = visitText(candidate.TEXT());
      return new OLiteral(txt);
      
    }else if(candidate.number()!= null){
      return new OLiteral(visit(candidate.number()));
      
    }else if(candidate.NULL()!= null){
      return new OLiteral(null);
      
    }else if(candidate.TRUE()!= null){
      return new OLiteral(Boolean.TRUE);
      
    }else if(candidate.FALSE()!= null){
      return new OLiteral(Boolean.FALSE);
      
    }else if(candidate.DATE()!= null){
      final String datetxt = candidate.DATE().getText();
      Date date = null;
      try{
        date = toDate(datetxt);
      }catch(ParseException ex){
        throw new OCommandSQLParsingException("Invalid date format : "+ datetxt);
      }
      return new OLiteral(date);
      
    }else{
      throw new OCommandSQLParsingException("Should not happen");
    }
  }
  
  public static Date toDate(String datetxt) throws ParseException {
    if (datetxt == null) {
      throw new ParseException("Null Date string", -1);
    }

    if (!datetxt.contains("T")) {
      synchronized (DF1) {
        return DF1.parse(datetxt);
      }
    } else {
      if (datetxt.contains("Z")) {
        synchronized (DF2) {
          return DF2.parse(datetxt);
        }
      } else {
        synchronized (DF3) {
          return DF3.parse(datetxt);
        }
      }
    }
  }

  public static String visitText(TerminalNode candidate) {
    String txt = candidate.getText();
    txt = txt.substring(1, txt.length() - 1);
    txt = txt.replaceAll("''", "'");
    return txt;
  }

  public static OExpressionWithChildren visit(FunctionCallContext candidate) throws OCommandSQLParsingException {
    final String name = visitAsString((CleanreferenceContext)candidate.getChild(0));
    final List<OExpression> args = visit( ((ArgumentsContext)candidate.getChild(1)) );
    OExpressionWithChildren fct = null;
    try{
        fct = createFunction(name);
    }catch (OCommandSQLParsingException ex1){
        try{
            fct = createMethod(name);
        }catch (OCommandSQLParsingException ex2){
            throw new OCommandSQLParsingException("No function or method for name : "+name);
        }
    }

    fct.getChildren().addAll(args);
    return fct;
  }

  public static OExpression visit(ProjectionContext candidate) throws OCommandSQLParsingException {
    
    OExpression exp;
    if(candidate.filter() != null){
      exp = visit(candidate.filter());
    }else if(candidate.expression()!= null){
      exp = visit(candidate.expression());
    }else{
      throw new OCommandSQLParsingException("Unknowned command " + candidate.getClass()+" "+candidate);
    }

    if(candidate.alias() != null){
      exp.setAlias(visit(candidate.alias()));
    }

    return exp;
  }

  public static String visit(AliasContext candidate) throws OCommandSQLParsingException{
    if(candidate.literal() != null){
      return String.valueOf(visit(candidate.literal()).getValue());
    }else{
      return visitAsString(candidate.reference());
    }
  }

  public static OExpression visit(FilterContext candidate) throws OCommandSQLParsingException {
    final int nbChild = candidate.getChildCount();
    
    if(candidate.traverse() != null){
        final TraverseContext tc = candidate.traverse();
        final OExpressionTraverse trs = new OExpressionTraverse();
        int i=0;
        
        //parse source
        if(tc.traverseAll() != null){
            trs.setSource(OExpressionTraverse.SOURCE.ALL);
        }else if(tc.traverseAny() != null){
            trs.setSource(OExpressionTraverse.SOURCE.ANY);
        }else{
            final OExpression source = visitAsExpression(tc.cleanreference());
            trs.setSource(source);
        }
        
        //parse depths
        if(tc.nint().size()==1){
            trs.setStartDepth(visit(tc.nint(0)));
            trs.setEndDepth(-1);
        }else{
            trs.setStartDepth(visit(tc.nint(0)));
            trs.setEndDepth(visit(tc.nint(1)));
        }
        
        //parse subfields
        final List<ExpressionContext> refs = tc.expression();
        for(int k=i,n=refs.size();k<n;k++){
            trs.getSubfields().add(visit(refs.get(k)));
        }
        
        //parse condition
        trs.setFilter(visit(tc.filter()));
        
        return trs;
    }
    
    if(nbChild == 1){
      //can be a word, literal, functionCall, expression
      return (OExpression) visit(candidate.getChild(0));
    }else if(nbChild == 2){
      //can be :
      //filter filterAnd
      //filter filterOr
      //expression filterIn
      //expression filterBetween
      //NOT filter
      if(candidate.filterAnd() != null){
        return new OAnd(
                (OExpression)visit(candidate.getChild(0)), 
                (OExpression)visit(candidate.filterAnd().filter()));
      }else if(candidate.filterOr() != null){
        return new OOr(
                (OExpression)visit(candidate.getChild(0)), 
                (OExpression)visit(candidate.filterOr().filter()));
      }else if(candidate.filterIn() != null){
        final OExpression left = (OExpression)visit(candidate.getChild(0));
        final OExpression right;
        if(candidate.filterIn().expression() != null){
          right = (OExpression)visit(candidate.filterIn().expression());
        }else if(candidate.filterIn().sourceQuery()!= null){
          List<OIdentifiable> lst = visit(candidate.filterIn().sourceQuery());
          right = new OLiteral(lst);
        }else{
          throw new OCommandSQLParsingException("Unexpected arguments");
        }
        return new OIn(left,right);
      }else if(candidate.filterBetween()!= null){
        final OExpression target = (OExpression)visit(candidate.getChild(0));
        final OExpression left = (OExpression)visit(candidate.filterBetween().expression(0));
        final OExpression right = (OExpression)visit(candidate.filterBetween().expression(1));
        return new OBetween(target,left,right);
      }else if(candidate.NOT() != null){
        return new ONot(
                (OExpression)visit(candidate.getChild(1)));
      }else{
        throw new OCommandSQLParsingException("Unexpected arguments");
      }
    }else if(nbChild == 3){
      //can be :
      // '(' filter ')'
      //filter COMPARE_X filter
      //filter IS NULL
      //filter IS DEFINED
      //filter LIKE filter
      //filter INSTANCEOF filter
      if(candidate.COMPARE_EQL()!= null){
        return new OEquals(
                (OExpression) visit(candidate.getChild(0)),
                (OExpression) visit(candidate.getChild(2)));
      }else if(candidate.COMPARE_DIF()!= null){
        return new ONotEquals(
                (OExpression) visit(candidate.getChild(0)),
                (OExpression) visit(candidate.getChild(2)));
      }else if(candidate.COMPARE_INF()!= null){
        return new OInferior(
                (OExpression) visit(candidate.getChild(0)),
                (OExpression) visit(candidate.getChild(2)));
      }else if(candidate.COMPARE_INF_EQL()!= null){
        return new OInferiorEquals(
                (OExpression) visit(candidate.getChild(0)),
                (OExpression) visit(candidate.getChild(2)));
      }else if(candidate.COMPARE_SUP()!= null){
        return new OSuperior(
                (OExpression) visit(candidate.getChild(0)),
                (OExpression) visit(candidate.getChild(2)));
      }else if(candidate.COMPARE_SUP_EQL()!= null){
        return new OSuperiorEquals(
                (OExpression) visit(candidate.getChild(0)),
                (OExpression) visit(candidate.getChild(2)));
      }else if(candidate.LIKE()!= null){
        return new OLike(
                (OExpression) visit(candidate.getChild(0)),
                (OExpression) visit(candidate.getChild(2)));
      }else if(candidate.INSTANCEOF()!= null){
        return new OInstanceOf(
                (OExpression) visit(candidate.getChild(0)),
                (OExpression) visit(candidate.getChild(2)));
      }else if(candidate.DEFINED()!= null){
          return new OIsDefined(
                  (OName)visit(candidate.getChild(0)));
      }else if(candidate.IS()!= null){
        return new OIsNull(
              (OExpression) visit(candidate.getChild(0)));
      }else{
        return (OExpression) visit(candidate.getChild(1));
      }
      
    }else if(nbChild == 4){
      //can be :
      //filter IS NOT NULL
      return new OIsNotNull(
              (OExpression) visit(candidate.getChild(0)));
    }else{
      throw new OCommandSQLParsingException("Unexpected number of arguments");
    }
  }
  
  public static List<OExpression> visit(ArgumentsContext candidate) throws OCommandSQLParsingException {
    final int nbChild = candidate.getChildCount();
    final List<OExpression> elements = new ArrayList<OExpression>(nbChild);
    
    if(candidate.MULT() != null){
      elements.add(new OExpressionThis());
      return elements;
    }
    
    for(int i=1;i<nbChild-1;i+=2){
      final ParseTree child = candidate.getChild(i);
      elements.add((OExpression)visit(child));
    }
    return elements;
  }
    
  public static List<OIdentifiable> visit(SourceContext candidate) throws OCommandSQLParsingException {
    List<OIdentifiable> ids = new ArrayList<OIdentifiable>();
    if(candidate.orid() != null){
      //single identifier
      final OLiteral literal = visit(candidate.orid());
      final OIdentifiable id = (OIdentifiable) literal.evaluate(null, null);
      ids.add(id);
      
    }else if(candidate.collection() != null){
      //collection of identifier
      final OCollection col = visit(candidate.collection());
      List lst = (List) col.evaluate(null, null);
      for(Object obj : lst){
        ids.add( ((ORecordId)obj));
      }
      
    }else if(candidate.sourceQuery() != null){
      //sub query
      ids.addAll(visit(candidate.sourceQuery()));
    }
    return ids;
  }

  public static List<OIdentifiable> visit(SourceQueryContext candidate) throws OCommandSQLParsingException {
    if(candidate.commandSelect() != null){
      final List<OIdentifiable> ids = new ArrayList<OIdentifiable>();
      final OCommandSelect sub = new OCommandSelect();
      sub.parse(candidate.commandSelect());
      for(Object obj : sub){
        final ORID cid = ((OIdentifiable)obj).getIdentity();
        if(cid.getClusterId() != Integer.MIN_VALUE){
          ids.add((OIdentifiable)obj);
        }
      }
      return ids;
    }else if(candidate.commandTraverse() != null){
      final List<OIdentifiable> ids = new ArrayList<OIdentifiable>();
      final OCommandTraverse sub = new OCommandTraverse();
      sub.parse(candidate.commandTraverse());
      for(Object obj : sub){
        final ORID cid;
        if(obj instanceof ODocument){
          cid = ((ODocument)obj).getIdentity();
        }else{
          cid = ((ORecordId)obj).getIdentity();
        }
        if(cid.getClusterId()>=0){
          ids.add(cid);
        }
      }
      return ids;
    }else{
      return visit(candidate.sourceQuery());
    }
  }
  
  public static OSQLMethod createMethod(String name) throws OCommandSQLParsingException{
    final Iterator<OSQLMethodFactory> ite = lookupProviderWithOrientClassLoader(OSQLMethodFactory.class, CLASSLOADER);
    while (ite.hasNext()) {
      final OSQLMethodFactory factory = ite.next();
      if (factory.hasMethod(name)) {
        return factory.createMethod(name);
      }
    }
    throw new OCommandSQLParsingException("No method for name : "+name);
  }
  
  public static OSQLFunction createFunction(String name) throws OCommandSQLParsingException{
    final Iterator<OSQLFunctionFactory> ite = lookupProviderWithOrientClassLoader(OSQLFunctionFactory.class, CLASSLOADER);
    while (ite.hasNext()) {
      final OSQLFunctionFactory factory = ite.next();
      if (factory.hasFunction(name)) {
        return factory.createFunction(name);
      }
    }
    throw new OCommandSQLParsingException("No function for name : "+name);
  }

}
