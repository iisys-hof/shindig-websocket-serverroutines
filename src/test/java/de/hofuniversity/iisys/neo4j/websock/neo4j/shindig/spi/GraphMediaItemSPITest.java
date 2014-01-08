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
 * Test for the MediaItemService implementation of the graph back-end.
 */
public class GraphMediaItemSPITest {
  private static final String ID_FIELD = "id";
  private static final String TITLE_FIELD = "title";

  private static final String JOHN_ID = "john.doe", JANE_ID = "jane.doe", HORST_ID = "horst";

  private static final String JOHN_ALB_1_ID = "john1";
  private static final String JOHN_ALB_2_ID = "john2";
  private static final String JANE_ALB_1_ID = "jane1";
  private static final String HORST_ALB_ID = "horst";

  private static final String JOHN_ALB_1_TITLE = "john's album 1";
  private static final String JOHN_ALB_2_TITLE = "john's album 2";
  private static final String JANE_ALB_1_TITLE = "jane's album 1";
  private static final String HORST_ALB_TITLE = "horst's album";

  private static final String MEDIA_ITEM_1_ID = "1";
  private static final String MEDIA_ITEM_2_ID = "2";
  private static final String MEDIA_ITEM_3_ID = "3";
  private static final String MEDIA_ITEM_4_ID = "4";
  private static final String MEDIA_ITEM_5_ID = "5";
  private static final String MEDIA_ITEM_6_ID = "6";
  private static final String MEDIA_ITEM_7_ID = "7";

  private static final String MEDIA_ITEM_1_TITLE = "title 1";
  private static final String MEDIA_ITEM_2_TITLE = "title 2";
  private static final String MEDIA_ITEM_3_TITLE = "title 3";
  private static final String MEDIA_ITEM_4_TITLE = "title 4";
  private static final String MEDIA_ITEM_5_TITLE = "title 5";
  private static final String MEDIA_ITEM_6_TITLE = "title 6";
  private static final String MEDIA_ITEM_7_TITLE = "title 7";

  private GraphDatabaseService fDb;
  private GraphPersonSPI fPersonSPI;
  private GraphMediaItemSPI fMediaItemSPI;

  /**
   * Sets up an impermanent database with some test data and a media item service for testing
   * purposes.
   */
  @Before
  public void setupService() {
    final TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
    this.fDb = fact.newImpermanentDatabase();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (GraphMediaItemSPITest.this.fDb != null) {
          GraphMediaItemSPITest.this.fDb.shutdown();
        }
      }
    });

    this.fPersonSPI = new GraphPersonSPI(this.fDb, new GraphConfig(true), new ImplUtil(
            BasicBSONList.class, BasicBSONObject.class));
    this.fMediaItemSPI = new GraphMediaItemSPI(this.fDb, this.fPersonSPI, new IDManager(this.fDb),
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
    johndoe.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.JOHN_ID);
    personNodes.add(johndoe, GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.JOHN_ID);

    final Node janedoe = this.fDb.createNode();
    janedoe.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.JANE_ID);
    personNodes.add(janedoe, GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.JANE_ID);

    final Node horst = this.fDb.createNode();
    horst.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.HORST_ID);
    personNodes.add(horst, GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.HORST_ID);

    johndoe.createRelationshipTo(janedoe, Neo4jRelTypes.FRIEND_OF);
    janedoe.createRelationshipTo(johndoe, Neo4jRelTypes.FRIEND_OF);

    johndoe.createRelationshipTo(horst, Neo4jRelTypes.FRIEND_OF);
    horst.createRelationshipTo(johndoe, Neo4jRelTypes.FRIEND_OF);

    // albums
    final Node johnAlbum1 = this.fDb.createNode();
    final Node johnAlbum2 = this.fDb.createNode();
    final Node janeAlbum1 = this.fDb.createNode();
    final Node horstAlbum = this.fDb.createNode();

    johnAlbum1.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.JOHN_ALB_1_ID);
    johnAlbum2.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.JOHN_ALB_2_ID);
    janeAlbum1.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.JANE_ALB_1_ID);
    horstAlbum.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.HORST_ALB_ID);

    johnAlbum1.setProperty(GraphMediaItemSPITest.TITLE_FIELD,
            GraphMediaItemSPITest.JOHN_ALB_1_TITLE);
    johnAlbum2.setProperty(GraphMediaItemSPITest.TITLE_FIELD,
            GraphMediaItemSPITest.JOHN_ALB_2_TITLE);
    janeAlbum1.setProperty(GraphMediaItemSPITest.TITLE_FIELD,
            GraphMediaItemSPITest.JANE_ALB_1_TITLE);
    horstAlbum
            .setProperty(GraphMediaItemSPITest.TITLE_FIELD, GraphMediaItemSPITest.HORST_ALB_TITLE);

    johndoe.createRelationshipTo(johnAlbum1, Neo4jRelTypes.OWNER_OF);
    johndoe.createRelationshipTo(johnAlbum2, Neo4jRelTypes.OWNER_OF);
    janedoe.createRelationshipTo(janeAlbum1, Neo4jRelTypes.OWNER_OF);
    horst.createRelationshipTo(horstAlbum, Neo4jRelTypes.OWNER_OF);

    // media items
    final Node mediaItem1 = this.fDb.createNode();
    final Node mediaItem2 = this.fDb.createNode();
    final Node mediaItem3 = this.fDb.createNode();
    final Node mediaItem4 = this.fDb.createNode();
    final Node mediaItem5 = this.fDb.createNode();
    final Node mediaItem6 = this.fDb.createNode();
    final Node mediaItem7 = this.fDb.createNode();

    mediaItem1.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.MEDIA_ITEM_1_ID);
    mediaItem2.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.MEDIA_ITEM_2_ID);
    mediaItem3.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.MEDIA_ITEM_3_ID);
    mediaItem4.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.MEDIA_ITEM_4_ID);
    mediaItem5.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.MEDIA_ITEM_5_ID);
    mediaItem6.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.MEDIA_ITEM_6_ID);
    mediaItem7.setProperty(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.MEDIA_ITEM_7_ID);

    mediaItem1.setProperty(GraphMediaItemSPITest.TITLE_FIELD,
            GraphMediaItemSPITest.MEDIA_ITEM_1_TITLE);
    mediaItem2.setProperty(GraphMediaItemSPITest.TITLE_FIELD,
            GraphMediaItemSPITest.MEDIA_ITEM_2_TITLE);
    mediaItem3.setProperty(GraphMediaItemSPITest.TITLE_FIELD,
            GraphMediaItemSPITest.MEDIA_ITEM_3_TITLE);
    mediaItem4.setProperty(GraphMediaItemSPITest.TITLE_FIELD,
            GraphMediaItemSPITest.MEDIA_ITEM_4_TITLE);
    mediaItem5.setProperty(GraphMediaItemSPITest.TITLE_FIELD,
            GraphMediaItemSPITest.MEDIA_ITEM_5_TITLE);
    mediaItem6.setProperty(GraphMediaItemSPITest.TITLE_FIELD,
            GraphMediaItemSPITest.MEDIA_ITEM_6_TITLE);
    mediaItem7.setProperty(GraphMediaItemSPITest.TITLE_FIELD,
            GraphMediaItemSPITest.MEDIA_ITEM_7_TITLE);

    johnAlbum1.createRelationshipTo(mediaItem1, Neo4jRelTypes.CONTAINS);
    johnAlbum1.createRelationshipTo(mediaItem2, Neo4jRelTypes.CONTAINS);
    johnAlbum2.createRelationshipTo(mediaItem3, Neo4jRelTypes.CONTAINS);
    janeAlbum1.createRelationshipTo(mediaItem4, Neo4jRelTypes.CONTAINS);
    janeAlbum1.createRelationshipTo(mediaItem5, Neo4jRelTypes.CONTAINS);
    horstAlbum.createRelationshipTo(mediaItem6, Neo4jRelTypes.CONTAINS);
    horstAlbum.createRelationshipTo(mediaItem7, Neo4jRelTypes.CONTAINS);

    trans.success();
    trans.finish();

  }

  /**
   * Tests the retrieval of single items by their ID.
   */
  @Test
  public void singleRetrievalTest() {
    final SingleResult result = this.fMediaItemSPI.getMediaItem(GraphMediaItemSPITest.JOHN_ID,
            null, GraphMediaItemSPITest.JOHN_ALB_1_ID, GraphMediaItemSPITest.MEDIA_ITEM_2_ID, null);
    final Map<String, ?> map = result.getResults();

    Assert.assertEquals(GraphMediaItemSPITest.MEDIA_ITEM_2_ID,
            map.get(GraphMediaItemSPITest.ID_FIELD));
    Assert.assertEquals(GraphMediaItemSPITest.MEDIA_ITEM_2_TITLE,
            map.get(GraphMediaItemSPITest.TITLE_FIELD));
  }

  /**
   * Tests the retrieval of a list of items by their IDs.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void listRetrievalTest() {
    final List<String> idList = new ArrayList<String>();
    idList.add(GraphMediaItemSPITest.MEDIA_ITEM_1_ID);
    idList.add(GraphMediaItemSPITest.MEDIA_ITEM_2_ID);
    idList.add(GraphMediaItemSPITest.MEDIA_ITEM_3_ID);
    idList.add(GraphMediaItemSPITest.MEDIA_ITEM_5_ID);

    final ListResult result = this.fMediaItemSPI.getMediaItems(GraphMediaItemSPITest.JOHN_ID, null,
            GraphMediaItemSPITest.JOHN_ALB_1_ID, idList, new HashMap<String, Object>(), null);
    Assert.assertEquals(2, result.getSize());

    final List<Map<String, Object>> resList = (List<Map<String, Object>>) result.getResults();

    boolean found1 = false;
    boolean found2 = false;
    for (final Map<String, Object> map : resList) {
      if (GraphMediaItemSPITest.MEDIA_ITEM_1_ID.equals(map.get(GraphMediaItemSPITest.ID_FIELD))) {
        Assert.assertEquals(GraphMediaItemSPITest.MEDIA_ITEM_1_TITLE,
                map.get(GraphMediaItemSPITest.TITLE_FIELD));

        found1 = true;
      } else if (GraphMediaItemSPITest.MEDIA_ITEM_2_ID.equals(map
              .get(GraphMediaItemSPITest.ID_FIELD))) {
        Assert.assertEquals(GraphMediaItemSPITest.MEDIA_ITEM_2_TITLE,
                map.get(GraphMediaItemSPITest.TITLE_FIELD));

        found2 = true;
      } else {
        throw new RuntimeException("unexpected ID: " + map.get(GraphMediaItemSPITest.ID_FIELD));
      }
    }

    Assert.assertTrue(found1);
    Assert.assertTrue(found2);
  }

  /**
   * Tests the retrieval of all media items for a person's or people's albums.
   */
  @Test
  public void allRetrievalTest() {
    // single person, single album
    ListResult result = this.fMediaItemSPI.getMediaItems(GraphMediaItemSPITest.JOHN_ID, null,
            GraphMediaItemSPITest.JOHN_ALB_1_ID, new HashMap<String, Object>(), null);

    assertContains(result, GraphMediaItemSPITest.MEDIA_ITEM_1_ID,
            GraphMediaItemSPITest.MEDIA_ITEM_1_TITLE);
    assertContains(result, GraphMediaItemSPITest.MEDIA_ITEM_2_ID,
            GraphMediaItemSPITest.MEDIA_ITEM_2_TITLE);

    // multiple people
    final List<String> userIds = new ArrayList<String>();
    userIds.add(GraphMediaItemSPITest.JANE_ID);
    userIds.add(GraphMediaItemSPITest.HORST_ID);

    result = this.fMediaItemSPI.getMediaItems(userIds, null, null, new HashMap<String, Object>(),
            null);

    assertContains(result, GraphMediaItemSPITest.MEDIA_ITEM_4_ID,
            GraphMediaItemSPITest.MEDIA_ITEM_4_TITLE);
    assertContains(result, GraphMediaItemSPITest.MEDIA_ITEM_5_ID,
            GraphMediaItemSPITest.MEDIA_ITEM_5_TITLE);
    assertContains(result, GraphMediaItemSPITest.MEDIA_ITEM_6_ID,
            GraphMediaItemSPITest.MEDIA_ITEM_6_TITLE);
    assertContains(result, GraphMediaItemSPITest.MEDIA_ITEM_7_ID,
            GraphMediaItemSPITest.MEDIA_ITEM_7_TITLE);

    // friends of a person
    userIds.clear();
    userIds.add(GraphMediaItemSPITest.JOHN_ID);

    result = this.fMediaItemSPI.getMediaItems(userIds, "@friends", null,
            new HashMap<String, Object>(), null);

    assertContains(result, GraphMediaItemSPITest.MEDIA_ITEM_4_ID,
            GraphMediaItemSPITest.MEDIA_ITEM_4_TITLE);
    assertContains(result, GraphMediaItemSPITest.MEDIA_ITEM_5_ID,
            GraphMediaItemSPITest.MEDIA_ITEM_5_TITLE);
    assertContains(result, GraphMediaItemSPITest.MEDIA_ITEM_6_ID,
            GraphMediaItemSPITest.MEDIA_ITEM_6_TITLE);
    assertContains(result, GraphMediaItemSPITest.MEDIA_ITEM_7_ID,
            GraphMediaItemSPITest.MEDIA_ITEM_7_TITLE);
  }

  @SuppressWarnings("unchecked")
  private void assertContains(ListResult results, final String id, String title) {
    final List<Map<String, Object>> resList = (List<Map<String, Object>>) results.getResults();

    boolean found = false;

    for (final Map<String, Object> resMap : resList) {
      if (id.equals(resMap.get(GraphMediaItemSPITest.ID_FIELD))) {
        Assert.assertEquals(title, resMap.get(GraphMediaItemSPITest.TITLE_FIELD));
        found = true;
        break;
      }
    }

    Assert.assertTrue(found);
  }

  /**
   * Tests the creation of a media item.
   */
  @Test
  public void creationTest() {
    // create
    final Map<String, Object> itemMap = new HashMap<String, Object>();
    itemMap.put(GraphMediaItemSPITest.ID_FIELD, "8");
    itemMap.put(GraphMediaItemSPITest.TITLE_FIELD, "title 8");

    this.fMediaItemSPI.createMediaItem(GraphMediaItemSPITest.HORST_ID, null,
            GraphMediaItemSPITest.HORST_ALB_ID, itemMap);

    // check
    final SingleResult result = this.fMediaItemSPI.getMediaItem(GraphMediaItemSPITest.HORST_ID,
            null, GraphMediaItemSPITest.HORST_ALB_ID, "8", null);

    final Map<String, ?> map = result.getResults();
    Assert.assertEquals("8", map.get(GraphMediaItemSPITest.ID_FIELD));
    Assert.assertEquals("title 8", map.get(GraphMediaItemSPITest.TITLE_FIELD));
  }

  /**
   * Tests updating of a media item.
   */
  @Test
  public void updateTest() {
    // update
    final Map<String, Object> itemMap = new HashMap<String, Object>();
    itemMap.put(GraphMediaItemSPITest.ID_FIELD, GraphMediaItemSPITest.MEDIA_ITEM_7_ID);
    itemMap.put(GraphMediaItemSPITest.TITLE_FIELD, GraphMediaItemSPITest.MEDIA_ITEM_7_TITLE
            + "_mod");

    this.fMediaItemSPI.updateMediaItem(GraphMediaItemSPITest.HORST_ID, null,
            GraphMediaItemSPITest.HORST_ALB_ID, GraphMediaItemSPITest.MEDIA_ITEM_7_ID, itemMap);

    // check
    final SingleResult result = this.fMediaItemSPI.getMediaItem(GraphMediaItemSPITest.HORST_ID,
            null, GraphMediaItemSPITest.HORST_ALB_ID, GraphMediaItemSPITest.MEDIA_ITEM_7_ID, null);

    final Map<String, ?> map = result.getResults();
    Assert.assertEquals(GraphMediaItemSPITest.MEDIA_ITEM_7_ID,
            map.get(GraphMediaItemSPITest.ID_FIELD));
    Assert.assertEquals(GraphMediaItemSPITest.MEDIA_ITEM_7_TITLE + "_mod",
            map.get(GraphMediaItemSPITest.TITLE_FIELD));
  }

  /**
   * Tests the deletion of a media item.
   */
  @Test
  public void deletionTest() {
    // delete
    this.fMediaItemSPI.deleteMediaItem(GraphMediaItemSPITest.HORST_ID, null,
            GraphMediaItemSPITest.HORST_ALB_ID, GraphMediaItemSPITest.MEDIA_ITEM_6_ID);

    // check
    boolean success = false;

    try {
      final SingleResult result = this.fMediaItemSPI
              .getMediaItem(GraphMediaItemSPITest.HORST_ID, null,
                      GraphMediaItemSPITest.HORST_ALB_ID, GraphMediaItemSPITest.MEDIA_ITEM_6_ID,
                      null);

      if (result == null || result.getResults() == null || result.getSize() == 0) {
        success = true;
      }
    } catch (final Exception e) {
      success = true;
    }

    Assert.assertTrue(success);
  }
}
