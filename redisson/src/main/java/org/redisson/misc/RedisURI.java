/**
 * Copyright (c) 2013-2024 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson.misc;

import io.netty.util.NetUtil;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 
 * @author Nikita Koksharov
 *
 */
public final class RedisURI {

    public static final String REDIS_PROTOCOL= "redis://";
    public static final String REDIS_SSL_PROTOCOL = "rediss://";

    private final boolean ssl;
    private final String host;
    private final int port;
    private String username;
    private String password;
    private int hashCode;

    public static boolean isValid(String url) {
        return url.startsWith(REDIS_PROTOCOL) || url.startsWith(REDIS_SSL_PROTOCOL);
    }

    public RedisURI(String scheme, String host, int port) {
        this.ssl = "rediss".equals(scheme);
        this.host = host;
        this.port = port;
        this.hashCode = Objects.hash(ssl, host, port);
    }

    public RedisURI(String uri) {
        if (!isValid(uri)) {
            throw new IllegalArgumentException("Redis url should start with redis:// or rediss:// (for SSL connection)");
        }

        if (uri.split(":").length < 3) {
            throw new IllegalArgumentException("Redis url doesn't contain a port");
        }

        String urlHost = parseUrl(uri);

        try {
            URL url = new URL(urlHost);
            if (url.getUserInfo() != null) {
                String[] details = url.getUserInfo().split(":", 2);
                if (details.length == 2) {
                    if (!details[0].isEmpty()) {
                        username = URLDecoder.decode(details[0], StandardCharsets.UTF_8.toString());
                    }
                    password = URLDecoder.decode(details[1], StandardCharsets.UTF_8.toString());
                }
            }
            if (url.getHost().isEmpty()) {
                throw new IllegalArgumentException("Redis host can't be parsed");
            }

            host = url.getHost();
            port = url.getPort();
            ssl = uri.startsWith("rediss://");
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String parseUrl(String uri) {
        int hostStartIndex = uri.indexOf("://") + 3;
        String urlHost = "http://" + uri.substring(hostStartIndex);
        String ipV6Host = uri.substring(hostStartIndex, uri.lastIndexOf(":"));
        if (ipV6Host.contains("@")) {
            ipV6Host = ipV6Host.split("@")[1];
        }
        if (ipV6Host.contains(":") && !ipV6Host.startsWith("[")) {
            urlHost = urlHost.replace(ipV6Host, "[" + ipV6Host + "]");
        }
        return urlHost;
    }

    public String getScheme() {
        if (ssl) {
            return "rediss";
        }
        return "redis";
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isSsl() {
        return ssl;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isIP() {
        return NetUtil.createByteArrayFromIpAddressString(host) != null;
    }

    private String trimIpv6Brackets(String host) {
        if (host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }
    
    public boolean equals(InetSocketAddress entryAddr) {
        String ip = trimIpv6Brackets(getHost());
        if (((entryAddr.getHostName() != null && entryAddr.getHostName().equals(ip))
                || entryAddr.getAddress().getHostAddress().equals(ip))
                && entryAddr.getPort() == getPort()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisURI redisURI = (RedisURI) o;
        return ssl == redisURI.ssl && port == redisURI.port && Objects.equals(host, redisURI.host);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return getScheme() + "://" + trimIpv6Brackets(host) + ":" + port;
    }
    
}
