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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Test for the AppDataService implementation of the graph back-end.
 */
public class GraphAppDataSPITest {
  private static final String ID_FIELD = "id";

  private static final String JOHN_ID = "john.doe", JANE_ID = "jane.doe", JACK_ID = "jack.doe",
          HORST_ID = "horst";

  private static final String APP1_ID = "app1", APP2_ID = "app2", APP3_ID = "app3";

  private static final String ATT1_ID = "attribute1", ATT2_ID = "attribute2",
          ATT3_ID = "attribute3", ATT4_ID = "attribute4";

  private GraphDatabaseService fDb;
  private GraphPersonSPI fPersonSPI;
  private ApplicationService fApplications;
  private GraphAppDataSPI fAppDataSPI;

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
        if (GraphAppDataSPITest.this.fDb != null) {
          GraphAppDataSPITest.this.fDb.shutdown();
        }
      }
    });

    final Map<String, String> config = new HashMap<String, String>();

    this.fPersonSPI = new GraphPersonSPI(this.fDb, config, new ImplUtil(BasicBSONList.class,
            BasicBSONObject.class));
    this.fApplications = new ApplicationService(this.fDb);
    this.fAppDataSPI = new GraphAppDataSPI(this.fDb, this.fPersonSPI, this.fApplications,
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
    johndoe.setProperty(GraphAppDataSPITest.ID_FIELD, GraphAppDataSPITest.JOHN_ID);
    personNodes.add(johndoe, GraphAppDataSPITest.ID_FIELD, GraphAppDataSPITest.JOHN_ID);

    final Node janedoe = this.fDb.createNode();
    janedoe.setProperty(GraphAppDataSPITest.ID_FIELD, GraphAppDataSPITest.JANE_ID);
    personNodes.add(janedoe, GraphAppDataSPITest.ID_FIELD, GraphAppDataSPITest.JANE_ID);

    final Node jackdoe = this.fDb.createNode();
    jackdoe.setProperty(GraphAppDataSPITest.ID_FIELD, GraphAppDataSPITest.JACK_ID);
    personNodes.add(jackdoe, GraphAppDataSPITest.ID_FIELD, GraphAppDataSPITest.JACK_ID);

    final Node horst = this.fDb.createNode();
    horst.setProperty(GraphAppDataSPITest.ID_FIELD, GraphAppDataSPITest.HORST_ID);
    personNodes.add(horst, GraphAppDataSPITest.ID_FIELD, GraphAppDataSPITest.HORST_ID);

    // application data
    final Node johnApp1Data = this.fDb.createNode();
    johnApp1Data.setProperty(GraphAppDataSPITest.ATT1_ID, "johndoe");
    johnApp1Data.setProperty(GraphAppDataSPITest.ATT2_ID, "father");
    final Relationship johnDataRel1 = johndoe.createRelationshipTo(johnApp1Data,
            Neo4jRelTypes.HAS_DATA);
    johnDataRel1.setProperty(OSFields.APP_ID, GraphAppDataSPITest.APP1_ID);

    final Node johnApp2Data = this.fDb.createNode();
    johnApp2Data.setProperty(GraphAppDataSPITest.ATT1_ID, "john.doe");
    johnApp2Data.setProperty(GraphAppDataSPITest.ATT2_ID, "admin");
    final Relationship johnDataRel2 = johndoe.createRelationshipTo(johnApp2Data,
            Neo4jRelTypes.HAS_DATA);
    johnDataRel2.setProperty(OSFields.APP_ID, GraphAppDataSPITest.APP2_ID);

    final Node janeApp1Data = this.fDb.createNode();
    janeApp1Data.setProperty(GraphAppDataSPITest.ATT1_ID, "janedoe");
    janeApp1Data.setProperty(GraphAppDataSPITest.ATT2_ID, "mother");
    final Relationship janeDataRel1 = janedoe.createRelationshipTo(janeApp1Data,
            Neo4jRelTypes.HAS_DATA);
    janeDataRel1.setProperty(OSFields.APP_ID, GraphAppDataSPITest.APP1_ID);

    final Node janeApp2Data = this.fDb.createNode();
    janeApp2Data.setProperty(GraphAppDataSPITest.ATT1_ID, "jane.doe");
    janeApp2Data.setProperty(GraphAppDataSPITest.ATT2_ID, "manager");
    final Relationship janeDataRel2 = janedoe.createRelationshipTo(janeApp2Data,
            Neo4jRelTypes.HAS_DATA);
    janeDataRel2.setProperty(OSFields.APP_ID, GraphAppDataSPITest.APP2_ID);

    final Node jackApp1Data = this.fDb.createNode();
    jackApp1Data.setProperty(GraphAppDataSPITest.ATT1_ID, "jackdoe");
    jackApp1Data.setProperty(GraphAppDataSPITest.ATT2_ID, "son");
    final Relationship jackDataRel1 = jackdoe.createRelationshipTo(jackApp1Data,
            Neo4jRelTypes.HAS_DATA);
    jackDataRel1.setProperty(OSFields.APP_ID, GraphAppDataSPITest.APP1_ID);

    final Node jackApp2Data = this.fDb.createNode();
    jackApp2Data.setProperty(GraphAppDataSPITest.ATT1_ID, "jack.doe");
    jackApp2Data.setProperty(GraphAppDataSPITest.ATT2_ID, "user");
    final Relationship jackDataRel2 = jackdoe.createRelationshipTo(jackApp2Data,
            Neo4jRelTypes.HAS_DATA);
    jackDataRel2.setProperty(OSFields.APP_ID, GraphAppDataSPITest.APP2_ID);

    final Node horstApp1Data = this.fDb.createNode();
    horstApp1Data.setProperty(GraphAppDataSPITest.ATT1_ID, "horst");
    horstApp1Data.setProperty(GraphAppDataSPITest.ATT2_ID, "unrelated");
    final Relationship horstDataRel1 = horst.createRelationshipTo(horstApp1Data,
            Neo4jRelTypes.HAS_DATA);
    horstDataRel1.setProperty(OSFields.APP_ID, GraphAppDataSPITest.APP1_ID);

    final Node horstApp2Data = this.fDb.createNode();
    horstApp2Data.setProperty(GraphAppDataSPITest.ATT1_ID, "horst");
    horstApp2Data.setProperty(GraphAppDataSPITest.ATT2_ID, "viewer");
    final Relationship horstDataRel2 = horst.createRelationshipTo(horstApp2Data,
            Neo4jRelTypes.HAS_DATA);
    horstDataRel2.setProperty(OSFields.APP_ID, GraphAppDataSPITest.APP2_ID);

    trans.success();
    trans.finish();
  }

  /**
   * Test for the retrieval of application data.
   *
   * @throws Exception
   *           if the test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void retrievalTest() throws Exception {
    final List<String> userIds = new ArrayList<String>();
    SingleResult result = null;
    Map<String, Map<String, String>> data = null;
    Map<String, String> appData = null;

    // single user
    userIds.add(GraphAppDataSPITest.JOHN_ID);

    result = this.fAppDataSPI.getPersonData(userIds, null, GraphAppDataSPITest.APP1_ID, null);
    data = (Map<String, Map<String, String>>) result.getResults();
    appData = data.get(GraphAppDataSPITest.JOHN_ID);

    Assert.assertEquals(1, data.size());
    Assert.assertEquals(2, appData.size());

    Assert.assertEquals("johndoe", appData.get(GraphAppDataSPITest.ATT1_ID));
    Assert.assertEquals("father", appData.get(GraphAppDataSPITest.ATT2_ID));

    // multiple users
    userIds.add(GraphAppDataSPITest.JANE_ID);
    userIds.add(GraphAppDataSPITest.HORST_ID);

    result = this.fAppDataSPI.getPersonData(userIds, null, GraphAppDataSPITest.APP2_ID, null);
    data = (Map<String, Map<String, String>>) result.getResults();

    Assert.assertEquals(3, data.size());

    appData = data.get(GraphAppDataSPITest.JOHN_ID);
    Assert.assertEquals(2, appData.size());
    Assert.assertEquals("john.doe", appData.get(GraphAppDataSPITest.ATT1_ID));
    Assert.assertEquals("admin", appData.get(GraphAppDataSPITest.ATT2_ID));

    appData = data.get(GraphAppDataSPITest.JANE_ID);
    Assert.assertEquals(2, appData.size());
    Assert.assertEquals("jane.doe", appData.get(GraphAppDataSPITest.ATT1_ID));
    Assert.assertEquals("manager", appData.get(GraphAppDataSPITest.ATT2_ID));

    appData = data.get(GraphAppDataSPITest.HORST_ID);
    Assert.assertEquals(2, appData.size());
    Assert.assertEquals("horst", appData.get(GraphAppDataSPITest.ATT1_ID));
    Assert.assertEquals("viewer", appData.get(GraphAppDataSPITest.ATT2_ID));
  }

  /**
   * Test for the storage of application data.
   *
   * @throws Exception
   *           if the test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void storageTest() throws Exception {
    SingleResult result = null;
    Map<String, Map<String, String>> data = null;
    Map<String, String> appData = null;

    final List<String> userIds = new ArrayList<String>();
    userIds.add(GraphAppDataSPITest.HORST_ID);

    // non-existent attributes
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(GraphAppDataSPITest.ATT3_ID, "firstValue");
    values.put(GraphAppDataSPITest.ATT4_ID, "firstValue");

    this.fAppDataSPI.updatePersonData(GraphAppDataSPITest.HORST_ID, null,
            GraphAppDataSPITest.APP3_ID, values);

    result = this.fAppDataSPI.getPersonData(userIds, null, GraphAppDataSPITest.APP3_ID, null);
    data = (Map<String, Map<String, String>>) result.getResults();
    appData = data.get(GraphAppDataSPITest.HORST_ID);

    Assert.assertEquals(appData.get(GraphAppDataSPITest.ATT3_ID), "firstValue");
    Assert.assertEquals(appData.get(GraphAppDataSPITest.ATT4_ID), "firstValue");

    // update of existing attributes
    values = new HashMap<String, Object>();
    values.put(GraphAppDataSPITest.ATT3_ID, "secondValue");
    values.put(GraphAppDataSPITest.ATT4_ID, "secondValue");

    this.fAppDataSPI.updatePersonData(GraphAppDataSPITest.HORST_ID, null,
            GraphAppDataSPITest.APP3_ID, values);

    result = this.fAppDataSPI.getPersonData(userIds, null, GraphAppDataSPITest.APP3_ID, null);
    data = (Map<String, Map<String, String>>) result.getResults();
    appData = data.get(GraphAppDataSPITest.HORST_ID);

    Assert.assertEquals(appData.get(GraphAppDataSPITest.ATT3_ID), "secondValue");
    Assert.assertEquals(appData.get(GraphAppDataSPITest.ATT4_ID), "secondValue");

    // partial update
    values = new HashMap<String, Object>();
    values.put(GraphAppDataSPITest.ATT3_ID, "thirdValue");

    this.fAppDataSPI.updatePersonData(GraphAppDataSPITest.HORST_ID, null,
            GraphAppDataSPITest.APP3_ID, values);

    result = this.fAppDataSPI.getPersonData(userIds, null, GraphAppDataSPITest.APP3_ID, null);
    data = (Map<String, Map<String, String>>) result.getResults();
    appData = data.get(GraphAppDataSPITest.HORST_ID);

    Assert.assertEquals(appData.get(GraphAppDataSPITest.ATT3_ID), "thirdValue");
    Assert.assertEquals(appData.get(GraphAppDataSPITest.ATT4_ID), "secondValue");

    // TODO: new options, values other than strings
  }

  /**
   * Test for the deletion of application data.
   *
   * @throws Exception
   *           if the test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void deletionTest() throws Exception {
    SingleResult result = null;
    Map<String, Map<String, Object>> data = null;
    Map<String, Object> appData = null;

    final List<String> userIds = new ArrayList<String>();
    userIds.add(GraphAppDataSPITest.HORST_ID);

    final Map<String, Object> values = new HashMap<String, Object>();
    values.put(GraphAppDataSPITest.ATT3_ID, "value");
    values.put(GraphAppDataSPITest.ATT4_ID, "value");

    this.fAppDataSPI.updatePersonData(GraphAppDataSPITest.HORST_ID, null,
            GraphAppDataSPITest.APP3_ID, values);

    // partial deletion
    final List<String> fields = new ArrayList<String>();
    fields.add(GraphAppDataSPITest.ATT3_ID);
    this.fAppDataSPI.deletePersonData(GraphAppDataSPITest.HORST_ID, null,
            GraphAppDataSPITest.APP3_ID, fields);

    result = this.fAppDataSPI.getPersonData(userIds, null, GraphAppDataSPITest.APP3_ID, null);
    data = (Map<String, Map<String, Object>>) result.getResults();
    appData = data.get(GraphAppDataSPITest.HORST_ID);

    Assert.assertEquals(appData.get(GraphAppDataSPITest.ATT3_ID), null);
    Assert.assertEquals(appData.get(GraphAppDataSPITest.ATT4_ID), "value");

    // full deletion
    this.fAppDataSPI.updatePersonData(GraphAppDataSPITest.HORST_ID, null,
            GraphAppDataSPITest.APP3_ID, values);

    this.fAppDataSPI.deletePersonData(GraphAppDataSPITest.HORST_ID, null,
            GraphAppDataSPITest.APP3_ID, null);

    result = this.fAppDataSPI.getPersonData(userIds, null, GraphAppDataSPITest.APP3_ID, null);
    data = (Map<String, Map<String, Object>>) result.getResults();
    appData = data.get(GraphAppDataSPITest.HORST_ID);

    Assert.assertTrue(appData == null || appData.isEmpty());
  }
}
