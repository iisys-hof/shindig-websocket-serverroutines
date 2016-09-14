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
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {

  public static String basicAuthRequestGET(URL url, String username, String password) {
    return HttpUtil.basicAuthRequest("GET", url, username, password, null);
  }

  public static String basicAuthRequestPOST(URL url, String username, String password) {
    return HttpUtil.basicAuthRequest("POST", url, username, password, null);
  }

  public static String basicAuthSendRequestPOST(URL url, String username, String password,
          String json) {
    return HttpUtil.basicAuthRequest("POST", url, username, password, json);
  }

  public static String basicAuthRequest(String requestMethod, URL url, String username,
          String password, String json) {
    try {
      // authentication
      final String userpass = username + ":" + password;
      final String basicAuth = "Basic "
              + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());

      final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod(requestMethod);
      connection.setRequestProperty("Authorization", basicAuth);

      // send json
      if (json != null) {
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", String.valueOf(json.getBytes().length));

        final OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
        out.write(json);
        out.flush();
        out.close();
      }

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

    } catch (final IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
