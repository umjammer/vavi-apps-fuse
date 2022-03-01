/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.github.fge.filesystem.attributes.provider.UserDefinedFileAttributesProvider;
import com.microsoft.graph.models.extensions.DriveItem;

import vavi.nio.file.onedrive4.OneDriveFileAttributesFactory.Metadata;
import vavi.util.Debug;


/**
 * OneDriveUserDefinedFileAttributesProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/09/08 umjammer initial version <br>
 */
public class OneDriveUserDefinedFileAttributesProvider extends UserDefinedFileAttributesProvider {

    /** driver & file entry */
    private final Metadata entry;

    /** */
    public OneDriveUserDefinedFileAttributesProvider(Metadata entry) throws IOException {
        this.entry = entry;
    }

    /** pre-listed */
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
        },
        /** whole image file */
        thumbnail {
            /** */
            Map<DriveItem, String> thumbnailCache = new ConcurrentHashMap<>(); // TODO LRU

            /** */
            private String getUrl(Metadata entry) throws IOException {
                String url = thumbnailCache.get(entry.driveItem);
                if (url != null) {
                    return url;
                } else {
                    url = entry.driver.getThumbnail(entry.driveItem);
                    if (url != null) {
                        thumbnailCache.put(entry.driveItem, url);
                    }
                    return url;
                }
            }

            /** */
            private byte[] getThumbnail(Metadata entry) throws IOException {
                String url = getUrl(entry);
                InputStream is = new BufferedInputStream(new URL(url).openStream());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8024];
                int l = 0;
                while ((l = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, l);
                }
                return baos.toByteArray();
            }

            @Override
            public int size(Metadata entry) throws IOException {
                String url = getUrl(entry);
                if (url != null) {
                    byte[] thumbnail = getThumbnail(entry);
                    if (thumbnail != null) {
                        return thumbnail.length;
                    }
                }
                return 0;
            }

            @Override
            public int read(Metadata entry, ByteBuffer dst) throws IOException {
                String url = getUrl(entry);
                if (url != null) {
                    byte[] thumbnail = getThumbnail(entry);
                    if (thumbnail != null) {
                        dst.put(thumbnail);
                        return thumbnail.length;
                    }
                }
                return 0;
            }

            @Override
            public int write(Metadata entry, ByteBuffer src) throws IOException {
                byte[] thumbnail = src.array();
                entry.driver.setThumbnail(entry.driveItem, thumbnail);
                return thumbnail.length;
            }
        };
    }
}

/* */
