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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert;

import java.util.Arrays;
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
 * Test for the message converter class.
 */
public class GraphMessageTest {
  private static final String APP_URL_FIELD = "appUrl";
  private static final String BODY_FIELD = "body";
  private static final String BODY_ID_FIELD = "bodyId";
  private static final String ID_FIELD = "id";
  private static final String REPLY_TO_FIELD = "inReplyTo";
  private static final String SENDER_ID_FIELD = "senderId";
  private static final String TITLE_FIELD = "title";
  private static final String TITLE_ID_FIELD = "titleId";
  private static final String TYPE_FIELD = "type";
  private static final String TIME_FIELD = "timeSent";
  private static final String UPDATED_FIELD = "updated";
  private static final String RECIPIENTS_FIELD = "recipients";
  private static final String URLS_FIELD = "urls";
  private static final String URL_TYPES_FIELD = "urls_types";
  private static final String URL_TEXTS_FIELD = "urls_linkTexts";
  private static final String STATUS_FIELD = "status";
  private static final String COLL_IDS_FIELD = "collectionIds";
  private static final String REPLIES_FIELD = "replies";

  private static final String APP_URL = "application URL";
  private static final String BODY = "message body";
  private static final String BODY_ID = "message body ID";
  private static final String ID = "message ID";
  private static final String REPLY_TO = "previous message ID";
  private static final String SENDER_ID = "sender ID";
  private static final String TITLE = "message title";
  private static final String TITLE_ID = "message title ID";
  private static final String TYPE = "PRIVATE_MESSAGE";
  private static final Long TIME = System.currentTimeMillis();
  private static final Long UPDATED = System.currentTimeMillis();
  private static final String[] RECIPIENTS = { "ID1", "ID2", "ID3" };
  private static final String[] URLS = { "URL1", "URL2", "URL3" };
  private static final String[] URL_TEXTS = { "TEXT1", "TEXT2", "TEXT3" };
  private static final String[] URL_TYPES = { "TYPE1", "TYPE2", "TYPE3" };

  private static final String COLL1_ID = "collection 1", COLL2_ID = "collection 2";
  private static final String STATUS = "NEW";

  private static final String REPLY1_ID = "reply 1", REPLY2_ID = "reply 2";

  private GraphDatabaseService fDb;
  private Node fMessageNode;
  private Relationship fConRel;

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
        if (GraphMessageTest.this.fDb != null) {
          GraphMessageTest.this.fDb.shutdown();
        }
      }
    });

    createTestData();
  }

  private void createTestData() {
    final Transaction trans = this.fDb.beginTx();

    this.fMessageNode = this.fDb.createNode();

    // properties
    this.fMessageNode.setProperty(GraphMessageTest.APP_URL_FIELD, GraphMessageTest.APP_URL);
    this.fMessageNode.setProperty(GraphMessageTest.BODY_FIELD, GraphMessageTest.BODY);
    this.fMessageNode.setProperty(GraphMessageTest.BODY_ID_FIELD, GraphMessageTest.BODY_ID);
    this.fMessageNode.setProperty(GraphMessageTest.ID_FIELD, GraphMessageTest.ID);
    this.fMessageNode.setProperty(GraphMessageTest.REPLY_TO_FIELD, GraphMessageTest.REPLY_TO);
    this.fMessageNode.setProperty(GraphMessageTest.SENDER_ID_FIELD, GraphMessageTest.SENDER_ID);
    this.fMessageNode.setProperty(GraphMessageTest.TITLE_FIELD, GraphMessageTest.TITLE);
    this.fMessageNode.setProperty(GraphMessageTest.TITLE_ID_FIELD, GraphMessageTest.TITLE_ID);
    this.fMessageNode.setProperty(GraphMessageTest.TYPE_FIELD, GraphMessageTest.TYPE);
    this.fMessageNode.setProperty(GraphMessageTest.TIME_FIELD, GraphMessageTest.TIME);
    this.fMessageNode.setProperty(GraphMessageTest.UPDATED_FIELD, GraphMessageTest.UPDATED);
    this.fMessageNode.setProperty(GraphMessageTest.RECIPIENTS_FIELD, GraphMessageTest.RECIPIENTS);

    this.fMessageNode.setProperty(GraphMessageTest.URLS_FIELD, GraphMessageTest.URLS);
    this.fMessageNode.setProperty(GraphMessageTest.URL_TYPES_FIELD, GraphMessageTest.URL_TYPES);
    this.fMessageNode.setProperty(GraphMessageTest.URL_TEXTS_FIELD, GraphMessageTest.URL_TEXTS);

    // user
    final Node userNode = this.fDb.createNode();

    // collections
    final Node coll1Node = this.fDb.createNode();
    coll1Node.setProperty(GraphMessageTest.ID_FIELD, GraphMessageTest.COLL1_ID);
    this.fConRel = coll1Node.createRelationshipTo(this.fMessageNode, Neo4jRelTypes.CONTAINS);
    this.fConRel.setProperty(GraphMessageTest.STATUS_FIELD, GraphMessageTest.STATUS);

    userNode.createRelationshipTo(coll1Node, Neo4jRelTypes.OWNS);

    final Node coll2Node = this.fDb.createNode();
    coll2Node.setProperty(GraphMessageTest.ID_FIELD, GraphMessageTest.COLL2_ID);
    coll2Node.createRelationshipTo(this.fMessageNode, Neo4jRelTypes.CONTAINS);

    userNode.createRelationshipTo(coll2Node, Neo4jRelTypes.OWNS);

    // replies
    final Node reply1Node = this.fDb.createNode();
    reply1Node.setProperty(GraphMessageTest.ID_FIELD, GraphMessageTest.REPLY1_ID);
    reply1Node.createRelationshipTo(this.fMessageNode, Neo4jRelTypes.REPLY_TO);

    final Node reply2Node = this.fDb.createNode();
    reply2Node.setProperty(GraphMessageTest.ID_FIELD, GraphMessageTest.REPLY2_ID);
    reply2Node.createRelationshipTo(this.fMessageNode, Neo4jRelTypes.REPLY_TO);

    trans.success();
    trans.finish();
  }

  /**
   * Test for conversion of existing data.
   */
  @Test
  public void conversionTest() {
    final Map<String, Object> m = new GraphMessage(this.fMessageNode, this.fConRel).toMap(null);

    Assert.assertEquals(GraphMessageTest.APP_URL, m.get(GraphMessageTest.APP_URL_FIELD));
    Assert.assertEquals(GraphMessageTest.BODY, m.get(GraphMessageTest.BODY_FIELD));
    Assert.assertEquals(GraphMessageTest.BODY_ID, m.get(GraphMessageTest.BODY_ID_FIELD));
    Assert.assertEquals(GraphMessageTest.ID, m.get(GraphMessageTest.ID_FIELD));
    Assert.assertEquals(GraphMessageTest.REPLY_TO, m.get(GraphMessageTest.REPLY_TO_FIELD));
    Assert.assertEquals(GraphMessageTest.SENDER_ID, m.get(GraphMessageTest.SENDER_ID_FIELD));
    Assert.assertEquals(GraphMessageTest.STATUS, m.get(GraphMessageTest.STATUS_FIELD));
    Assert.assertEquals(GraphMessageTest.TITLE, m.get(GraphMessageTest.TITLE_FIELD));
    Assert.assertEquals(GraphMessageTest.TITLE_ID, m.get(GraphMessageTest.TITLE_ID_FIELD));
    Assert.assertEquals(GraphMessageTest.TYPE, m.get(GraphMessageTest.TYPE_FIELD));
    Assert.assertEquals(GraphMessageTest.TIME, m.get(GraphMessageTest.TIME_FIELD));
    Assert.assertEquals(GraphMessageTest.UPDATED, m.get(GraphMessageTest.UPDATED_FIELD));

    final String[] recipients = (String[]) m.get(GraphMessageTest.RECIPIENTS_FIELD);
    Assert.assertArrayEquals(GraphMessageTest.RECIPIENTS, recipients);

    String[] urls = (String[]) m.get(GraphMessageTest.URLS_FIELD);
    Assert.assertArrayEquals(GraphMessageTest.URLS, urls);

    urls = (String[]) m.get(GraphMessageTest.URL_TYPES_FIELD);
    Assert.assertTrue(Arrays.equals(urls, GraphMessageTest.URL_TYPES));

    urls = (String[]) m.get(GraphMessageTest.URL_TEXTS_FIELD);
    Assert.assertTrue(Arrays.equals(urls, GraphMessageTest.URL_TEXTS));

    final String[] colls = (String[]) m.get(GraphMessageTest.COLL_IDS_FIELD);
    Assert.assertEquals(GraphMessageTest.COLL1_ID, colls[0]);
    Assert.assertEquals(GraphMessageTest.COLL2_ID, colls[1]);

    final String[] replies = (String[]) m.get(GraphMessageTest.REPLIES_FIELD);
    Assert.assertEquals(GraphMessageTest.REPLY1_ID, replies[0]);
    Assert.assertEquals(GraphMessageTest.REPLY2_ID, replies[1]);
  }

  /**
   * Test for value storing capabilities.
   */
  @Test
  public void storageTest() {
    final Map<String, Object> m = new GraphMessage(this.fMessageNode, this.fConRel).toMap(null);

    final Transaction trans = this.fDb.beginTx();

    final Node newMess = this.fDb.createNode();
    final GraphMessage gMess = new GraphMessage(newMess, null);
    gMess.setData(m);

    trans.success();
    trans.finish();

    Assert.assertEquals(GraphMessageTest.APP_URL,
            newMess.getProperty(GraphMessageTest.APP_URL_FIELD));
    Assert.assertEquals(GraphMessageTest.BODY, newMess.getProperty(GraphMessageTest.BODY_FIELD));
    Assert.assertEquals(GraphMessageTest.BODY_ID,
            newMess.getProperty(GraphMessageTest.BODY_ID_FIELD));
    Assert.assertEquals(GraphMessageTest.REPLY_TO,
            newMess.getProperty(GraphMessageTest.REPLY_TO_FIELD));
    Assert.assertEquals(GraphMessageTest.SENDER_ID,
            newMess.getProperty(GraphMessageTest.SENDER_ID_FIELD));
    Assert.assertEquals(GraphMessageTest.TITLE, newMess.getProperty(GraphMessageTest.TITLE_FIELD));
    Assert.assertEquals(GraphMessageTest.TITLE_ID,
            newMess.getProperty(GraphMessageTest.TITLE_ID_FIELD));
    Assert.assertEquals(GraphMessageTest.TYPE, newMess.getProperty(GraphMessageTest.TYPE_FIELD));
    Assert.assertEquals(GraphMessageTest.TIME, newMess.getProperty(GraphMessageTest.TIME_FIELD));
    Assert.assertEquals(GraphMessageTest.UPDATED,
            newMess.getProperty(GraphMessageTest.UPDATED_FIELD));

    final String[] recipients = (String[]) newMess.getProperty(GraphMessageTest.RECIPIENTS_FIELD);
    Assert.assertTrue(Arrays.equals(recipients, GraphMessageTest.RECIPIENTS));

    String[] urls = (String[]) newMess.getProperty(GraphMessageTest.URLS_FIELD);
    Assert.assertTrue(Arrays.equals(urls, GraphMessageTest.URLS));

    urls = (String[]) newMess.getProperty(GraphMessageTest.URL_TYPES_FIELD);
    Assert.assertTrue(Arrays.equals(urls, GraphMessageTest.URL_TYPES));

    urls = (String[]) newMess.getProperty(GraphMessageTest.URL_TEXTS_FIELD);
    Assert.assertTrue(Arrays.equals(urls, GraphMessageTest.URL_TEXTS));
  }
}
