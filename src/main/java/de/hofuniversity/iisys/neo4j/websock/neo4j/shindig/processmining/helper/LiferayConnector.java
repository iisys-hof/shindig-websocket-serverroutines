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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.processmining.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

public class LiferayConnector {

  private final String username = "baerbel";
  private final String pw = "bitte";

  private static String PATH_BLOGENTRY = "blogsentry/get-entry/entry-id/";

  private final String liferayURL;

  public LiferayConnector(String liferayURL) {
    this.liferayURL = liferayURL;
  }

  private String connectLiferay(String requestMethod, String contextPath) {
    try {
      final URL url = new URL(this.liferayURL + "/" + contextPath);
      // String encoding = Base64Encoder.encode(username+":"+pw);
      final String userpass = this.username + ":" + this.pw;
      final String basicAuth = "Basic "
              + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());

      final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod(requestMethod);
      connection.setDoOutput(true);
      connection.setRequestProperty("Authorization", basicAuth);

      // read response:
      final BufferedReader in = new BufferedReader(new InputStreamReader(
              connection.getInputStream()));

      final StringBuffer buf = new StringBuffer();
      String line;
      while ((line = in.readLine()) != null) {
        buf.append(line);
      }
      in.close();

      return buf.toString();

    } catch (final MalformedURLException e) {
      e.printStackTrace();
    } catch (final IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private String connectLiferayGET(String contextPath) {
    return this.connectLiferay("GET", contextPath);
  }

  public JSONObject getBlogEntry(String entryId) {
    final String jsonString = this.connectLiferayGET(LiferayConnector.PATH_BLOGENTRY + entryId);
    try {
      final JSONObject json = new JSONObject(jsonString);
      return json;
    } catch (final JSONException e) {
      e.printStackTrace();
      return null;
    }
  }
}
