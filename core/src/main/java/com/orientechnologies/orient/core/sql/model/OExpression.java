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
package com.orientechnologies.orient.core.sql.model;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An Expression is an unresolved operation which result change based
 * on the context and the tested object.
 * Use the evaluate method to obtain it's value.
 * 
 * @author Johann Sorel (Geomatys)
 */
public interface OExpression {
  
  /** Request to skip this record */
  public static final Object POST_ACTION_DISCARD = new Object();
    
  /**
   * Expression which always return TRUE.
   */
  public static final Include INCLUDE = new Include();
  /**
   * Expression which always return FALSE.
   */
  public static final Exclude EXCLUDE = new Exclude();
  
  /**
   * Evaluate the expression.
   * 
   * @param context
   * @param candidate
   * @return Object  can be null
   */
  Object evaluate(OCommandContext context, Object candidate);
  
  /**
   * Get the alias of this expression.
   * Used in projections.
   * @param alias 
   */
  String getAlias();
  
  /**
   * Set the alias of this expression.
   * Used in projections.
   * @param alias 
   */
  void setAlias(String alias);
  
  /**
   * Check if the expression evaluation is affected by document or context.
   * If an expression is static it can be evaluated only once.
   * @return true if expression is document or context sensitive
   */
  boolean isStatic();
  
  /**
   * Check if the expression evaluation is affected by the passed context.
   * @return true if expression is context sensitive
   */
  boolean isContextFree();
  
  /**
   * Check if the expression evaluation is affected by the passed document.
   * @return true if expression is document sensitive
   */
  boolean isDocumentFree();
  
  /**
   * Some expression like : count,avg,sum,... are aggregations
   * The queries interpretation changes is such expressions are in the query.
   * @return true is expression is an aggregation
   */
  boolean isAgregation();
  
  /**
   * Prepare filter taking advantage of indexes when possible.
   * 
   * @param clazz
   * @param sorts
   * @return OIndexResult
   */
  OSearchResult searchIndex(OSearchContext searchContext);
  
  /**
   * Get current OSearchResult.
   * @return OSearchResult
   */
  OSearchResult getSearchResult();
  
  /**
   * Visitor pattern.
   * @param visitor
   * @param data
   * @return 
   */
  Object accept(OExpressionVisitor visitor, Object data);
  
  /**
   * Duplicate this expression
   * @return copy of this expression
   */
  OExpression copy();
  
  public static final class Include extends OExpressionAbstract{

    private Include() {}
    
    @Override
    protected String thisToString() {
      return "INCLUDE";
    }

    @Override
    protected  Object evaluateNow(OCommandContext context, Object candidate) {
      return Boolean.TRUE;
    }

    @Override
    public boolean isContextFree() {
      return true;
    }

    @Override
    public boolean isDocumentFree() {
      return true;
    }

    @Override
    public OSearchResult searchIndex(OSearchContext searchContext) {
      final OSearchResult res = new OSearchResult(this);
      res.setState(OSearchResult.STATE.FILTER);
      res.setIncluded(OSearchResult.ALL);
      return res;
    }

    @Override
    public Object accept(OExpressionVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    @Override
    public OExpression copy() {
      //immutable
      return this;
    }
    
  }
  
  public static final class Exclude extends OExpressionAbstract{

    private Exclude() {}
    
    @Override
    protected String thisToString() {
      return "EXCLUDE";
    }

    @Override
    protected Object evaluateNow(OCommandContext context, Object candidate) {
      return Boolean.FALSE;
    }

    @Override
    public boolean isContextFree() {
      return true;
    }

    @Override
    public boolean isDocumentFree() {
      return true;
    }

    @Override
    public OSearchResult searchIndex(OSearchContext searchContext) {
      final OSearchResult res = new OSearchResult(this);
      res.setState(OSearchResult.STATE.FILTER);
      res.setExcluded(OSearchResult.ALL);
      return res;
    }

    @Override
    public Object accept(OExpressionVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    @Override
    public OExpression copy() {
      //immutable
      return this;
    }
    
  }
  
  /**
   * Expression which return a collection of all properties is the document.
   * Warning, this expression evaluates differently based on the related expressions.
   */
  public static final class Any extends OExpressionAbstract{

    public Any() {}
    
    @Override
    protected String thisToString() {
      return "ANY";
    }

    @Override
    protected Object evaluateNow(OCommandContext context, Object candidate) {
      if(candidate instanceof OIdentifiable){
        final ODocument doc = (ODocument) ((OIdentifiable)candidate).getRecord();
        final Object[] values = doc.fieldValues();
        final List<Object> col = new ArrayList<Object>();
        for(Object o : values){
            col.add(o);
        }
        return col;
      }else if(candidate instanceof Map){
        return ((Map)candidate).values();
      }
    return candidate;
    }

    @Override
    public boolean isContextFree() {
      return true;
    }

    @Override
    public boolean isDocumentFree() {
      return false;
    }

    @Override
    public OSearchResult searchIndex(OSearchContext searchContext) {
      final OSearchResult res = new OSearchResult(this);
      return res;
    }

    @Override
    public Object accept(OExpressionVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    @Override
    public OExpression copy() {
      //immutable
      return this;
    }
    
  }
    
  /**
   * Expression which return a collection of all properties is the document.
   * Warning, this expression evaluates differently based on the related expressions.
   */
  public static final class All extends OExpressionAbstract{

    public All() {}
    
    @Override
    protected String thisToString() {
      return "ALL";
    }

    @Override
    protected Object evaluateNow(OCommandContext context, Object candidate) {
      if(candidate instanceof OIdentifiable){
        final ODocument doc = (ODocument) ((OIdentifiable)candidate).getRecord();
        final Object[] values = doc.fieldValues();
        final List<Object> col = new ArrayList<Object>();
        for(Object o : values){
            col.add(o);
        }
        return col;
      }else if(candidate instanceof Map){
        return ((Map)candidate).values();
      }
    return candidate;
    }

    @Override
    public boolean isContextFree() {
      return true;
    }

    @Override
    public boolean isDocumentFree() {
      return false;
    }

    @Override
    public OSearchResult searchIndex(OSearchContext searchContext) {
      final OSearchResult res = new OSearchResult(this);
      return res;
    }

    @Override
    public Object accept(OExpressionVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    @Override
    public OExpression copy() {
      //immutable
      return this;
    }
    
  }
  
}
