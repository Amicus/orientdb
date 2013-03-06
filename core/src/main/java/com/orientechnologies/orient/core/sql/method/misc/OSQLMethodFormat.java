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
package com.orientechnologies.orient.core.sql.method.misc;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.sql.method.OSQLMethod;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @author Luca Garulli
 */
public class OSQLMethodFormat extends OSQLMethod {

  public static final String NAME = "format";

  public OSQLMethodFormat() {
    super(NAME, 1);
  }

  @Override
  protected Object evaluateNow(OCommandContext context, Object candidate) {
    Object value = getSource().evaluate(context, candidate);
    final Object v = getMethodArguments().get(0).evaluate(context, candidate);
    if (v != null) {
      if (value instanceof Date) {
        value = new SimpleDateFormat(v.toString()).format(value);
      } else {
        value = value != null ? String.format(v.toString(), value) : null;
      }
    }

    return value;
  }
  
  @Override
  public OSQLMethodFormat copy() {
    final OSQLMethodFormat method = new OSQLMethodFormat();
    method.getArguments().addAll(getArguments());
    method.setAlias(getAlias());
    return method;
  }
}
