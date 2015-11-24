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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util.ShindigRelTypes;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Test for the skill service implementation of the graph back-end.
 */
public class GraphSkillSPITest {
  private static final String ID_FIELD = "id";
  private static final String NAME_FIELD = "name";

  private static final String JOHN_ID = "john.doe", JANE_ID = "jane.doe";
  private static final String JAVA_SKILL = "Java Programming", NEO_SKILL = "Neo4j",
          C_SKILL = "C/C++ Programming";

  private GraphDatabaseService fDb;
  private GraphPersonSPI fPersonSPI;
  private GraphSkillSPI fSkillSPI;

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
        if (GraphSkillSPITest.this.fDb != null) {
          GraphSkillSPITest.this.fDb.shutdown();
        }
      }
    });

    final Map<String, String> config = new HashMap<String, String>();

    this.fPersonSPI = new GraphPersonSPI(this.fDb, config, new ImplUtil(BasicBSONList.class,
            BasicBSONObject.class));
    this.fSkillSPI = new GraphSkillSPI(this.fDb, config, this.fPersonSPI, new ImplUtil(
            BasicBSONList.class, BasicBSONObject.class));

    createTestData();
  }

  @After
  public void stopDatabase() {
    this.fDb.shutdown();
  }

  private void createTestData() {
    final Transaction trans = this.fDb.beginTx();

    final Index<Node> personNodes = this.fDb.index().forNodes(ShindigConstants.PERSON_NODES);
    final Index<Node> skillNodes = this.fDb.index().forNodes(ShindigConstants.SKILL_NODES);

    // people
    final Node johndoe = this.fDb.createNode();
    johndoe.setProperty(GraphSkillSPITest.ID_FIELD, GraphSkillSPITest.JOHN_ID);
    personNodes.add(johndoe, GraphSkillSPITest.ID_FIELD, GraphSkillSPITest.JOHN_ID);

    final Node janedoe = this.fDb.createNode();
    janedoe.setProperty(GraphSkillSPITest.ID_FIELD, GraphSkillSPITest.JANE_ID);
    personNodes.add(janedoe, GraphSkillSPITest.ID_FIELD, GraphSkillSPITest.JANE_ID);

    // skills (global)
    final Node javaNode = this.fDb.createNode();
    javaNode.setProperty(GraphSkillSPITest.ID_FIELD, GraphSkillSPITest.JAVA_SKILL.toLowerCase());
    javaNode.setProperty(GraphSkillSPITest.NAME_FIELD, GraphSkillSPITest.JAVA_SKILL);
    skillNodes
            .add(javaNode, GraphSkillSPITest.ID_FIELD, GraphSkillSPITest.JAVA_SKILL.toLowerCase());

    final Node neoNode = this.fDb.createNode();
    neoNode.setProperty(GraphSkillSPITest.ID_FIELD, GraphSkillSPITest.NEO_SKILL.toLowerCase());
    neoNode.setProperty(GraphSkillSPITest.NAME_FIELD, GraphSkillSPITest.NEO_SKILL);
    skillNodes.add(neoNode, GraphSkillSPITest.ID_FIELD, GraphSkillSPITest.NEO_SKILL.toLowerCase());

    // skill links
    final Node javaLinkNode = this.fDb.createNode();
    javaLinkNode.createRelationshipTo(javaNode, ShindigRelTypes.IS_SKILL);

    final Relationship linkRel = johndoe.createRelationshipTo(javaLinkNode,
            ShindigRelTypes.HAS_SKILL);
    linkRel.setProperty(GraphSkillSPITest.ID_FIELD, GraphSkillSPITest.JAVA_SKILL);

    javaLinkNode.createRelationshipTo(johndoe, ShindigRelTypes.LINKED_BY);
    javaLinkNode.createRelationshipTo(janedoe, ShindigRelTypes.LINKED_BY);

    trans.success();
    trans.finish();
  }

  /**
   * Tests the retrieval of skill autocompletion suggestions.
   *
   * @throws Exception
   *           if the test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void autocompletionTest() {
    final Map<String, Object> collOpts = new HashMap<String, Object>();

    // specific
    ListResult result = this.fSkillSPI.getSkillAutocomp("program", collOpts);

    List<String> completions = (List<String>) result.getResults();
    Assert.assertEquals(1, completions.size());
    Assert.assertEquals(GraphSkillSPITest.JAVA_SKILL, completions.get(0));

    // all
    result = this.fSkillSPI.getSkillAutocomp("", collOpts);

    completions = (List<String>) result.getResults();
    Assert.assertEquals(2, completions.size());
    Assert.assertTrue(completions.contains(GraphSkillSPITest.JAVA_SKILL));
    Assert.assertTrue(completions.contains(GraphSkillSPITest.NEO_SKILL));
  }

  /**
   * Tests the retrieval of skills for a person.
   *
   * @throws Exception
   *           if the test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void skillRetrievalTest() {
    final Map<String, Object> collOpts = new HashMap<String, Object>();

    // some
    ListResult result = this.fSkillSPI.getSkills(GraphSkillSPITest.JOHN_ID, collOpts);

    List<Map<String, Object>> skills = (List<Map<String, Object>>) result.getResults();
    Assert.assertEquals(1, skills.size());
    final Map<String, Object> skill = skills.get(0);
    Assert.assertEquals(GraphSkillSPITest.JAVA_SKILL, skill.get(GraphSkillSPITest.NAME_FIELD));

    expectPeople(skill, new String[] { GraphSkillSPITest.JOHN_ID, GraphSkillSPITest.JANE_ID });

    // none
    result = this.fSkillSPI.getSkills(GraphSkillSPITest.JANE_ID, collOpts);

    skills = (List<Map<String, Object>>) result.getResults();
    Assert.assertEquals(0, skills.size());
  }

  @SuppressWarnings("unchecked")
  private void expectPeople(Map<String, Object> skill, String[] people) {
    // checks whether a specified set of people has linked a skill
    final List<Map<String, Object>> peopleList = (List<Map<String, Object>>) skill.get("people");
    Assert.assertEquals(people.length, peopleList.size());
    final Set<String> foundIds = new HashSet<String>();

    for (final Map<String, Object> person : peopleList) {
      foundIds.add(person.get(GraphSkillSPITest.ID_FIELD).toString());
    }

    for (final String person : people) {
      Assert.assertTrue(foundIds.contains(person));
    }
  }

  /**
   * Tests linking a skill to a person.
   *
   * @throws Exception
   *           if the test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void skillAddingTest() {
    final Map<String, Object> collOpts = new HashMap<String, Object>();

    // adding (globally) existing skill
    this.fSkillSPI.addSkill(GraphSkillSPITest.JOHN_ID, GraphSkillSPITest.JANE_ID,
            GraphSkillSPITest.NEO_SKILL);

    // verify
    ListResult result = this.fSkillSPI.getSkills(GraphSkillSPITest.JOHN_ID, collOpts);
    List<Map<String, Object>> skills = (List<Map<String, Object>>) result.getResults();
    Assert.assertEquals(2, skills.size());

    boolean found = false;
    for (final Map<String, Object> skill : skills) {
      if (GraphSkillSPITest.NEO_SKILL.equals(skill.get(GraphSkillSPITest.NAME_FIELD))) {
        found = true;
        expectPeople(skill, new String[] { GraphSkillSPITest.JANE_ID });
      }
    }
    Assert.assertTrue(found);

    // second link
    this.fSkillSPI.addSkill(GraphSkillSPITest.JOHN_ID, GraphSkillSPITest.JOHN_ID,
            GraphSkillSPITest.NEO_SKILL);

    // verify
    result = this.fSkillSPI.getSkills(GraphSkillSPITest.JOHN_ID, collOpts);
    skills = (List<Map<String, Object>>) result.getResults();
    Assert.assertEquals(2, skills.size());

    for (final Map<String, Object> skill : skills) {
      if (GraphSkillSPITest.NEO_SKILL.equals(skill.get(GraphSkillSPITest.NAME_FIELD))) {
        expectPeople(skill, new String[] { GraphSkillSPITest.JOHN_ID, GraphSkillSPITest.JANE_ID });
      }
    }

    // adding non-existent
    this.fSkillSPI.addSkill(GraphSkillSPITest.JOHN_ID, GraphSkillSPITest.JOHN_ID,
            GraphSkillSPITest.C_SKILL);

    // verify
    result = this.fSkillSPI.getSkills(GraphSkillSPITest.JOHN_ID, collOpts);
    skills = (List<Map<String, Object>>) result.getResults();
    Assert.assertEquals(3, skills.size());

    found = false;
    for (final Map<String, Object> skill : skills) {
      if (GraphSkillSPITest.C_SKILL.equals(skill.get(GraphSkillSPITest.NAME_FIELD))) {
        found = true;
        expectPeople(skill, new String[] { GraphSkillSPITest.JOHN_ID });
      }
    }
    Assert.assertTrue(found);

    // re-adding to check for duplicates or errors
    this.fSkillSPI.addSkill(GraphSkillSPITest.JOHN_ID, GraphSkillSPITest.JOHN_ID,
            GraphSkillSPITest.C_SKILL);

    // verify
    result = this.fSkillSPI.getSkills(GraphSkillSPITest.JOHN_ID, collOpts);
    skills = (List<Map<String, Object>>) result.getResults();
    Assert.assertEquals(3, skills.size());

    for (final Map<String, Object> skill : skills) {
      if (GraphSkillSPITest.C_SKILL.equals(skill.get(GraphSkillSPITest.NAME_FIELD))) {
        found = true;
        expectPeople(skill, new String[] { GraphSkillSPITest.JOHN_ID });
      }
    }
  }

  /**
   * Tests removing a skill link from a person.
   *
   * @throws Exception
   *           if the test fails
   */
  @SuppressWarnings("unchecked")
  @Test
  public void skillRemovalTest() {
    final Map<String, Object> collOpts = new HashMap<String, Object>();

    // add new links first
    skillAddingTest();

    // removing one of two links
    this.fSkillSPI.removeSkill(GraphSkillSPITest.JOHN_ID, GraphSkillSPITest.JANE_ID,
            GraphSkillSPITest.NEO_SKILL);

    // verify
    ListResult result = this.fSkillSPI.getSkills(GraphSkillSPITest.JOHN_ID, collOpts);
    List<Map<String, Object>> skills = (List<Map<String, Object>>) result.getResults();
    Assert.assertEquals(3, skills.size());

    boolean found = false;
    for (final Map<String, Object> skill : skills) {
      if (GraphSkillSPITest.NEO_SKILL.equals(skill.get(GraphSkillSPITest.NAME_FIELD))) {
        found = true;
        expectPeople(skill, new String[] { GraphSkillSPITest.JOHN_ID });
      }
    }
    Assert.assertTrue(found);

    // removing the second link
    this.fSkillSPI.removeSkill(GraphSkillSPITest.JOHN_ID, GraphSkillSPITest.JOHN_ID,
            GraphSkillSPITest.NEO_SKILL);

    // verify
    result = this.fSkillSPI.getSkills(GraphSkillSPITest.JOHN_ID, collOpts);
    skills = (List<Map<String, Object>>) result.getResults();
    Assert.assertEquals(2, skills.size());

    found = false;
    for (final Map<String, Object> skill : skills) {
      if (GraphSkillSPITest.NEO_SKILL.equals(skill.get(GraphSkillSPITest.NAME_FIELD))) {
        found = true;
      }
    }
    Assert.assertFalse(found);

    // remove non-existent link to check for errors
    this.fSkillSPI.removeSkill(GraphSkillSPITest.JOHN_ID, GraphSkillSPITest.JOHN_ID,
            GraphSkillSPITest.NEO_SKILL);

    // verify
    result = this.fSkillSPI.getSkills(GraphSkillSPITest.JOHN_ID, collOpts);
    skills = (List<Map<String, Object>>) result.getResults();
    Assert.assertEquals(2, skills.size());
  }
}
