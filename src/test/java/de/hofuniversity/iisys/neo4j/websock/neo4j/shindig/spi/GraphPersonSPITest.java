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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Test for the PersonService implementation of the graph back-end.
 */
public class GraphPersonSPITest {
  private static final String ID_FIELD = "id";
  private static final String TITLE_FIELD = "title";
  private static final String DESC_FIELD = "description";

  private static final String NAME_FIELD = "name";
  private static final String FORMATTED_FIELD = "formatted";
  private static final String GIV_NAME_FIELD = "givenName";
  private static final String FAM_NAME_FIELD = "familyName";

  private static final String AGE_FIELD = "age";
  private static final String BDAY_FIELD = "birthday";

  private static final String ORGS_FIELD = "organizations";
  private static final String SALARY_FIELD = "salary";
  private static final String START_DATE_FIELD = "startDate";
  private static final String FIELD_FIELD = "field";
  private static final String WEBPAGE_FIELD = "webpage";

  private static final String COUNTRY_FIELD = "country";

  private static final String JOHN_ID = "john.doe", JANE_ID = "jane.doe", JACK_ID = "jack.doe",
          HORST_ID = "horst", FRED_ID = "FRED";

  private static final String DOE_FAM_ID = "fam.doe.group", DENKER_ID = "denker";

  private GraphDatabaseService fDb;
  private GraphPersonSPI fPersonSPI;

  /**
   * Sets up an impermanent database with some test data and a person service for testing purposes.
   */
  @Before
  public void setupService() {
    final TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
    this.fDb = fact.newImpermanentDatabase();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (GraphPersonSPITest.this.fDb != null) {
          GraphPersonSPITest.this.fDb.shutdown();
        }
      }
    });

    this.fPersonSPI = new GraphPersonSPI(this.fDb, new GraphConfig(true), new ImplUtil(
            BasicBSONList.class, BasicBSONObject.class));

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
    famGroup.setProperty(GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.DOE_FAM_ID);
    famGroup.setProperty(GraphPersonSPITest.DESC_FIELD,
            "private group for members of the Doe family");
    famGroup.setProperty(GraphPersonSPITest.TITLE_FIELD, "Doe family");
    groupNodes.add(famGroup, GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.DOE_FAM_ID);

    final Node denkGroup = this.fDb.createNode();
    denkGroup.setProperty(GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.DENKER_ID);
    denkGroup.setProperty(GraphPersonSPITest.TITLE_FIELD, "Club der Denker");
    groupNodes.add(denkGroup, GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.DENKER_ID);

    // organizations
    final Node pfuschGmbH = this.fDb.createNode();
    pfuschGmbH.setProperty(GraphPersonSPITest.NAME_FIELD, "Pfusch und Bastel GmbH");
    pfuschGmbH.setProperty(GraphPersonSPITest.DESC_FIELD,
            "Führender Hersteller fragwürdiger, wenn auch kreativer "
                    + "Bastellösungen im IT-Bereich");
    pfuschGmbH.setProperty(GraphPersonSPITest.FIELD_FIELD, "IT Solutions");
    pfuschGmbH.setProperty(GraphPersonSPITest.WEBPAGE_FIELD, "www.pfusch.bastel.gm.bh");

    // addresses
    final Node pfuschAdd = this.fDb.createNode();
    pfuschAdd.setProperty(GraphPersonSPITest.COUNTRY_FIELD, "Germany");
    pfuschAdd.setProperty(GraphPersonSPITest.FORMATTED_FIELD, "Bielefeld, Germany");

    // people
    final Node johndoe = this.fDb.createNode();
    johndoe.setProperty(GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.JOHN_ID);
    johndoe.setProperty(GraphPersonSPITest.FORMATTED_FIELD, "John Doe");
    johndoe.setProperty(GraphPersonSPITest.GIV_NAME_FIELD, "John");
    johndoe.setProperty(GraphPersonSPITest.FAM_NAME_FIELD, "Doe");
    personNodes.add(johndoe, GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.JOHN_ID);

    final Node janedoe = this.fDb.createNode();
    janedoe.setProperty(GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.JANE_ID);
    janedoe.setProperty(GraphPersonSPITest.FORMATTED_FIELD, "Jane Doe");
    janedoe.setProperty(GraphPersonSPITest.GIV_NAME_FIELD, "Jane");
    janedoe.setProperty(GraphPersonSPITest.FAM_NAME_FIELD, "Doe");
    personNodes.add(janedoe, GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.JANE_ID);

    final Node jackdoe = this.fDb.createNode();
    jackdoe.setProperty(GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.JACK_ID);
    jackdoe.setProperty(GraphPersonSPITest.FORMATTED_FIELD, "Jack Doe");
    jackdoe.setProperty(GraphPersonSPITest.GIV_NAME_FIELD, "Jack");
    jackdoe.setProperty(GraphPersonSPITest.FAM_NAME_FIELD, "Doe");
    jackdoe.setProperty(GraphPersonSPITest.BDAY_FIELD, new Long(0));
    personNodes.add(jackdoe, GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.JACK_ID);

    final Node horst = this.fDb.createNode();
    horst.setProperty(GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.HORST_ID);
    horst.setProperty(GraphPersonSPITest.FORMATTED_FIELD, "Horst");
    horst.setProperty(GraphPersonSPITest.GIV_NAME_FIELD, "Horst");
    horst.setProperty(GraphPersonSPITest.FAM_NAME_FIELD, "Horstsen");
    horst.setProperty(GraphPersonSPITest.AGE_FIELD, new Integer(60));
    personNodes.add(horst, GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.HORST_ID);

    // inter-person relations
    johndoe.createRelationshipTo(janedoe, Neo4jRelTypes.FRIEND_OF);
    janedoe.createRelationshipTo(johndoe, Neo4jRelTypes.FRIEND_OF);

    janedoe.createRelationshipTo(horst, Neo4jRelTypes.KNOWS);
    horst.createRelationshipTo(janedoe, Neo4jRelTypes.KNOWS);
    horst.createRelationshipTo(johndoe, Neo4jRelTypes.KNOWS);

    johndoe.createRelationshipTo(jackdoe, Neo4jRelTypes.FRIEND_OF);
    jackdoe.createRelationshipTo(johndoe, Neo4jRelTypes.FRIEND_OF);

    janedoe.createRelationshipTo(jackdoe, Neo4jRelTypes.FRIEND_OF);
    jackdoe.createRelationshipTo(janedoe, Neo4jRelTypes.FRIEND_OF);

    // group relations
    johndoe.createRelationshipTo(famGroup, Neo4jRelTypes.OWNER_OF);
    johndoe.createRelationshipTo(famGroup, Neo4jRelTypes.MEMBER_OF);
    janedoe.createRelationshipTo(famGroup, Neo4jRelTypes.MEMBER_OF);
    jackdoe.createRelationshipTo(famGroup, Neo4jRelTypes.MEMBER_OF);

    horst.createRelationshipTo(denkGroup, Neo4jRelTypes.OWNER_OF);
    horst.createRelationshipTo(denkGroup, Neo4jRelTypes.MEMBER_OF);

    // organization relations
    Relationship workRel = johndoe.createRelationshipTo(pfuschGmbH, Neo4jRelTypes.AFFILIATED);
    workRel.setProperty(GraphPersonSPITest.TITLE_FIELD, "big boss");
    workRel.setProperty(GraphPersonSPITest.SALARY_FIELD, "500,000$ / year");
    workRel.setProperty(GraphPersonSPITest.START_DATE_FIELD, new Long(0));

    workRel = janedoe.createRelationshipTo(pfuschGmbH, Neo4jRelTypes.AFFILIATED);
    workRel.setProperty(GraphPersonSPITest.TITLE_FIELD, "secretary");
    workRel.setProperty(GraphPersonSPITest.SALARY_FIELD, "75,000$ / year");

    workRel = horst.createRelationshipTo(pfuschGmbH, Neo4jRelTypes.AFFILIATED);
    workRel.setProperty(GraphPersonSPITest.TITLE_FIELD, "janitor");
    workRel.setProperty(GraphPersonSPITest.SALARY_FIELD, "10,000$ / year");

    pfuschGmbH.createRelationshipTo(pfuschAdd, Neo4jRelTypes.LOCATED_AT);

    trans.success();
    trans.finish();
  }

  /**
   * Tests single person retrieval. Checks some relevant fields representing categories in the
   * result.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @SuppressWarnings("unchecked")
  @Test
  public void personRetrievalTest() throws Exception {
    // direct node retrieval
    final Node johnDoeNode = this.fPersonSPI.getPersonNode(GraphPersonSPITest.JOHN_ID);

    Assert.assertEquals(GraphPersonSPITest.JOHN_ID,
            johnDoeNode.getProperty(GraphPersonSPITest.ID_FIELD));
    Assert.assertEquals("John Doe", johnDoeNode.getProperty(GraphPersonSPITest.FORMATTED_FIELD));

    // data transfer object
    final Map<String, Object> janeDoeDTO = this.fPersonSPI.getPersonMap(GraphPersonSPITest.JANE_ID,
            null);

    Assert.assertEquals(GraphPersonSPITest.JANE_ID, janeDoeDTO.get(GraphPersonSPITest.ID_FIELD));
    Assert.assertEquals("Jane Doe", janeDoeDTO.get(GraphPersonSPITest.FORMATTED_FIELD));

    final List<Map<String, Object>> janeOrgs = (List<Map<String, Object>>) janeDoeDTO
            .get(GraphPersonSPITest.ORGS_FIELD);
    Assert.assertEquals(1, janeOrgs.size());
    Assert.assertEquals("Pfusch und Bastel GmbH", janeOrgs.get(0)
            .get(GraphPersonSPITest.NAME_FIELD));
    Assert.assertEquals("75,000$ / year", janeOrgs.get(0).get(GraphPersonSPITest.SALARY_FIELD));

    // service method
    final SingleResult horstFut = this.fPersonSPI.getPerson(GraphPersonSPITest.HORST_ID, null);

    final Map<String, Object> horst = (Map<String, Object>) horstFut.getResults();
    Assert.assertEquals(GraphPersonSPITest.HORST_ID, horst.get(GraphPersonSPITest.ID_FIELD));
    Assert.assertEquals(new Integer(60), horst.get(GraphPersonSPITest.AGE_FIELD));

    final SingleResult jackFut = this.fPersonSPI.getPerson(GraphPersonSPITest.JACK_ID, null);

    final Map<String, Object> jackDoe = (Map<String, Object>) jackFut.getResults();
    Assert.assertEquals(0L, jackDoe.get(GraphPersonSPITest.BDAY_FIELD));
  }

  /**
   * Tests the retrieval of multiple people, via IDs, relations and groups. Checks the validity of
   * the collections returned.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @SuppressWarnings("unchecked")
  @Test
  public void peopleRetrievalTest() throws Exception {
    // IDs (self)
    final Map<String, Object> collOpts = new HashMap<String, Object>();
    final List<String> idSet = new ArrayList<String>();
    idSet.add(GraphPersonSPITest.JANE_ID);
    idSet.add(GraphPersonSPITest.JACK_ID);
    idSet.add(GraphPersonSPITest.HORST_ID);

    ListResult peopleColl = this.fPersonSPI.getPeople(idSet, OSFields.GROUP_TYPE_SELF, collOpts,
            null);
    List<Map<String, Object>> people = (List<Map<String, Object>>) peopleColl.getResults();

    Assert.assertEquals(3, people.size());

    boolean johnFound = false;
    boolean janeFound = false;
    boolean jackFound = false;
    boolean horstFound = false;

    String id = null;
    for (final Map<String, Object> person : people) {
      id = person.get(GraphPersonSPITest.ID_FIELD).toString();
      if (id.equals(GraphPersonSPITest.JANE_ID)) {
        janeFound = true;
      } else if (id.equals(GraphPersonSPITest.JACK_ID)) {
        jackFound = true;
      } else if (id.equals(GraphPersonSPITest.HORST_ID)) {
        horstFound = true;
      } else {
        throw new Exception("unexpected person");
      }
    }
    Assert.assertTrue(janeFound && jackFound && horstFound);

    // friends
    idSet.clear();
    idSet.add(GraphPersonSPITest.JANE_ID);

    peopleColl = this.fPersonSPI.getPeople(idSet, OSFields.GROUP_TYPE_FRIENDS, collOpts, null);
    people = (List<Map<String, Object>>) peopleColl.getResults();

    Assert.assertEquals(2, people.size());

    johnFound = false;
    jackFound = false;

    for (final Map<String, Object> person : people) {
      id = person.get(GraphPersonSPITest.ID_FIELD).toString();
      if (id.equals(GraphPersonSPITest.JOHN_ID)) {
        johnFound = true;
      } else if (id.equals(GraphPersonSPITest.JACK_ID)) {
        jackFound = true;
      } else {
        throw new Exception("unexpected person");
      }
    }
    Assert.assertTrue(johnFound && jackFound);

    // all relations
    peopleColl = this.fPersonSPI.getPeople(idSet, OSFields.GROUP_TYPE_ALL, collOpts, null);
    people = (List<Map<String, Object>>) peopleColl.getResults();

    Assert.assertEquals(3, people.size());

    johnFound = false;
    jackFound = false;
    horstFound = false;

    for (final Map<String, Object> person : people) {
      id = person.get(GraphPersonSPITest.ID_FIELD).toString();
      if (id.equals(GraphPersonSPITest.JOHN_ID)) {
        johnFound = true;
      } else if (id.equals(GraphPersonSPITest.JACK_ID)) {
        jackFound = true;
      } else if (id.equals(GraphPersonSPITest.HORST_ID)) {
        horstFound = true;
      } else {
        throw new Exception("unexpected person");
      }
    }
    Assert.assertTrue(johnFound && jackFound && horstFound);

    // groups
    idSet.clear();
    idSet.add(GraphPersonSPITest.JOHN_ID);

    peopleColl = this.fPersonSPI.getPeople(idSet, GraphPersonSPITest.DOE_FAM_ID, collOpts, null);
    people = (List<Map<String, Object>>) peopleColl.getResults();

    Assert.assertEquals(3, people.size());

    johnFound = false;
    janeFound = false;
    jackFound = false;

    for (final Map<String, Object> person : people) {
      id = person.get(GraphPersonSPITest.ID_FIELD).toString();
      if (id.equals(GraphPersonSPITest.JOHN_ID)) {
        johnFound = true;
      } else if (id.equals(GraphPersonSPITest.JANE_ID)) {
        janeFound = true;
      } else if (id.equals(GraphPersonSPITest.JACK_ID)) {
        jackFound = true;
      } else {
        throw new Exception("unexpected person");
      }
    }
    Assert.assertTrue(johnFound && janeFound && jackFound);

    peopleColl = this.fPersonSPI.getPeople(idSet, GraphPersonSPITest.DENKER_ID, collOpts, null);
    people = (List<Map<String, Object>>) peopleColl.getResults();

    Assert.assertEquals(1, people.size());
    Assert.assertEquals(GraphPersonSPITest.HORST_ID, people.get(0).get(GraphPersonSPITest.ID_FIELD));
  }

  /**
   * Tests updating of a person
   */
  @SuppressWarnings("unchecked")
  @Test
  public void updateTest() throws Exception {
    // create person for updating
    personCreationTest();

    // update
    Map<String, Object> p = new HashMap<String, Object>();
    p.put(GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.FRED_ID);
    p.put(GraphPersonSPITest.GIV_NAME_FIELD, "Frederick");
    p.put(GraphPersonSPITest.FAM_NAME_FIELD, "Eddison");
    p.put(GraphPersonSPITest.FORMATTED_FIELD, "Frederick Eddison");

    this.fPersonSPI.updatePerson(GraphPersonSPITest.FRED_ID, p);

    // check
    p = (Map<String, Object>) this.fPersonSPI.getPerson(GraphPersonSPITest.FRED_ID, null)
            .getResults();

    Assert.assertEquals(GraphPersonSPITest.FRED_ID, p.get(GraphPersonSPITest.ID_FIELD));
    Assert.assertEquals("Frederick", p.get(GraphPersonSPITest.GIV_NAME_FIELD));
    Assert.assertEquals("Eddison", p.get(GraphPersonSPITest.FAM_NAME_FIELD));
    Assert.assertEquals("Frederick Eddison", p.get(GraphPersonSPITest.FORMATTED_FIELD));
  }

  // extended functionality

  /**
   * Tests the creation of users. Checks the validity of the person returned and whether it can be
   * found.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @SuppressWarnings("unchecked")
  @Test
  public void personCreationTest() throws Exception {
    Map<String, Object> p = new HashMap<String, Object>();
    p.put(GraphPersonSPITest.ID_FIELD, GraphPersonSPITest.FRED_ID);
    p.put(GraphPersonSPITest.GIV_NAME_FIELD, "Fred");
    p.put(GraphPersonSPITest.FAM_NAME_FIELD, "Edison");
    p.put(GraphPersonSPITest.FORMATTED_FIELD, "Dr. Fred Edison");

    this.fPersonSPI.createPerson(p);

    p = (Map<String, Object>) this.fPersonSPI.getPerson(GraphPersonSPITest.FRED_ID, null)
            .getResults();

    Assert.assertEquals(GraphPersonSPITest.FRED_ID, p.get(GraphPersonSPITest.ID_FIELD));
    Assert.assertEquals("Fred", p.get(GraphPersonSPITest.GIV_NAME_FIELD));
    Assert.assertEquals("Edison", p.get(GraphPersonSPITest.FAM_NAME_FIELD));
    Assert.assertEquals("Dr. Fred Edison", p.get(GraphPersonSPITest.FORMATTED_FIELD));
  }

  /**
   * Tests the retrieval of all people.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void allPeopleRetrievalTest() throws Exception {
    // get all people (extended functionality)
    final ListResult peopleColl = this.fPersonSPI.getAllPeople(new HashMap<String, Object>(), null);
    final List<Map<String, Object>> people = (List<Map<String, Object>>) peopleColl.getResults();

    boolean johnFound = false;
    boolean janeFound = false;
    boolean jackFound = false;
    boolean horstFound = false;

    String id = null;
    for (final Map<String, Object> person : people) {
      id = person.get(GraphPersonSPITest.ID_FIELD).toString();
      if (id.equals(GraphPersonSPITest.JOHN_ID)) {
        johnFound = true;
      } else if (id.equals(GraphPersonSPITest.JANE_ID)) {
        janeFound = true;
      } else if (id.equals(GraphPersonSPITest.JACK_ID)) {
        jackFound = true;
      } else if (id.equals(GraphPersonSPITest.HORST_ID)) {
        horstFound = true;
      } else {
        throw new Exception("unexpected person");
      }
    }

    Assert.assertTrue(johnFound && janeFound && jackFound && horstFound);
  }

  /**
   * Tests the deletion of a person. Currently no-op.
   */
  @Test
  public void deletionTest() {
    this.fPersonSPI.deletePerson(GraphPersonSPITest.HORST_ID);
  }
}
