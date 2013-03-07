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
import com.orientechnologies.orient.core.query.OQueryHelper;
import java.util.Collection;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class OLike extends OExpressionWithChildren{
  
  public OLike(OExpression left, OExpression right) {
    this(null,left,right);
  }

  public OLike(String alias, OExpression left, OExpression right) {
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
    return "(Like)";
  }

  @Override
  protected Object evaluateNow(OCommandContext context, Object candidate) {
    return equals(getLeft(), getRight(), context, candidate);
  }

  static boolean equals(OExpression left, OExpression right, OCommandContext context, Object candidate){
    final Object value1 = left.evaluate(context, candidate);
    final Object value2 = right.evaluate(context, candidate);
    
    if(value1 == null || value2 == null){
      return false;
    }
    
    if(value1 instanceof Collection && left instanceof Any){
        for(Object o : ((Collection)value1) ){
            if(o instanceof String && OQueryHelper.like((String)o, String.valueOf(value2))){
                return true;
            }
        }
        return false;
    }else if(value1 instanceof Collection && left instanceof All){
        for(Object o : ((Collection)value1) ){
            if(o instanceof String && !OQueryHelper.like((String)o, String.valueOf(value2))){
                return false;
            }
        }
        return true;
    }
    
    return value1 instanceof String && OQueryHelper.like((String)value1, String.valueOf(value2));    
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
  public OLike copy() {
    return new OLike(alias, getLeft(), getRight());
  }
  
}
