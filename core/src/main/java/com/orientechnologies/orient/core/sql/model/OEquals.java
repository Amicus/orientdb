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

import com.orientechnologies.common.collection.OCompositeKey;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class OEquals extends OExpressionWithChildren{
  
  private static final double EPS = 1E-12;

  public OEquals(OExpression left, OExpression right) {
    this(null,left,right);
  }

  public OEquals(String alias, OExpression left, OExpression right) {
    super(alias,left,right);
  }
  
  public OExpression getLeft(){
    return children.get(0);
  }
  
  public OExpression getRight(){
    return children.get(1);
  }
  
  @Override
  protected String thisToString() {
    return "(Equals)";
  }

  @Override
  protected Object evaluateNow(OCommandContext context, Object candidate) {
    return equals(getLeft(), getRight(), context, candidate);
  }

  @Override
  protected void analyzeSearchIndex(OSearchContext searchContext, OSearchResult result) {
    final String className = searchContext.getSource().getTargetClasse();
    if(className == null){
      //no optimisation
      return;
    }
    
    //test is equality match pattern : field = value
    OName fieldName;
    OLiteral literal;
    if(getLeft() instanceof OName && getRight() instanceof OLiteral){
      fieldName = (OName) getLeft();
      literal = (OLiteral) getRight();
    }else if(getLeft() instanceof OLiteral && getRight() instanceof OName){
      fieldName = (OName) getRight();
      literal = (OLiteral) getLeft();
    }else{
      //no optimisation
      return;
    }
    
    //search for an index
    final OClass clazz = getDatabase().getMetadata().getSchema().getClass(className);
    final Set<OIndex<?>> indexes = clazz.getClassInvolvedIndexes(fieldName.getName());
    if(indexes == null || indexes.isEmpty()){
      //no index usable
      return;
    }
    
    for(OIndex index : indexes){
      if(index.getKeyTypes().length != 1){
        continue;
      }
      
      if(OClass.INDEX_TYPE.UNIQUE.toString().equals(index.getType())){
        //found a usable index
        final Collection searchFor = Collections.singleton(literal.getValue());
        final Collection<OIdentifiable> ids = index.getValues(searchFor);
        searchResult.setState(OSearchResult.STATE.FILTER);
        searchResult.setIncluded(ids);
        return;
      }
    }
    
  }

  public static boolean equals(OExpression left, OExpression right, OCommandContext context, Object candidate){
    final Object value1 = left.evaluate(context, candidate);
    final Object value2 = right.evaluate(context, candidate);
    return equals(value1, value2);
  }
  
  public static boolean equals(Object value1, Object value2){
    
    if (value1 == value2) {
      // Includes the (value1 == null && value2 == null) case.
      return true;
    }
    if (value1 == null || value2 == null) {
      // No need to check for (value2 != null) or (value1 != null).
      // If they were null, the previous check would have caugh them.
      return false;
    }
    
    //check if value is wrapped in a document
    if( !(value1 instanceof ODocument) && value2 instanceof ODocument){
      final ODocument d2 = (ODocument) value2;
      final Object[] values = d2.fieldValues();
      if(values.length == 1){
          return equals(value1, values[0]);
      }
  }
    
    if(value1 instanceof ORID && value2 instanceof String){
      //orid check
      try{
        ORID r = new ORecordId((String)value2);
        return value1.equals(r);
      }catch(IllegalArgumentException ex){
        //string is not an id
      }
    }else if(value1 instanceof String && value2 instanceof ORID){
      //orid check
      try{
        ORID r = new ORecordId((String)value1);
        return value2.equals(r);
      }catch(IllegalArgumentException ex){
        //string is not an id
      }
    }
    
    //composite key equality test
    if(value1 instanceof OCompositeKey){
        //try to convert right to Compositekey
        final OCompositeKey lkey = (OCompositeKey) value1;
        OCompositeKey rkey = null;
        if(value2 instanceof Collection){
            rkey = new OCompositeKey( ((Collection)value2).toArray() );
        }else{
            rkey = new OCompositeKey( value2 );
        }
        return lkey.compareTo(rkey) == 0;
    }else if(value2 instanceof OCompositeKey){
        //try to convert left to Compositekey
        final OCompositeKey rkey = (OCompositeKey) value2;
        OCompositeKey lkey = null;
        if(value1 instanceof Collection){
            lkey = new OCompositeKey( ((Collection)value1).toArray() );
        }else{
            lkey = new OCompositeKey( value1 );
        }
        return lkey.compareTo(rkey) == 0;
    }

    //resolving for numbers
    if (value1 instanceof Number && value2 instanceof Number) {
      //test number case
      return numberEqual((Number) value1, (Number) value2);
    } else if (value1.equals(value2)) {
      //test standard equal
      //but classes are not the same, so will have to use the converters
      //to ensure a proper compare
      return true;
    }
        
    return false;
  }
  
  private static boolean numberEqual(final Number value1, final Number value2) {
    final Number n1 = (Number) value1;
    final Number n2 = (Number) value2;

    //compare using less precise format
    if(n1.getClass() == n2.getClass()){
        return n1.equals(n2);
    }else if(n1 instanceof Float || n2 instanceof Double){
        return n1.floatValue() == n2.floatValue();
    }else if(n1 instanceof Double || n2 instanceof Double){
        return n1.doubleValue() == n2.doubleValue();
    }

    return n1.longValue() == n2.longValue();
  }

  @Override
  public Object accept(OExpressionVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return super.equals(obj);
  }
  
  @Override
  public OEquals copy() {
    return new OEquals(alias, getLeft(), getRight());
  }
  
}
