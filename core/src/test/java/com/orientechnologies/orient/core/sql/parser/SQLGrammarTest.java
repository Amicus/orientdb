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

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.sql.model.OExpression;
import com.orientechnologies.orient.core.sql.model.OLiteral;
import com.orientechnologies.orient.core.sql.model.OOperatorPlus;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class SQLGrammarTest {
  
  @Test
  public void oridTest(){ 
    final String str = "13:1";
    final OExpression exp = SQLGrammarUtils.parseExpression(str);
    assertEquals(exp, new OLiteral(new ORecordId("#13:1")));
  }
  
  @Test
  public void mathopTest(){
    final String str = "3+1";
    final OExpression exp = SQLGrammarUtils.parseExpression(str);
    assertEquals(exp, new OOperatorPlus(new OLiteral(3), new OLiteral(1)));
  }
  
}
