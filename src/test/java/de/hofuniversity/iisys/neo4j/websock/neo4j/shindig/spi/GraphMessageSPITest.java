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
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Test for the MessageService implementation of the graph back-end.
 */
public class GraphMessageSPITest {
  private static final String ID_FIELD = "id";
  private static final String TITLE_FIELD = "title";
  private static final String SENDER_FIELD = "senderId";
  private static final String RECIPIENTS_FIELD = "recipients";

  private static final String ALL_NAME = "@all";
  private static final String OUTBOX_NAME = "@outbox";

  private static final String JOHN_ID = "john.doe", JANE_ID = "jane.doe", JACK_ID = "jack.doe",
          HORST_ID = "horst";

  private GraphDatabaseService fDb;
  private GraphPersonSPI fPersonSPI;
  private GraphMessageSPI fMessageSPI;

  private String fCreatedId;

  /**
   * Sets up an impermanent database with some test data, a person service and a message service for
   * testing purposes.
   */
  @Before
  public void setupService() {
    final TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
    this.fDb = fact.newImpermanentDatabase();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (GraphMessageSPITest.this.fDb != null) {
          GraphMessageSPITest.this.fDb.shutdown();
        }
      }
    });

    this.fPersonSPI = new GraphPersonSPI(this.fDb, new GraphConfig(true), new ImplUtil(
            BasicBSONList.class, BasicBSONObject.class));
    this.fMessageSPI = new GraphMessageSPI(this.fDb, this.fPersonSPI, new IDManager(this.fDb),
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
    johndoe.setProperty(GraphMessageSPITest.ID_FIELD, GraphMessageSPITest.JOHN_ID);
    personNodes.add(johndoe, GraphMessageSPITest.ID_FIELD, GraphMessageSPITest.JOHN_ID);

    final Node janedoe = this.fDb.createNode();
    janedoe.setProperty(GraphMessageSPITest.ID_FIELD, GraphMessageSPITest.JANE_ID);
    personNodes.add(janedoe, GraphMessageSPITest.ID_FIELD, GraphMessageSPITest.JANE_ID);

    final Node jackdoe = this.fDb.createNode();
    jackdoe.setProperty(GraphMessageSPITest.ID_FIELD, GraphMessageSPITest.JACK_ID);
    personNodes.add(jackdoe, GraphMessageSPITest.ID_FIELD, GraphMessageSPITest.JACK_ID);

    final Node horst = this.fDb.createNode();
    horst.setProperty(GraphMessageSPITest.ID_FIELD, GraphMessageSPITest.HORST_ID);
    personNodes.add(horst, GraphMessageSPITest.ID_FIELD, GraphMessageSPITest.HORST_ID);

    // message collections
    final Node johnIn = this.fDb.createNode();
    johnIn.setProperty(GraphMessageSPITest.ID_FIELD, OSFields.INBOX_NAME);
    johnIn.setProperty(GraphMessageSPITest.TITLE_FIELD, "John's inbox");
    johndoe.createRelationshipTo(johnIn, Neo4jRelTypes.OWNS);

    final Node johnOut = this.fDb.createNode();
    johnOut.setProperty(GraphMessageSPITest.ID_FIELD, GraphMessageSPITest.OUTBOX_NAME);
    johnOut.setProperty(GraphMessageSPITest.TITLE_FIELD, "John's outbox");
    johndoe.createRelationshipTo(johnOut, Neo4jRelTypes.OWNS);

    final Node janeIn = this.fDb.createNode();
    janeIn.setProperty(GraphMessageSPITest.ID_FIELD, OSFields.INBOX_NAME);
    janeIn.setProperty(GraphMessageSPITest.TITLE_FIELD, "Jane's inbox");
    janedoe.createRelationshipTo(janeIn, Neo4jRelTypes.OWNS);

    final Node janeOut = this.fDb.createNode();
    janeOut.setProperty(GraphMessageSPITest.ID_FIELD, GraphMessageSPITest.OUTBOX_NAME);
    janeOut.setProperty(GraphMessageSPITest.TITLE_FIELD, "Jane's outbox");
    janedoe.createRelationshipTo(janeOut, Neo4jRelTypes.OWNS);

    final Node jackIn = this.fDb.createNode();
    jackIn.setProperty(GraphMessageSPITest.ID_FIELD, OSFields.INBOX_NAME);
    jackIn.setProperty(GraphMessageSPITest.TITLE_FIELD, "Jack's inbox");
    jackdoe.createRelationshipTo(jackIn, Neo4jRelTypes.OWNS);

    final Node jackOut = this.fDb.createNode();
    jackOut.setProperty(GraphMessageSPITest.ID_FIELD, GraphMessageSPITest.OUTBOX_NAME);
    jackOut.setProperty(GraphMessageSPITest.TITLE_FIELD, "Jack's outbox");
    jackdoe.createRelationshipTo(jackOut, Neo4jRelTypes.OWNS);

    final Node horstIn = this.fDb.createNode();
    horstIn.setProperty(GraphMessageSPITest.ID_FIELD, OSFields.INBOX_NAME);
    horstIn.setProperty(GraphMessageSPITest.TITLE_FIELD, "Horst's inbox");
    horst.createRelationshipTo(horstIn, Neo4jRelTypes.OWNS);

    final Node horstOut = this.fDb.createNode();
    horstOut.setProperty(GraphMessageSPITest.ID_FIELD, GraphMessageSPITest.OUTBOX_NAME);
    horstOut.setProperty(GraphMessageSPITest.TITLE_FIELD, "Horst's outbox");
    horst.createRelationshipTo(horstOut, Neo4jRelTypes.OWNS);

    // messages
    final Node mess1Node = this.fDb.createNode();
    mess1Node.setProperty(GraphMessageSPITest.ID_FIELD, "1");
    mess1Node.setProperty(GraphMessageSPITest.TITLE_FIELD, "Hallo Horst");

    janeOut.createRelationshipTo(mess1Node, Neo4jRelTypes.CONTAINS);
    janedoe.createRelationshipTo(mess1Node, Neo4jRelTypes.SENT);
    horstIn.createRelationshipTo(mess1Node, Neo4jRelTypes.CONTAINS);
    mess1Node.createRelationshipTo(horst, Neo4jRelTypes.SENT_TO);

    final Node mess2Node = this.fDb.createNode();
    mess2Node.setProperty(GraphMessageSPITest.ID_FIELD, "2");
    mess2Node.setProperty(GraphMessageSPITest.TITLE_FIELD, "Hallo Familie");

    johnOut.createRelationshipTo(mess2Node, Neo4jRelTypes.CONTAINS);
    janeIn.createRelationshipTo(mess2Node, Neo4jRelTypes.CONTAINS);
    jackIn.createRelationshipTo(mess2Node, Neo4jRelTypes.CONTAINS);
    johndoe.createRelationshipTo(mess2Node, Neo4jRelTypes.SENT);
    mess2Node.createRelationshipTo(janedoe, Neo4jRelTypes.SENT_TO);
    mess2Node.createRelationshipTo(jackdoe, Neo4jRelTypes.SENT_TO);

    final Node mess3Node = this.fDb.createNode();
    mess3Node.setProperty(GraphMessageSPITest.ID_FIELD, "3");
    mess3Node.setProperty(GraphMessageSPITest.TITLE_FIELD, "Liebe Kollegen");

    janeOut.createRelationshipTo(mess3Node, Neo4jRelTypes.CONTAINS);
    johnIn.createRelationshipTo(mess3Node, Neo4jRelTypes.CONTAINS);
    horstIn.createRelationshipTo(mess3Node, Neo4jRelTypes.CONTAINS);
    janedoe.createRelationshipTo(mess3Node, Neo4jRelTypes.SENT);
    mess3Node.createRelationshipTo(johndoe, Neo4jRelTypes.SENT_TO);
    mess3Node.createRelationshipTo(horst, Neo4jRelTypes.SENT_TO);

    final Node mess4Node = this.fDb.createNode();
    mess4Node.setProperty(GraphMessageSPITest.ID_FIELD, "4");
    mess4Node.setProperty(GraphMessageSPITest.TITLE_FIELD, "Hallo Mutti");

    jackOut.createRelationshipTo(mess4Node, Neo4jRelTypes.CONTAINS);
    janeIn.createRelationshipTo(mess4Node, Neo4jRelTypes.CONTAINS);
    jackdoe.createRelationshipTo(mess4Node, Neo4jRelTypes.SENT);
    mess4Node.createRelationshipTo(janedoe, Neo4jRelTypes.SENT_TO);

    trans.success();
    trans.finish();
  }

  /**
   * Tests the retrieval of message collections.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @SuppressWarnings("unchecked")
  @Test
  public void collectionRetrievalTest() throws Exception {
    final ListResult colls = this.fMessageSPI.getMessageCollections(GraphMessageSPITest.JOHN_ID,
            new HashMap<String, Object>(), null);

    final List<Map<String, Object>> messColls = (List<Map<String, Object>>) colls.getResults();

    Assert.assertEquals(2, messColls.size());

    boolean inFound = false;
    boolean outFound = false;

    String name = null;
    for (final Map<String, Object> c : messColls) {
      name = c.get(GraphMessageSPITest.TITLE_FIELD).toString();

      if (name.equals("John's inbox")) {
        inFound = true;
      } else if (name.equals("John's outbox")) {
        outFound = true;
      } else {
        throw new Exception("unexpected collection");
      }
    }
    Assert.assertTrue(inFound && outFound);
  }

  /**
   * Tests the creation of a message collection.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void collectionCreationTest() {
    // create message collection
    Map<String, Object> msgCollection = new HashMap<String, Object>();
    msgCollection.put(GraphMessageSPITest.ID_FIELD, "newCollId");
    msgCollection.put(GraphMessageSPITest.TITLE_FIELD, "new collection");

    this.fMessageSPI.createMessageCollection(GraphMessageSPITest.HORST_ID, msgCollection);

    // check
    final ListResult result = this.fMessageSPI.getMessageCollections(GraphMessageSPITest.HORST_ID,
            new HashMap<String, Object>(), null);

    boolean found = false;
    for (final Object collObj : result.getResults()) {
      msgCollection = (Map<String, Object>) collObj;

      if ("newCollId".equals(msgCollection.get(GraphMessageSPITest.ID_FIELD))) {
        Assert.assertEquals("new collection", msgCollection.get(GraphMessageSPITest.TITLE_FIELD));
        found = true;
        break;
      }
    }
    Assert.assertTrue(found);
  }

  /**
   * Tests the modification of a message collection.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void collectionModificationTest() {
    // create collection to modify
    collectionCreationTest();

    // modify collection
    Map<String, Object> msgCollection = new HashMap<String, Object>();
    msgCollection.put(GraphMessageSPITest.ID_FIELD, "newCollId");
    msgCollection.put(GraphMessageSPITest.TITLE_FIELD, "new name");

    this.fMessageSPI.modifyMessageCollection(GraphMessageSPITest.HORST_ID, msgCollection);

    // check
    final ListResult result = this.fMessageSPI.getMessageCollections(GraphMessageSPITest.HORST_ID,
            new HashMap<String, Object>(), null);

    boolean found = false;
    for (final Object collObj : result.getResults()) {
      msgCollection = (Map<String, Object>) collObj;

      if ("newCollId".equals(msgCollection.get(GraphMessageSPITest.ID_FIELD))) {
        Assert.assertEquals("new name", msgCollection.get(GraphMessageSPITest.TITLE_FIELD));
        found = true;
        break;
      }
    }
    Assert.assertTrue(found);
  }

  /**
   * Tests the deletion of a message Collection
   */
  @SuppressWarnings("unchecked")
  @Test
  public void collectionDeletionTest() {
    // create collection to delete
    collectionCreationTest();

    // delete collection
    this.fMessageSPI.deleteMessageCollection(GraphMessageSPITest.HORST_ID, "newCollId");

    // check
    final ListResult result = this.fMessageSPI.getMessageCollections(GraphMessageSPITest.HORST_ID,
            new HashMap<String, Object>(), null);

    boolean found = false;
    Map<String, Object> msgCollection = null;
    for (final Object collObj : result.getResults()) {
      msgCollection = (Map<String, Object>) collObj;

      if ("newCollId".equals(msgCollection.get(GraphMessageSPITest.ID_FIELD))) {
        found = true;
        break;
      }
    }
    Assert.assertFalse(found);
  }

  /**
   * Tests the retrieval of message collections.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @SuppressWarnings("unchecked")
  @Test
  public void messageRetrievalTest() throws Exception {
    final Map<String, Object> collOpts = new HashMap<String, Object>();

    // retrieve all
    ListResult messColl = this.fMessageSPI.getMessages(GraphMessageSPITest.JANE_ID,
            GraphMessageSPITest.ALL_NAME, null, collOpts, null);

    List<Map<String, Object>> messages = (List<Map<String, Object>>) messColl.getResults();

    Assert.assertEquals(4, messages.size());

    final boolean found[] = { false, false, false, false };
    for (final Map<String, Object> m : messages) {
      if (m.get(GraphMessageSPITest.ID_FIELD).equals("1")) {
        found[0] = true;
      } else if (m.get(GraphMessageSPITest.ID_FIELD).equals("2")) {
        found[1] = true;
      } else if (m.get(GraphMessageSPITest.ID_FIELD).equals("3")) {
        found[2] = true;
      } else if (m.get(GraphMessageSPITest.ID_FIELD).equals("4")) {
        found[3] = true;
      }
    }
    Assert.assertTrue(found[0] && found[1] && found[2] && found[3]);

    // by collection
    messColl = this.fMessageSPI.getMessages(GraphMessageSPITest.JANE_ID, OSFields.INBOX_NAME, null,
            collOpts, null);

    messages = (List<Map<String, Object>>) messColl.getResults();

    Assert.assertEquals(2, messages.size());

    found[1] = false;
    found[3] = false;
    for (final Map<String, Object> m : messages) {
      if (m.get(GraphMessageSPITest.ID_FIELD).equals("2")) {
        found[1] = true;
      } else if (m.get(GraphMessageSPITest.ID_FIELD).equals("4")) {
        found[3] = true;
      } else {
        throw new Exception("unexpected message " + m.get(GraphMessageSPITest.ID_FIELD));
      }
    }
    Assert.assertTrue(found[1] && found[3]);

    // by id
    final List<String> messIds = new ArrayList<String>();
    messIds.add("1");
    messIds.add("4");

    messColl = this.fMessageSPI.getMessages(GraphMessageSPITest.JANE_ID,
            GraphMessageSPITest.ALL_NAME, messIds, collOpts, null);

    messages = (List<Map<String, Object>>) messColl.getResults();

    Assert.assertEquals(2, messages.size());

    found[0] = false;
    found[3] = false;
    for (final Map<String, Object> m : messages) {
      if (m.get(GraphMessageSPITest.ID_FIELD).equals("1")) {
        found[0] = true;
      } else if (m.get(GraphMessageSPITest.ID_FIELD).equals("4")) {
        found[3] = true;
      } else {
        throw new Exception("unexpected message " + m.get(GraphMessageSPITest.ID_FIELD));
      }
    }
    Assert.assertTrue(found[0] && found[3]);
  }

  /**
   * Tests the creation/sending of new messages to one or more recipients.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @SuppressWarnings("unchecked")
  @Test
  public void messageSendTest() throws Exception {
    // sending to single person
    Map<String, Object> testMsg = new HashMap<String, Object>();
    testMsg.put(GraphMessageSPITest.TITLE_FIELD, "testmessage1");
    testMsg.put(GraphMessageSPITest.SENDER_FIELD, GraphMessageSPITest.JANE_ID);

    String[] recipients = { GraphMessageSPITest.HORST_ID };
    testMsg.put(GraphMessageSPITest.RECIPIENTS_FIELD, recipients);

    this.fMessageSPI.createMessage(GraphMessageSPITest.JANE_ID, null,
            GraphMessageSPITest.OUTBOX_NAME, testMsg);

    // check sender's out box
    final Map<String, Object> collOpts = new HashMap<String, Object>();
    ListResult messColl = this.fMessageSPI.getMessages(GraphMessageSPITest.JANE_ID,
            GraphMessageSPITest.OUTBOX_NAME, null, collOpts, null);
    List<Map<String, Object>> messages = (List<Map<String, Object>>) messColl.getResults();

    Assert.assertEquals(3, messages.size());
    boolean found = false;
    for (final Map<String, Object> m : messages) {
      if (m.get(GraphMessageSPITest.TITLE_FIELD).equals("testmessage1")) {
        found = true;
      }
    }
    Assert.assertTrue(found);

    // check recipient's in box
    messColl = this.fMessageSPI.getMessages(GraphMessageSPITest.HORST_ID, OSFields.INBOX_NAME,
            null, collOpts, null);
    messages = (List<Map<String, Object>>) messColl.getResults();

    Assert.assertEquals(3, messages.size());
    found = false;
    for (final Map<String, Object> m : messages) {
      if (m.get(GraphMessageSPITest.TITLE_FIELD).equals("testmessage1")) {
        found = true;
      }
    }
    Assert.assertTrue(found);

    // sending to multiple people
    // TODO:?
    testMsg = new HashMap<String, Object>();
    testMsg.put(GraphMessageSPITest.TITLE_FIELD, "testmessage2");
    testMsg.put(GraphMessageSPITest.SENDER_FIELD, GraphMessageSPITest.JANE_ID);

    recipients = new String[] { GraphMessageSPITest.JOHN_ID };
    testMsg.put(GraphMessageSPITest.RECIPIENTS_FIELD, recipients);

    this.fMessageSPI.createMessage(GraphMessageSPITest.JANE_ID, null,
            GraphMessageSPITest.OUTBOX_NAME, testMsg);

    // check sender's out box
    messColl = this.fMessageSPI.getMessages(GraphMessageSPITest.JANE_ID,
            GraphMessageSPITest.OUTBOX_NAME, null, collOpts, null);
    messages = (List<Map<String, Object>>) messColl.getResults();

    Assert.assertEquals(4, messages.size());
    found = false;
    for (final Map<String, Object> m : messages) {
      if (m.get(GraphMessageSPITest.TITLE_FIELD).equals("testmessage2")) {
        found = true;
      }
    }
    Assert.assertTrue(found);

    // check recipient's in box
    messColl = this.fMessageSPI.getMessages(GraphMessageSPITest.JOHN_ID, OSFields.INBOX_NAME, null,
            collOpts, null);
    messages = (List<Map<String, Object>>) messColl.getResults();

    Assert.assertEquals(2, messages.size());
    found = false;
    for (final Map<String, Object> m : messages) {
      if (m.get(GraphMessageSPITest.TITLE_FIELD).equals("testmessage2")) {
        found = true;
      }
    }
    Assert.assertTrue(found);
  }

  /**
   * Tests the creation of a message without sending it.
   *
   * @return ID of the message created
   */
  @SuppressWarnings("unchecked")
  @Test
  public void messageCreationTest() {
    // create collection to use for tests
    final Map<String, Object> msgCollection = new HashMap<String, Object>();
    msgCollection.put(GraphMessageSPITest.ID_FIELD, "bogus");
    msgCollection.put(GraphMessageSPITest.TITLE_FIELD, "bogus collection");
    this.fMessageSPI.createMessageCollection(GraphMessageSPITest.JACK_ID, msgCollection);

    // create message
    final Map<String, Object> message = new HashMap<String, Object>();
    message.put(GraphMessageSPITest.TITLE_FIELD, "bogus title");

    final List<String> recipients = new ArrayList<String>();
    recipients.add(GraphMessageSPITest.JANE_ID);
    message.put(GraphMessageSPITest.RECIPIENTS_FIELD, recipients);

    this.fMessageSPI.createMessage(GraphMessageSPITest.JACK_ID, "testapp", "bogus", message);

    // check - it should be in the creator's collection, but not in the recipient's
    ListResult result = this.fMessageSPI.getMessages(GraphMessageSPITest.JACK_ID, "bogus", null,
            new HashMap<String, Object>(), null);
    List<Map<String, Object>> resList = (List<Map<String, Object>>) result.getResults();

    boolean found = false;

    for (final Map<String, Object> msg : resList) {
      if ("bogus title".equals(msg.get(GraphMessageSPITest.TITLE_FIELD))) {
        found = true;
        this.fCreatedId = msg.get(GraphMessageSPITest.ID_FIELD).toString();
        break;
      }
    }

    Assert.assertTrue(found);

    result = this.fMessageSPI.getMessages(GraphMessageSPITest.JANE_ID, OSFields.INBOX_NAME, null,
            new HashMap<String, Object>(), null);
    resList = (List<Map<String, Object>>) result.getResults();

    found = false;

    for (final Map<String, Object> msg : resList) {
      if ("bogus title".equals(msg.get(GraphMessageSPITest.TITLE_FIELD))) {
        found = true;
        break;
      }
    }

    Assert.assertFalse(found);
  }

  /**
   * Tests the modification of a message
   */
  @SuppressWarnings("unchecked")
  @Test
  public void messageModificationTest() {
    // create message to modify
    messageCreationTest();

    // simple modification
    Map<String, Object> message = new HashMap<String, Object>();
    message.put(GraphMessageSPITest.ID_FIELD, this.fCreatedId);
    message.put(GraphMessageSPITest.TITLE_FIELD, "new title");

    this.fMessageSPI.modifyMessage(GraphMessageSPITest.JACK_ID, "bogus", this.fCreatedId, message);

    final List<String> msgIds = new ArrayList<String>();
    final ListResult result = this.fMessageSPI.getMessages(GraphMessageSPITest.JACK_ID, "bogus",
            msgIds, new HashMap<String, Object>(), null);

    // check
    message = (Map<String, Object>) result.getResults().get(0);
    Assert.assertEquals("new title", message.get(GraphMessageSPITest.TITLE_FIELD));

    // try modifying a message that has already been sent
    message = new HashMap<String, Object>();
    message.put(GraphMessageSPITest.ID_FIELD, "4");
    message.put(GraphMessageSPITest.TITLE_FIELD, "new title");

    boolean fail = false;
    try {
      this.fMessageSPI.modifyMessage(GraphMessageSPITest.JACK_ID, GraphMessageSPITest.OUTBOX_NAME,
              "4", message);
    } catch (final Exception e) {
      fail = true;
    }
    Assert.assertTrue(fail);
  }

  /**
   * Tests the deletion of new messages to one or more recipients.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @SuppressWarnings("unchecked")
  @Test
  public void messageDeletionTest() throws Exception {
    // create test messages
    messageSendTest();

    // determine ID of first test message
    final Map<String, Object> collOpts = new HashMap<String, Object>();
    final List<String> delIDs = new ArrayList<String>();
    ListResult messColl = this.fMessageSPI.getMessages(GraphMessageSPITest.JANE_ID,
            GraphMessageSPITest.OUTBOX_NAME, null, collOpts, null);
    List<Map<String, Object>> messages = (List<Map<String, Object>>) messColl.getResults();

    for (final Map<String, Object> m : messages) {
      if (m.get(GraphMessageSPITest.TITLE_FIELD).equals("testmessage1")) {
        delIDs.add(m.get(GraphMessageSPITest.ID_FIELD).toString());
      }
    }

    // delete from sender's out box
    this.fMessageSPI.deleteMessages(GraphMessageSPITest.JANE_ID, GraphMessageSPITest.OUTBOX_NAME,
            delIDs);

    // check sender's out box for deletion
    messColl = this.fMessageSPI.getMessages(GraphMessageSPITest.JANE_ID,
            GraphMessageSPITest.OUTBOX_NAME, null, collOpts, null);
    messages = (List<Map<String, Object>>) messColl.getResults();

    Assert.assertEquals(3, messages.size());
    boolean found = false;
    for (final Map<String, Object> m : messages) {
      if (m.get(GraphMessageSPITest.TITLE_FIELD).equals("testmessage1")) {
        found = true;
      }
    }
    Assert.assertFalse(found);

    // delete from recipient's in box
    this.fMessageSPI.deleteMessages(GraphMessageSPITest.HORST_ID, OSFields.INBOX_NAME, delIDs);

    // check recipient's in box
    messColl = this.fMessageSPI.getMessages(GraphMessageSPITest.HORST_ID, OSFields.INBOX_NAME,
            null, collOpts, null);
    messages = (List<Map<String, Object>>) messColl.getResults();

    Assert.assertEquals(2, messages.size());
    found = false;
    for (final Map<String, Object> m : messages) {
      if (m.get(GraphMessageSPITest.TITLE_FIELD).equals("testmessage1")) {
        found = true;
      }
    }
    Assert.assertFalse(found);
  }
}
