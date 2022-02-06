/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.github.fge.filesystem.attributes.provider.UserDefinedFileAttributesProvider;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import vavi.nio.file.googledrive.GoogleDriveFileAttributesFactory.Metadata;
import vavi.util.Debug;


/**
 * GoogleDriveUserDefinedFileAttributesProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/09/08 umjammer initial version <br>
 */
public class GoogleDriveUserDefinedFileAttributesProvider extends UserDefinedFileAttributesProvider {

    /** driver & file entry */
    private final Metadata entry;

    /** */
    public GoogleDriveUserDefinedFileAttributesProvider(Metadata entry) throws IOException {
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

    /** */
    private interface UserAttribute<T> {
        int size(T entry) throws IOException;
        int read(T entry, ByteBuffer dst) throws IOException;
        int write(T entry, ByteBuffer src) throws IOException;
    }

    /** user:foo */
    private enum UserAttributes implements UserAttribute<Metadata> {
        /** {@link String} */
        description {
            /** */
            Map<File, String> descriptionCache = new ConcurrentHashMap<>(); // TODO LRU

            /** */
            private String getDescription(File file) {
                if (descriptionCache.containsKey(file)) {
                    return descriptionCache.get(file);
                } else {
                    String description = file.getDescription();
                    descriptionCache.put(file, description);
                    return description;
                }
            }

            @Override
            public int size(Metadata entry) throws IOException {
                String description = getDescription(entry.file);
if (description != null) {
 Debug.println(Level.FINE, "size " + name() + ": " + description);
}
                return description == null ? 0 : description.getBytes().length;
            }

            @Override
            public int read(Metadata entry, ByteBuffer dst) throws IOException {
                String description = getDescription(entry.file);
if (description != null) {
 Debug.println(Level.FINE, "read " + name() + ": " + description);
}
                if (description != null) {
                    dst.put(description.getBytes());
                }
                return dst.array().length;
            }

            @Override
            public int write(Metadata entry, ByteBuffer src) throws IOException {
                String description = new String(src.array());
Debug.println(Level.FINE, "write " + name() + ": " + description);
                entry.driver.patchEntryDescription(entry.file, description);
                return description.getBytes().length;
            }
        },
        /** {@link String}, "\n" separated */
        revisions {
            /** */
            Map<File, List<String>> revisionsCache = new ConcurrentHashMap<>(); // TODO LRU

            /** */
            private List<String> getRevisions(Metadata entry) throws IOException {
                if (revisionsCache.containsKey(entry.file)) {
                    return revisionsCache.get(entry.file);
                } else {
                    List<Revision> revisions = entry.driver.getRevisions(entry.file); 
                    List<String> results = revisions.stream().map(r -> r.toString()).collect(Collectors.toList());
                    revisionsCache.put(entry.file, results);
                    return results;
                }
            }

            @Override
            public int size(Metadata entry) throws IOException {
                // joined by '\n'
                int len = getRevisions(entry).stream().mapToInt(r -> r.toString().getBytes().length + 1).sum() - 1;
if (len > 0) {
 Debug.println(Level.FINE, "size " + name() + ": " + len);
}
                return len;
            }

            @Override
            public int read(Metadata entry, ByteBuffer dst) throws IOException {
Debug.println(Level.FINE, "read " + name() + ":\n" + String.join("\n", getRevisions(entry)));
                dst.put(String.join("\n", getRevisions(entry)).getBytes());
                return dst.array().length;
            }

            @Override
            public int write(Metadata entry, ByteBuffer src) throws IOException {
                String[] revisions = RevisionsUtil.split(src.array());
Arrays.stream(revisions).forEach(r -> {
 Debug.println(Level.INFO, "write " + name() + ": " + r);
});
                // to be deleted
                List<String> toDeleted = new ArrayList<>(); 
                Arrays.stream(revisions)
                    .map(y -> (String) gson.fromJson(y, Map.class).get("id"))
                    .forEach(b -> {
                        try {
                            toDeleted.addAll(getRevisions(entry).stream()
                                    .map(x -> (String) gson.fromJson(x, Map.class).get("id"))
                                    .filter(a -> !a.equals(b))
                                    .collect(Collectors.toList()));
                        } catch (IOException e) {
Debug.printStackTrace(Level.WARNING, e);
                        }
                    });

                toDeleted.forEach(id -> {
                    try {
                        entry.driver.removeRevision(entry.file, id);
                    } catch (IOException e) {
Debug.printStackTrace(Level.WARNING, e);
                    }
                });

                revisionsCache.remove(entry.file);

                // TODO to be added?

                return src.array().length;
            }
        },
        /** whole image file */
        thumbnail {
            /** */
            Map<File, String> thumbnailCache = new ConcurrentHashMap<>(); // TODO LRU

            /** */
            private String getUrl(Metadata entry) throws IOException {
                if (thumbnailCache.containsKey(entry.file)) {
                    return thumbnailCache.get(entry.file);
                } else {
                    String url = entry.driver.getThumbnail(entry.file); 
                    thumbnailCache.put(entry.file, url);
                    return url;
                }
            }

            /** */
            private byte[] getThumbnail(Metadata entry) throws IOException {
try {
                String url = getUrl(entry);
                InputStream is = new BufferedInputStream(new URL(url).openStream());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8024];
                int l = 0;
                while ((l = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, l);
                }
                return baos.toByteArray();
} catch (java.io.FileNotFoundException e) {
 Debug.println(Level.WARNING, e.toString());
 return null;
}
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
                entry.driver.setThumbnail(entry.file, thumbnail);
                return thumbnail.length;
            }
        };
    }

    /** */
    private static Gson gson = new GsonBuilder().create();

    /** utility */
    public static class RevisionsUtil {
        /** sort by "modifiedTime" desc */
        public static byte[] getLatestOnly(byte[] revisions) {
            List<Map<String, String>> revisionList = Arrays.stream(split(revisions))
                    .map(r -> gson.fromJson(r, Map.class))
                    .sorted((o1, o2) -> {
                        OffsetDateTime odt1 = OffsetDateTime.parse((String) o1.get("modifiedTime"));
                        OffsetDateTime odt2 = OffsetDateTime.parse((String) o2.get("modifiedTime"));
                        return odt2.compareTo(odt1);
                    })
                    .collect(Collectors.toList());
//revisionList.forEach(System.err::println);
            return gson.toJson(revisionList.get(0)).getBytes(); 
        }
        public static String[] split(byte[] revisions) {
            return new String((byte[]) revisions).split("\n");
        }
        public static int size(Object revisions) {
            return split((byte[]) revisions).length;
        }
    }
}

/* */
