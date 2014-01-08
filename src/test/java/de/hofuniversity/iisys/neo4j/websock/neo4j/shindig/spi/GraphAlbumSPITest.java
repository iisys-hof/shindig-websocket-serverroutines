/*
 * Copyright (c) 2012-2013 Institute of Information Systems, Hof University
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.service.IDManager;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Test for the AlbumService implementation of the graph back-end.
 */
public class GraphAlbumSPITest {
  private static final String ID_FIELD = "id";
  private static final String TITLE_FIELD = "title";

  private static final String JOHN_ID = "john.doe", JANE_ID = "jane.doe", HORST_ID = "horst";

  private static final String JOHN_ALB_1_ID = "john1";
  private static final String JOHN_ALB_2_ID = "john2";
  private static final String JANE_ALB_1_ID = "jane1";
  private static final String JANE_ALB_2_ID = "jane2";
  private static final String JANE_ALB_3_ID = "jane3";
  private static final String HORST_ALB_ID = "horst";

  private static final String JOHN_ALB_1_TITLE = "john's album 1";
  private static final String JOHN_ALB_2_TITLE = "john's album 2";
  private static final String JANE_ALB_1_TITLE = "jane's album 1";
  private static final String JANE_ALB_2_TITLE = "jane's album 2";
  private static final String JANE_ALB_3_TITLE = "jane's album 3";
  private static final String HORST_ALB_TITLE = "horst's album";

  private GraphDatabaseService fDb;
  private GraphPersonSPI fPersonSPI;
  private GraphAlbumSPI fAlbumSPI;

  /**
   * Sets up an impermanent database with some test data and an album service for testing purposes.
   */
  @Before
  public void setupService() {
    final TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
    this.fDb = fact.newImpermanentDatabase();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (GraphAlbumSPITest.this.fDb != null) {
          GraphAlbumSPITest.this.fDb.shutdown();
        }
      }
    });

    this.fPersonSPI = new GraphPersonSPI(this.fDb, new GraphConfig(true), new ImplUtil(
            BasicBSONList.class, BasicBSONObject.class));
    this.fAlbumSPI = new GraphAlbumSPI(this.fDb, this.fPersonSPI, new IDManager(this.fDb),
            new ImplUtil(BasicBSONList.class, BasicBSONObject.class));

    createTestData();
  }

  @After
  public void stopDatabase() {
    this.fDb.shutdown();
  }

  private void createTestData() {
    final Index<Node> personNodes = this.fDb.index().forNodes(ShindigConstants.PERSON_NODES);

    final Transaction trans = this.fDb.beginTx();

    // people
    final Node johndoe = this.fDb.createNode();
    johndoe.setProperty(GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.JOHN_ID);
    personNodes.add(johndoe, GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.JOHN_ID);

    final Node janedoe = this.fDb.createNode();
    janedoe.setProperty(GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.JANE_ID);
    personNodes.add(janedoe, GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.JANE_ID);

    final Node horst = this.fDb.createNode();
    horst.setProperty(GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.HORST_ID);
    personNodes.add(horst, GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.HORST_ID);

    johndoe.createRelationshipTo(janedoe, Neo4jRelTypes.FRIEND_OF);
    janedoe.createRelationshipTo(johndoe, Neo4jRelTypes.FRIEND_OF);

    johndoe.createRelationshipTo(horst, Neo4jRelTypes.FRIEND_OF);
    horst.createRelationshipTo(johndoe, Neo4jRelTypes.FRIEND_OF);

    // albums
    final Node johnAlbum1 = this.fDb.createNode();
    final Node johnAlbum2 = this.fDb.createNode();
    final Node janeAlbum1 = this.fDb.createNode();
    final Node janeAlbum2 = this.fDb.createNode();
    final Node janeAlbum3 = this.fDb.createNode();
    final Node horstAlbum = this.fDb.createNode();

    johnAlbum1.setProperty(GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.JOHN_ALB_1_ID);
    johnAlbum2.setProperty(GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.JOHN_ALB_2_ID);
    janeAlbum1.setProperty(GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.JANE_ALB_1_ID);
    janeAlbum2.setProperty(GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.JANE_ALB_2_ID);
    janeAlbum3.setProperty(GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.JANE_ALB_3_ID);
    horstAlbum.setProperty(GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.HORST_ALB_ID);

    johnAlbum1.setProperty(GraphAlbumSPITest.TITLE_FIELD, GraphAlbumSPITest.JOHN_ALB_1_TITLE);
    johnAlbum2.setProperty(GraphAlbumSPITest.TITLE_FIELD, GraphAlbumSPITest.JOHN_ALB_2_TITLE);
    janeAlbum1.setProperty(GraphAlbumSPITest.TITLE_FIELD, GraphAlbumSPITest.JANE_ALB_1_TITLE);
    janeAlbum2.setProperty(GraphAlbumSPITest.TITLE_FIELD, GraphAlbumSPITest.JANE_ALB_2_TITLE);
    janeAlbum3.setProperty(GraphAlbumSPITest.TITLE_FIELD, GraphAlbumSPITest.JANE_ALB_3_TITLE);
    horstAlbum.setProperty(GraphAlbumSPITest.TITLE_FIELD, GraphAlbumSPITest.HORST_ALB_TITLE);

    johndoe.createRelationshipTo(johnAlbum1, Neo4jRelTypes.OWNER_OF);
    johndoe.createRelationshipTo(johnAlbum2, Neo4jRelTypes.OWNER_OF);
    janedoe.createRelationshipTo(janeAlbum1, Neo4jRelTypes.OWNER_OF);
    janedoe.createRelationshipTo(janeAlbum2, Neo4jRelTypes.OWNER_OF);
    janedoe.createRelationshipTo(janeAlbum3, Neo4jRelTypes.OWNER_OF);
    horst.createRelationshipTo(horstAlbum, Neo4jRelTypes.OWNER_OF);

    trans.success();
    trans.finish();
  }

  /**
   * Tests the retrieval of a single album with a specific ID.
   */
  @Test
  public void singleRetrievalTest() {
    final SingleResult result = this.fAlbumSPI.getAlbum(GraphAlbumSPITest.JOHN_ID, null,
            GraphAlbumSPITest.JOHN_ALB_2_ID, null);
    final Map<String, ?> map = result.getResults();

    Assert.assertEquals(GraphAlbumSPITest.JOHN_ALB_2_ID, map.get(GraphAlbumSPITest.ID_FIELD));
    Assert.assertEquals(GraphAlbumSPITest.JOHN_ALB_2_TITLE, map.get(GraphAlbumSPITest.TITLE_FIELD));
  }

  /**
   * Tests the retrieval of multiple albums with specific IDs.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void listRetrievalTest() {
    // only the first two should be retrieved
    final List<String> albumIds = new ArrayList<String>();
    albumIds.add(GraphAlbumSPITest.JANE_ALB_1_ID);
    albumIds.add(GraphAlbumSPITest.JANE_ALB_3_ID);
    albumIds.add(GraphAlbumSPITest.HORST_ALB_ID);

    final ListResult result = this.fAlbumSPI.getAlbums(GraphAlbumSPITest.JANE_ID, null, albumIds,
            new HashMap<String, Object>(), null);

    Assert.assertEquals(2, result.getSize());

    boolean found1 = false;
    boolean found3 = false;
    for (final Object o : result.getResults()) {
      final Map<String, Object> oMap = (Map<String, Object>) o;

      if (oMap.get(GraphAlbumSPITest.ID_FIELD).equals(GraphAlbumSPITest.JANE_ALB_1_ID)) {
        Assert.assertEquals(GraphAlbumSPITest.JANE_ALB_1_TITLE,
                oMap.get(GraphAlbumSPITest.TITLE_FIELD));
        found1 = true;
      } else if (oMap.get(GraphAlbumSPITest.ID_FIELD).equals(GraphAlbumSPITest.JANE_ALB_3_ID)) {
        Assert.assertEquals(GraphAlbumSPITest.JANE_ALB_3_TITLE,
                oMap.get(GraphAlbumSPITest.TITLE_FIELD));
        found3 = true;
      } else {
        throw new RuntimeException("unexpected album ID: " + oMap.get(GraphAlbumSPITest.ID_FIELD));
      }
    }

    Assert.assertTrue(found1);
    Assert.assertTrue(found3);
  }

  /**
   * Tests the retrieval of albums by owner or a certain group.
   */
  @Test
  public void allRetrievalTest() {
    final List<String> userIds = new ArrayList<String>();
    userIds.add(GraphAlbumSPITest.JOHN_ID);

    // single person
    ListResult results = this.fAlbumSPI.getAlbums(userIds, null, null,
            new HashMap<String, Object>(), null);
    assertContains(results, GraphAlbumSPITest.JOHN_ALB_1_ID, GraphAlbumSPITest.JOHN_ALB_1_TITLE);
    assertContains(results, GraphAlbumSPITest.JOHN_ALB_2_ID, GraphAlbumSPITest.JOHN_ALB_2_TITLE);

    // multiple people
    userIds.add(GraphAlbumSPITest.HORST_ID);

    results = this.fAlbumSPI.getAlbums(userIds, null, null, new HashMap<String, Object>(), null);
    assertContains(results, GraphAlbumSPITest.JOHN_ALB_1_ID, GraphAlbumSPITest.JOHN_ALB_1_TITLE);
    assertContains(results, GraphAlbumSPITest.JOHN_ALB_2_ID, GraphAlbumSPITest.JOHN_ALB_2_TITLE);
    assertContains(results, GraphAlbumSPITest.HORST_ALB_ID, GraphAlbumSPITest.HORST_ALB_TITLE);

    // friends
    userIds.clear();
    userIds.add(GraphAlbumSPITest.JOHN_ID);

    results = this.fAlbumSPI.getAlbums(userIds, "@friends", null, new HashMap<String, Object>(),
            null);
    assertContains(results, GraphAlbumSPITest.JANE_ALB_1_ID, GraphAlbumSPITest.JANE_ALB_1_TITLE);
    assertContains(results, GraphAlbumSPITest.JANE_ALB_2_ID, GraphAlbumSPITest.JANE_ALB_2_TITLE);
    assertContains(results, GraphAlbumSPITest.JANE_ALB_3_ID, GraphAlbumSPITest.JANE_ALB_3_TITLE);
    assertContains(results, GraphAlbumSPITest.HORST_ALB_ID, GraphAlbumSPITest.HORST_ALB_TITLE);
  }

  @SuppressWarnings("unchecked")
  private void assertContains(ListResult results, final String id, String title) {
    final List<Map<String, Object>> resList = (List<Map<String, Object>>) results.getResults();

    boolean found = false;

    for (final Map<String, Object> resMap : resList) {
      if (id.equals(resMap.get(GraphAlbumSPITest.ID_FIELD))) {
        Assert.assertEquals(title, resMap.get(GraphAlbumSPITest.TITLE_FIELD));
        found = true;
        break;
      }
    }

    Assert.assertTrue(found);
  }

  /**
   * Tests the creation of albums.
   */
  @Test
  public void creationTest() {
    // create
    final Map<String, Object> newAlbum = new HashMap<String, Object>();
    newAlbum.put(GraphAlbumSPITest.ID_FIELD, "newId");
    newAlbum.put(GraphAlbumSPITest.TITLE_FIELD, "new title");

    this.fAlbumSPI.createAlbum(GraphAlbumSPITest.HORST_ID, null, newAlbum);

    // retrieve
    final SingleResult result = this.fAlbumSPI.getAlbum(GraphAlbumSPITest.HORST_ID, null, "newId",
            null);
    final Map<String, ?> albMap = result.getResults();

    Assert.assertEquals("newId", albMap.get(GraphAlbumSPITest.ID_FIELD));
    Assert.assertEquals("new title", albMap.get(GraphAlbumSPITest.TITLE_FIELD));
  }

  /**
   * Tests the updating of albums.
   */
  @Test
  public void updateTest() {
    // update
    final Map<String, Object> updatedAlbum = new HashMap<String, Object>();
    updatedAlbum.put(GraphAlbumSPITest.ID_FIELD, GraphAlbumSPITest.HORST_ALB_ID);
    updatedAlbum.put(GraphAlbumSPITest.TITLE_FIELD, "new title");

    this.fAlbumSPI.updateAlbum(GraphAlbumSPITest.HORST_ID, null, GraphAlbumSPITest.HORST_ALB_ID,
            updatedAlbum);

    // retrieve
    final SingleResult result = this.fAlbumSPI.getAlbum(GraphAlbumSPITest.HORST_ID, null,
            GraphAlbumSPITest.HORST_ALB_ID, null);
    final Map<String, ?> albMap = result.getResults();

    Assert.assertEquals(GraphAlbumSPITest.HORST_ALB_ID, albMap.get(GraphAlbumSPITest.ID_FIELD));
    Assert.assertEquals("new title", albMap.get(GraphAlbumSPITest.TITLE_FIELD));
  }

  /**
   * Tests the deletion of albums.
   */
  @Test
  public void deletionTest() {
    // delete
    this.fAlbumSPI.deleteAlbum(GraphAlbumSPITest.HORST_ID, null, GraphAlbumSPITest.HORST_ALB_ID);

    // check
    boolean success = false;

    try {
      final SingleResult result = this.fAlbumSPI.getAlbum(GraphAlbumSPITest.HORST_ID, null,
              GraphAlbumSPITest.HORST_ALB_ID, null);

      if (result == null || result.getResults() == null || result.getSize() == 0) {
        success = true;
      }
    } catch (final Exception e) {
      success = true;
    }

    Assert.assertTrue(success);
  }
}
