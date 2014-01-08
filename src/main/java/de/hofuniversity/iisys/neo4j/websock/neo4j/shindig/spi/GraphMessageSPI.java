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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.service.IDManager;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphMessage;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphMessageCollection;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeFilter;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.NodeSorter;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Implementation of the message service retrieving message and message collection data from the
 * Neo4j graph database.
 */
public class GraphMessageSPI {
  // TODO: verify fields
  private static final String ID_FIELD = "id";
  private static final String TITLE_FIELD = "title";
  private static final String STATUS_FIELD = "status";
  private static final String TIME_SENT_FIELD = "timeSent";
  private static final String UPDATED_FIELD = "updated";
  private static final String IN_REPLY_TO_FIELD = "inReplyTo";
  private static final String RECIPIENTS_FIELD = "recipients";

  private static final String NEW_NAME = "NEW";

  private static final String ALL_NAME = "@all";
  private static final String OUTBOX_NAME = "@outbox";

  private final GraphDatabaseService fDatabase;
  private final GraphPersonSPI fPersonSPI;
  private final Index<Node> fMessageNodes;
  private final IDManager fIDMan;

  private final ImplUtil fImpl;

  private final Logger fLogger;

  /**
   * Creates a graph person service using data from the given person data provider and the given
   * graph database. Throws a NullPointerException if one of the given services is null.
   *
   * @param database
   *          graph database to use
   * @param personSPI
   *          person data provider to use
   * @param idMan
   *          ID manager to use
   * @param listClass
   *          list implementation to use
   * @param mapClass
   *          map implementation to use
   */
  public GraphMessageSPI(GraphDatabaseService database, GraphPersonSPI personSPI, IDManager idMan,
          ImplUtil impl) {
    if (database == null) {
      throw new NullPointerException("database service was null");
    }
    if (personSPI == null) {
      throw new NullPointerException("person service was null");
    }
    if (idMan == null) {
      throw new NullPointerException("id manager was null");
    }
    if (impl == null) {
      throw new NullPointerException("list implementation was null");
    }

    this.fDatabase = database;
    this.fPersonSPI = personSPI;
    this.fMessageNodes = this.fDatabase.index().forNodes(ShindigConstants.MESSAGE_NODES);
    this.fIDMan = idMan;

    this.fImpl = impl;

    this.fLogger = Logger.getLogger(this.getClass().getName());
  }

  private Node getCollection(String userId, String collId) {
    final Node person = this.fPersonSPI.getPersonNode(userId);
    Node collection = null;

    if (person != null) {
      final Iterable<Relationship> hasColls = person.getRelationships(Neo4jRelTypes.OWNS);

      Node tmpColl = null;
      String tmpId = null;
      for (final Relationship rel : hasColls) {
        tmpColl = rel.getEndNode();
        tmpId = (String) tmpColl.getProperty(GraphMessageSPI.ID_FIELD);

        if (collId.equals(tmpId)) {
          collection = tmpColl;
          break;
        }
      }
    }

    return collection;
  }

  private List<Node> getCollectionNodes(String userId) {
    final List<Node> nodes = new LinkedList<Node>();

    final Node userNode = this.fPersonSPI.getPersonNode(userId);

    if (userNode != null) {
      final Iterable<Relationship> hasColls = userNode.getRelationships(Neo4jRelTypes.OWNS);

      Node collection = null;
      for (final Relationship rel : hasColls) {
        collection = rel.getEndNode();
        nodes.add(collection);
      }
    }

    return nodes;
  }

  /**
   * Retrieves all message collections for the given user.
   *
   * @param userId
   *          user to retrieve message collections for
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @return list of message collections
   */
  public ListResult getMessageCollections(String userId, Map<String, Object> options,
          List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    final List<Node> collNodes = getCollectionNodes(userId);

    final List<Map<String, Object>> collections = this.fImpl.newList();

    // filter
    NodeFilter.filterNodes(collNodes, options);
    // TODO: other filters?

    // sort as defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null) {
      options.put(WebsockConstants.SORT_FIELD, GraphMessageSPI.TITLE_FIELD);
    }
    NodeSorter.sortNodes(collNodes, options);

    // determine the first and last index of entries to fetch
    int max = 0;
    if (options.get(WebsockConstants.SUBSET_SIZE) != null) {
      max = (Integer) options.get(WebsockConstants.SUBSET_SIZE);
    }

    // if parameters are undefined, return all
    if (max == 0) {
      max = collNodes.size();
    }

    int first = 0;
    if (options.get(WebsockConstants.SUBSET_START) != null) {
      first = (Integer) options.get(WebsockConstants.SUBSET_START);
    }

    final int last = Math.min(collNodes.size(), first + max);

    // convert the items requested
    Map<String, Object> dto = null;
    for (int index = first; index < last; ++index) {
      dto = new GraphMessageCollection(collNodes.get(index), this.fImpl).toMap(fieldSet);
      collections.add(dto);
    }

    final ListResult collColl = new ListResult(collections);
    collColl.setFirst(first);
    collColl.setMax(max);
    collColl.setTotal(collNodes.size());
    return collColl;
  }

  /**
   * Creates a new message collection based on the given data, using the given ID if possible.
   * Otherwise a new ID will be generated.
   *
   * @param userId
   *          user to retrieve message collections for
   * @param msgCollection
   *          message collection data
   * @return newly created message collection
   */
  public SingleResult createMessageCollection(String userId, Map<String, Object> msgCollection) {
    final Node person = this.fPersonSPI.getPersonNode(userId);
    if (person == null) {
      throw new RuntimeException("person not found");
    }
    GraphMessageCollection gMessColl = null;

    // generate ID if necessary
    if (msgCollection.get(GraphMessageSPI.ID_FIELD) == null
            || msgCollection.get(GraphMessageSPI.ID_FIELD).toString().isEmpty()) {
      msgCollection.put(GraphMessageSPI.ID_FIELD,
              this.fIDMan.genID(ShindigConstants.MESSAGE_COLLECTION_NODES));
    }

    final Transaction tx = this.fDatabase.beginTx();

    try {
      final Node collNode = this.fDatabase.createNode();

      // link to user
      person.createRelationshipTo(collNode, Neo4jRelTypes.OWNS);

      // set properties
      gMessColl = new GraphMessageCollection(collNode, this.fImpl);
      gMessColl.setData(msgCollection);

      tx.success();
      tx.finish();
    } catch (final Exception e) {
      tx.failure();
      tx.finish();

      throw new RuntimeException("message collection could not be created:\n" + e.getMessage());
    }

    return new SingleResult(gMessColl.toMap(null));
  }

  /**
   * Modifies the specified message collection using the given data for the given user ID.
   *
   * @param userId
   *          ID of the user to modify a collection for
   * @param msgCollection
   *          new message collection data
   */
  public void modifyMessageCollection(String userId, Map<String, Object> msgCollection) {
    final Node person = this.fPersonSPI.getPersonNode(userId);
    if (person == null) {
      throw new RuntimeException("person not found");
    }

    // find message collection
    final String id = msgCollection.get(GraphMessageSPI.ID_FIELD).toString();
    Node collection = null;

    final Iterable<Relationship> relIter = person.getRelationships(Neo4jRelTypes.OWNS,
            Direction.OUTGOING);
    Node tmpNode = null;
    for (final Relationship rel : relIter) {
      tmpNode = rel.getEndNode();
      if (id.equals(tmpNode.getProperty(GraphMessageSPI.ID_FIELD))) {
        collection = tmpNode;
        break;
      }
    }

    if (collection == null) {
      throw new RuntimeException("collection not found");
    }

    // update
    final Transaction tx = this.fDatabase.beginTx();

    try {
      new GraphMessageCollection(collection, this.fImpl).setData(msgCollection);

      tx.success();
      tx.finish();
    } catch (final Exception e) {
      tx.failure();
      tx.finish();

      throw new RuntimeException("message collection could not be updated:\n" + e.getMessage());
    }
  }

  /**
   * Deletes the specified message collection for the given user ID.
   *
   * @param userId
   *          ID of the user to modify a collection for
   * @param msgCollId
   *          ID of the collection to delete
   */
  public void deleteMessageCollection(String userId, final String msgCollId) {
    final Node person = this.fPersonSPI.getPersonNode(userId);

    // get collection
    Node collection = null;

    final Iterable<Relationship> relIter = person.getRelationships(Neo4jRelTypes.OWNS,
            Direction.OUTGOING);
    Node tmpNode = null;
    for (final Relationship rel : relIter) {
      tmpNode = rel.getEndNode();
      if (msgCollId.equals(tmpNode.getProperty(GraphMessageSPI.ID_FIELD))) {
        collection = tmpNode;
        break;
      }
    }

    if (collection == null) {
      throw new RuntimeException("collection not found");
    }

    // check which messages were in that collection
    final List<Node> msgs = new LinkedList<Node>();
    final Iterable<Relationship> conRels = collection.getRelationships(Neo4jRelTypes.CONTAINS,
            Direction.OUTGOING);
    for (final Relationship rel : conRels) {
      msgs.add(rel.getEndNode());
    }

    final Transaction tx = this.fDatabase.beginTx();
    try {
      // delete the collection
      Iterable<Relationship> rels = collection.getRelationships();
      for (final Relationship rel : rels) {
        rel.delete();
      }
      collection.delete();

      // delete all messages that are not stored in any collection anymore
      // TODO: possibility to deactivate
      for (final Node msg : msgs) {
        if (!msg.hasRelationship(Neo4jRelTypes.CONTAINS, Direction.INCOMING)) {
          rels = msg.getRelationships();

          for (final Relationship rel : rels) {
            rel.delete();
          }
          msg.delete();
        }
      }

      tx.success();
      tx.finish();
    } catch (final Exception e) {
      tx.failure();
      tx.finish();

      throw new RuntimeException("message collection could not be deleted:\n" + e.getMessage());
    }
  }

  private List<Relationship> addMessages(Node collection, final List<String> msgIds,
          final List<Node> messList) {
    final List<Relationship> rels = new LinkedList<Relationship>();
    // TODO: are message IDs unique? - maybe build an index

    Node messNode = null;
    final Iterable<Relationship> conRels = collection.getRelationships(Neo4jRelTypes.CONTAINS);

    if (msgIds == null || msgIds.isEmpty()) {
      // add all messages if none are specified
      for (final Relationship rel : conRels) {
        rels.add(rel);
        messNode = rel.getEndNode();
        messList.add(messNode);
      }
    } else {
      // check all message nodes whether they are requested by id
      String msgId = null;
      for (final Relationship rel : conRels) {
        messNode = rel.getEndNode();
        msgId = (String) messNode.getProperty(GraphMessageSPI.ID_FIELD);

        if (msgIds.contains(msgId)) {
          rels.add(rel);
          messList.add(messNode);
        }
      }
    }

    return rels;
  }

  private List<Relationship> addAllMessages(String userId, final List<String> msgIds,
          final List<Node> messList) {
    final List<Relationship> rels = new LinkedList<Relationship>();
    final Node person = this.fPersonSPI.getPersonNode(userId);
    Node msgNode = null;
    String msgId = null;

    // received and sent messages
    final Iterable<Relationship> msgRels = person.getRelationships(Neo4jRelTypes.SENT_TO,
            Neo4jRelTypes.SENT);

    if (msgIds == null || msgIds.isEmpty()) {
      for (final Relationship rel : msgRels) {
        rels.add(rel);
        msgNode = rel.getOtherNode(person);
        messList.add(msgNode);
      }
    } else {
      for (final Relationship rel : msgRels) {
        msgNode = rel.getOtherNode(person);
        msgId = (String) msgNode.getProperty(GraphMessageSPI.ID_FIELD);

        if (msgIds.contains(msgId)) {
          rels.add(rel);
          messList.add(msgNode);
        }
      }
    }

    return rels;
  }

  /**
   * Retrieves messages for the user and message collection specified, optionally only the messages
   * specified by their ID.
   *
   * @param userId
   *          user to retrieve messages for
   * @param msgCollId
   *          message collection to retrieve messages from
   * @param msgIds
   *          list of message IDs to retrieve
   * @param options
   *          retrieval options
   * @param fields
   *          fields to retrieve
   * @return list of messages
   */
  public ListResult getMessages(String userId, String msgCollId, List<String> msgIds,
          Map<String, Object> options, List<String> fields) {
    final Set<String> fieldSet = new HashSet<String>();
    if (fields != null) {
      fieldSet.addAll(fields);
    }
    final List<Node> messNodes = new LinkedList<Node>();
    List<Relationship> messRels = null;

    final List<Map<String, Object>> messages = this.fImpl.newList();

    if (msgCollId == null || msgCollId.equals(GraphMessageSPI.ALL_NAME)) {
      // go directly over sent/received relationships
      messRels = addAllMessages(userId, msgIds, messNodes);
    } else {
      final Node collNode = getCollection(userId, msgCollId);
      messRels = addMessages(collNode, msgIds, messNodes);
    }

    // filter
    NodeFilter.filterNodes(messNodes, options);
    // TODO: other filters?

    // sort as defined by parameters
    final String sortField = (String) options.get(WebsockConstants.SORT_FIELD);
    if (sortField == null) {
      options.put(WebsockConstants.SORT_FIELD, GraphMessageSPI.ID_FIELD);
    }
    NodeSorter.sortNodes(messNodes, options);

    // determine the first and last index of entries to fetch
    int max = 0;
    if (options.get(WebsockConstants.SUBSET_SIZE) != null) {
      max = (Integer) options.get(WebsockConstants.SUBSET_SIZE);
    }

    // if parameters are undefined, return all
    if (max == 0) {
      max = messNodes.size();
    }

    int first = 0;
    if (options.get(WebsockConstants.SUBSET_START) != null) {
      first = (Integer) options.get(WebsockConstants.SUBSET_START);
    }

    final int last = Math.min(messNodes.size(), first + max);

    // convert the items requested
    Map<String, Object> dto = null;
    for (int index = first; index < last; ++index) {
      dto = new GraphMessage(messNodes.get(index), messRels.get(index), this.fImpl).toMap(fieldSet);
      messages.add(dto);
    }

    final ListResult messageColl = new ListResult(messages);
    messageColl.setFirst(first);
    messageColl.setMax(max);
    messageColl.setTotal(messNodes.size());
    return messageColl;
  }

  private void addReply(String msgId, Node reply) {
    try {
      final IndexHits<Node> hits = this.fMessageNodes.get(GraphMessageSPI.ID_FIELD, msgId);
      final Node message = hits.getSingle();
      hits.close();
      reply.createRelationshipTo(message, Neo4jRelTypes.REPLY_TO);
    } catch (final Exception e) {
      System.err.println("Invalid 'reply to' ID");
      e.printStackTrace();
    }
  }

  /**
   * Creates a message for a user in the message collection specified, sending it to all recipients.
   *
   * @param userId
   *          user to create the message for
   * @param appId
   *          application to create the message for
   * @param msgCollId
   *          message collection to create the message in
   * @param message
   *          message data to use
   */
  public void createMessage(String userId, String appId, String msgCollId,
          Map<String, Object> message) {

    final Transaction tx = this.fDatabase.beginTx();

    try {
      final String id = this.fIDMan.genID(ShindigConstants.MESSAGE_NODES);

      // create message node, set properties, register
      final Node msgNode = this.fDatabase.createNode();
      message.put(GraphMessageSPI.ID_FIELD, id);

      final Long time = System.currentTimeMillis();
      message.put(GraphMessageSPI.TIME_SENT_FIELD, time);
      message.put(GraphMessageSPI.UPDATED_FIELD, time);

      new GraphMessage(msgNode, null).setData(message);
      this.fMessageNodes.add(msgNode, GraphMessageSPI.ID_FIELD, id);

      // add to own collection
      final Node sender = this.fPersonSPI.getPersonNode(userId);
      if (msgCollId == null || msgCollId.isEmpty()) {
        throw new RuntimeException("no message collection specified");
      }
      // messages can't be created in the own inbox
      if (OSFields.INBOX_NAME.equals(msgCollId)) {
        throw new RuntimeException("messages can not be added to the inbox manually");
      }
      final Node coll = getCollection(userId, msgCollId);

      sender.createRelationshipTo(msgNode, Neo4jRelTypes.SENT);
      coll.createRelationshipTo(msgNode, Neo4jRelTypes.CONTAINS);

      // check for reply status and link
      final String repTo = (String) message.get(GraphMessageSPI.IN_REPLY_TO_FIELD);
      if (repTo != null && !repTo.isEmpty()) {
        addReply(repTo, msgNode);
      }

      // send to recipients (link and put in in box)
      if (msgCollId.equals(GraphMessageSPI.OUTBOX_NAME)) {
        send(message, msgNode);
      }

      tx.success();
      tx.finish();
    } catch (final Exception e) {
      tx.failure();
      tx.finish();

      throw e;
    }
  }

  private void send(final Map<String, Object> message, final Node msgNode) {
    final String[] recipients = (String[]) message.get(GraphMessageSPI.RECIPIENTS_FIELD);
    Node recNode = null;
    Node collNode = null;
    Relationship conRel = null;

    if (recipients == null || recipients.length == 0) {
      throw new RuntimeException("no recipients to send to");
    }

    for (final String recId : recipients) {
      recNode = this.fPersonSPI.getPersonNode(recId);
      collNode = getCollection(recId, OSFields.INBOX_NAME);

      msgNode.createRelationshipTo(recNode, Neo4jRelTypes.SENT_TO);
      conRel = collNode.createRelationshipTo(msgNode, Neo4jRelTypes.CONTAINS);
      conRel.setProperty(GraphMessageSPI.STATUS_FIELD, GraphMessageSPI.NEW_NAME);
    }
  }

  private void delete(Node collection, List<String> ids) {
    int deleted = 0;
    final int toDelete = ids.size();
    Node message = null;
    final Iterable<Relationship> relIter = collection.getRelationships(Neo4jRelTypes.CONTAINS);

    for (final Relationship conRel : relIter) {
      message = conRel.getEndNode();

      if (ids.contains(message.getProperty(GraphMessageSPI.ID_FIELD))) {
        // remove from person's collection
        conRel.delete();

        // TODO: also delete SENT or SENT_TO?

        // check if anybody still has this message, delete otherwise
        if (message.hasRelationship(Neo4jRelTypes.CONTAINS, Direction.INCOMING)) {
          this.fMessageNodes.remove(message);
          for (final Relationship rel : message.getRelationships()) {
            rel.delete();
          }
          message.delete();
        }

        // break if all were deleted
        if (++deleted == toDelete) {
          break;
        }
      }
    }
  }

  /**
   * Deletes a list of messages identified by their ID for a user in the specified message
   * collection.
   *
   * @param userId
   *          user to delete messages for
   * @param msgCollId
   *          message collection to delete messages from
   * @param ids
   *          list of message IDs to delete
   */
  public void deleteMessages(String userId, String msgCollId, List<String> ids) {
    final Node collNode = getCollection(userId, msgCollId);

    final Transaction tx = this.fDatabase.beginTx();

    try {
      delete(collNode, ids);

      tx.success();
      tx.finish();
    } catch (final Exception e) {
      tx.failure();
      tx.finish();

      throw new RuntimeException("messages could not be deleted:\n" + e.getMessage());
    }
  }

  /**
   * Modifies a message for a user with the given data.
   *
   * @param userId
   *          ID of the user to modify the message for
   * @param msgCollId
   *          ID of the collection to modify the message in
   * @param messageId
   *          ID of the message to modify
   * @param message
   *          map containing the new message data
   */
  public void modifyMessage(String userId, String msgCollId, final String messageId,
          Map<String, Object> message) {
    // get message node and appropriate "contains" relationship
    Node msgNode = null;
    Relationship conRel = null;

    final Node collection = getCollection(userId, msgCollId);
    final Iterable<Relationship> conIter = collection.getRelationships(Neo4jRelTypes.CONTAINS,
            Direction.OUTGOING);

    Node tmpNode = null;
    for (final Relationship rel : conIter) {
      tmpNode = rel.getEndNode();

      if (messageId.equals(tmpNode.getProperty(GraphMessageSPI.ID_FIELD))) {
        conRel = rel;
        msgNode = tmpNode;
        break;
      }
    }

    if (msgNode == null) {
      throw new RuntimeException("messages could not found");
    }

    // only update messages that haven't already been sent
    if (!msgNode.hasRelationship(Direction.OUTGOING, Neo4jRelTypes.SENT_TO)) {
      final Transaction tx = this.fDatabase.beginTx();

      try {
        // update
        new GraphMessage(msgNode, conRel, this.fImpl).setData(message);

        tx.success();
        tx.finish();
      } catch (final Exception e) {
        tx.failure();
        tx.finish();

        throw e;
      }
    } else {
      throw new RuntimeException("messages can not be modified after they were sent");
    }
  }
}
