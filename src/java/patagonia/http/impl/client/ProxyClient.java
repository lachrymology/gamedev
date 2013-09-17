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

package patagonia.http.impl.client;

import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLSession;


import patagonia.http.ConnectionReuseStrategy;
import patagonia.http.HttpEntity;
import patagonia.http.HttpException;
import patagonia.http.HttpHost;
import patagonia.http.HttpRequest;
import patagonia.http.HttpRequestInterceptor;
import patagonia.http.HttpResponse;
import patagonia.http.ProtocolVersion;
import patagonia.http.auth.AuthSchemeRegistry;
import patagonia.http.auth.AuthScope;
import patagonia.http.auth.AuthState;
import patagonia.http.auth.Credentials;
import patagonia.http.client.params.AuthPolicy;
import patagonia.http.client.params.HttpClientParams;
import patagonia.http.client.protocol.ClientContext;
import patagonia.http.client.protocol.RequestClientConnControl;
import patagonia.http.client.protocol.RequestProxyAuthentication;
import patagonia.http.conn.HttpRoutedConnection;
import patagonia.http.conn.routing.HttpRoute;
import patagonia.http.entity.BufferedHttpEntity;
import patagonia.http.impl.DefaultConnectionReuseStrategy;
import patagonia.http.impl.DefaultHttpClientConnection;
import patagonia.http.impl.auth.BasicSchemeFactory;
import patagonia.http.impl.auth.DigestSchemeFactory;
import patagonia.http.impl.auth.KerberosSchemeFactory;
import patagonia.http.impl.auth.NTLMSchemeFactory;
import patagonia.http.impl.auth.SPNegoSchemeFactory;
import patagonia.http.message.BasicHttpRequest;
import patagonia.http.params.BasicHttpParams;
import patagonia.http.params.HttpParams;
import patagonia.http.params.HttpProtocolParams;
import patagonia.http.protocol.BasicHttpContext;
import patagonia.http.protocol.ExecutionContext;
import patagonia.http.protocol.HttpContext;
import patagonia.http.protocol.HttpProcessor;
import patagonia.http.protocol.HttpRequestExecutor;
import patagonia.http.protocol.ImmutableHttpProcessor;
import patagonia.http.protocol.RequestContent;
import patagonia.http.protocol.RequestTargetHost;
import patagonia.http.protocol.RequestUserAgent;
import patagonia.http.util.EntityUtils;

public class ProxyClient {

    private final HttpProcessor httpProcessor;
    private final HttpRequestExecutor requestExec;
    private final ProxyAuthenticationStrategy proxyAuthStrategy;
    private final HttpAuthenticator authenticator;
    private final AuthState proxyAuthState;
    private final AuthSchemeRegistry authSchemeRegistry;
    private final ConnectionReuseStrategy reuseStrategy;
    private final HttpParams params;

    public ProxyClient(final HttpParams params) {
        super();
        if (params == null) {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        this.httpProcessor = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
                new RequestContent(),
                new RequestTargetHost(),
                new RequestClientConnControl(),
                new RequestUserAgent(),
                new RequestProxyAuthentication()
        } );
        this.requestExec = new HttpRequestExecutor();
        this.proxyAuthStrategy = new ProxyAuthenticationStrategy();
        this.authenticator = new HttpAuthenticator();
        this.proxyAuthState = new AuthState();
        this.authSchemeRegistry = new AuthSchemeRegistry();
        this.authSchemeRegistry.register(AuthPolicy.BASIC, new BasicSchemeFactory());
        this.authSchemeRegistry.register(AuthPolicy.DIGEST, new DigestSchemeFactory());
        this.authSchemeRegistry.register(AuthPolicy.NTLM, new NTLMSchemeFactory());
        this.authSchemeRegistry.register(AuthPolicy.SPNEGO, new SPNegoSchemeFactory());
        this.authSchemeRegistry.register(AuthPolicy.KERBEROS, new KerberosSchemeFactory());
        this.reuseStrategy = new DefaultConnectionReuseStrategy();
        this.params = params;
    }

    public ProxyClient() {
        this(new BasicHttpParams());
    }

    public HttpParams getParams() {
        return this.params;
    }

    public AuthSchemeRegistry getAuthSchemeRegistry() {
        return this.authSchemeRegistry;
    }

    public Socket tunnel(
            final HttpHost proxy,
            final HttpHost target,
            final Credentials credentials) throws IOException, HttpException {
        ProxyConnection conn = new ProxyConnection(new HttpRoute(proxy));
        HttpContext context = new BasicHttpContext();
        HttpResponse response = null;

        for (;;) {
            if (!conn.isOpen()) {
                Socket socket = new Socket(proxy.getHostName(), proxy.getPort());
                conn.bind(socket, this.params);
            }
            String host = target.getHostName();
            int port = target.getPort();
            if (port < 0) {
                port = 80;
            }

            StringBuilder buffer = new StringBuilder(host.length() + 6);
            buffer.append(host);
            buffer.append(':');
            buffer.append(Integer.toString(port));

            String authority = buffer.toString();
            ProtocolVersion ver = HttpProtocolParams.getVersion(this.params);
            HttpRequest connect = new BasicHttpRequest("CONNECT", authority, ver);
            connect.setParams(this.params);

            BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(proxy), credentials);

            // Populate the execution context
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, target);
            context.setAttribute(ExecutionContext.HTTP_PROXY_HOST, proxy);
            context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
            context.setAttribute(ExecutionContext.HTTP_REQUEST, connect);
            context.setAttribute(ClientContext.PROXY_AUTH_STATE, this.proxyAuthState);
            context.setAttribute(ClientContext.CREDS_PROVIDER, credsProvider);
            context.setAttribute(ClientContext.AUTHSCHEME_REGISTRY, this.authSchemeRegistry);

            this.requestExec.preProcess(connect, this.httpProcessor, context);

            response = this.requestExec.execute(connect, conn, context);

            response.setParams(this.params);
            this.requestExec.postProcess(response, this.httpProcessor, context);

            int status = response.getStatusLine().getStatusCode();
            if (status < 200) {
                throw new HttpException("Unexpected response to CONNECT request: " +
                        response.getStatusLine());
            }

            if (HttpClientParams.isAuthenticating(this.params)) {
                if (this.authenticator.isAuthenticationRequested(proxy, response,
                        this.proxyAuthStrategy, this.proxyAuthState, context)) {
                    if (this.authenticator.authenticate(proxy, response,
                            this.proxyAuthStrategy, this.proxyAuthState, context)) {
                        // Retry request
                        if (this.reuseStrategy.keepAlive(response, context)) {
                            // Consume response content
                            HttpEntity entity = response.getEntity();
                            EntityUtils.consume(entity);
                        } else {
                            conn.close();
                        }
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        int status = response.getStatusLine().getStatusCode();

        if (status > 299) {

            // Buffer response content
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                response.setEntity(new BufferedHttpEntity(entity));
            }

            conn.close();
            throw new TunnelRefusedException("CONNECT refused by proxy: " +
                    response.getStatusLine(), response);
        }
        return conn.getSocket();
    }

    static class ProxyConnection extends DefaultHttpClientConnection implements HttpRoutedConnection {

        private final HttpRoute route;

        ProxyConnection(final HttpRoute route) {
            super();
            this.route = route;
        }

        public HttpRoute getRoute() {
            return this.route;
        }

        public boolean isSecure() {
            return false;
        }

        public SSLSession getSSLSession() {
            return null;
        }

        @Override
        public Socket getSocket() {
            return super.getSocket();
        }

    }

}
