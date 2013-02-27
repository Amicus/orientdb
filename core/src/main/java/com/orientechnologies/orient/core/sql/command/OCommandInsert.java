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
package com.orientechnologies.orient.core.sql.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.command.OCommandDistributedConditionalReplicateRequest;
import com.orientechnologies.orient.core.command.OCommandExecutor;
import com.orientechnologies.orient.core.command.OCommandListener;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLSetAware;
import com.orientechnologies.orient.core.sql.OCommandParameters;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.model.OExpression;
import com.orientechnologies.orient.core.sql.model.OLiteral;
import com.orientechnologies.orient.core.sql.model.OName;
import com.orientechnologies.orient.core.sql.model.OUnset;
import com.orientechnologies.orient.core.sql.parser.OSQLParser;
import com.orientechnologies.orient.core.sql.parser.SQLGrammarUtils;
import com.orientechnologies.orient.core.sql.parser.OUnknownResolverVisitor;
import static com.orientechnologies.orient.core.sql.parser.SQLGrammarUtils.*;

/**
 * SQL INSERT command.
 * 
 * @author Luca Garulli
 * @author Johann Sorel (Geomatys)
 */
public class OCommandInsert extends OCommandExecutorSQLSetAware implements
    OCommandDistributedConditionalReplicateRequest, OCommandListener {
  
  public static final String KEYWORD_INSERT = "INSERT";

  private String target;
  private String[] fields;
  private final Map<String,OProperty> fieldProperties = new HashMap<String, OProperty>();
  private List<Map<String,Object>> newRecords;
  private OSQLParser.CommandSelectContext source;
  private final List<ODocument> results = new ArrayList<ODocument>();
  
  private String className = null;
  private String clusterName = null;
  private String indexName = null;

  public OCommandInsert(){}
  
  public OCommandInsert(String target, String cluster, List<String> fields, List<Map<String,Object>> records) {
    this.target = target;
    this.fields = fields.toArray(new String[fields.size()]);
    this.newRecords = records;
    this.clusterName = cluster;
  }

  @Override
  public <RET extends OCommandExecutor> RET parse(OCommandRequest iRequest) {
    
    final OSQLParser.CommandInsertContext candidate = getCommand(iRequest, OSQLParser.CommandInsertContext.class);
    visit(candidate);
    
    final ODatabaseRecord database = getDatabase();
    database.checkSecurity(ODatabaseSecurityResources.COMMAND, ORole.PERMISSION_READ);
    
    if (target.startsWith(CLUSTER_PREFIX)) {
      // CLUSTER
      clusterName = target.substring(CLUSTER_PREFIX.length());

    } else if (target.startsWith(INDEX_PREFIX)) {
      // INDEX
      indexName = target.substring(INDEX_PREFIX.length());

    } else {
      // CLASS
      if (target.startsWith(CLASS_PREFIX)) {
        target = target.substring(CLASS_PREFIX.length());
      }

      final OClass cls = database.getMetadata().getSchema().getClass(target);
      if (cls == null) {
        throw new OException("Class " + target + " not found in database");
      }else{
        fieldProperties.clear();
        for(int i=0;i<fields.length;i++){
          final OProperty prop = cls.getProperty(fields[i]);
          if(prop != null){
            fieldProperties.put(fields[i], prop);
          }
        }
      }

      className = cls.getName();
    }

    return (RET)this;
  }

  public String getTarget() {
    return target;
  }

  public String[] getFields() {
    return fields;
  }

  public List<Map<String,Object>> getRecords() {
    return newRecords;
  }
  
  @Override
  public Object execute(final Map<Object, Object> iArgs) {
    if(iArgs != null && !iArgs.isEmpty()){
      //we need to set value where we have OUnknowned
      final OUnknownResolverVisitor visitor = new OUnknownResolverVisitor(iArgs);
      for(Map<String,Object> newRecord : newRecords){
        for(Entry<String,Object> entry : newRecord.entrySet()){
          if(entry.getValue() instanceof OUnset){
            newRecord.put(entry.getKey(), visitor.visit((OUnset)entry.getValue(), null));
          }
        }
      }
    }
      
    if(source != null){
      final OCommandSelect select = new OCommandSelect();
      select.parse(source);
      select.addListener(this);
      select.execute(iArgs);
    }
    
    final OCommandParameters commandParameters = new OCommandParameters(iArgs);
    if (indexName != null) {
      final OIndex<?> index = getDatabase().getMetadata().getIndexManager().getIndex(indexName);
      if (index == null)
        throw new OCommandExecutionException("Target index '" + indexName + "' not found");

      // BIND VALUES
      Map<String,Object> record = new HashMap<String, Object>();
      Map<String, Object> result = null;
      for (Map<String,Object> newRecord : newRecords) {
        record.clear();
        for(Entry<String,Object> entry : newRecord.entrySet()){
          record.put(entry.getKey(), evaluate(entry.getValue()) );
        }
        index.put(getIndexKeyValue(commandParameters, record), getIndexValue(commandParameters, record));
        result = record;
      }

      // RETURN LAST ENTRY
      return new ODocument(result);
    } else {

      // CREATE NEW DOCUMENTS
      for (Map<String,Object> newRecord : newRecords) {
        final ODocument doc = className != null ? new ODocument(className) : new ODocument();
        int i=0;
        for(Entry<String,Object> entry : newRecord.entrySet()){
          Object value = evaluate(entry.getValue());
          final OProperty prop = fieldProperties.get(entry.getKey());
          if(prop != null){
            doc.field(entry.getKey(), value, prop.getType());
          }else{
            doc.field(entry.getKey(), value);
          }
          i++;
        }

        if (clusterName != null) {
          doc.save(clusterName);
        } else {
          doc.save();
        }
        results.add(doc);
      }

      if (results.size() == 1) {
        return results.get(0);
      } else {
        return results;
      }
    }
  }
  
  private Object getIndexKeyValue(OCommandParameters commandParameters, Map<String, Object> candidate) {
    final Object parsedKey = candidate.get(KEYWORD_KEY);
    return parsedKey;
  }

  private OIdentifiable getIndexValue(OCommandParameters commandParameters, Map<String, Object> candidate) {
    final Object parsedRid = candidate.get(KEYWORD_RID);
    return (OIdentifiable) parsedRid;
  }

  public boolean isReplicated() {
    return indexName != null;
  }

  @Override
  public String getSyntax() {
    return "INSERT INTO [class:]<class>|cluster:<cluster>|index:<index> [(<field>[,]*) VALUES (<expression>[,]*)[,]*]|[SET <field> = <expression>|<sub-command>[,]*]";
  }
  
  // GRAMMAR PARSING ///////////////////////////////////////////////////////////
  
    
  private void visit(OSQLParser.CommandInsertContext candidate) throws OCommandSQLParsingException{    
    //variables
    String target;
    final List<String> fields = new ArrayList<String>();
    
    //parsing
    target = visitAsString(candidate.reference());
    if(candidate.CLUSTER() != null){
      target = "CLUSTER:"+target;
    }else  if(candidate.INDEX()!= null){
      target = "INDEX:"+target;
    }
    
    String cluster = null;
    if(candidate.insertCluster() != null){
      cluster = visitAsString(candidate.insertCluster().reference());
    }
    
    if(candidate.VALUES() != null){
      //entries or sub query
      for(OSQLParser.ReferenceContext wc : candidate.insertFields().reference()){
        fields.add(visitAsString(wc));
      }
      this.fields = fields.toArray(new String[0]);
      visit(candidate.insertSource());
    }else{
      //SET operations
      final Map<String,Object> values = new HashMap<String,Object>();
      for(OSQLParser.InsertSetContext entry : candidate.insertSet()){
        final String att = visitAsString(entry.reference());
        fields.add(att);
        final OSQLParser.ExpressionContext exp = entry.expression();
        values.put(att,SQLGrammarUtils.visit(exp));
      }
      this.fields = fields.toArray(new String[0]);
      final List<Map<String,Object>> entries = new ArrayList<Map<String,Object>>();
      entries.add(values);
      this.newRecords = entries;
    }
    
    this.target = target;
    this.clusterName = cluster;
  }
  
  private void visit(OSQLParser.InsertSourceContext candidate) throws OCommandSQLParsingException{  
    if(candidate.insertSource() != null){
      visit(candidate.insertSource());
      return;
    }
    
    final List<Map<String,Object>> entries = new ArrayList<Map<String,Object>>();
    if(candidate.commandSelect() != null){
      this.source = candidate.commandSelect();
      //executed later
    }else{
      //entry serie
      for(OSQLParser.InsertEntryContext entry : candidate.insertEntry()){
        final List<OSQLParser.ExpressionContext> exps = entry.expression();
        final Map<String,Object> values = new HashMap<String, Object>();
        for(int i=0;i<exps.size();i++){
          OExpression exp = SQLGrammarUtils.visit(exps.get(i));
          if(exp instanceof OName){
              //we are tolerant here, this literal was escaped with " whre it should have been '.
              exp = new OLiteral(((OName)exp).getName());
          }
          values.put(fields[i], exp);
        }
        entries.add(values);
      }
    }
    this.newRecords = entries;
  }
  
  
  //////////////////////////////////////////////////////////////////////////////
  //Sub select events //////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////
  
  @Override
  public boolean result(Object iRecord) {
    final ODocument record = (ODocument) ((OIdentifiable) iRecord).getRecord();
    final OCommandContext ctx = getContext();
    
    final ODocument doc = className != null ? new ODocument(className) : new ODocument();
    for(String str : record.fieldNames()){
      doc.field(str, doc.field(str));
    }
    
    if (clusterName != null) {
      doc.save(clusterName);
    } else {
      doc.save();
    }
    results.add(doc);
    return true;
  }

  @Override
  public void end() {
  }

  @Override
  public void onBegin(Object iTask, long iTotal) {
  }

  @Override
  public boolean onProgress(Object iTask, long iCounter, float iPercent) {
    return true;
  }

  @Override
  public void onCompletition(Object iTask, boolean iSucceed) {
  }
  
}
