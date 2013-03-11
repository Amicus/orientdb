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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class OPath extends OExpressionWithChildren{
  
  public OPath(OExpression left, OExpression right) {
    this(null,left,right);
  }

  public OPath(String alias, OExpression left, OExpression right) {
    super(alias,left,right);
    if(alias == null){
        setAlias(left.getAlias());
    }
  }
  
  public OExpression getLeft(){
    return children.get(0);
  }
  
  public OExpression getRight(){
    return children.get(1);
  }
  
  @Override
  protected String thisToString() {
    return "(path)";
  }

  @Override
  protected Object evaluateNow(OCommandContext context, Object candidate) {
    final Object left = getLeft().evaluate(context, candidate);
    final Object right = getRight().evaluate(context, left);
    return right;
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
  public OPath copy() {
    return new OPath(alias, getLeft(),getRight());
  }
  
  /**
   * Unfold this path.
   * Example : a.b.c.d
   * will return list : [a,b,c,d]
   * @param path
   * @param names
   * @return List<OName>
   */
  public List<OExpression> unfold(){
      final List<OExpression> lst = new ArrayList<OExpression>();
      unfold(this, lst);
      return lst;
  }
  
  private void unfold(OExpression exp, List<OExpression> lst){
      if(exp instanceof OPath){
          unfold(((OPath)exp).getLeft(), lst);
          unfold(((OPath)exp).getRight(), lst);
      }else{
          lst.add(exp);
      }
  }
}
