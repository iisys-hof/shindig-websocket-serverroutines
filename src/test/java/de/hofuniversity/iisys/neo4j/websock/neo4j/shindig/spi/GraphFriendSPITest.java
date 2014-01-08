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
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Test routine to check whether the friendship service implementation is working correctly.
 */
public class GraphFriendSPITest {
  private static final String ID_FIELD = "id";

  private static final String JOHN_ID = "john.doe", JANE_ID = "jane.doe", JACK_ID = "jack.doe",
          HORST_ID = "horst", FRED_ID = "FRED";

  private GraphDatabaseService fDb;
  private GraphPersonSPI fPersonSPI;
  private GraphFriendSPI fFriendSPI;

  /**
   * Sets up an impermanent database with some test data and person and friend services for testing
   * purposes.
   */
  @Before
  public void setupService() {
    final TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
    this.fDb = fact.newImpermanentDatabase();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (GraphFriendSPITest.this.fDb != null) {
          GraphFriendSPITest.this.fDb.shutdown();
        }
      }
    });

    this.fPersonSPI = new GraphPersonSPI(this.fDb, new GraphConfig(true), new ImplUtil(
            BasicBSONList.class, BasicBSONObject.class));
    this.fFriendSPI = new GraphFriendSPI(this.fDb, this.fPersonSPI, new ImplUtil(
            BasicBSONList.class, BasicBSONObject.class));

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
    johndoe.setProperty(GraphFriendSPITest.ID_FIELD, GraphFriendSPITest.JOHN_ID);
    personNodes.add(johndoe, GraphFriendSPITest.ID_FIELD, GraphFriendSPITest.JOHN_ID);

    final Node janedoe = this.fDb.createNode();
    janedoe.setProperty(GraphFriendSPITest.ID_FIELD, GraphFriendSPITest.JANE_ID);
    personNodes.add(janedoe, GraphFriendSPITest.ID_FIELD, GraphFriendSPITest.JANE_ID);

    final Node jackdoe = this.fDb.createNode();
    jackdoe.setProperty(GraphFriendSPITest.ID_FIELD, GraphFriendSPITest.JACK_ID);
    personNodes.add(jackdoe, GraphFriendSPITest.ID_FIELD, GraphFriendSPITest.JACK_ID);

    final Node horst = this.fDb.createNode();
    horst.setProperty(GraphFriendSPITest.ID_FIELD, GraphFriendSPITest.HORST_ID);
    personNodes.add(horst, GraphFriendSPITest.ID_FIELD, GraphFriendSPITest.HORST_ID);

    final Node fred = this.fDb.createNode();
    fred.setProperty(GraphFriendSPITest.ID_FIELD, GraphFriendSPITest.FRED_ID);
    personNodes.add(fred, GraphFriendSPITest.ID_FIELD, GraphFriendSPITest.FRED_ID);

    // inter-person relations
    johndoe.createRelationshipTo(janedoe, Neo4jRelTypes.FRIEND_OF);
    janedoe.createRelationshipTo(johndoe, Neo4jRelTypes.FRIEND_OF);

    johndoe.createRelationshipTo(jackdoe, Neo4jRelTypes.FRIEND_OF);
    jackdoe.createRelationshipTo(johndoe, Neo4jRelTypes.FRIEND_OF);

    janedoe.createRelationshipTo(jackdoe, Neo4jRelTypes.FRIEND_OF);
    jackdoe.createRelationshipTo(janedoe, Neo4jRelTypes.FRIEND_OF);

    janedoe.createRelationshipTo(horst, Neo4jRelTypes.FRIEND_REQUEST);
    jackdoe.createRelationshipTo(horst, Neo4jRelTypes.FRIEND_REQUEST);

    trans.success();
    trans.finish();
  }

  /**
   * Test routine for the retrieval of people from whom the requesting user has pending friend
   * requests.
   *
   * @throws Exception
   *           if a test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void getRequestsTest() throws Exception {
    final Map<String, Object> collOpts = new HashMap<String, Object>();

    ListResult peopleColl = null;
    List<Map<String, Object>> people = null;

    // no requests
    peopleColl = this.fFriendSPI.getRequests(GraphFriendSPITest.JOHN_ID, collOpts, null);

    Assert.assertEquals(0, peopleColl.getSize());

    // existing requests
    peopleColl = this.fFriendSPI.getRequests(GraphFriendSPITest.HORST_ID, collOpts, null);
    people = (List<Map<String, Object>>) peopleColl.getResults();

    Assert.assertEquals(2, people.size());

    boolean janeFound = false;
    boolean jackFound = false;
    for (final Map<String, Object> p : people) {
      if (p.get(GraphFriendSPITest.ID_FIELD).equals(GraphFriendSPITest.JANE_ID)) {
        janeFound = true;
      } else if (p.get(GraphFriendSPITest.ID_FIELD).equals(GraphFriendSPITest.JACK_ID)) {
        jackFound = true;
      }
    }
    Assert.assertTrue(janeFound);
    Assert.assertTrue(jackFound);
  }

  /**
   * Test routine for making and accepting friendship requests.
   *
   * @throws Exception
   *           if a test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void requestTest() throws Exception {
    final Map<String, Object> collOpts = new HashMap<String, Object>();

    ListResult peopleColl = null;
    List<Map<String, Object>> people = null;

    // request to self - no action
    this.fFriendSPI.requestFriendship(GraphFriendSPITest.FRED_ID, GraphFriendSPITest.FRED_ID);

    peopleColl = this.fFriendSPI.getRequests(GraphFriendSPITest.FRED_ID, collOpts, null);
    Assert.assertEquals(0, peopleColl.getSize());

    // create request
    this.fFriendSPI.requestFriendship(GraphFriendSPITest.HORST_ID, GraphFriendSPITest.FRED_ID);

    peopleColl = this.fFriendSPI.getRequests(GraphFriendSPITest.FRED_ID, collOpts, null);
    people = (List<Map<String, Object>>) peopleColl.getResults();

    Assert.assertEquals(1, people.size());
    Assert.assertEquals(GraphFriendSPITest.HORST_ID, people.get(0).get(GraphFriendSPITest.ID_FIELD));

    // request already exists - no action
    this.fFriendSPI.requestFriendship(GraphFriendSPITest.HORST_ID, GraphFriendSPITest.FRED_ID);

    peopleColl = this.fFriendSPI.getRequests(GraphFriendSPITest.FRED_ID, collOpts, null);
    people = (List<Map<String, Object>>) peopleColl.getResults();

    Assert.assertEquals(1, people.size());
    Assert.assertEquals(GraphFriendSPITest.HORST_ID, people.get(0).get(GraphFriendSPITest.ID_FIELD));

    // accept request
    this.fFriendSPI.requestFriendship(GraphFriendSPITest.FRED_ID, GraphFriendSPITest.HORST_ID);

    peopleColl = this.fFriendSPI.getRequests(GraphFriendSPITest.FRED_ID, collOpts, null);
    Assert.assertEquals(0, peopleColl.getSize());

    final List<String> userIds = new ArrayList<String>();
    userIds.add(GraphFriendSPITest.HORST_ID);
    peopleColl = this.fPersonSPI.getPeople(userIds, "@friends", collOpts, null);
    people = (List<Map<String, Object>>) peopleColl.getResults();

    Assert.assertEquals(1, people.size());
    Assert.assertEquals(GraphFriendSPITest.FRED_ID, people.get(0).get(GraphFriendSPITest.ID_FIELD));

    // friendship already exists - no action
    this.fFriendSPI.requestFriendship(GraphFriendSPITest.HORST_ID, GraphFriendSPITest.HORST_ID);

    peopleColl = this.fFriendSPI.getRequests(GraphFriendSPITest.FRED_ID, collOpts, null);
    Assert.assertEquals(0, peopleColl.getSize());
  }

  /**
   * Test routine for denying and deleting friendships.
   *
   * @throws Exception
   *           if a test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void denyTest() throws Exception {
    final Map<String, Object> collOpts = new HashMap<String, Object>();

    ListResult peopleColl = null;
    List<Map<String, Object>> people = null;

    // no request or friendship - no action
    this.fFriendSPI.denyFriendship(GraphFriendSPITest.HORST_ID, GraphFriendSPITest.FRED_ID);

    // deny request
    this.fFriendSPI.denyFriendship(GraphFriendSPITest.HORST_ID, GraphFriendSPITest.JANE_ID);

    peopleColl = this.fFriendSPI.getRequests(GraphFriendSPITest.HORST_ID, collOpts, null);
    people = (List<Map<String, Object>>) peopleColl.getResults();

    Assert.assertEquals(1, people.size());
    Assert.assertEquals(GraphFriendSPITest.JACK_ID, people.get(0).get(GraphFriendSPITest.ID_FIELD));

    // terminate friendship
    this.fFriendSPI.denyFriendship(GraphFriendSPITest.JANE_ID, GraphFriendSPITest.JACK_ID);

    final List<String> userIds = new ArrayList<String>();
    userIds.add(GraphFriendSPITest.JANE_ID);
    peopleColl = this.fPersonSPI.getPeople(userIds, "@friends", collOpts, null);
    people = (List<Map<String, Object>>) peopleColl.getResults();

    Assert.assertEquals(1, people.size());

    boolean deleted = true;
    for (final Map<String, Object> p : people) {
      if (p.get(GraphFriendSPITest.ID_FIELD).equals(GraphFriendSPITest.JACK_ID)) {
        deleted = false;
      }
    }
    Assert.assertTrue(deleted);
  }
}
