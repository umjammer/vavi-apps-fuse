/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.box;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.BoxFolder;
import com.box.sdk.BoxItem;

import vavi.net.fuse.Getter;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * Main2. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/17 umjammer initial version <br>
 */
@PropsEntity(url = "file://${user.home}/.vavifuse/box/{0}")
public class Main2 {

    @Property(name = "box.accessToken")
    private transient String accessToken;
    
    /**
     * 
     * @param args 0: email
     */
    public static void main(String[] args) throws Exception {
        String email = args[0];
        Main2 app = new Main2();
        PropsEntity.Util.bind(app, email);
        
        BoxAPIConnection api = new BoxAPIConnection(app.accessToken);

        if (app.accessToken == null || app.accessToken.isEmpty()) {
            Getter getter = new BoxFxGetter(email);
            app.accessToken = getter.get("");
        }
        
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);
        for (BoxItem.Info itemInfo : rootFolder) {
            System.out.format("[%s] %s\n", itemInfo.getID(), itemInfo.getName());
        }
    }
}

/* */
