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
import java.util.HashSet;
import java.util.List;
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

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Test for the IGraphService implementation of the graph back-end.
 */
public class GraphSPITest {
  private static final String ID_FIELD = "id";

  private static final String JOHN_ID = "john.doe", JANE_ID = "jane.doe", JACK_ID = "jack.doe",
          HORST_ID = "horst", HANS_ID = "hans", HANNO_ID = "hanno", BOGUS_ID = "bogus";

  private static final String DOE_FAM_ID = "fam.doe.group", DENKER_ID = "denker",
          POKER_ID = "poker";

  private GraphDatabaseService fDb;
  private GraphPersonSPI fPersonSPI;
  private GraphSPI fGraphSPI;

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
        if (GraphSPITest.this.fDb != null) {
          GraphSPITest.this.fDb.shutdown();
        }
      }
    });

    this.fPersonSPI = new GraphPersonSPI(this.fDb, new GraphConfig(true), new ImplUtil(
            BasicBSONList.class, BasicBSONObject.class));
    this.fGraphSPI = new GraphSPI(this.fPersonSPI, new ImplUtil(BasicBSONList.class,
            BasicBSONObject.class));

    createTestData();
  }

  @After
  public void stopDatabase() {
    this.fDb.shutdown();
  }

  private void createTestData() {
    final Index<Node> personNodes = this.fDb.index().forNodes(ShindigConstants.PERSON_NODES);
    final Index<Node> groupNodes = this.fDb.index().forNodes(ShindigConstants.GROUP_NODES);

    final Transaction trans = this.fDb.beginTx();

    // groups
    final Node famGroup = this.fDb.createNode();
    famGroup.setProperty(GraphSPITest.ID_FIELD, GraphSPITest.DOE_FAM_ID);
    groupNodes.add(famGroup, GraphSPITest.ID_FIELD, GraphSPITest.DOE_FAM_ID);

    final Node denkGroup = this.fDb.createNode();
    denkGroup.setProperty(GraphSPITest.ID_FIELD, GraphSPITest.DENKER_ID);
    groupNodes.add(denkGroup, GraphSPITest.ID_FIELD, GraphSPITest.DENKER_ID);

    final Node pokerGroup = this.fDb.createNode();
    pokerGroup.setProperty(GraphSPITest.ID_FIELD, GraphSPITest.POKER_ID);
    groupNodes.add(pokerGroup, GraphSPITest.ID_FIELD, GraphSPITest.POKER_ID);

    // people
    final Node johndoe = this.fDb.createNode();
    johndoe.setProperty(GraphSPITest.ID_FIELD, GraphSPITest.JOHN_ID);
    personNodes.add(johndoe, GraphSPITest.ID_FIELD, GraphSPITest.JOHN_ID);

    final Node janedoe = this.fDb.createNode();
    janedoe.setProperty(GraphSPITest.ID_FIELD, GraphSPITest.JANE_ID);
    personNodes.add(janedoe, GraphSPITest.ID_FIELD, GraphSPITest.JANE_ID);

    final Node jackdoe = this.fDb.createNode();
    jackdoe.setProperty(GraphSPITest.ID_FIELD, GraphSPITest.JACK_ID);
    personNodes.add(jackdoe, GraphSPITest.ID_FIELD, GraphSPITest.JACK_ID);

    final Node horst = this.fDb.createNode();
    horst.setProperty(GraphSPITest.ID_FIELD, GraphSPITest.HORST_ID);
    personNodes.add(horst, GraphSPITest.ID_FIELD, GraphSPITest.HORST_ID);

    final Node hans = this.fDb.createNode();
    hans.setProperty(GraphSPITest.ID_FIELD, GraphSPITest.HANS_ID);
    personNodes.add(hans, GraphSPITest.ID_FIELD, GraphSPITest.HANS_ID);

    final Node hanno = this.fDb.createNode();
    hanno.setProperty(GraphSPITest.ID_FIELD, GraphSPITest.HANNO_ID);
    personNodes.add(hanno, GraphSPITest.ID_FIELD, GraphSPITest.HANNO_ID);

    final Node bogus = this.fDb.createNode();
    bogus.setProperty(GraphSPITest.ID_FIELD, GraphSPITest.BOGUS_ID);
    personNodes.add(bogus, GraphSPITest.ID_FIELD, GraphSPITest.BOGUS_ID);

    // inter-person relations
    johndoe.createRelationshipTo(janedoe, Neo4jRelTypes.FRIEND_OF);
    janedoe.createRelationshipTo(johndoe, Neo4jRelTypes.FRIEND_OF);

    johndoe.createRelationshipTo(jackdoe, Neo4jRelTypes.FRIEND_OF);
    jackdoe.createRelationshipTo(johndoe, Neo4jRelTypes.FRIEND_OF);

    janedoe.createRelationshipTo(jackdoe, Neo4jRelTypes.FRIEND_OF);
    jackdoe.createRelationshipTo(janedoe, Neo4jRelTypes.FRIEND_OF);

    horst.createRelationshipTo(janedoe, Neo4jRelTypes.FRIEND_OF);
    janedoe.createRelationshipTo(horst, Neo4jRelTypes.FRIEND_OF);

    horst.createRelationshipTo(hans, Neo4jRelTypes.FRIEND_OF);
    hans.createRelationshipTo(horst, Neo4jRelTypes.FRIEND_OF);

    hans.createRelationshipTo(hanno, Neo4jRelTypes.FRIEND_OF);
    hanno.createRelationshipTo(hans, Neo4jRelTypes.FRIEND_OF);

    // group relations
    johndoe.createRelationshipTo(famGroup, Neo4jRelTypes.OWNER_OF);
    johndoe.createRelationshipTo(famGroup, Neo4jRelTypes.MEMBER_OF);
    janedoe.createRelationshipTo(famGroup, Neo4jRelTypes.MEMBER_OF);
    jackdoe.createRelationshipTo(famGroup, Neo4jRelTypes.MEMBER_OF);

    horst.createRelationshipTo(denkGroup, Neo4jRelTypes.OWNER_OF);
    horst.createRelationshipTo(denkGroup, Neo4jRelTypes.MEMBER_OF);
    hans.createRelationshipTo(denkGroup, Neo4jRelTypes.MEMBER_OF);

    hans.createRelationshipTo(pokerGroup, Neo4jRelTypes.OWNER_OF);
    hans.createRelationshipTo(pokerGroup, Neo4jRelTypes.MEMBER_OF);
    jackdoe.createRelationshipTo(pokerGroup, Neo4jRelTypes.MEMBER_OF);
    horst.createRelationshipTo(pokerGroup, Neo4jRelTypes.MEMBER_OF);

    trans.success();
    trans.finish();
  }

  /**
   * Tests the retrieval of friends of friends up to a certain depth.
   *
   * @throws Exception
   *           if the test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void friendsOfFriendsTest() throws Exception {
    final Map<String, Object> collOpts = new HashMap<String, Object>();
    final List<String> userIds = new ArrayList<String>();
    List<Map<String, Object>> results = null;
    final Set<String> foundIds = new HashSet<String>();

    // full traversal with max depth
    userIds.add(GraphSPITest.HANNO_ID);
    results = (List<Map<String, Object>>) this.fGraphSPI.getFriendsOfFriends(userIds, 10, false,
            collOpts, null).getResults();

    // add IDs
    for (final Map<String, Object> p : results) {
      foundIds.add(p.get(GraphSPITest.ID_FIELD).toString());
    }

    Assert.assertEquals(5, results.size());
    Assert.assertTrue(foundIds.contains(GraphSPITest.JOHN_ID));
    Assert.assertTrue(foundIds.contains(GraphSPITest.JANE_ID));
    Assert.assertTrue(foundIds.contains(GraphSPITest.JACK_ID));
    Assert.assertTrue(foundIds.contains(GraphSPITest.HORST_ID));
    Assert.assertTrue(foundIds.contains(GraphSPITest.HANS_ID));

    // multiple users, unknown people
    userIds.clear();
    userIds.add(GraphSPITest.JANE_ID);
    userIds.add(GraphSPITest.JACK_ID);
    userIds.add(GraphSPITest.JOHN_ID);
    results = (List<Map<String, Object>>) this.fGraphSPI.getFriendsOfFriends(userIds, 2, true,
            collOpts, null).getResults();

    // add IDs
    foundIds.clear();
    for (final Map<String, Object> p : results) {
      foundIds.add(p.get(GraphSPITest.ID_FIELD).toString());
    }

    Assert.assertEquals(1, results.size());
    Assert.assertTrue(foundIds.contains(GraphSPITest.HANS_ID));
  }

  /**
   * Tests the search for a shortest path between two people.
   *
   * @throws Exception
   *           if the test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void shortestPathTest() throws Exception {
    List<Map<String, Object>> results = null;
    final Map<String, Object> collOpts = new HashMap<String, Object>();

    // full path test
    results = (List<Map<String, Object>>) this.fGraphSPI.getShortestPath(GraphSPITest.JOHN_ID,
            GraphSPITest.HANNO_ID, collOpts, null).getResults();

    Assert.assertEquals(5, results.size());

    // check for correct path
    Assert.assertEquals(GraphSPITest.JOHN_ID, results.get(0).get(GraphSPITest.ID_FIELD));
    Assert.assertEquals(GraphSPITest.JANE_ID, results.get(1).get(GraphSPITest.ID_FIELD));
    Assert.assertEquals(GraphSPITest.HORST_ID, results.get(2).get(GraphSPITest.ID_FIELD));
    Assert.assertEquals(GraphSPITest.HANS_ID, results.get(3).get(GraphSPITest.ID_FIELD));
    Assert.assertEquals(GraphSPITest.HANNO_ID, results.get(4).get(GraphSPITest.ID_FIELD));

    // single link
    results = (List<Map<String, Object>>) this.fGraphSPI.getShortestPath(GraphSPITest.HANS_ID,
            GraphSPITest.HORST_ID, collOpts, null).getResults();

    Assert.assertEquals(2, results.size());
    Assert.assertEquals(GraphSPITest.HANS_ID, results.get(0).get(GraphSPITest.ID_FIELD));
    Assert.assertEquals(GraphSPITest.HORST_ID, results.get(1).get(GraphSPITest.ID_FIELD));

    // identity
    results = (List<Map<String, Object>>) this.fGraphSPI.getShortestPath(GraphSPITest.HANS_ID,
            GraphSPITest.HANS_ID, collOpts, null).getResults();

    Assert.assertTrue(results == null || results.isEmpty());

    // no path
    results = (List<Map<String, Object>>) this.fGraphSPI.getShortestPath(GraphSPITest.HANS_ID,
            GraphSPITest.BOGUS_ID, collOpts, null).getResults();

    Assert.assertTrue(results == null || results.isEmpty());
  }

  /**
   * Tests the group suggestion method based on friends' memberships.
   *
   * @throws Exception
   *           if the test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void groupSuggestionTest() throws Exception {
    final Set<String> foundIds = new HashSet<String>();
    List<Map<String, Object>> results = null;
    final Map<String, Object> collOpts = new HashMap<String, Object>();

    // most
    results = (List<Map<String, Object>>) this.fGraphSPI.getGroupRecommendation(
            GraphSPITest.JANE_ID, 1, collOpts, null).getResults();

    for (final Map<String, Object> g : results) {
      foundIds.add(g.get(GraphSPITest.ID_FIELD).toString());
    }

    Assert.assertEquals(2, results.size());
    Assert.assertTrue(foundIds.contains(GraphSPITest.DENKER_ID));
    Assert.assertTrue(foundIds.contains(GraphSPITest.POKER_ID));

    // none
    results = (List<Map<String, Object>>) this.fGraphSPI.getGroupRecommendation(
            GraphSPITest.HANS_ID, 1, collOpts, null).getResults();

    Assert.assertEquals(0, results.size());
  }

  /**
   * Tests the friend suggestion method based on friends' friends.
   *
   * @throws Exception
   *           if the test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void friendSuggestionTest() throws Exception {
    List<Map<String, Object>> results = null;
    final Map<String, Object> collOpts = new HashMap<String, Object>();

    // only one connected person
    results = (List<Map<String, Object>>) this.fGraphSPI.getFriendRecommendation(
            GraphSPITest.JANE_ID, 1, collOpts, null).getResults();

    Assert.assertEquals(1, results.size());
    Assert.assertEquals(GraphSPITest.HANS_ID, results.get(0).get(GraphSPITest.ID_FIELD));

    // no suggestions possible
    results = (List<Map<String, Object>>) this.fGraphSPI.getFriendRecommendation(
            GraphSPITest.BOGUS_ID, 1, collOpts, null).getResults();

    Assert.assertEquals(0, results.size());
  }
}
