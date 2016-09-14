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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.processmining;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.spi.GraphProcessMiningSPI;
import de.iisys.schub.processMining.activities.ActivityController;
import de.iisys.schub.processMining.activities.model.ProcessCycle;

public class MiningController {

	private static final String ACT_ACTOR_FIELD  ="actor";
	private static final String ACT_OBJECT_FIELD = "object";
	private static final String ACT_TARGET_FIELD = "target";

	private static final String ID_FIELD = "id";
	private static final String OBJ_TYPE_FIELD = "objectType";
	private static final String PUBLISHED_FIELD = "published";
	private static final String TITLE_FIELD = "title";
	private static final String VERB_FIELD = "verb";

  

  private final List<Map<String, Object>> processCycles;
  private final List<List<Map<String, Object>>> activityLists;
  
  private final Logger fLogger;
  

  public MiningController(List<Map<String, Object>> processCycles,
          List<List<Map<String, Object>>> activityLists) {
    this.processCycles = processCycles;
    this.activityLists = activityLists;
    
    this.fLogger = Logger.getLogger("ProcessMining");
  }
  
  
  /**
   * Creates JSON objects from the given activities and starts the process mining after 
   * adding ProcessCycle objects to the de.iisys.schub.processMining.activities.ActivityController
   */
  @SuppressWarnings("unchecked")
  public void startMining() {
	  this.fLogger.log(Level.INFO, "Start mining for type "+processCycles.get(0).get(GraphProcessMiningSPI.DOC_TYPE_FIELD));
	  List<ProcessCycle> cycles = new ArrayList<ProcessCycle>();
	  
	  int i=0;
	  for(final Map<String, Object> mCycle : processCycles) {
		  
		  String docId = (String)mCycle.get(GraphProcessMiningSPI.DOC_ID_FIELD);
		  
		  ProcessCycle pc = new ProcessCycle(docId, (String)mCycle.get(GraphProcessMiningSPI.DOC_TYPE_FIELD));
		  
		  List<JSONObject> activitiesJson = new ArrayList<JSONObject>();
		  // add activities to cycle:
		  List<Map<String, Object>> activities = this.activityLists.get(i);
		  for(Map<String,Object> map : activities) {
			  String verb = (String)map.get(VERB_FIELD);
//			  if(!(verb.equals("post") || verb.equals("send") || verb.equals("add")))
//				  continue;
			  
			  JSONObject json = new JSONObject();
			  json.put(ID_FIELD, (String)map.get(ID_FIELD));
			  json.put(TITLE_FIELD, (String)map.get(TITLE_FIELD));
			  json.put(VERB_FIELD, verb);
			  json.put(PUBLISHED_FIELD, (String)map.get(PUBLISHED_FIELD));
			  
			  Map<String,Object> objectMap = (Map<String,Object>)map.get(ACT_OBJECT_FIELD);
			  JSONObject object = new JSONObject();
			  
			  String objectId = (String)objectMap.get(ID_FIELD);
			  if(objectId.equals(docId))
				  continue;			  
			  object.put(ID_FIELD, objectId);
			  
			  String objectType = (String) objectMap.get(OBJ_TYPE_FIELD);
			  if(!ActivityController.SUPPORTED_ACTIVITY_OBJECTS.contains(objectType))
				  continue;
			  object.put(OBJ_TYPE_FIELD, objectType);
			  json.put(ACT_OBJECT_FIELD, object);
			  
			  Map<String,Object> actorMap = (Map<String,Object>)map.get(ACT_ACTOR_FIELD);
			  JSONObject actor = new JSONObject();
			  actor.put(ID_FIELD, actorMap.get(ID_FIELD));
			  actor.put(OBJ_TYPE_FIELD, actorMap.get(OBJ_TYPE_FIELD));
			  json.put(ACT_ACTOR_FIELD, actor);
			  
			  Map<String,Object> targetMap = (Map<String,Object>)map.get(ACT_TARGET_FIELD);
			  if(targetMap!=null) {
				  JSONObject target = new JSONObject();
				  target.put(ID_FIELD, targetMap.get(ID_FIELD));
				  target.put(OBJ_TYPE_FIELD, targetMap.get(OBJ_TYPE_FIELD));
				  json.put(ACT_TARGET_FIELD, target);
			  }
			  
			  activitiesJson.add(json);
		  }
		  pc.setActivitiesJSONs(activitiesJson);
		  
		  // add userIds to cycle:
		  /*
		  final List<Map<String, Object>> userList = (List<Map<String, Object>>) mCycle
	              .get(GraphProcessMiningSPI.USER_LIST_FIELD);
		  for(final Map<String, Object> user : userList) {
			  pc.addUserId(user.get("id").toString());
		  } */
		  final List<String> userList = (List<String>) mCycle.get(GraphProcessMiningSPI.USER_LIST_FIELD);
		  pc.setUserIds(new HashSet<String>(userList));
		  
		  // start and end time:
		  final Long start = (Long) mCycle.get(GraphProcessMiningSPI.START_FIELD);
	      final Long end = (Long) mCycle.get(GraphProcessMiningSPI.END_FIELD);
	      pc.setStartDate(new Date(start));
	      pc.setEndDate(new Date(end));
	      
	      cycles.add(pc);
	      i++;
	  }
	  
	  ActivityController control = new ActivityController(cycles);
	  control.startPipeline();
  }

}
