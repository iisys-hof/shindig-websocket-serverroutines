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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util;

import java.util.ArrayList;
import java.util.List;

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
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.ShindigConstants;

/**
 * Test routine to check whether the special person node filtering utility is working correctly.
 */
public class PersonFilterTest {
  private static final String ID_FIELD = "id";

  private static final String NAME_ATT = "formatted";
  private static final String AGE_ATT = "age";
  private static final String TAGS_ATT = "tags";
  private static final String ADD_FORM_ATT = "formatted";
  private static final String ORG_NAME_ATT = "name";
  private static final String AFF_TITLE_ATT = "title";
  private static final String VALUE_ATT = "value";

  private static final String JOHN_ID = "john.doe", JANE_ID = "jane.doe", JACK_ID = "jack.doe";

  private static final String ORG1_NAME = "organization", ORG2_NAME = "corporation";

  private static final String ADD1 = "13 Fleet Street, London",
          ADD2 = "42 Memory Lane, San Francisco";

  private static final String JANE_TITLE1 = "doctor", JANE_TITLE2 = "secretary",
          JACK_TITLE = "spin doctor";

  private static final String[] JACK_MAILS = { "jack@example.com", "jdoe@wyutani.com" };

  private static final String[] JOHN_TAGS = { "nonsense", "father" }, JACK_TAGS = { "nonsense" };

  private final Node[] fPeople = new Node[3];
  private GraphDatabaseService fDb;

  @Before
  public void setupService() {
    final TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
    this.fDb = fact.newImpermanentDatabase();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (PersonFilterTest.this.fDb != null) {
          PersonFilterTest.this.fDb.shutdown();
        }
      }
    });

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
    johndoe.setProperty(PersonFilterTest.ID_FIELD, PersonFilterTest.JOHN_ID);
    johndoe.setProperty(PersonFilterTest.NAME_ATT, "John Doe");
    johndoe.setProperty(PersonFilterTest.AGE_ATT, 42);
    johndoe.setProperty(PersonFilterTest.TAGS_ATT, PersonFilterTest.JOHN_TAGS);
    personNodes.add(johndoe, PersonFilterTest.ID_FIELD, PersonFilterTest.JOHN_ID);
    this.fPeople[0] = johndoe;

    final Node janedoe = this.fDb.createNode();
    janedoe.setProperty(PersonFilterTest.ID_FIELD, PersonFilterTest.JANE_ID);
    janedoe.setProperty(PersonFilterTest.NAME_ATT, "Jane Doe");
    janedoe.setProperty(PersonFilterTest.AGE_ATT, 37);
    personNodes.add(janedoe, PersonFilterTest.ID_FIELD, PersonFilterTest.JANE_ID);
    this.fPeople[1] = janedoe;

    final Node jackdoe = this.fDb.createNode();
    jackdoe.setProperty(PersonFilterTest.ID_FIELD, PersonFilterTest.JACK_ID);
    jackdoe.setProperty(PersonFilterTest.NAME_ATT, "Jack Doe");
    jackdoe.setProperty(PersonFilterTest.AGE_ATT, 22);
    jackdoe.setProperty(PersonFilterTest.TAGS_ATT, PersonFilterTest.JACK_TAGS);
    personNodes.add(jackdoe, PersonFilterTest.ID_FIELD, PersonFilterTest.JACK_ID);
    this.fPeople[2] = jackdoe;

    // addresses
    final Node johnAdd1 = this.fDb.createNode();
    johnAdd1.setProperty(PersonFilterTest.ADD_FORM_ATT, PersonFilterTest.ADD1);
    johndoe.createRelationshipTo(johnAdd1, Neo4jRelTypes.LOCATED_AT);

    final Node johnAdd2 = this.fDb.createNode();
    johnAdd2.setProperty(PersonFilterTest.ADD_FORM_ATT, PersonFilterTest.ADD2);
    johndoe.createRelationshipTo(johnAdd2, Neo4jRelTypes.LOCATED_AT);

    // organizations
    final Node org1 = this.fDb.createNode();
    org1.setProperty(PersonFilterTest.ORG_NAME_ATT, PersonFilterTest.ORG1_NAME);

    final Node org2 = this.fDb.createNode();
    org2.setProperty(PersonFilterTest.ORG_NAME_ATT, PersonFilterTest.ORG2_NAME);

    // affiliations
    Relationship workRel = janedoe.createRelationshipTo(org1, Neo4jRelTypes.AFFILIATED);
    workRel.setProperty(PersonFilterTest.AFF_TITLE_ATT, PersonFilterTest.JANE_TITLE1);

    workRel = janedoe.createRelationshipTo(org2, Neo4jRelTypes.AFFILIATED);
    workRel.setProperty(PersonFilterTest.AFF_TITLE_ATT, PersonFilterTest.JANE_TITLE2);

    workRel = jackdoe.createRelationshipTo(org1, Neo4jRelTypes.AFFILIATED);
    workRel.setProperty(PersonFilterTest.AFF_TITLE_ATT, PersonFilterTest.JACK_TITLE);

    // e-mail addresses
    final Node jackMails = this.fDb.createNode();
    jackdoe.createRelationshipTo(jackMails, Neo4jRelTypes.EMAILS);

    jackMails.setProperty(PersonFilterTest.VALUE_ATT, PersonFilterTest.JACK_MAILS);

    // inter-person relationships
    johndoe.createRelationshipTo(janedoe, Neo4jRelTypes.FRIEND_OF);
    jackdoe.createRelationshipTo(janedoe, Neo4jRelTypes.FRIEND_OF);

    trans.success();
    trans.finish();
  }

  /**
   * Test routine for the "contains" filter operation on all of a person's fields including those of
   * additional data nodes.
   */
  @Test
  public void allFieldsTest() {
    List<Node> list = null;

    // by name - all Doe family members
    list = new ArrayList<Node>();
    list.add(this.fPeople[0]);
    list.add(this.fPeople[1]);
    list.add(this.fPeople[2]);

    PersonFilter.filterNodes(list, "doe");

    Assert.assertEquals(3, list.size());

    // number in age
    list = new ArrayList<Node>();
    list.add(this.fPeople[0]);
    list.add(this.fPeople[1]);
    list.add(this.fPeople[2]);

    PersonFilter.filterNodes(list, "2");

    Assert.assertEquals(2, list.size());
    Assert.assertTrue(list.contains(this.fPeople[0]));
    Assert.assertTrue(list.contains(this.fPeople[2]));

    // tags
    list = new ArrayList<Node>();
    list.add(this.fPeople[0]);
    list.add(this.fPeople[1]);
    list.add(this.fPeople[2]);

    PersonFilter.filterNodes(list, "nonsense");

    Assert.assertEquals(2, list.size());
    Assert.assertTrue(list.contains(this.fPeople[0]));
    Assert.assertTrue(list.contains(this.fPeople[2]));

    list = new ArrayList<Node>();
    list.add(this.fPeople[0]);
    list.add(this.fPeople[1]);
    list.add(this.fPeople[2]);

    PersonFilter.filterNodes(list, "father");

    Assert.assertEquals(1, list.size());
    Assert.assertTrue(list.contains(this.fPeople[0]));

    // addresses
    list = new ArrayList<Node>();
    list.add(this.fPeople[0]);
    list.add(this.fPeople[1]);
    list.add(this.fPeople[2]);

    PersonFilter.filterNodes(list, "san francisco");

    Assert.assertEquals(1, list.size());
    Assert.assertTrue(list.contains(this.fPeople[0]));

    // e-mail addresses
    list = new ArrayList<Node>();
    list.add(this.fPeople[0]);
    list.add(this.fPeople[1]);
    list.add(this.fPeople[2]);

    PersonFilter.filterNodes(list, ".com");

    Assert.assertEquals(1, list.size());
    Assert.assertTrue(list.contains(this.fPeople[2]));

    // affiliations
    list = new ArrayList<Node>();
    list.add(this.fPeople[0]);
    list.add(this.fPeople[1]);
    list.add(this.fPeople[2]);

    PersonFilter.filterNodes(list, "doctor");

    Assert.assertEquals(2, list.size());
    Assert.assertTrue(list.contains(this.fPeople[1]));
    Assert.assertTrue(list.contains(this.fPeople[2]));

    // organizations
    list = new ArrayList<Node>();
    list.add(this.fPeople[0]);
    list.add(this.fPeople[1]);
    list.add(this.fPeople[2]);

    PersonFilter.filterNodes(list, PersonFilterTest.ORG1_NAME);

    Assert.assertEquals(2, list.size());
    Assert.assertTrue(list.contains(this.fPeople[1]));
    Assert.assertTrue(list.contains(this.fPeople[2]));
  }

  /**
   * Test routine for the "isFriendsWith" filter routine.
   */
  @Test
  public void friendFilterTest() {
    List<Node> list = new ArrayList<Node>();
    list.add(this.fPeople[0]);
    list.add(this.fPeople[1]);
    list.add(this.fPeople[2]);

    PersonFilter.filterNodes(list, this.fPeople[0]);

    // should return Jane and John himself
    Assert.assertEquals(2, list.size());
    Assert.assertTrue(list.contains(this.fPeople[0]));
    Assert.assertTrue(list.contains(this.fPeople[1]));

    list = new ArrayList<Node>();
    list.add(this.fPeople[0]);
    list.add(this.fPeople[1]);
    list.add(this.fPeople[2]);

    PersonFilter.filterNodes(list, this.fPeople[1]);

    // should return only Jane herself
    Assert.assertEquals(1, list.size());
    Assert.assertTrue(list.contains(this.fPeople[1]));
  }
}
