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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.ConvHelper;
import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.IGraphObject;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Converter class converting message objects stored in the graph to transferable objects and vice
 * versa.
 */
public class GraphMessage implements IGraphObject {
  private static final String COLL_IDS_FIELD = "collectionIds";
  private static final String REPLIES_FIELD = "replies";
  private static final String STATUS_FIELD = "status";

  private static final String APP_URL_FIELD = "appUrl";
  private static final String BODY_FIELD = "body";
  private static final String BODY_ID_FIELD = "bodyId";
  private static final String ID_FIELD = "id";
  private static final String REPLY_FIELD = "inReplyTo";
  private static final String SENDER_ID_FIELD = "senderId";
  private static final String TITLE_FIELD = "title";
  private static final String TITLE_ID_FIELD = "titleId";

  private static final String TYPE_FIELD = "type";
  private static final String TIME_FIELD = "timeSent";
  private static final String UPDATED_FIELD = "updated";
  private static final String RECIPIENTS_FIELD = "recipients";
  private static final String URLS_FIELD = "urls";

  private static final ConvHelper HELPER = createHelper();

  private final Node fNode;
  private final Relationship fStatusRel;

  private final ImplUtil fImpl;

  private static ConvHelper createHelper() {
    final Set<String> relMapped = new HashSet<String>();
    relMapped.add(GraphMessage.COLL_IDS_FIELD);
    relMapped.add(GraphMessage.REPLIES_FIELD);
    relMapped.add(GraphMessage.STATUS_FIELD);

    return new ConvHelper(null, relMapped, null);
  }

  /**
   * Creates a message converter, taking properties from the given node and optionally a status from
   * a given Relationship. Throws a NullPointerException if the node parameter is null.
   *
   * @param node
   *          node representing the message
   * @param statusRel
   *          relationship to get a message status from
   */
  public GraphMessage(Node node, Relationship statusRel) {
    this(node, statusRel, new ImplUtil(LinkedList.class, HashMap.class));
  }

  /**
   * Creates a message converter, taking properties from the given node and optionally a status from
   * a given Relationship. Throws a NullPointerException if the node parameter is null.
   *
   * @param node
   *          node representing the message
   * @param statusRel
   *          relationship to get a message status from
   */
  public GraphMessage(Node node, Relationship statusRel, ImplUtil impl) {
    if (node == null) {
      throw new NullPointerException("underlying node was null");
    }

    this.fNode = node;
    this.fStatusRel = statusRel;

    this.fImpl = impl;
  }

  @Override
  public Map<String, Object> toMap(Set<String> fields) {
    final Map<String, Object> dto = this.fImpl.newMap();

    if (fields == null || fields.isEmpty()) {
      for (final String key : this.fNode.getPropertyKeys()) {
        dto.put(key, this.fNode.getProperty(key));
      }

      copyAllRelMapped(dto);
    } else {
      copyRelMapped(dto, fields);

      final Set<String> newProps = new HashSet<String>(fields);
      newProps.removeAll(GraphMessage.HELPER.getRelationshipMapped());
      fields = newProps;

      for (final String prop : fields) {
        if (this.fNode.hasProperty(prop)) {
          dto.put(prop, this.fNode.getProperty(prop));
        }
      }
    }

    return dto;
  }

  private void copyRelMapped(final Map<String, Object> dto, final Set<String> properties) {
    if (properties.contains(GraphMessage.COLL_IDS_FIELD)) {
      copyCollIds(dto);
    }

    if (properties.contains(GraphMessage.REPLIES_FIELD)) {
      copyReplies(dto);
    }

    if (properties.contains(GraphMessage.STATUS_FIELD)) {
      copyStatus(dto);
    }
  }

  private void copyAllRelMapped(final Map<String, Object> dto) {
    copyCollIds(dto);
    copyReplies(dto);
    copyStatus(dto);
  }

  private void copyCollIds(Map<String, Object> dto) {
    /*
     * get IDs from the starting points (collections) of all relations that assign this message to a
     * message collection
     */
    final Set<String> collectionIds = new HashSet<String>();

    // get all message collections for the current user to match them
    final Set<Node> collNodes = new HashSet<Node>();
    if (this.fStatusRel != null) {
      final Node coll = this.fStatusRel.getStartNode();
      if (coll != null) {
        final Relationship userRel = coll.getSingleRelationship(Neo4jRelTypes.OWNS,
                Direction.INCOMING);

        if (userRel != null) {
          final Node user = userRel.getStartNode();

          final Iterable<Relationship> collRels = user.getRelationships(Neo4jRelTypes.OWNS,
                  Direction.OUTGOING);
          for (final Relationship rel : collRels) {
            collNodes.add(rel.getEndNode());
          }
        }
      }
    }

    // filter all "contained in" relationships
    final Iterable<Relationship> containedIns = this.fNode.getRelationships(Neo4jRelTypes.CONTAINS);

    Node collection = null;
    String collId = null;
    for (final Relationship rel : containedIns) {
      collection = rel.getStartNode();

      // filter out all collections that belong to a different user
      if (collNodes.contains(collection)) {
        collId = (String) collection.getProperty(GraphMessage.ID_FIELD);
        collectionIds.add(collId);
      }
    }

    if (!collectionIds.isEmpty()) {
      dto.put(GraphMessage.COLL_IDS_FIELD, collectionIds.toArray(new String[collectionIds.size()]));
    }
  }

  private void copyReplies(Map<String, Object> dto) {
    // get the IDs of all messages linked to this one as their parent
    final List<String> replies = new ArrayList<String>();

    final Iterable<Relationship> replyRels = this.fNode.getRelationships(Neo4jRelTypes.REPLY_TO,
            Direction.INCOMING);

    Node replyNode = null;
    String reply = null;
    for (final Relationship rel : replyRels) {
      replyNode = rel.getStartNode();
      reply = (String) replyNode.getProperty(GraphMessage.ID_FIELD);
      replies.add(reply);
    }

    if (!replies.isEmpty()) {
      dto.put(GraphMessage.REPLIES_FIELD, replies.toArray(new String[replies.size()]));
    }
  }

  private void copyStatus(Map<String, Object> dto) {
    /*
     * a message status is usually attached to the relation between a user's message collection and
     * the message itself
     */
    if (this.fStatusRel != null && this.fStatusRel.hasProperty(GraphMessage.STATUS_FIELD)) {
      final Object value = this.fStatusRel.getProperty(GraphMessage.STATUS_FIELD);
      final String status = (String) value;
      dto.put(GraphMessage.STATUS_FIELD, status);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setData(final Map<String, ?> message) {
    // collect new values
    final Map<String, Object> newValues = new HashMap<String, Object>();

    // atomic
    newValues.put(GraphMessage.APP_URL_FIELD, message.get(GraphMessage.APP_URL_FIELD));
    newValues.put(GraphMessage.BODY_FIELD, message.get(GraphMessage.BODY_FIELD));
    newValues.put(GraphMessage.BODY_ID_FIELD, message.get(GraphMessage.BODY_ID_FIELD));
    newValues.put(GraphMessage.ID_FIELD, message.get(GraphMessage.ID_FIELD));
    newValues.put(GraphMessage.REPLY_FIELD, message.get(GraphMessage.REPLY_FIELD));
    newValues.put(GraphMessage.SENDER_ID_FIELD, message.get(GraphMessage.SENDER_ID_FIELD));
    newValues.put(GraphMessage.TITLE_FIELD, message.get(GraphMessage.TITLE_FIELD));
    newValues.put(GraphMessage.TITLE_ID_FIELD, message.get(GraphMessage.TITLE_ID_FIELD));
    newValues.put(GraphMessage.TYPE_FIELD, message.get(GraphMessage.TYPE_FIELD));
    newValues.put(GraphMessage.TIME_FIELD, message.get(GraphMessage.TIME_FIELD));
    newValues.put(GraphMessage.UPDATED_FIELD, message.get(GraphMessage.UPDATED_FIELD));

    // lists
    final Object recVal = message.get(GraphMessage.RECIPIENTS_FIELD);

    if (recVal != null) {
      String[] recipients = null;

      if (recVal instanceof String[]) {
        recipients = (String[]) recVal;
      } else {
        final List<String> recList = (List<String>) recVal;
        recipients = recList.toArray(new String[recList.size()]);
      }

      newValues.put(GraphMessage.RECIPIENTS_FIELD, recipients);
    }

    final Object urlVal = message.get(GraphMessage.URLS_FIELD);
    if (urlVal != null) {
      String[] urls = null;

      if (urlVal instanceof String[]) {
        urls = (String[]) urlVal;
      } else {
        final List<String> urlList = (List<String>) urlVal;
        urls = urlList.toArray(new String[urlList.size()]);
      }

      newValues.put(GraphMessage.URLS_FIELD, urls);
    }

    // set new values
    for (final Entry<String, Object> valE : newValues.entrySet()) {
      if (valE.getValue() != null) {
        this.fNode.setProperty(valE.getKey(), valE.getValue());
      }
    }
  }

}
