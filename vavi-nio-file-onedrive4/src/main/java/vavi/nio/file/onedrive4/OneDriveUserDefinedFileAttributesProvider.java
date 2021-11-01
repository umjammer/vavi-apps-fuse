/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.fge.filesystem.attributes.provider.UserDefinedFileAttributesProvider;

import vavi.nio.file.onedrive4.OneDriveFileAttributesFactory.Metadata;
import vavi.util.Debug;


/**
 * OneDriveUserDefinedFileAttributesProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/09/08 umjammer initial version <br>
 */
public class OneDriveUserDefinedFileAttributesProvider extends UserDefinedFileAttributesProvider {

    /** */
    private final Metadata entry;

    /** */
    public OneDriveUserDefinedFileAttributesProvider(Metadata entry) throws IOException {
        this.entry = entry;
    }

    private static final List<String> list = Arrays.stream(UserAttributes.values()).map(e -> e.name()).collect(Collectors.toList());

    @Override
    public List<String> list() throws IOException {
        return list;
    }

    @Override
    public int size(String name) throws IOException {
    	return UserAttributes.valueOf(name).size(entry);
    }

    @Override
    public int read(String name, ByteBuffer dst) throws IOException {
    	return UserAttributes.valueOf(name).read(entry, dst);
    }

    @Override
    public int write(final String name, final ByteBuffer src) throws IOException {
    	return UserAttributes.valueOf(name).write(entry, src);
    }

    private interface UserAttribute<T> {
    	int size(T entry) throws IOException;
        int read(T entry, ByteBuffer dst) throws IOException;
        int write(T entry, ByteBuffer src) throws IOException;
    }

    private enum UserAttributes implements UserAttribute<Metadata> {
    	description {
    	    public int size(Metadata entry) throws IOException {
    	        String description = entry.driveItem.description;
Debug.println("size " + name() + ": " + description);
    	        return description == null ? 0 : description.getBytes().length;
    	    }
    	    public int read(Metadata entry, ByteBuffer dst) throws IOException {
    	        String description = entry.driveItem.description;
Debug.println("read " + name() + ": " + description);
    	        if (description != null) {
    	            dst.put(description.getBytes());
    	        }
    	        return dst.array().length;
    	    }
    	    public int write(Metadata entry, ByteBuffer src) throws IOException {
    	        String description = new String(src.array());
Debug.println("write " + name() + ": " + description);
				entry.driver.patchEntryDescription(entry.driveItem, description);
    	        return description.getBytes().length;
    	    }
    	};
    }
}

/* */
