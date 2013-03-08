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

import java.util.List;
import java.util.Collection;
import java.util.TreeSet;

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordAbstract;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.iterator.ORecordIteratorClass;
import com.orientechnologies.orient.core.iterator.ORecordIteratorClusters;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.command.OCommandSelect;
import com.orientechnologies.orient.core.sql.parser.OSQLParser;
import com.orientechnologies.orient.core.sql.parser.SQLGrammarUtils;
import static com.orientechnologies.orient.core.sql.parser.SQLGrammarUtils.*;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class OQuerySource {
  
  protected Iterable<OIdentifiable> targetRecords;
  protected String targetCluster;
  protected String targetClasse;
  protected String targetIndex;
  
  public OQuerySource() {
    super();
  }

  public String getTargetClasse() {
    return targetClasse;
  }

  public void setTargetClasse(String targetClasse) {
    this.targetClasse = targetClasse;
  }
  
  public String getTargetCluster() {
    return targetCluster;
  }

  public void setTargetCluster(String targetCluster) {
    this.targetCluster = targetCluster;
  }
  
  public String getTargetIndex() {
    return targetIndex;
  }

  public void setTargetIndex(String targetIndex) {
    this.targetIndex = targetIndex;
  }
  
  public Iterable<? extends OIdentifiable> getTargetRecords() {
    return targetRecords;
  }
  
  public Iterable<? extends OIdentifiable> createIterator(){
    return createIterator(null, null);
  }
  
  public Iterable<? extends OIdentifiable> createIterator(ORID start, ORID end){
    
    if(targetRecords != null){
      return targetRecords;
      
    }else if(targetClasse != null){
      final ODatabaseRecord db = getDatabase();
      db.checkSecurity(ODatabaseSecurityResources.CLASS, ORole.PERMISSION_READ, targetClasse);
      ORecordIteratorClass ite = new ORecordIteratorClass(db, (ODatabaseRecordAbstract)db, targetClasse, true);
      if(start != null || end != null){
          ite = (ORecordIteratorClass) ite.setRange(start, end);
      }
      
      return ite;
    }else if(targetCluster != null){
      final ODatabaseRecord db = getDatabase();
      final int[] clIds = new int[]{db.getClusterIdByName(targetCluster)};
      ORecordIteratorClusters ite =  new ORecordIteratorClusters<ORecordInternal<?>>(db, db, clIds, false, false).setRange(start, end);
      if(start != null || end != null){
          ite = (ORecordIteratorClass) ite.setRange(start, end);
      }
      return ite;
    }else{
      throw new OException("Source not supported yet");
    }
  }
  
  public void parse(OSQLParser.FromContext from) throws OCommandSQLParsingException {
    parse(from.source());
  }
  
  public void parse(OSQLParser.SourceContext candidate) throws OCommandSQLParsingException {
    
    if(candidate.orid() != null){
      //single identifier
      final OLiteral literal = visit(candidate.orid());
      final OIdentifiable id = (OIdentifiable) literal.evaluate(null, null);
      targetRecords = new TreeSet<OIdentifiable>();
      ((TreeSet<OIdentifiable>) targetRecords).add(id);
      
    }else if(candidate.collection() != null){
      //collection of identifier
      final OCollection col = SQLGrammarUtils.visit(candidate.collection());
      final Collection c = (Collection) col.evaluate(null, null);
      targetRecords = new TreeSet<OIdentifiable>();
      ((TreeSet)targetRecords).addAll(c);
      
    }else if(candidate.commandSelect() != null){
      //sub query
      final OCommandSelect sub = new OCommandSelect();
      sub.parse(candidate.commandSelect());
      targetRecords = sub;
      
    }else if(candidate.CLUSTER() != null){
      //cluster
      targetCluster = visitAsString(candidate.reference());
    }else if(candidate.INDEX()!= null){
      //index
      targetIndex = visitAsString(candidate.reference());
      
    }else if(candidate.DICTIONARY()!= null){
      //dictionnay
      final String key = visitAsString(candidate.reference());
      targetRecords = new TreeSet<OIdentifiable>();
      final OIdentifiable value = ODatabaseRecordThreadLocal.INSTANCE.get().getDictionary().get(key);
      if (value != null) {
        ((List<OIdentifiable>) targetRecords).add(value);
      }

    }else if(candidate.reference()!= null){
      //class
      targetClasse = visitAsString(candidate.reference());
    }else{
      throw new OCommandSQLParsingException("Unexpected source definition.");
    }
    
  }
    
  private ODatabaseRecord getDatabase() {
    return ODatabaseRecordThreadLocal.INSTANCE.get();
  }
    
}
