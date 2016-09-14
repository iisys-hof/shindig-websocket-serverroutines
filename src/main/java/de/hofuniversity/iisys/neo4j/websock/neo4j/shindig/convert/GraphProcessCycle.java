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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.IGraphObject;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util.ShindigRelTypes;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

public class GraphProcessCycle implements IGraphObject {

  private static final String DOCTYPE_FIELD = "type";
  private static final String DOCID_FIELD = "docId";
  private static final String START_FIELD = "startDate";
  private static final String END_FIELD = "endDate";
  private static final String USERLIST_FIELD = "userList";

  private static final Set<String> USERLIST_FIELDS;
  
  private final Logger fLogger;

  static {
    USERLIST_FIELDS = new HashSet<String>();
    GraphProcessCycle.USERLIST_FIELDS.add("id");
    GraphProcessCycle.USERLIST_FIELDS.add("displayName");
  }

  private final Node fNode;
  private final ImplUtil fImpl;

  public GraphProcessCycle(Node node) {
    this(node, new ImplUtil(LinkedList.class, HashMap.class));
  }

  public GraphProcessCycle(Node node, ImplUtil impl) {
    if (node == null) {
      throw new NullPointerException("Underlying node was null!");
    }

    this.fNode = node;
    this.fImpl = impl;
    
    this.fLogger = Logger.getLogger(this.getClass().getName());
  }

  @Override
  public Map<String, Object> toMap(Set<String> fields) {
    final Map<String, Object> dto = this.fImpl.newMap();

    // copy normal fields
    if (fields == null || fields.isEmpty()) {
      for (final String field : this.fNode.getPropertyKeys()) {
        dto.put(field, this.fNode.getProperty(field));
 //       this.fLogger.log(Level.INFO, field + ": "+this.fNode.getProperty(field).toString());
      }
    } else {
      for (final String field : fields) {
        if (this.fNode.hasProperty(field)) {
          dto.put(field, this.fNode.getProperty(field));
        }
      }
    }

    if (fields == null || fields.isEmpty() || fields.contains(GraphProcessCycle.USERLIST_FIELD)) {
      getUserList(dto);
    }

    return dto;
  }

  private void getUserList(Map<String, Object> dto) {
    // people who contributed to the document
    final List<String> people = this.fImpl.newList();
    final List<Node> nodeList = new ArrayList<Node>();

    // TODO: get from given array instead (would require person service)?
    final Iterable<Relationship> rels = this.fNode.getRelationships(ShindigRelTypes.CONTRIBUTED_BY,
            Direction.OUTGOING);

    GraphPerson person = null;
    for (final Relationship r : rels) {
      nodeList.add(r.getEndNode());
    }

    for (final Node node : nodeList) {
//      person = new GraphPerson(node, this.fImpl);
//      people.add(person.toMap(GraphProcessCycle.USERLIST_FIELDS));
    	people.add(node.getProperty("id","").toString());
    }

    dto.put(GraphProcessCycle.USERLIST_FIELD, people);
  }

  @Override
  public void setData(Map<String, ?> cycle) {
    final Map<String, Object> newValues = new HashMap<String, Object>();

    // atomic:
    newValues.put(GraphProcessCycle.DOCID_FIELD, cycle.get(GraphProcessCycle.DOCID_FIELD));
    newValues.put(GraphProcessCycle.DOCTYPE_FIELD, cycle.get(GraphProcessCycle.DOCTYPE_FIELD));
    newValues.put(GraphProcessCycle.START_FIELD, cycle.get(GraphProcessCycle.START_FIELD));
    newValues.put(GraphProcessCycle.END_FIELD, cycle.get(GraphProcessCycle.END_FIELD));

    // lists:
    final Object contributorsVal = cycle.get(GraphProcessCycle.USERLIST_FIELD);

    if (contributorsVal != null) {
      String[] contributors = null;

      if (contributorsVal instanceof String[]) {
        contributors = (String[]) contributorsVal;
      } else {
        @SuppressWarnings("unchecked")
        final List<String> contrList = (List<String>) contributorsVal;
        contributors = contrList.toArray(new String[contrList.size()]);
      }

      newValues.put(GraphProcessCycle.USERLIST_FIELD, contributors);
    }

    // set new values
    for (final Entry<String, Object> valE : newValues.entrySet()) {
    	String log = valE.getKey()+" - "+valE.getValue();
    	if(valE.getKey().equals("type"))
    		log += "\n";
    	this.fLogger.log(Level.INFO, log);
    	
      if (valE.getValue() != null) {
        this.fNode.setProperty(valE.getKey(), valE.getValue());
      }
    }
  }

}
