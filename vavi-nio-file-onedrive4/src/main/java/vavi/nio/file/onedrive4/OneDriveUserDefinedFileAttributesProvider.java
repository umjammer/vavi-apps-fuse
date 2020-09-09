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

    /** */
    public static final String ATTRIBUTE_DESCRIPTION = "description";

    /** */
    private static final List<String> list = Arrays.asList(ATTRIBUTE_DESCRIPTION);

    @Override
    public List<String> list() throws IOException {
        return list;
    }

    @Override
    public int size(String name) throws IOException {
        switch (name) {
        case ATTRIBUTE_DESCRIPTION:
            return sizeDescription();
        default:
            return 0;
        }
    }

    @Override
    public int read(String name, ByteBuffer dst) throws IOException {
        switch (name) {
        case ATTRIBUTE_DESCRIPTION:
            return readDescription(dst);
        default:
            return 0;
        }
    }

    @Override
    public int write(final String name, final ByteBuffer src) throws IOException {
        switch (name) {
        case ATTRIBUTE_DESCRIPTION:
            return writeDescription(src);
        default:
            return 0;
        }
    }

    /** description */
    private int sizeDescription() throws IOException {
        String description = entry.driveItem.description;
Debug.println("size description: " + description);
        return description == null ? 0 : description.getBytes().length;
    }

    /** description */
    private int readDescription(ByteBuffer dst) throws IOException {
        String description = entry.driveItem.description;
Debug.println("read description: " + description);
        if (description != null) {
            dst.put(description.getBytes());
        }
        return dst.array().length;
    }

    /** description */
    private int writeDescription(final ByteBuffer src) throws IOException {
        String description = new String(src.array());
Debug.println("write description: " + description);
        entry.driver.patchEntryDescription(entry.driveItem, description);
        return description.getBytes().length;
    }
}

/* */
