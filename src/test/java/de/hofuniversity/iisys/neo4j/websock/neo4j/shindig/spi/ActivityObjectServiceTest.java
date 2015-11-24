/*
 * Copyright (c) 2012-2015 Institute of Information Systems, Hof University
 *
 * This file is part of "Apache Shindig WebSocket Server Routines".
 *
 * "Apache Shindig WebSocket Server Routines" is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.service.IDManager;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Test for the graph back-end's internal activity object service.
 */
public class ActivityObjectServiceTest {
  private static final String ID_FIELD = "id";
  private static final String NAME_FIELD = "displayName";
  private static final String TYPE_FIELD = "objectType";

  private static final String TYPE1 = "bogus", TYPE2 = "nonesense", TYPE3 = "used";

  private static final String ID1 = "1", ID2 = "2", ID3 = "3", ID4 = "4";

  private GraphDatabaseService fDb;
  private ActivityObjectService fActObjSPI;

  private Node fObj1, fObj4;

  /**
   * Sets up an impermanent database with some test data for testing purposes.
   */
  @Before
  public void setupService() {
    final TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
    this.fDb = fact.newImpermanentDatabase();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (ActivityObjectServiceTest.this.fDb != null) {
          ActivityObjectServiceTest.this.fDb.shutdown();
        }
      }
    });

    final Map<String, String> config = new HashMap<String, String>();
    config.put("activityobjects.deduplicate", "true");

    this.fActObjSPI = new ActivityObjectService(this.fDb, config, new IDManager(this.fDb),
            new ImplUtil(BasicBSONList.class, BasicBSONObject.class));

    createTestData();
  }

  @After
  public void stopDatabase() {
    this.fDb.shutdown();
  }

  private void createTestData() {
    final Transaction trans = this.fDb.beginTx();

    this.fObj1 = this.fDb.createNode();
    this.fObj1.setProperty(ActivityObjectServiceTest.ID_FIELD, ActivityObjectServiceTest.ID1);
    this.fObj1.setProperty(ActivityObjectServiceTest.NAME_FIELD, ActivityObjectServiceTest.ID1);
    this.fObj1.setProperty(ActivityObjectServiceTest.TYPE_FIELD, ActivityObjectServiceTest.TYPE1);
    Index<Node> objNodes = this.fDb.index().forNodes("bogus_activityobject");
    objNodes.add(this.fObj1, "id", ActivityObjectServiceTest.ID1);

    this.fObj4 = this.fDb.createNode();
    this.fObj4.setProperty(ActivityObjectServiceTest.ID_FIELD, ActivityObjectServiceTest.ID4);
    this.fObj4.setProperty(ActivityObjectServiceTest.NAME_FIELD, ActivityObjectServiceTest.ID4);
    this.fObj4.setProperty(ActivityObjectServiceTest.TYPE_FIELD, ActivityObjectServiceTest.TYPE3);
    objNodes = this.fDb.index().forNodes("used_activityobject");
    objNodes.add(this.fObj4, "id", ActivityObjectServiceTest.ID4);

    final Node entry = this.fDb.createNode();
    entry.createRelationshipTo(this.fObj4, Neo4jRelTypes.OBJECT);

    trans.success();
    trans.finish();
  }

  /**
   * Test for the retrieval of existing activity objects.
   */
  @Test
  public void retrievalTest() {
    final Map<String, Object> obj = new HashMap<String, Object>();
    obj.put(ActivityObjectServiceTest.ID_FIELD, ActivityObjectServiceTest.ID1);
    obj.put(ActivityObjectServiceTest.NAME_FIELD, ActivityObjectServiceTest.ID1);
    obj.put(ActivityObjectServiceTest.TYPE_FIELD, ActivityObjectServiceTest.TYPE1);

    final Node node = this.fActObjSPI.getObjectNode(obj);
    Assert.assertEquals(this.fObj1, node);
  }

  /**
   * Test for the creation of new activity objects.
   */
  @Test
  public void creationTest() {
    // the service is not supposed/required to handle its own transactions
    final Transaction trans = this.fDb.beginTx();

    // existing index
    Map<String, Object> obj = new HashMap<String, Object>();
    obj.put(ActivityObjectServiceTest.ID_FIELD, ActivityObjectServiceTest.ID2);
    obj.put(ActivityObjectServiceTest.NAME_FIELD, ActivityObjectServiceTest.ID2);
    obj.put(ActivityObjectServiceTest.TYPE_FIELD, ActivityObjectServiceTest.TYPE1);

    Node node = this.fActObjSPI.getObjectNode(obj);
    Assert.assertEquals(node, this.fActObjSPI.getObjectNode(obj));

    // new index
    obj = new HashMap<String, Object>();
    obj.put(ActivityObjectServiceTest.ID_FIELD, ActivityObjectServiceTest.ID3);
    obj.put(ActivityObjectServiceTest.NAME_FIELD, ActivityObjectServiceTest.ID3);
    obj.put(ActivityObjectServiceTest.TYPE_FIELD, ActivityObjectServiceTest.TYPE2);

    node = this.fActObjSPI.getObjectNode(obj);
    Assert.assertEquals(node, this.fActObjSPI.getObjectNode(obj));

    trans.success();
    trans.finish();
  }

  /**
   * Test for the deletion of unused activity objects.
   */
  @Test
  public void deletionTest() {
    // the service is not supposed/required to handle its own transactions
    final Transaction trans = this.fDb.beginTx();

    // actual deletion
    Map<String, Object> obj = new HashMap<String, Object>();
    obj.put(ActivityObjectServiceTest.ID_FIELD, ActivityObjectServiceTest.ID1);
    obj.put(ActivityObjectServiceTest.NAME_FIELD, ActivityObjectServiceTest.ID1);
    obj.put(ActivityObjectServiceTest.TYPE_FIELD, ActivityObjectServiceTest.TYPE1);

    // old node
    final Node node = this.fActObjSPI.getObjectNode(obj);

    Set<Node> nodeSet = new HashSet<Node>();
    nodeSet.add(node);
    this.fActObjSPI.deleteIfUnused(nodeSet);

    // new one should be created
    Assert.assertTrue(!node.equals(this.fActObjSPI.getObjectNode(obj)));

    // used node, should not be deleted
    obj = new HashMap<String, Object>();
    obj.put(ActivityObjectServiceTest.ID_FIELD, ActivityObjectServiceTest.ID4);
    obj.put(ActivityObjectServiceTest.NAME_FIELD, ActivityObjectServiceTest.ID4);
    obj.put(ActivityObjectServiceTest.TYPE_FIELD, ActivityObjectServiceTest.TYPE3);

    nodeSet = new HashSet<Node>();
    nodeSet.add(this.fObj4);
    this.fActObjSPI.deleteIfUnused(nodeSet);

    Assert.assertEquals(this.fObj4, this.fActObjSPI.getObjectNode(obj));

    trans.success();
    trans.finish();
  }
}
