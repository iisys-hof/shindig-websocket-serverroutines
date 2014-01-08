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

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;

/**
 * Test for the message collection converter class.
 */
public class GraphMessageCollectionTest {
  private static final String ID_FIELD = "id";
  private static final String TITLE_FIELD = "title";
  private static final String UPDATED_FIELD = "updated";
  private static final String URLS_FIELD = "urls";
  private static final String STATUS_FIELD = "status";

  private static final String UNREAD_FIELD = "unread";
  private static final String TOTAL_FIELD = "total";

  private static final String ID = "collection ID";
  private static final String TITLE = "collection title";
  private static final Long UPDATED = System.currentTimeMillis();
  private static final String[] URLS = { "URL1", "URL2", "URL3" };

  private static final String NEW_STATUS = "NEW";

  private GraphDatabaseService fDb;
  private Node fCollNode;

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
        if (GraphMessageCollectionTest.this.fDb != null) {
          GraphMessageCollectionTest.this.fDb.shutdown();
        }
      }
    });

    createTestData();
  }

  private void createTestData() {
    final Transaction trans = this.fDb.beginTx();

    this.fCollNode = this.fDb.createNode();

    // properties
    this.fCollNode.setProperty(GraphMessageCollectionTest.ID_FIELD, GraphMessageCollectionTest.ID);
    this.fCollNode.setProperty(GraphMessageCollectionTest.TITLE_FIELD,
            GraphMessageCollectionTest.TITLE);
    this.fCollNode.setProperty(GraphMessageCollectionTest.UPDATED_FIELD,
            GraphMessageCollectionTest.UPDATED);
    this.fCollNode.setProperty(GraphMessageCollectionTest.URLS_FIELD,
            GraphMessageCollectionTest.URLS);

    // relations - 2 unread, 1 undefined
    Relationship rel = this.fCollNode.createRelationshipTo(this.fDb.createNode(),
            Neo4jRelTypes.CONTAINS);
    rel.setProperty(GraphMessageCollectionTest.STATUS_FIELD, GraphMessageCollectionTest.NEW_STATUS);
    rel = this.fCollNode.createRelationshipTo(this.fDb.createNode(), Neo4jRelTypes.CONTAINS);
    rel.setProperty(GraphMessageCollectionTest.STATUS_FIELD, GraphMessageCollectionTest.NEW_STATUS);
    rel = this.fCollNode.createRelationshipTo(this.fDb.createNode(), Neo4jRelTypes.CONTAINS);

    trans.success();
    trans.finish();
  }

  /**
   * Test for conversion of existing data.
   */
  @Test
  public void conversionTest() {
    final Map<String, Object> coll = new GraphMessageCollection(this.fCollNode).toMap(null);

    Assert.assertEquals(GraphMessageCollectionTest.ID,
            coll.get(GraphMessageCollectionTest.ID_FIELD));
    Assert.assertEquals(GraphMessageCollectionTest.TITLE,
            coll.get(GraphMessageCollectionTest.TITLE_FIELD));
    Assert.assertEquals(GraphMessageCollectionTest.UPDATED,
            coll.get(GraphMessageCollectionTest.UPDATED_FIELD));

    boolean url1 = false, url2 = false, url3 = false;
    final String[] urls = (String[]) coll.get(GraphMessageCollectionTest.URLS_FIELD);
    Assert.assertEquals(3, urls.length);

    for (final String url : urls) {
      if (url.equals("URL1")) {
        url1 = true;
      } else if (url.equals("URL2")) {
        url2 = true;
      } else if (url.equals("URL3")) {
        url3 = true;
      }
    }
    Assert.assertTrue(url1 && url2 && url3);

    Assert.assertEquals(new Integer(2), coll.get(GraphMessageCollectionTest.UNREAD_FIELD));
    Assert.assertEquals(new Integer(3), coll.get(GraphMessageCollectionTest.TOTAL_FIELD));
  }
}
