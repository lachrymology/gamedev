/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package patagonia.http.impl.conn;


import java.net.InetAddress;





import patagonia.http.HttpException;
import patagonia.http.HttpHost;
import patagonia.http.HttpRequest;
import patagonia.http.annotation.ThreadSafe;
import patagonia.http.conn.params.ConnRouteParams;
import patagonia.http.conn.routing.HttpRoute;
import patagonia.http.conn.routing.HttpRoutePlanner;
import patagonia.http.conn.scheme.Scheme;
import patagonia.http.conn.scheme.SchemeRegistry;
import patagonia.http.protocol.HttpContext;

/**
 * Default implementation of an {@link HttpRoutePlanner}. This implementation
 * is based on {@link patagonia.http.conn.params.ConnRoutePNames parameters}.
 * It will not make use of any Java system properties, nor of system or
 * browser proxy settings.
 * <p>
 * The following parameters can be used to customize the behavior of this
 * class:
 * <ul>
 *  <li>{@link patagonia.http.conn.params.ConnRoutePNames#DEFAULT_PROXY}</li>
 *  <li>{@link patagonia.http.conn.params.ConnRoutePNames#LOCAL_ADDRESS}</li>
 *  <li>{@link patagonia.http.conn.params.ConnRoutePNames#FORCED_ROUTE}</li>
 * </ul>
 *
 * @since 4.0
 */
@ThreadSafe
public class DefaultHttpRoutePlanner implements HttpRoutePlanner {

    /** The scheme registry. */
    protected final SchemeRegistry schemeRegistry; // class is @ThreadSafe

    /**
     * Creates a new default route planner.
     *
     * @param schreg    the scheme registry
     */
    public DefaultHttpRoutePlanner(SchemeRegistry schreg) {
        if (schreg == null) {
            throw new IllegalArgumentException
                ("SchemeRegistry must not be null.");
        }
        schemeRegistry = schreg;
    }

    public HttpRoute determineRoute(HttpHost target,
                                    HttpRequest request,
                                    HttpContext context)
        throws HttpException {

        if (request == null) {
            throw new IllegalStateException
                ("Request must not be null.");
        }

        // If we have a forced route, we can do without a target.
        HttpRoute route =
            ConnRouteParams.getForcedRoute(request.getParams());
        if (route != null)
            return route;

        // If we get here, there is no forced route.
        // So we need a target to compute a route.

        if (target == null) {
            throw new IllegalStateException
                ("Target host must not be null.");
        }

        final InetAddress local =
            ConnRouteParams.getLocalAddress(request.getParams());
        final HttpHost proxy =
            ConnRouteParams.getDefaultProxy(request.getParams());

        final Scheme schm;
        try {
            schm = schemeRegistry.getScheme(target.getSchemeName());
        } catch (IllegalStateException ex) {
            throw new HttpException(ex.getMessage());
        }
        // as it is typically used for TLS/SSL, we assume that
        // a layered scheme implies a secure connection
        final boolean secure = schm.isLayered();

        if (proxy == null) {
            route = new HttpRoute(target, local, secure);
        } else {
            route = new HttpRoute(target, local, proxy, secure);
        }
        return route;
    }

}
