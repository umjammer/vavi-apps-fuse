/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.proprietary.vfs;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import vavi.net.auth.AppCredential;
import vavi.net.auth.UserCredential;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * VfsCredential.
 * <p>
 * properties file "~/vavifuse/credentials.properties"
 * <ul>
 * <li> vfs.username.alias
 * <li> vfs.password.alias
 * <li> vfs.host.alias
 * <li> vfs.port.alias
 * </ul>
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/02/15 umjammer initial version <br>
 */
@PropsEntity(url = "file://${HOME}/.vavifuse/credentials.properties")
public class VfsCredential implements UserCredential, AppCredential {

    @Property(name = "vfs.username.{0}")
    protected String username;
    @Property(name = "vfs.password.{0}")
    protected transient String password;
    @Property(name = "vfs.host.{0}")
    protected String host;
    @Property(name = "vfs.port.{0}")
    protected int port = -1;

    /** */
    protected String scheme;

    /**
     * @param uri scheme://{username}:{password}@{host}:{port}
     */
    public VfsCredential(URI uri) {
        this.scheme = uri.getScheme();
        String[] userInfo = uri.getUserInfo() != null ? uri.getUserInfo().split(":") : null;
        if (userInfo != null && !userInfo[0].isEmpty()) {
            this.username = userInfo[0];
        }
        if (userInfo != null && userInfo.length > 1 && !userInfo[1].isEmpty()) {
            this.password = userInfo[1];
        }
        if (uri.getHost() != null && !uri.getHost().isEmpty()) {
            this.host = uri.getHost();
        }
        if (uri.getPort() != -1) {
            this.port = uri.getPort();
        }
    }

    /**
     * @param alias
     */
    public VfsCredential(String alias) {
        try {
            PropsEntity.Util.bind(this, alias);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getClientId() {
        return scheme;
    }

    @Override
    public String getApplicationName() {
        return "vavi-apps-fuse";
    }

    @Override
    public String getScheme() {
        return "vfs";
    }

    @Override
    public String getId() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /** */
    public String getHost() {
        return host;
    }

    /** */
    public int getPort() {
        return port;
    }

    /** */
    public String buildBaseUrl() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClientId());
        sb.append("://");
        if (username != null) {
            sb.append(URLEncoder.encode(username, StandardCharsets.UTF_8));
        }
        if (password != null) {
            sb.append(":");
            sb.append(password);
        }
        if (host != null) {
            if (username != null || password != null) {
                sb.append("@");
            }
            sb.append(host);
        }
        if (port != -1) {
            sb.append(":");
            sb.append(port);
        }
        return sb.toString();
    }
}

/* */
