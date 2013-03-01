/*
 * Copyright 2010-2012 Luca Garulli (l.garulli--at--orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.core.sql.functions;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.sql.model.OSearchContext;
import com.orientechnologies.orient.core.sql.model.OSearchResult;
import com.orientechnologies.orient.core.sql.model.OSortBy;
import java.math.BigDecimal;

/**
 * Abstract class to extend to build Custom SQL Functions. Extend it and register it with:
 * <code>OSQLParser.getInstance().registerStatelessFunction()</code> or
 * <code>OSQLParser.getInstance().registerStatefullFunction()</code> to being used by the SQL engine.
 * 
 * @author Luca Garulli (l.garulli--at--orientechnologies.com)
 * 
 */
public abstract class OSQLFunctionAbstract extends OSQLFunction {
  
  protected final String name;
  protected final int minParams;
  protected final int maxParams;
  
  public OSQLFunctionAbstract(String name){
    this(name, 0);
  }
  
  public OSQLFunctionAbstract(String name, int nbparams) {
    this(name, nbparams, nbparams);
  }

  public OSQLFunctionAbstract(String name, int minparams, int maxparams) {
    this.name = name;
    this.minParams = minparams;
    this.maxParams = maxparams;
    this.alias = name; //use name as default alias
  }
  
  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getMinParams() {
    return minParams;
  }

  @Override
  public int getMaxParams() {
    return maxParams;
  }

  @Override
  protected String thisToString() {
    return "(Function) "+getName();
  }
  
  @Override
  public int compareTo(OSQLFunction o) {
    return this.getName().compareTo(o.getName());
  }
  
  public static  Class<? extends Number> getClassWithMorePrecision(final Class<? extends Number> iClass1,
			final Class<? extends Number> iClass2) {
		if (iClass1 == iClass2)
			return iClass1;

		if (iClass1 == Integer.class
				&& (iClass2 == Long.class || iClass2 == Float.class || iClass2 == Double.class || iClass2 == BigDecimal.class))
			return iClass2;
		else if (iClass1 == Long.class && (iClass2 == Float.class || iClass2 == Double.class || iClass2 == BigDecimal.class))
			return iClass2;
		else if (iClass1 == Float.class && (iClass2 == Double.class || iClass2 == BigDecimal.class))
			return iClass2;

		return iClass1;
	}
  
}
