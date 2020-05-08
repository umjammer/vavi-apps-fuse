/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.gathered;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;


/**
 * NameMap.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/08 umjammer initial version <br>
 */
public class NameMap {

    private BiMap<String, String> nameMap = HashBiMap.create();

    /** id -> display name */
    public String encodeFsName(String id) throws IOException {
        if (!nameMap.isEmpty()) {
            return nameMap.get(id);
        } else {
            return URLEncoder.encode(id, "utf-8");
        }
    }

    /** display name -> id */
    public String decodeFsName(String path) throws IOException {
        if (!nameMap.isEmpty()) {
            return nameMap.inverse().get(path);
        } else {
            return URLDecoder.decode(path, "utf-8");
        }
    }

    public void put(String id, String encodeFsName) {
        nameMap.put(id, encodeFsName);
    }

    public String get(String id) {
        return nameMap.get(id);
    }

    public void remove(String id) {
        nameMap.remove(id);
    }

    /** TODO */
    public Map<String, String> map() {
        return nameMap;
    }
}

/* */
