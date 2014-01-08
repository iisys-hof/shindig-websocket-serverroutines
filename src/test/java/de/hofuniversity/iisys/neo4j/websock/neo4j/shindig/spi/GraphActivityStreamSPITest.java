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
 * Test for the ActivityStreamService implementation of the graph back-end.
 */
public class GraphActivityStreamSPITest {
  private static final String ID_FIELD = "id";
  private static final String NAME_FIELD = "displayName";
  private static final String TITLE_FIELD = "title";
  private static final String VERB_FIELD = "verb";
  private static final String TYPE_FIELD = "objectType";
  private static final String ACTOR_FIELD = "actor";
  private static final String OBJECT_FIELD = "object";
  private static final String TARGET_FIELD = "target";
  private static final String GENERATOR_FIELD = "generator";

  private static final String JOHN_ID = "john.doe", JANE_ID = "jane.doe", HORST_ID = "horst";

  private GraphDatabaseService fDb;
  private GraphPersonSPI fPersonSPI;
  private ActivityObjectService fObjectSPI;
  private ApplicationService fAppSPI;
  private GraphActivityStreamSPI fActivityStreamSPI;

  /**
   * Sets up an impermanent database with some test data, a person service and an activity stream
   * service for testing purposes.
   */
  @Before
  public void setupService() {
    final TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
    this.fDb = fact.newImpermanentDatabase();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (GraphActivityStreamSPITest.this.fDb != null) {
          GraphActivityStreamSPITest.this.fDb.shutdown();
        }
      }
    });

    final GraphConfig config = new GraphConfig(true);
    config.setProperty("activityobjects.deduplicate", "true");

    this.fPersonSPI = new GraphPersonSPI(this.fDb, config, new ImplUtil(BasicBSONList.class,
            BasicBSONObject.class));
    this.fObjectSPI = new ActivityObjectService(this.fDb, config, new ImplUtil(BasicBSONList.class,
            BasicBSONObject.class));
    this.fAppSPI = new ApplicationService(this.fDb);
    this.fActivityStreamSPI = new GraphActivityStreamSPI(this.fDb, this.fPersonSPI,
            this.fObjectSPI, this.fAppSPI, new IDManager(this.fDb), new ImplUtil(
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
    johndoe.setProperty(GraphActivityStreamSPITest.ID_FIELD, GraphActivityStreamSPITest.JOHN_ID);
    personNodes.add(johndoe, GraphActivityStreamSPITest.ID_FIELD,
            GraphActivityStreamSPITest.JOHN_ID);

    final Node janedoe = this.fDb.createNode();
    janedoe.setProperty(GraphActivityStreamSPITest.ID_FIELD, GraphActivityStreamSPITest.JANE_ID);
    personNodes.add(janedoe, GraphActivityStreamSPITest.ID_FIELD,
            GraphActivityStreamSPITest.JANE_ID);

    final Node horst = this.fDb.createNode();
    horst.setProperty(GraphActivityStreamSPITest.ID_FIELD, GraphActivityStreamSPITest.HORST_ID);
    personNodes
            .add(horst, GraphActivityStreamSPITest.ID_FIELD, GraphActivityStreamSPITest.HORST_ID);

    // activity objects
    final Node johnObject = this.fDb.createNode();
    johnObject.setProperty(GraphActivityStreamSPITest.ID_FIELD, GraphActivityStreamSPITest.JOHN_ID);
    johnObject.setProperty(GraphActivityStreamSPITest.NAME_FIELD, "John Doe");

    final Node janeObject = this.fDb.createNode();
    janeObject.setProperty(GraphActivityStreamSPITest.ID_FIELD, GraphActivityStreamSPITest.JANE_ID);
    janeObject.setProperty(GraphActivityStreamSPITest.NAME_FIELD, "Jane Doe");

    final Node horstObject = this.fDb.createNode();
    horstObject.setProperty(GraphActivityStreamSPITest.ID_FIELD,
            GraphActivityStreamSPITest.HORST_ID);
    horstObject.setProperty(GraphActivityStreamSPITest.NAME_FIELD, "Horst");

    final Node vacObject = this.fDb.createNode();
    vacObject.setProperty(GraphActivityStreamSPITest.ID_FIELD, "vacancy");
    vacObject.setProperty(GraphActivityStreamSPITest.NAME_FIELD, "Stellenanzeige");

    final Node appObject = this.fDb.createNode();
    appObject.setProperty(GraphActivityStreamSPITest.ID_FIELD, "application");
    appObject.setProperty(GraphActivityStreamSPITest.NAME_FIELD, "Bewerbung");

    // activity entries
    final Node hireJaneAct = this.fDb.createNode();
    hireJaneAct.setProperty(GraphActivityStreamSPITest.ID_FIELD, "1");
    hireJaneAct.setProperty(GraphActivityStreamSPITest.TITLE_FIELD, "Einstellung");
    hireJaneAct.setProperty(GraphActivityStreamSPITest.VERB_FIELD, "hat eingestellt");

    hireJaneAct.createRelationshipTo(johnObject, Neo4jRelTypes.ACTOR);
    hireJaneAct.createRelationshipTo(janeObject, Neo4jRelTypes.TARGET);

    johndoe.createRelationshipTo(hireJaneAct, Neo4jRelTypes.ACTED);

    final Node postVacAct = this.fDb.createNode();
    postVacAct.setProperty(GraphActivityStreamSPITest.ID_FIELD, "2");
    postVacAct.setProperty(GraphActivityStreamSPITest.TITLE_FIELD, "neue Stellenanzeige");
    postVacAct.setProperty(GraphActivityStreamSPITest.VERB_FIELD, "erstellte");

    postVacAct.createRelationshipTo(janeObject, Neo4jRelTypes.ACTOR);
    postVacAct.createRelationshipTo(vacObject, Neo4jRelTypes.TARGET);

    janedoe.createRelationshipTo(postVacAct, Neo4jRelTypes.ACTED);

    final Node applicAct = this.fDb.createNode();
    applicAct.setProperty(GraphActivityStreamSPITest.ID_FIELD, "3");
    applicAct.setProperty(GraphActivityStreamSPITest.TITLE_FIELD, "Bewerbung");
    applicAct.setProperty(GraphActivityStreamSPITest.VERB_FIELD, "versendete");

    applicAct.createRelationshipTo(horstObject, Neo4jRelTypes.ACTOR);
    applicAct.createRelationshipTo(appObject, Neo4jRelTypes.OBJECT);
    applicAct.createRelationshipTo(janeObject, Neo4jRelTypes.TARGET);

    horst.createRelationshipTo(applicAct, Neo4jRelTypes.ACTED);

    final Node hireHorstAct = this.fDb.createNode();
    hireHorstAct.setProperty(GraphActivityStreamSPITest.ID_FIELD, "4");
    hireHorstAct.setProperty(GraphActivityStreamSPITest.TITLE_FIELD, "Einstellung");
    hireHorstAct.setProperty(GraphActivityStreamSPITest.VERB_FIELD, "hat eingestellt");

    hireHorstAct.createRelationshipTo(janeObject, Neo4jRelTypes.ACTOR);
    hireHorstAct.createRelationshipTo(horstObject, Neo4jRelTypes.TARGET);

    janedoe.createRelationshipTo(hireHorstAct, Neo4jRelTypes.ACTED);

    final Node welcomeAct = this.fDb.createNode();
    welcomeAct.setProperty(GraphActivityStreamSPITest.ID_FIELD, "5");
    welcomeAct.setProperty(GraphActivityStreamSPITest.TITLE_FIELD, "Neuer Mitarbeiter");
    welcomeAct.setProperty(GraphActivityStreamSPITest.VERB_FIELD, "heißt willkommen");

    welcomeAct.createRelationshipTo(johnObject, Neo4jRelTypes.ACTOR);
    welcomeAct.createRelationshipTo(horstObject, Neo4jRelTypes.TARGET);

    johndoe.createRelationshipTo(welcomeAct, Neo4jRelTypes.ACTED);

    final Node welcomeAct2 = this.fDb.createNode();
    welcomeAct2.setProperty(GraphActivityStreamSPITest.ID_FIELD, "6");
    welcomeAct2.setProperty(GraphActivityStreamSPITest.TITLE_FIELD, "Neuer Mitarbeiter");
    welcomeAct2.setProperty(GraphActivityStreamSPITest.VERB_FIELD, "heißt willkommen");

    welcomeAct2.createRelationshipTo(janeObject, Neo4jRelTypes.ACTOR);
    welcomeAct2.createRelationshipTo(horstObject, Neo4jRelTypes.TARGET);

    janedoe.createRelationshipTo(welcomeAct2, Neo4jRelTypes.ACTED);

    trans.success();
    trans.finish();
  }

  /**
   * Tests retrieval of all activity entries for people.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @Test
  public void allRetrievalTest() throws Exception {
    final List<String> userIds = new ArrayList<String>();
    userIds.add(GraphActivityStreamSPITest.JOHN_ID);
    userIds.add(GraphActivityStreamSPITest.HORST_ID);

    final ListResult entryColl = this.fActivityStreamSPI.getActivityEntries(userIds, null, null,
            new HashMap<String, Object>(), null);

    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> actEntries = (List<Map<String, Object>>) entryColl.getResults();

    Assert.assertEquals(3, actEntries.size());

    boolean oneFound = false;
    boolean threeFound = false;
    boolean fiveFound = false;

    String id = null;
    for (final Map<String, Object> entry : actEntries) {
      id = entry.get(GraphActivityStreamSPITest.ID_FIELD).toString();

      if (id.equals("1")) {
        oneFound = true;
      } else if (id.equals("3")) {
        threeFound = true;
      } else if (id.equals("5")) {
        fiveFound = true;
      } else {
        throw new Exception("unexpected activity entry");
      }
    }
    Assert.assertTrue(oneFound && threeFound && fiveFound);
  }

  /**
   * Tests retrieval of certain activity entries for a person.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @Test
  public void idSetRetrievalTest() throws Exception {
    final List<String> actIds = new ArrayList<String>();
    actIds.add("2");
    actIds.add("4");

    final ListResult entryColl = this.fActivityStreamSPI.getActivityEntries(
            GraphActivityStreamSPITest.JANE_ID, null, null, new HashMap<String, Object>(), null,
            actIds);

    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> actEntries = (List<Map<String, Object>>) entryColl.getResults();

    Assert.assertEquals(2, actEntries.size());

    boolean twoFound = false;
    boolean fourFound = false;

    String id = null;
    for (final Map<String, Object> entry : actEntries) {
      id = entry.get(GraphActivityStreamSPITest.ID_FIELD).toString();

      if (id.equals("2")) {
        twoFound = true;
      } else if (id.equals("4")) {
        fourFound = true;
      } else {
        throw new Exception("unexpected activity entry");
      }
    }
    Assert.assertTrue(twoFound && fourFound);
  }

  /**
   * Tests retrieval of certain activity entries for a person. Also checks for proper conversion.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @Test
  public void singleRetrievalTest() throws Exception {
    final SingleResult entryFut = this.fActivityStreamSPI.getActivityEntry(
            GraphActivityStreamSPITest.JOHN_ID, null, null, null, "5");
    final Map<String, ?> entry = entryFut.getResults();
    Assert.assertNotNull(entry);
    Assert.assertEquals("5", entry.get(GraphActivityStreamSPITest.ID_FIELD));

    Assert.assertNotNull(entry.get(GraphActivityStreamSPITest.ACTOR_FIELD));
    Assert.assertNotNull(entry.get(GraphActivityStreamSPITest.TARGET_FIELD));
  }

  /**
   * Tests the creation of activity entries for a person. Also checks for proper conversion.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @SuppressWarnings("unchecked")
  @Test
  public void creationTest() throws Exception {
    Map<String, Object> activity = new HashMap<String, Object>();
    activity.put(GraphActivityStreamSPITest.VERB_FIELD, "create");

    Map<String, Object> object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, GraphActivityStreamSPITest.JANE_ID);
    object.put(GraphActivityStreamSPITest.NAME_FIELD, "Jane Doe");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "person");
    activity.put(GraphActivityStreamSPITest.ACTOR_FIELD, object);

    object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, "testobject");
    object.put(GraphActivityStreamSPITest.NAME_FIELD, "Testdatei");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "file");
    activity.put(GraphActivityStreamSPITest.OBJECT_FIELD, object);

    object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, "testapp");
    object.put(GraphActivityStreamSPITest.NAME_FIELD, "Testanwendung");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "application");
    activity.put(GraphActivityStreamSPITest.TARGET_FIELD, object);
    activity.put(GraphActivityStreamSPITest.GENERATOR_FIELD, object);

    SingleResult entryFut = this.fActivityStreamSPI.createActivityEntry(
            GraphActivityStreamSPITest.JANE_ID, null, "testapp", activity, null);

    // check result
    activity = (Map<String, Object>) entryFut.getResults();
    final String firstId = activity.get(GraphActivityStreamSPITest.ID_FIELD).toString();
    Assert.assertNotNull(firstId);
    Assert.assertEquals("create", activity.get(GraphActivityStreamSPITest.VERB_FIELD));

    object = (Map<String, Object>) activity.get(GraphActivityStreamSPITest.ACTOR_FIELD);
    Assert.assertEquals(GraphActivityStreamSPITest.JANE_ID,
            object.get(GraphActivityStreamSPITest.ID_FIELD));

    object = (Map<String, Object>) activity.get(GraphActivityStreamSPITest.OBJECT_FIELD);
    Assert.assertEquals("testobject", object.get(GraphActivityStreamSPITest.ID_FIELD));

    object = (Map<String, Object>) activity.get(GraphActivityStreamSPITest.TARGET_FIELD);
    Assert.assertEquals("testapp", object.get(GraphActivityStreamSPITest.ID_FIELD));

    object = (Map<String, Object>) activity.get(GraphActivityStreamSPITest.GENERATOR_FIELD);
    Assert.assertEquals("testapp", object.get(GraphActivityStreamSPITest.ID_FIELD));

    // creation of second entry, potentially reusing objects
    activity = new HashMap<String, Object>();
    activity.put(GraphActivityStreamSPITest.VERB_FIELD, "view");

    object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, GraphActivityStreamSPITest.HORST_ID);
    object.put(GraphActivityStreamSPITest.NAME_FIELD, "Horst");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "person");
    activity.put(GraphActivityStreamSPITest.ACTOR_FIELD, object);

    object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, "testobject");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "file");
    activity.put(GraphActivityStreamSPITest.OBJECT_FIELD, object);

    object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, "testapp");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "application");
    activity.put(GraphActivityStreamSPITest.GENERATOR_FIELD, object);

    entryFut = this.fActivityStreamSPI.createActivityEntry(GraphActivityStreamSPITest.HORST_ID,
            null, "testapp", activity, null);

    // check result
    activity = (Map<String, Object>) entryFut.getResults();
    Assert.assertNotNull(activity.get(GraphActivityStreamSPITest.ID_FIELD));
    Assert.assertNotSame(firstId, activity.get(GraphActivityStreamSPITest.ID_FIELD));
    Assert.assertEquals("view", activity.get(GraphActivityStreamSPITest.VERB_FIELD));

    object = (Map<String, Object>) activity.get(GraphActivityStreamSPITest.ACTOR_FIELD);
    Assert.assertEquals(GraphActivityStreamSPITest.HORST_ID,
            object.get(GraphActivityStreamSPITest.ID_FIELD));

    object = (Map<String, Object>) activity.get(GraphActivityStreamSPITest.OBJECT_FIELD);
    Assert.assertEquals("Testdatei", object.get(GraphActivityStreamSPITest.NAME_FIELD));

    object = (Map<String, Object>) activity.get(GraphActivityStreamSPITest.GENERATOR_FIELD);
    Assert.assertEquals("Testanwendung", object.get(GraphActivityStreamSPITest.NAME_FIELD));
  }

  /**
   * Tests updating an activity for a person.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @SuppressWarnings("unchecked")
  @Test
  public void updateTest() throws Exception {
    // create activity to update
    Map<String, Object> activity = new HashMap<String, Object>();
    activity.put(GraphActivityStreamSPITest.VERB_FIELD, "create");

    Map<String, Object> object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, GraphActivityStreamSPITest.JANE_ID);
    object.put(GraphActivityStreamSPITest.NAME_FIELD, "Jane Doe");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "person");
    activity.put(GraphActivityStreamSPITest.ACTOR_FIELD, object);

    object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, "testobject");
    object.put(GraphActivityStreamSPITest.NAME_FIELD, "Testdatei");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "file");
    activity.put(GraphActivityStreamSPITest.OBJECT_FIELD, object);

    object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, "testapp");
    object.put(GraphActivityStreamSPITest.NAME_FIELD, "Testanwendung");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "application");
    activity.put(GraphActivityStreamSPITest.TARGET_FIELD, object);
    activity.put(GraphActivityStreamSPITest.GENERATOR_FIELD, object);

    SingleResult entryFut = this.fActivityStreamSPI.createActivityEntry(
            GraphActivityStreamSPITest.JANE_ID, null, "testapp", activity, null);
    final String id = entryFut.getResults().get(GraphActivityStreamSPITest.ID_FIELD).toString();

    // change some values
    activity.put(GraphActivityStreamSPITest.VERB_FIELD, "update");

    object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, GraphActivityStreamSPITest.JOHN_ID);
    object.put(GraphActivityStreamSPITest.NAME_FIELD, "John Doe");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "person");
    activity.put(GraphActivityStreamSPITest.ACTOR_FIELD, object);

    object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, "testobject2");
    object.put(GraphActivityStreamSPITest.NAME_FIELD, "Testdatei2");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "file");
    activity.put(GraphActivityStreamSPITest.OBJECT_FIELD, object);

    // update
    entryFut = this.fActivityStreamSPI.updateActivityEntry(GraphActivityStreamSPITest.JANE_ID,
            null, "testapp", id, activity, null);

    // check result
    activity = (Map<String, Object>) entryFut.getResults();
    Assert.assertEquals(id, activity.get(GraphActivityStreamSPITest.ID_FIELD));
    Assert.assertEquals("update", activity.get(GraphActivityStreamSPITest.VERB_FIELD));

    object = (Map<String, Object>) activity.get(GraphActivityStreamSPITest.ACTOR_FIELD);
    Assert.assertEquals(GraphActivityStreamSPITest.JOHN_ID,
            object.get(GraphActivityStreamSPITest.ID_FIELD));

    object = (Map<String, Object>) activity.get(GraphActivityStreamSPITest.OBJECT_FIELD);
    Assert.assertEquals("testobject2", object.get(GraphActivityStreamSPITest.ID_FIELD));

    object = (Map<String, Object>) activity.get(GraphActivityStreamSPITest.TARGET_FIELD);
    Assert.assertEquals("testapp", object.get(GraphActivityStreamSPITest.ID_FIELD));

    object = (Map<String, Object>) activity.get(GraphActivityStreamSPITest.GENERATOR_FIELD);
    Assert.assertEquals("testapp", object.get(GraphActivityStreamSPITest.ID_FIELD));
  }

  /**
   * Tests deletion of certain activity entries for a person.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @SuppressWarnings("unchecked")
  @Test
  public void deletionTest() throws Exception {
    // create test activity to delete
    Map<String, Object> activity = new HashMap<String, Object>();
    activity.put(GraphActivityStreamSPITest.VERB_FIELD, "create");

    Map<String, Object> object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, GraphActivityStreamSPITest.JANE_ID);
    object.put(GraphActivityStreamSPITest.NAME_FIELD, "Jane Doe");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "person");
    activity.put(GraphActivityStreamSPITest.ACTOR_FIELD, object);

    object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, "testobject");
    object.put(GraphActivityStreamSPITest.NAME_FIELD, "Testdatei");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "file");
    activity.put(GraphActivityStreamSPITest.OBJECT_FIELD, object);

    object = new HashMap<String, Object>();
    object.put(GraphActivityStreamSPITest.ID_FIELD, "testapp");
    object.put(GraphActivityStreamSPITest.NAME_FIELD, "Testanwendung");
    object.put(GraphActivityStreamSPITest.TYPE_FIELD, "application");
    activity.put(GraphActivityStreamSPITest.TARGET_FIELD, object);
    activity.put(GraphActivityStreamSPITest.GENERATOR_FIELD, object);

    final SingleResult entryFut = this.fActivityStreamSPI.createActivityEntry(
            GraphActivityStreamSPITest.JANE_ID, null, "testapp", activity, null);

    activity = (Map<String, Object>) entryFut.getResults();
    final String id = activity.get(GraphActivityStreamSPITest.ID_FIELD).toString();

    // delete entry
    final List<String> actIDs = new ArrayList<String>();
    actIDs.add(id);

    this.fActivityStreamSPI.deleteActivityEntries(GraphActivityStreamSPITest.JANE_ID, null,
            "testapp", actIDs);

    // check deletion
    activity = (Map<String, Object>) this.fActivityStreamSPI.getActivityEntry(
            GraphActivityStreamSPITest.JANE_ID, null, "testapp", null, id).getResults();
    Assert.assertNull(activity);
  }
}
