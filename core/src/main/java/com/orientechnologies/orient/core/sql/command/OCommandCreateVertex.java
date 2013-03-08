/*
 * Copyright 2010-2012 Luca Garulli (l.garulli--at--orientechnologies.com)
 * Copyright 2013 Geomatys.
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
package com.orientechnologies.orient.core.sql.command;

import java.util.LinkedHashMap;
import java.util.Map;

import com.orientechnologies.orient.core.command.OCommandDistributedReplicateRequest;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandParameters;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.OSQLHelper;
import com.orientechnologies.orient.core.sql.model.OExpression;
import com.orientechnologies.orient.core.sql.parser.OSQLParser;
import static com.orientechnologies.orient.core.sql.parser.SQLGrammarUtils.*;

/**
 * SQL CREATE VERTEX command.
 * 
 * @author Luca Garulli
 * @author Johann Sorel (Geomatys)
 */
public class OCommandCreateVertex extends OCommandAbstract implements OCommandDistributedReplicateRequest{
  public static final String NAME = "CREATE VERTEX";
  private OClass clazz;
  private String clusterName;
  private LinkedHashMap<String, Object> fields;
  
  public OCommandCreateVertex() {
  }

  @Override
  protected OGraphDatabase getDatabase() {
      ODatabaseRecord db = super.getDatabase();
      if (!(db instanceof OGraphDatabase))
      db = new OGraphDatabase((ODatabaseRecordTx) db);
      return (OGraphDatabase) db;
  }
  
  public OCommandCreateVertex parse(final OCommandRequest iRequest) {    
    final ODatabaseRecord database = getDatabase();
    database.checkSecurity(ODatabaseSecurityResources.COMMAND, ORole.PERMISSION_READ);

    final OSQLParser.CommandCreateVertexContext candidate = getCommand(iRequest, OSQLParser.CommandCreateVertexContext.class);
    
    String className = "V";
    final int nbword = candidate.reference().size();
    if(nbword == 1){
      if(candidate.CLUSTER() == null){
        className = visitAsString(candidate.reference(0));
      }else{
        clusterName = visitAsString(candidate.reference(0));
      }
    }else if(nbword == 2){
      className = visitAsString(candidate.reference(0));
      clusterName = visitAsString(candidate.reference(1));
    }
    
    // GET/CHECK CLASS NAME
    clazz = database.getMetadata().getSchema().getClass(className);
    if (clazz == null){
      throw new OCommandSQLParsingException("Class " + className + " was not found");
    }

    //fields
    fields = new LinkedHashMap<String, Object>();
    for(OSQLParser.InsertSetContext ctx : candidate.insertSet()){
      final String propName = visitAsString(ctx.reference());
      final OExpression exp = visit(ctx.expression());
      fields.put(propName, exp.evaluate(null, null));      
    }
    
    return this;
  }

  
  /**
   * Execute the command and return the ODocument object created.
   */
  public Object execute(final Map<Object, Object> iArgs) {
    if (clazz == null)
      throw new OCommandExecutionException("Cannot execute the command because it has not been parsed yet");

    OGraphDatabase database = getDatabase();

    final ODocument vertex = database.createVertex(clazz.getName());

    OSQLHelper.bindParameters(vertex, fields, new OCommandParameters(iArgs));

    if (clusterName != null)
      vertex.save(clusterName);
    else
      vertex.save();

    return vertex;
  }

  @Override
  public String getSyntax() {
    return "CREATE VERTEX [<class>] [CLUSTER <cluster>] [SET <field> = <expression>[,]*]";
  }

}
