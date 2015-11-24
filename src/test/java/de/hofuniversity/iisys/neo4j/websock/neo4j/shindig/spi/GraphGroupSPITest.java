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

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Test for the GroupService implementation of the graph back-end.
 */
public class GraphGroupSPITest {
  private static final String ID_FIELD = "id";

  private static final String TITLE_FIELD = "title";
  private static final String DESC_FIELD = "description";

  private static final String FORMATTED_FIELD = "formatted";
  private static final String GIV_NAME_FIELD = "givenName";
  private static final String FAM_NAME_FIELD = "familyName";

  private static final String JOHN_ID = "john.doe", JANE_ID = "jane.doe", HORST_ID = "horst";

  private static final String DOE_FAM_ID = "fam.doe.group", DENKER_ID = "denker";

  private GraphDatabaseService fDb;
  private GraphPersonSPI fPersonSPI;
  private GraphGroupSPI fGroupSPI;

  /**
   * Sets up an impermanent database with some test data, a person service and a group service for
   * testing purposes.
   */
  @Before
  public void setupService() {
    final TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
    this.fDb = fact.newImpermanentDatabase();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (GraphGroupSPITest.this.fDb != null) {
          GraphGroupSPITest.this.fDb.shutdown();
        }
      }
    });
    final Map<String, String> config = new HashMap<String, String>();

    this.fPersonSPI = new GraphPersonSPI(this.fDb, config, new ImplUtil(BasicBSONList.class,
            BasicBSONObject.class));
    this.fGroupSPI = new GraphGroupSPI(this.fPersonSPI, new ImplUtil(BasicBSONList.class,
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
    famGroup.setProperty(GraphGroupSPITest.ID_FIELD, GraphGroupSPITest.DOE_FAM_ID);
    famGroup.setProperty(GraphGroupSPITest.DESC_FIELD,
            "private group for members of the Doe family");
    famGroup.setProperty(GraphGroupSPITest.TITLE_FIELD, "Doe family");
    groupNodes.add(famGroup, GraphGroupSPITest.ID_FIELD, GraphGroupSPITest.DOE_FAM_ID);

    final Node denkGroup = this.fDb.createNode();
    denkGroup.setProperty(GraphGroupSPITest.ID_FIELD, GraphGroupSPITest.DENKER_ID);
    denkGroup.setProperty(GraphGroupSPITest.TITLE_FIELD, "Club der Denker");
    groupNodes.add(denkGroup, GraphGroupSPITest.ID_FIELD, GraphGroupSPITest.DENKER_ID);

    // people
    final Node johndoe = this.fDb.createNode();
    johndoe.setProperty(GraphGroupSPITest.ID_FIELD, GraphGroupSPITest.JOHN_ID);
    johndoe.setProperty(GraphGroupSPITest.FORMATTED_FIELD, "John Doe");
    johndoe.setProperty(GraphGroupSPITest.GIV_NAME_FIELD, "John");
    johndoe.setProperty(GraphGroupSPITest.FAM_NAME_FIELD, "Doe");
    personNodes.add(johndoe, GraphGroupSPITest.ID_FIELD, GraphGroupSPITest.JOHN_ID);

    final Node janedoe = this.fDb.createNode();
    janedoe.setProperty(GraphGroupSPITest.ID_FIELD, GraphGroupSPITest.JANE_ID);
    janedoe.setProperty(GraphGroupSPITest.FORMATTED_FIELD, "Jane Doe");
    janedoe.setProperty(GraphGroupSPITest.GIV_NAME_FIELD, "Jane");
    janedoe.setProperty(GraphGroupSPITest.FAM_NAME_FIELD, "Doe");
    personNodes.add(janedoe, GraphGroupSPITest.ID_FIELD, GraphGroupSPITest.JANE_ID);

    final Node horst = this.fDb.createNode();
    horst.setProperty(GraphGroupSPITest.ID_FIELD, GraphGroupSPITest.HORST_ID);
    horst.setProperty(GraphGroupSPITest.FORMATTED_FIELD, "Horst");
    horst.setProperty(GraphGroupSPITest.GIV_NAME_FIELD, "Horst");
    horst.setProperty(GraphGroupSPITest.FAM_NAME_FIELD, "Horstsen");
    personNodes.add(horst, GraphGroupSPITest.ID_FIELD, GraphGroupSPITest.HORST_ID);

    // group relations
    johndoe.createRelationshipTo(famGroup, Neo4jRelTypes.OWNER_OF);
    johndoe.createRelationshipTo(famGroup, Neo4jRelTypes.MEMBER_OF);
    janedoe.createRelationshipTo(famGroup, Neo4jRelTypes.MEMBER_OF);

    janedoe.createRelationshipTo(denkGroup, Neo4jRelTypes.OWNER_OF);
    janedoe.createRelationshipTo(denkGroup, Neo4jRelTypes.MEMBER_OF);

    trans.success();
    trans.finish();
  }

  /**
   * Tests group retrieval for single people.
   *
   * @throws Exception
   *           if an exception occurs
   */
  @SuppressWarnings("unchecked")
  @Test
  public void groupRetrievalTest() throws Exception {
    final Map<String, Object> collOpts = new HashMap<String, Object>();

    // jane - member in both groups
    ListResult groupColl = this.fGroupSPI.getGroups(GraphGroupSPITest.JANE_ID, collOpts, null);
    List<Map<String, Object>> groups = (List<Map<String, Object>>) groupColl.getResults();

    Assert.assertEquals(2, groups.size());

    boolean famFound = false;
    boolean denkFound = false;

    String id = null;
    for (final Map<String, Object> group : groups) {
      id = (String) group.get(GraphGroupSPITest.ID_FIELD);

      if (id.equals(GraphGroupSPITest.DOE_FAM_ID)) {
        famFound = true;

        Assert.assertEquals("Doe family", group.get(GraphGroupSPITest.TITLE_FIELD));
      } else if (id.equals(GraphGroupSPITest.DENKER_ID)) {
        denkFound = true;

        Assert.assertEquals("Club der Denker", group.get(GraphGroupSPITest.TITLE_FIELD));
      } else {
        throw new Exception("unexpected group");
      }
    }
    Assert.assertTrue(famFound && denkFound);

    // john - only in family group
    groupColl = this.fGroupSPI.getGroups(GraphGroupSPITest.JOHN_ID, collOpts, null);
    groups = (List<Map<String, Object>>) groupColl.getResults();

    Assert.assertEquals(1, groups.size());
    Assert.assertEquals(GraphGroupSPITest.DOE_FAM_ID, groups.get(0).get(GraphGroupSPITest.ID_FIELD));

    // horst - not in any group
    groupColl = this.fGroupSPI.getGroups(GraphGroupSPITest.HORST_ID, collOpts, null);

    Assert.assertEquals(0, groupColl.getSize());

  }
}
