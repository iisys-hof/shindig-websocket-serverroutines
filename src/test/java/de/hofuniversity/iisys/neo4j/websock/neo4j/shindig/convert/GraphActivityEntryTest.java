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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.SimpleGraphObject;

/**
 * Test for the activity entry and activity object converter classes.
 */
public class GraphActivityEntryTest {
  private static final String CONTENT_FIELD = "content";
  private static final String ID_FIELD = "id";
  private static final String NAME_FIELD = "displayName";
  private static final String TYPE_FIELD = "objectType";
  private static final String PUBLISHED_FIELD = "published";
  private static final String SUMMARY_FIELD = "summary";
  private static final String TITLE_FIELD = "title";
  private static final String UPDATED_FIELD = "updated";
  private static final String URL_FIELD = "url";
  private static final String VERB_FIELD = "verb";

  private static final String CONTENT = "activity content";
  private static final String ID = "activity id";
  private static final String PUBLISHED = "activity published";
  private static final String TITLE = "activity title";
  private static final String UPDATED = "activity updated";
  private static final String URL = "activity url";
  private static final String VERB = "activity verb";

  private static final String OBJ_CONTENT = "activity object content";
  private static final String OBJ_DISP_NAME = "activity object display name";
  private static final String OBJ_TYPE = "activity object object type";
  private static final String OBJ_PUBLISHED = "activity object published";
  private static final String OBJ_SUMMARY = "activity object summary";
  private static final String OBJ_UPDATED = "activity object updated";
  private static final String OBJ_URL = "activity object url";

  private static final String ACTOR_ID = "actor";
  private static final String OBJECT_ID = "object";
  private static final String TARGET_ID = "target";
  private static final String PROVIDER_ID = "provider";
  private static final String GENERATOR_ID = "generator";

  private GraphDatabaseService fDb;
  private Node fEntryNode, fActorNode, fObjectNode, fTargetNode, fProviderNode, fGeneratorNode;

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
        if (GraphActivityEntryTest.this.fDb != null) {
          GraphActivityEntryTest.this.fDb.shutdown();
        }
      }
    });

    createTestData();
  }

  private void createTestData() {
    final Transaction trans = this.fDb.beginTx();

    this.fEntryNode = this.fDb.createNode();
    this.fActorNode = this.fDb.createNode();
    this.fObjectNode = this.fDb.createNode();
    this.fTargetNode = this.fDb.createNode();
    this.fGeneratorNode = this.fDb.createNode();
    this.fProviderNode = this.fDb.createNode();

    this.fEntryNode.createRelationshipTo(this.fActorNode, Neo4jRelTypes.ACTOR);
    this.fEntryNode.createRelationshipTo(this.fObjectNode, Neo4jRelTypes.OBJECT);
    this.fEntryNode.createRelationshipTo(this.fTargetNode, Neo4jRelTypes.TARGET);
    this.fEntryNode.createRelationshipTo(this.fGeneratorNode, Neo4jRelTypes.GENERATOR);
    this.fEntryNode.createRelationshipTo(this.fProviderNode, Neo4jRelTypes.PROVIDER);

    this.fEntryNode.setProperty(GraphActivityEntryTest.CONTENT_FIELD,
            GraphActivityEntryTest.CONTENT);
    this.fEntryNode.setProperty(GraphActivityEntryTest.ID_FIELD, GraphActivityEntryTest.ID);
    this.fEntryNode.setProperty(GraphActivityEntryTest.PUBLISHED_FIELD,
            GraphActivityEntryTest.PUBLISHED);
    this.fEntryNode.setProperty(GraphActivityEntryTest.TITLE_FIELD, GraphActivityEntryTest.TITLE);
    this.fEntryNode.setProperty(GraphActivityEntryTest.UPDATED_FIELD,
            GraphActivityEntryTest.UPDATED);
    this.fEntryNode.setProperty(GraphActivityEntryTest.URL_FIELD, GraphActivityEntryTest.URL);
    this.fEntryNode.setProperty(GraphActivityEntryTest.VERB_FIELD, GraphActivityEntryTest.VERB);

    fillActObj(this.fActorNode);
    this.fActorNode.setProperty(GraphActivityEntryTest.ID_FIELD, GraphActivityEntryTest.ACTOR_ID);

    fillActObj(this.fGeneratorNode);
    this.fGeneratorNode.setProperty(GraphActivityEntryTest.ID_FIELD,
            GraphActivityEntryTest.GENERATOR_ID);

    fillActObj(this.fObjectNode);
    this.fObjectNode.setProperty(GraphActivityEntryTest.ID_FIELD, GraphActivityEntryTest.OBJECT_ID);

    fillActObj(this.fProviderNode);
    this.fProviderNode.setProperty(GraphActivityEntryTest.ID_FIELD,
            GraphActivityEntryTest.PROVIDER_ID);

    fillActObj(this.fTargetNode);
    this.fTargetNode.setProperty(GraphActivityEntryTest.ID_FIELD, GraphActivityEntryTest.TARGET_ID);

    trans.success();
    trans.finish();
  }

  private void fillActObj(Node node) {
    node.setProperty(GraphActivityEntryTest.CONTENT_FIELD, GraphActivityEntryTest.OBJ_CONTENT);
    node.setProperty(GraphActivityEntryTest.NAME_FIELD, GraphActivityEntryTest.OBJ_DISP_NAME);
    node.setProperty(GraphActivityEntryTest.TYPE_FIELD, GraphActivityEntryTest.OBJ_TYPE);
    node.setProperty(GraphActivityEntryTest.PUBLISHED_FIELD, GraphActivityEntryTest.OBJ_PUBLISHED);
    node.setProperty(GraphActivityEntryTest.SUMMARY_FIELD, GraphActivityEntryTest.OBJ_SUMMARY);
    node.setProperty(GraphActivityEntryTest.UPDATED_FIELD, GraphActivityEntryTest.OBJ_UPDATED);
    node.setProperty(GraphActivityEntryTest.URL_FIELD, GraphActivityEntryTest.OBJ_URL);
  }

  /**
   * Test for conversion of existing data.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void conversionTest() {
    final Map<String, Object> actE = new GraphActivityEntry(this.fEntryNode).toMap(null);

    Assert.assertEquals(GraphActivityEntryTest.CONTENT,
            actE.get(GraphActivityEntryTest.CONTENT_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.ID, actE.get(GraphActivityEntryTest.ID_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.PUBLISHED,
            actE.get(GraphActivityEntryTest.PUBLISHED_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.TITLE, actE.get(GraphActivityEntryTest.TITLE_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.UPDATED,
            actE.get(GraphActivityEntryTest.UPDATED_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.URL, actE.get(GraphActivityEntryTest.URL_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.VERB, actE.get(GraphActivityEntryTest.VERB_FIELD));

    final Map<String, Object> actor = (Map<String, Object>) actE
            .get(GraphActivityEntryTest.ACTOR_ID);
    Assert.assertEquals(GraphActivityEntryTest.ACTOR_ID, actor.get(GraphActivityEntryTest.ID_FIELD));
    validateObject(actor);

    final Map<String, Object> object = (Map<String, Object>) actE
            .get(GraphActivityEntryTest.OBJECT_ID);
    Assert.assertEquals(GraphActivityEntryTest.OBJECT_ID,
            object.get(GraphActivityEntryTest.ID_FIELD));
    validateObject(object);

    final Map<String, Object> target = (Map<String, Object>) actE
            .get(GraphActivityEntryTest.TARGET_ID);
    Assert.assertEquals(GraphActivityEntryTest.TARGET_ID,
            target.get(GraphActivityEntryTest.ID_FIELD));
    validateObject(target);

    final Map<String, Object> generator = (Map<String, Object>) actE
            .get(GraphActivityEntryTest.GENERATOR_ID);
    Assert.assertEquals(GraphActivityEntryTest.GENERATOR_ID,
            generator.get(GraphActivityEntryTest.ID_FIELD));
    validateObject(target);

    final Map<String, Object> provider = (Map<String, Object>) actE
            .get(GraphActivityEntryTest.PROVIDER_ID);
    Assert.assertEquals(GraphActivityEntryTest.PROVIDER_ID,
            provider.get(GraphActivityEntryTest.ID_FIELD));
    validateObject(provider);
  }

  private void validateObject(Map<String, Object> obj) {
    Assert.assertEquals(GraphActivityEntryTest.OBJ_CONTENT,
            obj.get(GraphActivityEntryTest.CONTENT_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.OBJ_DISP_NAME,
            obj.get(GraphActivityEntryTest.NAME_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.OBJ_PUBLISHED,
            obj.get(GraphActivityEntryTest.PUBLISHED_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.OBJ_SUMMARY,
            obj.get(GraphActivityEntryTest.SUMMARY_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.OBJ_TYPE, obj.get(GraphActivityEntryTest.TYPE_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.OBJ_UPDATED,
            obj.get(GraphActivityEntryTest.UPDATED_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.OBJ_URL, obj.get(GraphActivityEntryTest.URL_FIELD));
  }

  /**
   * Test for value storing capabilities.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void storageTest() {
    final GraphActivityEntry gEntry = new GraphActivityEntry(this.fEntryNode);
    final Map<String, Object> actE = gEntry.toMap(null);

    final Map<String, Object> modActE = new HashMap<String, Object>();
    modActE.put(GraphActivityEntryTest.CONTENT_FIELD,
            actE.get(GraphActivityEntryTest.CONTENT_FIELD) + " mod");
    modActE.put(GraphActivityEntryTest.PUBLISHED_FIELD,
            actE.get(GraphActivityEntryTest.PUBLISHED_FIELD) + " mod");
    modActE.put(GraphActivityEntryTest.TITLE_FIELD, actE.get(GraphActivityEntryTest.TITLE_FIELD)
            + " mod");
    modActE.put(GraphActivityEntryTest.UPDATED_FIELD,
            actE.get(GraphActivityEntryTest.UPDATED_FIELD) + " mod");
    modActE.put(GraphActivityEntryTest.URL_FIELD, actE.get(GraphActivityEntryTest.URL_FIELD)
            + " mod");
    modActE.put(GraphActivityEntryTest.VERB_FIELD, actE.get(GraphActivityEntryTest.VERB_FIELD)
            + " mod");

    Map<String, Object> actor = (Map<String, Object>) actE.get(GraphActivityEntryTest.ACTOR_ID);
    actor = modifyObject(actor);

    Map<String, Object> object = (Map<String, Object>) actE.get(GraphActivityEntryTest.OBJECT_ID);
    object = modifyObject(object);

    Map<String, Object> target = (Map<String, Object>) actE.get(GraphActivityEntryTest.TARGET_ID);
    target = modifyObject(target);

    Map<String, Object> generator = (Map<String, Object>) actE
            .get(GraphActivityEntryTest.GENERATOR_ID);
    generator = modifyObject(generator);

    Map<String, Object> provider = (Map<String, Object>) actE
            .get(GraphActivityEntryTest.PROVIDER_ID);
    provider = modifyObject(provider);

    final Transaction trans = this.fDb.beginTx();

    new SimpleGraphObject(this.fActorNode).setData(actor);
    new SimpleGraphObject(this.fObjectNode).setData(object);
    new SimpleGraphObject(this.fTargetNode).setData(target);
    new SimpleGraphObject(this.fProviderNode).setData(provider);
    new SimpleGraphObject(this.fGeneratorNode).setData(generator);

    gEntry.setData(modActE);

    trans.success();
    trans.finish();

    Assert.assertEquals(GraphActivityEntryTest.CONTENT + " mod",
            this.fEntryNode.getProperty(GraphActivityEntryTest.CONTENT_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.PUBLISHED + " mod",
            this.fEntryNode.getProperty(GraphActivityEntryTest.PUBLISHED_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.TITLE + " mod",
            this.fEntryNode.getProperty(GraphActivityEntryTest.TITLE_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.UPDATED + " mod",
            this.fEntryNode.getProperty(GraphActivityEntryTest.UPDATED_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.URL + " mod",
            this.fEntryNode.getProperty(GraphActivityEntryTest.URL_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.VERB + " mod",
            this.fEntryNode.getProperty(GraphActivityEntryTest.VERB_FIELD));

    validateObject(this.fActorNode);
    validateObject(this.fObjectNode);
    validateObject(this.fTargetNode);
    validateObject(this.fGeneratorNode);
    validateObject(this.fProviderNode);
  }

  private Map<String, Object> modifyObject(Map<String, Object> obj) {
    final Map<String, Object> modObj = new HashMap<String, Object>();

    modObj.put(GraphActivityEntryTest.ID_FIELD, obj.get(GraphActivityEntryTest.ID_FIELD) + " mod");
    modObj.put(GraphActivityEntryTest.CONTENT_FIELD, obj.get(GraphActivityEntryTest.CONTENT_FIELD)
            + " mod");
    modObj.put(GraphActivityEntryTest.NAME_FIELD, obj.get(GraphActivityEntryTest.NAME_FIELD)
            + " mod");
    modObj.put(GraphActivityEntryTest.TYPE_FIELD, obj.get(GraphActivityEntryTest.TYPE_FIELD)
            + " mod");
    modObj.put(GraphActivityEntryTest.PUBLISHED_FIELD,
            obj.get(GraphActivityEntryTest.PUBLISHED_FIELD) + " mod");
    modObj.put(GraphActivityEntryTest.SUMMARY_FIELD, obj.get(GraphActivityEntryTest.SUMMARY_FIELD)
            + " mod");
    modObj.put(GraphActivityEntryTest.UPDATED_FIELD, obj.get(GraphActivityEntryTest.UPDATED_FIELD)
            + " mod");
    modObj.put(GraphActivityEntryTest.URL_FIELD, obj.get(GraphActivityEntryTest.URL_FIELD) + " mod");

    return modObj;
  }

  private void validateObject(Node obj) {
    Assert.assertTrue(obj.getProperty(GraphActivityEntryTest.ID_FIELD).toString().endsWith(" mod"));

    Assert.assertEquals(GraphActivityEntryTest.OBJ_CONTENT + " mod",
            obj.getProperty(GraphActivityEntryTest.CONTENT_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.OBJ_DISP_NAME + " mod",
            obj.getProperty(GraphActivityEntryTest.NAME_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.OBJ_PUBLISHED + " mod",
            obj.getProperty(GraphActivityEntryTest.PUBLISHED_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.OBJ_SUMMARY + " mod",
            obj.getProperty(GraphActivityEntryTest.SUMMARY_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.OBJ_TYPE + " mod",
            obj.getProperty(GraphActivityEntryTest.TYPE_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.OBJ_UPDATED + " mod",
            obj.getProperty(GraphActivityEntryTest.UPDATED_FIELD));
    Assert.assertEquals(GraphActivityEntryTest.OBJ_URL + " mod",
            obj.getProperty(GraphActivityEntryTest.URL_FIELD));
  }
}
