/*
 * $Id$
 * $Revision: 5697 $ $Date: Nov 28, 2013 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.jersey.resources;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mycore.frontend.jersey.access.MCRRequireLogin;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author Thomas Scheffler (yagee)
 */
@Path("/echo")
public class MCREchoResource {

    @Context
    HttpServletRequest request;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRRequireLogin.class)
    public String doEcho() {
        Gson gson = new Gson();
        JsonObject jRequest = new JsonObject();
        jRequest.addProperty("secure", request.isSecure());
        jRequest.addProperty("authType", request.getAuthType());
        jRequest.addProperty("context", request.getContextPath());
        jRequest.addProperty("localAddr", request.getLocalAddr());
        jRequest.addProperty("localName", request.getLocalName());
        jRequest.addProperty("method", request.getMethod());
        jRequest.addProperty("pathInfo", request.getPathInfo());
        jRequest.addProperty("protocol", request.getProtocol());
        jRequest.addProperty("queryString", request.getQueryString());
        jRequest.addProperty("remoteAddr", request.getRemoteAddr());
        jRequest.addProperty("remoteHost", request.getRemoteHost());
        jRequest.addProperty("remoteUser", request.getRemoteUser());
        jRequest.addProperty("remotePort", request.getRemotePort());
        jRequest.addProperty("requestURI", request.getRequestURI());
        jRequest.addProperty("scheme", request.getScheme());
        jRequest.addProperty("serverName", request.getServerName());
        jRequest.addProperty("servletPath", request.getServletPath());
        jRequest.addProperty("serverPort", request.getServerPort());
        jRequest.add("session", gson.toJsonTree(request.getSession(false)).getAsJsonObject().get("session"));
        jRequest.addProperty("localPort", request.getLocalPort());
        JsonArray jLocales = new JsonArray();
        Enumeration<Locale> locales = request.getLocales();
        while (locales.hasMoreElements()) {
            jLocales.add(gson.toJsonTree(locales.nextElement().toString()));
        }
        jRequest.add("locales", jLocales);
        JsonObject header = new JsonObject();
        jRequest.add("header", header);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            header.addProperty(headerName, headerValue);
        }
        JsonObject parameter = new JsonObject();
        jRequest.add("parameter", parameter);
        for (Map.Entry<String, String[]> param : request.getParameterMap().entrySet()) {
            if (param.getValue().length == 1) {
                parameter.add(param.getKey(), gson.toJsonTree(param.getValue()[0]));
            } else {
                parameter.add(param.getKey(), gson.toJsonTree(param.getValue()));
            }
        }
        return jRequest.toString();
    }

    @GET
    @Path("ping")
    public Response ping() {
        return Response.ok("pong").build();
    }
}
