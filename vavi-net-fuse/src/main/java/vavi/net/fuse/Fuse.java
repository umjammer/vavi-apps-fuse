/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystem;
import java.util.Map;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * Fuse.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/29 umjammer initial version <br>
 */
public interface Fuse {

    /** */
    void mount(FileSystem fs, String mountPoint, Map<String, String> env) throws IOException;

    /** */
    void unmount() throws IOException;

    /** */
    @PropsEntity
    class Factory {
        private Factory() {}

        @Property("vavi.net.fuse.fusejna.FuseJnaFuse")
        private String fuseClassName;

        public static Fuse getFuse() {
            try {
                Factory bean = new Factory();
                PropsEntity.Util.bind(bean);
Debug.println("fuseClassName: " + bean.fuseClassName);
                return Fuse.class.cast(Class.forName(bean.fuseClassName).getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                     InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException | IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}

/* */
