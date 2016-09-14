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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphActivityEntry;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.convert.GraphProcessCycle;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.processmining.MiningController;
import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.util.ShindigRelTypes;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Implementation of the process mining service retrieving process cycle data from the Neo4j graph
 * database.
 *
 */
public class GraphProcessMiningSPI {

  // Count of iterations which have to be saved per docType before the activity mining starts.
  private final int MIN_CYCLES_COUNT = 2;

  public static final String DOC_TYPE_FIELD = "type";
  public static final String DOC_ID_FIELD = "docId";
  public static final String USER_LIST_FIELD = "userList";
  public static final String START_FIELD = "startDate";
  public static final String END_FIELD = "endDate";

  public static final String PUBLISHED_FIELD = "published";

  private final GraphDatabaseService fDatabase;
  private final GraphPersonSPI fPersonSPI;

  private final Index<Node> fDocTypeNodes;

  // private final Index<Node> fProcessCycleNodes;

  private final ImplUtil fImpl;

  private final Logger fLogger;

  public GraphProcessMiningSPI(GraphDatabaseService database, Map<String, String> config,
          GraphPersonSPI personSPI, GraphActivityStreamSPI activitySPI, ImplUtil impl) {
    if (database == null) {
      throw new NullPointerException("graph database service was null");
    }
    if (config == null) {
      throw new NullPointerException("configuration object was null");
    }
    if (personSPI == null) {
      throw new NullPointerException("person service was null");
    }
    if (activitySPI == null) {
      throw new NullPointerException("activitystream service was null");
    }
    if (impl == null) {
      throw new NullPointerException("implementation utility was null");
    }

    this.fDatabase = database;
    this.fPersonSPI = personSPI;

    // document type index
    this.fDocTypeNodes = this.fDatabase.index().forNodes(ShindigConstants.DOC_TYPE_NODES);

    this.fImpl = impl;

    this.fLogger = Logger.getLogger(this.getClass().getName());
  }

  public SingleResult addProcessCycle(String docType, Map<String, Object> processCycle) {
    boolean success = false;
    Node typeNode = null;

    final Transaction tx = this.fDatabase.beginTx();
    try {

      // get indexed node for document type
      typeNode = getOrCreateTypeNode(docType);

      // create process cycle node
      final Node cycleNode = this.fDatabase.createNode();

      processCycle.put(GraphProcessMiningSPI.DOC_TYPE_FIELD, docType);

      new GraphProcessCycle(cycleNode, this.fImpl).setData(processCycle);

      // link to type
      typeNode.createRelationshipTo(cycleNode, ShindigRelTypes.PROCESS_CYCLE);

      // link to contributing people?
      if (cycleNode.hasProperty(GraphProcessMiningSPI.USER_LIST_FIELD)) {
        final String[] userIds = (String[]) cycleNode
                .getProperty(GraphProcessMiningSPI.USER_LIST_FIELD);
        Node personNode = null;
        for (final String id : userIds) {
          personNode = this.fPersonSPI.getPersonNode(id);

          if (personNode != null) {
            cycleNode.createRelationshipTo(personNode, ShindigRelTypes.CONTRIBUTED_BY);
          } else {
            // TODO: warning?
          }
        }
      }

      tx.success();
      tx.finish();
      success = true;
    } catch (final Exception e) {
      tx.failure();
      tx.finish();

      throw new RuntimeException("process cycle could not be added:\n" + e.getMessage());
    }

    if (success) {
      this.checkDocTypeCycles(docType, typeNode);
    }
    
    return new SingleResult(processCycle);
  }

  private Node getOrCreateTypeNode(String docType) {
    Node node = this.fDocTypeNodes.get(GraphProcessMiningSPI.DOC_TYPE_FIELD, docType).getSingle();

    if (node == null) {
      node = this.fDatabase.createNode();
      node.setProperty(GraphProcessMiningSPI.DOC_TYPE_FIELD, docType);
      this.fDocTypeNodes.add(node, GraphProcessMiningSPI.DOC_TYPE_FIELD, docType);
    }

    return node;
  }

  public List<Map<String, Object>> getProcessCycles(String docType) {
    final Node typeNode = this.fDocTypeNodes.get(GraphProcessMiningSPI.DOC_TYPE_FIELD, docType)
            .getSingle();

    if (typeNode != null) {
      return getProcessCycles(typeNode);
    } else {
      return null;
    }
  }

  public List<Map<String, Object>> getProcessCycles(Node typeNode) {
    final List<Map<String, Object>> cycles = new ArrayList<Map<String, Object>>();

    // get all entries linked to type node
    final Iterable<Relationship> cycleRels = typeNode.getRelationships(
            ShindigRelTypes.PROCESS_CYCLE, Direction.OUTGOING);

    Map<String, Object> cycle = null;
    for (final Relationship r : cycleRels) {
      // TODO: fields?
      cycle = new GraphProcessCycle(r.getEndNode(), this.fImpl).toMap(null);
      cycles.add(cycle);
    }

    return cycles;
  }

  private void checkDocTypeCycles(String docType, Node typeNode) {
    final int cycleCount = this.getDocTypeCyclesCount(typeNode);

    if (cycleCount == this.MIN_CYCLES_COUNT) { // initial mining phase

      this.startInitialMining(docType, this.getProcessCycles(typeNode));

    } else if (cycleCount > this.MIN_CYCLES_COUNT) {
      // TODO: ?
    }
  }

  private int getDocTypeCyclesCount(Node typeNode) {
    // check number of connections to node
    int num = 0;

    final Iterable<Relationship> cycleRels = typeNode.getRelationships(
            ShindigRelTypes.PROCESS_CYCLE, Direction.OUTGOING);
    for (@SuppressWarnings("unused") final Relationship r : cycleRels) {
      ++num;
    }

    return num;
  }

  /**
   * Collects the activities between start and enddate of the given processCycles.
   * Creates a new MiningController with this data.
   * @param docType
   * @param processCycles
   */
  private void startInitialMining(String docType, List<Map<String, Object>> processCycles) {
    // 1. Get all activities between start and end time:

    List<List<Map<String, Object>>> allActivities = new ArrayList<List<Map<String, Object>>>();

    for (final Map<String, Object> mCycle : processCycles) {
      final Long start = (Long) mCycle.get(GraphProcessMiningSPI.START_FIELD);
      final Long end = (Long) mCycle.get(GraphProcessMiningSPI.END_FIELD);

      /*
      @SuppressWarnings("unchecked")
      final List<Map<String, Object>> userList = (List<Map<String, Object>>) mCycle
              .get(GraphProcessMiningSPI.USER_LIST_FIELD);
      final List<String> userIds = new ArrayList<String>();
      for (final Map<String, Object> user : userList) {
        userIds.add(user.get("id").toString());
      } */
      
      @SuppressWarnings("unchecked")
      final List<String> userIds = (List<String>) mCycle.get(GraphProcessMiningSPI.USER_LIST_FIELD);
      
      /*
      StringBuffer debugBuffer = new StringBuffer();
      for(String id : userIds) {
    	  debugBuffer.append(id);
      }
      this.fLogger.log(Level.INFO, "Debug | userIds: "+debugBuffer.toString()); */

      if(start!=null && end!=null && userIds!=null) {
    	// get and add matching activities for all users
    	List<Map<String, Object>> activityEntries = getActivityEntries(userIds, start, end);
      	allActivities.add(activityEntries);
      } else {
    	  if(start==null)
    		  this.fLogger.log(Level.WARNING, "startdate is null!");
    	  if(end==null)
    		  this.fLogger.log(Level.WARNING, "enddate is null!");
    	  if(userIds==null)
    		  this.fLogger.log(Level.WARNING, "userIds are null!");
    	  
    	  allActivities.add(new ArrayList<Map<String,Object>>());
      }
    }

    MiningController mining = new MiningController(processCycles, allActivities);
    mining.startMining();
  }

  private List<Map<String, Object>> getActivityEntries(List<String> userIds, long start, long end) {
    final List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
    final List<Node> actNodes = new ArrayList<Node>();

    Node person = null;
    for (final String userId : userIds) {
      person = this.fPersonSPI.getPersonNode(userId);
      addActivities(person, actNodes);
    }

    // filter by timestamp and convert
    String published = null;
    long timestamp = 0;
    GraphActivityEntry gActEntry = null;
    for (final Node actNode : actNodes) {
      // convert iso8601 timestamp to unix timestamp
      published = (String) actNode.getProperty(GraphProcessMiningSPI.PUBLISHED_FIELD, null);

      if (published != null && !published.isEmpty()) {
        try {
        	timestamp = DatatypeConverter.parseDateTime(published).getTimeInMillis();
        } catch(IllegalArgumentException e) {
        	DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        	format.setTimeZone(TimeZone.getTimeZone("GMT"));
        	try {
				timestamp = format.parse(published).getTime();
			} catch (ParseException e1) {
				e1.printStackTrace();
			}
        }
        if (timestamp >= start && timestamp <= end) {
          gActEntry = new GraphActivityEntry(actNode, this.fImpl);
          activities.add(gActEntry.toMap(null));
        }
      } else {
        // TODO: keep or discard?
      }
    }

    return activities;
  }

  private void addActivities(Node person, final List<Node> activities) {
    Node actNode = null;

    final Iterable<Relationship> actRels = person.getRelationships(Neo4jRelTypes.ACTED);

    for (final Relationship rel : actRels) {
      actNode = rel.getEndNode();
      activities.add(actNode);
    }
  }
  
  
  public void deleteProcessCycles(String docType) {
	  
	  final Transaction tx = this.fDatabase.beginTx();
	  
	  try {
		  // get docType node:
		  final Node typeNode = this.fDocTypeNodes.get(GraphProcessMiningSPI.DOC_TYPE_FIELD, docType)
		            .getSingle();
		  
		  // get all entries linked to type node
		  final Iterable<Relationship> cycleRels = typeNode.getRelationships(
		            ShindigRelTypes.PROCESS_CYCLE, Direction.OUTGOING);
		
		  Node processCycleNode = null;
		  for (final Relationship cycleRel : cycleRels) {
			  processCycleNode = cycleRel.getEndNode();
			  
			  // delete relationships (to users):
			  final Iterable<Relationship> rels = processCycleNode.getRelationships(ShindigRelTypes.CONTRIBUTED_BY, Direction.OUTGOING);
		      for (final Relationship r : rels) {
		        r.delete();
		      }			  		  
			  // delete relationship:
			  cycleRel.delete();
			  // delete process cycle node:
			  processCycleNode.delete();
		  }
		  
		  tx.success();
	      tx.finish();
	  } catch (final Exception e) {
	      tx.failure();
	      tx.finish();
	      throw new RuntimeException("process cycle could not be deleted:\n" + e.getMessage());
	  }
  }
}
