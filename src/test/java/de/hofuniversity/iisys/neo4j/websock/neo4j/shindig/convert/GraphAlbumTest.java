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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;

/**
 * Test for the activity entry and activity object converter classes.
 */
public class GraphAlbumTest {

  private static final String DESCRIPTION_FIELD = "description";
  private static final String ID_FIELD = "id";
  private static final String OWNER_ID_FIELD = "ownerId";
  private static final String THUMBNAIL_FIELD = "thumbnailUrl";
  private static final String TITLE_FIELD = "title";

  private static final String LOCATION_FIELD = "location";
  private static final String MEDIA_ITEM_COUNT_FIELD = "mediaItemCount";
  private static final String MEDIA_MIME_TYPE_FIELD = "mediaMimeType";

  private static final String MEDIA_TYPE_FIELD = "mediaType";
  private static final String MIME_TYPE_FIELD = "mimeType";
  private static final String TYPE_FIELD = "type";

  private static final String FORMATTED_FIELD = "formatted";

  private static final String ALBUM_ID = "albumId";
  private static final String ALBUM_DESC = "description";
  private static final String ALBUM_OWNER_ID = "albumOwner";
  private static final String ALBUM_THUMB = "thumbnailUrl";
  private static final String ALBUM_TITLE = "title of album";

  private static final String ADD_FORMATTED = "bogus address";

  private static final String ITEM_MIME_TYPE = "itemMimeType";
  private static final String ITEM_TYPE = "itemType";

  private GraphDatabaseService fDb;

  private Node fAlbumNode, fLocNode, fItem1Node, fItem2Node, fItem3Node;

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
        if (GraphAlbumTest.this.fDb != null) {
          GraphAlbumTest.this.fDb.shutdown();
        }
      }
    });

    createTestData();
  }

  private void createTestData() {
    final Transaction trans = this.fDb.beginTx();

    // album
    this.fAlbumNode = this.fDb.createNode();

    this.fAlbumNode.setProperty(GraphAlbumTest.ID_FIELD, GraphAlbumTest.ALBUM_ID);
    this.fAlbumNode.setProperty(GraphAlbumTest.DESCRIPTION_FIELD, GraphAlbumTest.ALBUM_DESC);
    this.fAlbumNode.setProperty(GraphAlbumTest.OWNER_ID_FIELD, GraphAlbumTest.ALBUM_OWNER_ID);
    this.fAlbumNode.setProperty(GraphAlbumTest.TITLE_FIELD, GraphAlbumTest.ALBUM_TITLE);
    this.fAlbumNode.setProperty(GraphAlbumTest.THUMBNAIL_FIELD, GraphAlbumTest.ALBUM_THUMB);

    // location
    this.fLocNode = this.fDb.createNode();
    this.fAlbumNode.createRelationshipTo(this.fLocNode, Neo4jRelTypes.LOCATED_AT);

    this.fLocNode.setProperty(GraphAlbumTest.FORMATTED_FIELD, GraphAlbumTest.ADD_FORMATTED);

    // subordinate media items
    this.fItem1Node = this.fDb.createNode();
    this.fItem2Node = this.fDb.createNode();
    this.fItem3Node = this.fDb.createNode();

    this.fItem1Node.setProperty(GraphAlbumTest.MIME_TYPE_FIELD, GraphAlbumTest.ITEM_MIME_TYPE);
    this.fItem2Node.setProperty(GraphAlbumTest.MIME_TYPE_FIELD, GraphAlbumTest.ITEM_MIME_TYPE);
    this.fItem3Node.setProperty(GraphAlbumTest.MIME_TYPE_FIELD, GraphAlbumTest.ITEM_MIME_TYPE);

    this.fItem1Node.setProperty(GraphAlbumTest.TYPE_FIELD, GraphAlbumTest.ITEM_TYPE);
    this.fItem2Node.setProperty(GraphAlbumTest.TYPE_FIELD, GraphAlbumTest.ITEM_TYPE);
    this.fItem3Node.setProperty(GraphAlbumTest.TYPE_FIELD, GraphAlbumTest.ITEM_TYPE);

    this.fAlbumNode.createRelationshipTo(this.fItem1Node, Neo4jRelTypes.CONTAINS);
    this.fAlbumNode.createRelationshipTo(this.fItem2Node, Neo4jRelTypes.CONTAINS);
    this.fAlbumNode.createRelationshipTo(this.fItem3Node, Neo4jRelTypes.CONTAINS);

    trans.success();
    trans.finish();
  }

  /**
   * Test for conversion of existing data.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void conversionTest() {
    final GraphAlbum gAlbum = new GraphAlbum(this.fAlbumNode);
    final Map<String, Object> albumMap = gAlbum.toMap(null);

    // internal fields
    Assert.assertEquals(GraphAlbumTest.ALBUM_ID, albumMap.get(GraphAlbumTest.ID_FIELD));
    Assert.assertEquals(GraphAlbumTest.ALBUM_DESC, albumMap.get(GraphAlbumTest.DESCRIPTION_FIELD));
    Assert.assertEquals(GraphAlbumTest.ALBUM_OWNER_ID, albumMap.get(GraphAlbumTest.OWNER_ID_FIELD));
    Assert.assertEquals(GraphAlbumTest.ALBUM_THUMB, albumMap.get(GraphAlbumTest.THUMBNAIL_FIELD));
    Assert.assertEquals(GraphAlbumTest.ALBUM_TITLE, albumMap.get(GraphAlbumTest.TITLE_FIELD));

    // external fields
    // location
    final Map<String, Object> addressMap = (Map<String, Object>) albumMap
            .get(GraphAlbumTest.LOCATION_FIELD);
    Assert.assertEquals(GraphAlbumTest.ADD_FORMATTED,
            addressMap.get(GraphAlbumTest.FORMATTED_FIELD));

    // media item related
    Assert.assertEquals(3, albumMap.get(GraphAlbumTest.MEDIA_ITEM_COUNT_FIELD));

    final List<String> mediaTypes = (List<String>) albumMap.get(GraphAlbumTest.MEDIA_TYPE_FIELD);
    Assert.assertTrue(mediaTypes.contains(GraphAlbumTest.ITEM_TYPE));
    Assert.assertEquals(1, mediaTypes.size());

    final List<String> mimeTypes = (List<String>) albumMap
            .get(GraphAlbumTest.MEDIA_MIME_TYPE_FIELD);
    Assert.assertTrue(mimeTypes.contains(GraphAlbumTest.ITEM_MIME_TYPE));
    Assert.assertEquals(1, mimeTypes.size());
  }

  /**
   * Test for value storing capabilities.
   */
  @Test
  public void storageTest() {
    final GraphAlbum gAlbum = new GraphAlbum(this.fAlbumNode);
    final Map<String, Object> albumMap = new HashMap<String, Object>();

    // internal fields
    albumMap.put(GraphAlbumTest.ID_FIELD, GraphAlbumTest.ALBUM_ID + "_mod");
    albumMap.put(GraphAlbumTest.DESCRIPTION_FIELD, GraphAlbumTest.ALBUM_DESC + "_mod");
    albumMap.put(GraphAlbumTest.OWNER_ID_FIELD, GraphAlbumTest.ALBUM_OWNER_ID + "_mod");
    albumMap.put(GraphAlbumTest.THUMBNAIL_FIELD, GraphAlbumTest.ALBUM_THUMB + "_mod");
    albumMap.put(GraphAlbumTest.TITLE_FIELD, GraphAlbumTest.ALBUM_TITLE + "_mod");

    // address
    final Map<String, Object> addressMap = new HashMap<String, Object>();
    addressMap.put(GraphAlbumTest.FORMATTED_FIELD, GraphAlbumTest.ADD_FORMATTED + "_mod");
    albumMap.put(GraphAlbumTest.LOCATION_FIELD, addressMap);

    final Transaction tx = this.fDb.beginTx();
    gAlbum.setData(albumMap);
    tx.success();
    tx.finish();

    // verify
    Assert.assertEquals(GraphAlbumTest.ALBUM_ID + "_mod",
            this.fAlbumNode.getProperty(GraphAlbumTest.ID_FIELD));
    Assert.assertEquals(GraphAlbumTest.ALBUM_DESC + "_mod",
            this.fAlbumNode.getProperty(GraphAlbumTest.DESCRIPTION_FIELD));
    Assert.assertEquals(GraphAlbumTest.ALBUM_OWNER_ID + "_mod",
            this.fAlbumNode.getProperty(GraphAlbumTest.OWNER_ID_FIELD));
    Assert.assertEquals(GraphAlbumTest.THUMBNAIL_FIELD, GraphAlbumTest.ALBUM_THUMB + "_mod",
            this.fAlbumNode.getProperty(GraphAlbumTest.THUMBNAIL_FIELD));
    Assert.assertEquals(GraphAlbumTest.ALBUM_TITLE + "_mod",
            this.fAlbumNode.getProperty(GraphAlbumTest.TITLE_FIELD));

    // location
    final Relationship locRel = this.fAlbumNode.getSingleRelationship(Neo4jRelTypes.LOCATED_AT,
            Direction.OUTGOING);
    final Node locNode = locRel.getEndNode();

    Assert.assertEquals(GraphAlbumTest.ADD_FORMATTED + "_mod",
            locNode.getProperty(GraphAlbumTest.FORMATTED_FIELD));
  }
}
