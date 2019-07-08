/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.oauth2.box;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

@PropsEntity(url = "file://${HOME}/.vavifuse/box.properties")
public class BoxLocalAppCredential {
    @Property(name = "box.clientId")
    public transient String clientId;
    @Property(name = "box.clientSecret")
    public transient String clientSecret;
}

/* */
