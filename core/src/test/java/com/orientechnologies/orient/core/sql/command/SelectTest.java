/*
 * Copyright 2013 Orient Technologies.
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

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import java.io.File;
import java.io.IOException;
import java.util.*;

import org.testng.annotations.Test;

import static org.testng.Assert.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Test SQL Select command.
 *
 * @author Johann Sorel (Geomatys)
 */
@Test
public class SelectTest {

  private static File folder;
  static {
    try {
      folder = File.createTempFile("orientdb", "");
      folder.delete();
      folder.mkdirs();
    } catch (IOException ex) {
    }
  }

  private static final Date BASE_DATE = new Date(1000);

  private ODatabaseDocumentTx db;
  
  @BeforeMethod
  public void initMethod(){
    db = new ODatabaseDocumentTx("local:"+folder.getPath());
    db = db.open("admin", "admin");
  }
    
  @AfterMethod
  public void endMethod(){
    if(db != null){
      db.close();
    }
  }
  
  public SelectTest(){
    ODatabaseDocumentTx db = new ODatabaseDocumentTx("local:"+folder.getPath());
    db = db.create();
    
    final OSchema schema = db.getMetadata().getSchema();
    
    //car test type
    final OClass carClass = schema.createClass("Car");
    carClass.createProperty("name", OType.STRING);
    carClass.createProperty("size", OType.DOUBLE);    
    final ODocument car1 = db.newInstance(carClass.getName());
    car1.field("name","tempo");
    car1.field("size",250);
    car1.save();    
    final ODocument car2 = db.newInstance(carClass.getName());
    car2.field("name","fiesta");
    car2.field("size",160);
    car2.save();    
    final ODocument car3 = db.newInstance(carClass.getName());
    car3.field("size",260);
    car3.save();    
    final ODocument car4 = db.newInstance(carClass.getName());
    car4.field("name","supreme");
    car4.field("size",310);
    car4.save();
    
    //person test type
    final OClass personClass = schema.createClass("person");
    personClass.createProperty("name", OType.STRING);    
    personClass.createProperty("size", OType.FLOAT);
    personClass.createProperty("weight", OType.DOUBLE);
    personClass.createProperty("points", OType.INTEGER);
    personClass.createProperty("birthday", OType.DATETIME);
    personClass.createProperty("alive", OType.BOOLEAN);
    final ODocument person1 = db.newInstance(personClass.getName());
    person1.field("name","chief");
    person1.field("size",1.8);
    person1.field("weight",60);
    person1.field("points",100);
    person1.field("birthday",BASE_DATE);
    person1.field("alive",false);
    person1.save();    
    final ODocument person2 = db.newInstance(personClass.getName());
    person2.field("name","joe");
    person2.field("size",1.3);
    person2.field("weight",52);
    person2.field("points",80);
    person2.field("birthday",new Date(BASE_DATE.getTime()+100));
    person2.field("alive",false);
    person2.save();
    final ODocument person3 = db.newInstance(personClass.getName());
    person3.field("name","mary");
    person3.field("size",1.7);
    person3.field("weight",34.5);
    person3.field("points",100);
    person3.field("birthday",new Date(BASE_DATE.getTime()+50));
    person3.field("alive",false);
    person3.save();    
    final ODocument person4 = db.newInstance(personClass.getName());
    person4.field("name","alex");
    person4.field("size",2.1);
    person4.field("weight",52);
    person4.field("points",100);
    person4.field("birthday",new Date(BASE_DATE.getTime()-100));
    person4.field("alive",true);
    person4.save();    
    final ODocument person5 = db.newInstance(personClass.getName());
    person5.field("name","suzan");
    person5.field("size",1.55);
    person5.field("weight",52);
    person5.field("points",80);
    person5.field("birthday",new Date(BASE_DATE.getTime()-50));
    person5.field("alive",false);
    person5.save();
    
    //complex test type
    final OClass fishClass = schema.createClass("fish");
    fishClass.createProperty("name", OType.STRING);
    final OClass boatClass = schema.createClass("boat");
    boatClass.createProperty("name", OType.STRING);
    boatClass.createProperty("freight", OType.EMBEDDED,fishClass);
    final OClass dockClass = schema.createClass("dock");
    dockClass.createProperty("name", OType.STRING);
    dockClass.createProperty("capacity", OType.DOUBLE);
    dockClass.createProperty("boat", OType.EMBEDDED,boatClass);
    final OClass seaClass = schema.createClass("sea");
    seaClass.createProperty("name", OType.STRING);
    seaClass.createProperty("navigator", OType.EMBEDDED,boatClass);
    seaClass.createProperty("docks", OType.EMBEDDEDLIST,dockClass);
    seaClass.createProperty("docks2", OType.EMBEDDEDLIST,dockClass);
    
    final ODocument fish = db.newInstance(fishClass.getName());
    fish.field("name","thon");
    final ODocument boat = db.newInstance(boatClass.getName());
    boat.field("name","kiki");
    boat.field("freight",fish);
    final ODocument dock1 = db.newInstance(dockClass.getName());
    dock1.field("name","brest");
    dock1.field("capacity",120);
    dock1.field("boat",boat);
    final ODocument dock2 = db.newInstance(dockClass.getName());
    dock2.field("name","alger");
    dock2.field("capacity",90);
    final ODocument dock3 = db.newInstance(dockClass.getName());
    dock3.field("name","palerme");
    dock3.field("capacity",140);
    
    final ODocument sea = db.newInstance(seaClass.getName());
    sea.field("name","atlantic");
    sea.field("navigator",boat);
    sea.field("docks", Arrays.asList(dock1,dock2,dock3));
    sea.field("docks2", Arrays.asList(dock1));
    sea.save();
    
    db.close();
  }
  
  @Test
  public void selectAll1(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 4 );
    assertEquals(docs.get(0).fieldNames().length, 2);
  }
  
  @Test
  public void selectAll2(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT * FROM car");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 4 );
    assertEquals(docs.get(0).fieldNames().length, 2);
  }
  
  @Test
  public void selectField(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT name FROM car");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 4);
    assertEquals(docs.get(0).fieldNames().length, 1);
    assertEquals(docs.get(0).field("name"), "tempo");
    assertEquals(docs.get(1).field("name"), "fiesta");
    assertEquals(docs.get(2).field("name"), null);
    assertEquals(docs.get(3).field("name"), "supreme");
  }
  
  @Test
  public void selectAlias(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT name AS brand FROM car");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 4);
    assertEquals(docs.get(0).fieldNames().length, 1);
    assertEquals(docs.get(0).field("brand"), "tempo");
    assertEquals(docs.get(1).field("brand"), "fiesta");
    assertEquals(docs.get(2).field("brand"), null);
    assertEquals(docs.get(3).field("brand"), "supreme");
  }
  
  @Test
  public void selectLimit(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car LIMIT 2");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 2);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("name"), "tempo");
    assertEquals(docs.get(1).field("name"), "fiesta");
  }
  
  @Test
  public void selectAllPaging(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car LIMIT 1 BY PAGE");
    final List<ODocument> docs = new ArrayList<ODocument>();
    for (List<ODocument> resultset = db.query(query); !resultset.isEmpty(); resultset = db.query(query)) {
      docs.addAll(resultset);
    }
    assertEquals(docs.size(), 4 );
    assertEquals(docs.get(0).fieldNames().length, 2);
  }
  
  @Test
  public void selectSkip(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car SKIP 1 LIMIT 1");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("name"), "fiesta");
  }

  @Test
  public void selectSkip2(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car LIMIT 1 SKIP 1");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("name"), "fiesta");
  }
  
  @Test
  public void selectMethod(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT size.asInteger().append(' length') as append, name.charAt(2) AS letter FROM car");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 4);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("letter"), "m");
    assertEquals(docs.get(1).field("letter"), "e");
    assertEquals(docs.get(2).field("letter"), null);
    assertEquals(docs.get(3).field("letter"), "p");
    assertEquals(docs.get(0).field("append"), "250 length");
    assertEquals(docs.get(1).field("append"), "160 length");
    assertEquals(docs.get(2).field("append"), "260 length");
    assertEquals(docs.get(3).field("append"), "310 length");
  }
  
  @Test
  public void selectCount(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT count(*) AS nb FROM car");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 1);
    assertEquals(docs.get(0).field("nb"), 4l);
  }

  @Test
  public void selectOnIndex(){
    final OCommandSQL cmd = new OCommandSQL("CREATE INDEX Car.name UNIQUE");
    db.command(cmd).execute();

    final OSQLSynchQuery query = new OSQLSynchQuery("select key, rid from index:Car.name");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 3);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("key"), "fiesta");
    assertEquals(docs.get(1).field("key"), "supreme");
    assertEquals(docs.get(2).field("key"), "tempo");
  }

  @Test
  public void selectCustom1(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT name FROM person WHERE alive SKIP 0 LIMIT 1000");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 1);
    assertEquals(docs.get(0).field("name"), "alex");
  }
  
  @Test
  public void selectCustom2(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM sea WHERE docks[@class ='dock'][boat.freight.name] = 'thon'");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
  }
  
  @Test
  public void selectFlatten(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT flatten(docks2) FROM sea");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertTrue(docs.get(0).getClassName().equals("dock"));
  }
  
  @Test
  public void selectTranverse(){    
    OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM sea WHERE any() traverse(0,-1) (name = 'palerme')");
    List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 4);
    
    query = new OSQLSynchQuery("SELECT FROM sea WHERE any() traverse(0,-1) (name = 'notfound')");
    docs = db.query(query);
    assertEquals(docs.size(), 0);
  }
  
  @Test
  public void selectReflectionExpression(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT @rid as id, @this as doc, @size as size, @class as class FROM car WHERE name = 'fiesta' ");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 4);
    assertEquals(docs.get(0).field("id"), "#8:1");
    assertTrue(docs.get(0).field("doc") instanceof ODocument);
    assertEquals(docs.get(0).field("size"), 29);
    assertEquals(docs.get(0).field("class"), "Car");
  }
  
  @Test
  public void selectUsingReflectionExpression(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car WHERE @rid IN [#8:1] ");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).field("name"), "fiesta");
    assertEquals(docs.get(0).field("size"), 160d);
  }

  @Test
  public void selectUsingReflectionExpression2(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car WHERE @rid in 8:1 ");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).field("name"), "fiesta");
    assertEquals(docs.get(0).field("size"), 160d);
  }

  @Test
  public void selectFromOrid1(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM #8:1");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).field("name"), "fiesta");
    assertEquals(docs.get(0).field("size"), 160d);
  }

  @Test
  public void selectFromOrid2(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM 8:1");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).field("name"), "fiesta");
    assertEquals(docs.get(0).field("size"), 160d);
  }

  @Test
  public void selectFromOrid3(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM [#8:1]");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).field("name"), "fiesta");
    assertEquals(docs.get(0).field("size"), 160d);
  }
  
  @Test
  public void selectWrongCount(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT name,count(*) AS nb FROM car");
    try{
      final List<ODocument> docs = db.query(query);
      fail("should have raised a OCommandSQLParsingException");
    }catch(OCommandSQLParsingException ex){
      //ok
    }
  }
  
  @Test
  public void selectSubFilter(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT docks[name='alger'].capacity as capa from sea");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).field("capa"), 90d);
    
  }
  
  @Test
  public void selectPath(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT navigator.name AS boatname, navigator.freight.name AS freighttype FROM sea");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("boatname"), "kiki");
    assertEquals(docs.get(0).field("freighttype"), "thon");
  }
  
  @Test
  public void selectOperator(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT size+5, size-5, 2*size, size/2, size%15, size^2 FROM car WHERE name = 'fiesta' ");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 6);
    assertEquals(docs.get(0).field("0"), 165d);
    assertEquals(docs.get(0).field("1"), 155d);
    assertEquals(docs.get(0).field("2"), 320d);
    assertEquals(docs.get(0).field("3"), 80d);
    assertEquals(docs.get(0).field("4"), 10d);
    assertEquals(docs.get(0).field("5"), 25600d);
  }
  
  @Test
  public void selectOperatorPrecedence(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT size+5*4, size*2-5, 2*size+10, size/2*4 FROM car WHERE name = 'fiesta' ");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 4);
    assertEquals(docs.get(0).field("0"), 180d);
    assertEquals(docs.get(0).field("1"), 315d);
    assertEquals(docs.get(0).field("2"), 330d);
    assertEquals(docs.get(0).field("3"), 320d);
  }
  
  @Test
  public void selectFromSubQuery(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM (SELECT FROM car SKIP 1 LIMIT 1)");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("name"), "fiesta");
  }
  
  @Test
  public void selectFromSingleId(){    
    OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car SKIP 1 LIMIT 1");
    List<ODocument> docs = db.query(query);
    String id = docs.get(0).getIdentity().toString();
    
    query = new OSQLSynchQuery("SELECT FROM "+id);
    docs = db.query(query);    
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("name"), "fiesta");
  }
  
  @Test
  public void selectFromMultipleIds(){    
    OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car SKIP 1");
    List<ODocument> docs = db.query(query);
    String id1 = docs.get(0).getIdentity().toString();
    String id2 = docs.get(1).getIdentity().toString();
    String id3 = docs.get(2).getIdentity().toString();
    
    query = new OSQLSynchQuery("SELECT FROM ["+id1+","+id2+","+id3+"]");
    docs = db.query(query);    
    assertEquals(docs.size(), 3);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("name"), "fiesta");
    assertEquals(docs.get(1).field("name"), null);
    assertEquals(docs.get(2).field("name"), "supreme");
  }
  
  @Test
  public void selectWhereBasic(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car WHERE size > 200");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 3);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("name"), "tempo");
    assertEquals(docs.get(1).field("name"), null);
    assertEquals(docs.get(2).field("name"), "supreme");
  }

  @Test
  public void selectWhereDate(){
    final Date ref = new Date(BASE_DATE.getTime()+20);
    OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM person WHERE birthday > ?");
    List<ODocument> docs = db.query(query, ref);
    assertEquals(docs.size(), 2);

    query = new OSQLSynchQuery("SELECT FROM person WHERE birthday > ?");
    docs = db.query(query, ref.getTime());
    assertEquals(docs.size(), 2);
  }

  @Test
  public void selectWhereComplex(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car WHERE name IS NOT NULL AND (size < 200 OR name = 'tempo')");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 2);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("name"), "tempo");
    assertEquals(docs.get(1).field("name"), "fiesta");
  }
  
  @Test
  public void selectWhereInOne(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car WHERE name IN 'fiesta'");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("name"), "fiesta");
  }

  @Test
  public void selectWhereDoubleFloatCompare(){
    final OSQLSynchQuery query = new OSQLSynchQuery("select * from person where size = 1.3");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).field("name"), "joe");
  }

  @Test
  public void selectWhereInCollection(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car WHERE name IN ['tempo','fiesta']");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 2);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("name"), "tempo");
    assertEquals(docs.get(1).field("name"), "fiesta");
  }
  
  @Test
  public void selectWhereBetween(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car WHERE size BETWEEN 150 AND 200");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).fieldNames().length, 2);
    assertEquals(docs.get(0).field("name"), "fiesta");
  }
  
  @Test
  public void selectOrderBy(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM person ORDER BY size");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 5);
    assertEquals(docs.get(0).field("name"), "joe");
    assertEquals(docs.get(1).field("name"), "suzan");
    assertEquals(docs.get(2).field("name"), "mary");
    assertEquals(docs.get(3).field("name"), "chief");
    assertEquals(docs.get(4).field("name"), "alex");
  }
  
  @Test
  public void selectOrderByASC(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM person ORDER BY size ASC");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 5);
    assertEquals(docs.get(0).field("name"), "joe");
    assertEquals(docs.get(1).field("name"), "suzan");
    assertEquals(docs.get(2).field("name"), "mary");
    assertEquals(docs.get(3).field("name"), "chief");
    assertEquals(docs.get(4).field("name"), "alex");
  }
  
  @Test
  public void selectOrderByDESC(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM person ORDER BY size DESC");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 5);
    assertEquals(docs.get(0).field("name"), "alex");
    assertEquals(docs.get(1).field("name"), "chief");
    assertEquals(docs.get(2).field("name"), "mary");
    assertEquals(docs.get(3).field("name"), "suzan");
    assertEquals(docs.get(4).field("name"), "joe");
  }
  
  @Test
  public void selectOrderByMultiple(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM person ORDER BY weight ASC, size DESC ");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 5);
    assertEquals(docs.get(0).field("name"), "mary");
    assertEquals(docs.get(1).field("name"), "alex");
    assertEquals(docs.get(2).field("name"), "suzan");
    assertEquals(docs.get(3).field("name"), "joe");
    assertEquals(docs.get(4).field("name"), "chief");
  }
  
  @Test
  public void selectGroupBy(){
    final OSQLSynchQuery query = new OSQLSynchQuery(
            "SELECT weight AS w, count(name) AS nb, min(size) AS min, sum(points) AS sum "
            + "FROM person "
            + "GROUP BY weight "
            + "ORDER BY w");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 3);
    assertEquals(docs.get(0).field("w"), 34.5d);
    assertEquals(docs.get(0).field("nb"), 1l);
    assertEquals(docs.get(0).field("min"), 1.7f);
    assertEquals(docs.get(0).field("sum"), 100);
    assertEquals(docs.get(1).field("w"), 52d);
    assertEquals(docs.get(1).field("nb"), 3l);
    assertEquals(docs.get(1).field("min"), 1.3f);
    assertEquals(docs.get(1).field("sum"), 260d);
    assertEquals(docs.get(2).field("w"), 60d);
    assertEquals(docs.get(2).field("nb"), 1l);
    assertEquals(docs.get(2).field("min"), 1.8f);
    assertEquals(docs.get(2).field("sum"), 100);
  }
  
  @Test
  public void selectGroupByOrdeByComplex(){
    final OSQLSynchQuery query = new OSQLSynchQuery(
            "SELECT weight AS w, points AS p, count(name) AS nb, min(size) AS min, sum(points) AS sum "
            + "FROM person "
            + "GROUP BY weight, points ORDER BY w, sum DESC");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 4);
    assertEquals(docs.get(0).field("w"), 34.5d);
    assertEquals(docs.get(0).field("p"), 100);
    assertEquals(docs.get(0).field("nb"), 1l);
    assertEquals(docs.get(0).field("min"), 1.7f);
    assertEquals(docs.get(0).field("sum"), 100);
    
    assertEquals(docs.get(1).field("w"), 52d);
    assertEquals(docs.get(1).field("p"), 80);
    assertEquals(docs.get(1).field("nb"), 2l);
    assertEquals(docs.get(1).field("min"), 1.3f);
    assertEquals(docs.get(1).field("sum"), 160d);
    
    assertEquals(docs.get(2).field("w"), 52d);
    assertEquals(docs.get(2).field("p"), 100);
    assertEquals(docs.get(2).field("nb"), 1l);
    assertEquals(docs.get(2).field("min"), 2.1f);
    assertEquals(docs.get(2).field("sum"), 100);
    
    assertEquals(docs.get(3).field("w"), 60d);
    assertEquals(docs.get(3).field("p"), 100);
    assertEquals(docs.get(3).field("nb"), 1l);
    assertEquals(docs.get(3).field("min"), 1.8f);
    assertEquals(docs.get(3).field("sum"), 100);
  }
  
  @Test
  public void selectWithParameterUnnamed(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car WHERE name = ?");
    final List<ODocument> docs = db.query(query,"fiesta");
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).field("name"), "fiesta");
  }
  
  @Test
  public void selectWithParameterNamed(){    
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT FROM car WHERE name = :name");
    final Map parameters = new HashMap();
    parameters.put("name", "fiesta");
    final List<ODocument> docs = db.query(query,parameters);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).field("name"), "fiesta");
  }

  @Test
  public void selectNoFrom(){
    final OSQLSynchQuery query = new OSQLSynchQuery("SELECT ifnull('a', 'b')");
    final List<ODocument> docs = db.query(query);
    assertEquals(docs.size(), 1);
    assertEquals(docs.get(0).field("ifnull"), "a");

  }
  
}
