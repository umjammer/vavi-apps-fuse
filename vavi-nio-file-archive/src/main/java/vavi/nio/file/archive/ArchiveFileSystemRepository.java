/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.archive;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.util.archive.Archive;
import vavi.util.archive.Archives;


/**
 * ArchiveFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/06 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class ArchiveFileSystemRepository extends FileSystemRepositoryBase {

    public ArchiveFileSystemRepository() {
        super("archive", new ArchiveFileSystemFactoryProvider());
    }

    /**
     * @param uri "archive:file:/tmp/jar/exam.jar!/img/sample.png"
     */
    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        String[] rawSchemeSpecificParts = uri.getRawSchemeSpecificPart().split("!");
        URI file = URI.create(rawSchemeSpecificParts[0]);
        if (!"file".equals(file.getScheme())) {
            // currently only support "file"
            throw new IllegalArgumentException(file.toString());
        }
        // TODO virtual relative directory from rawSchemeSpecificParts[1]

        Archive archive = Archives.getArchive(Paths.get(file).toFile());

        ArchiveFileStore fileStore = new ArchiveFileStore(factoryProvider.getAttributesFactory());
        return new ArchiveFileSystemDriver(fileStore, factoryProvider, archive, env);
    }

    /* ad-hoc hack for ignoring checking opacity */
    protected void checkURI(@Nullable final URI uri) {
        Objects.requireNonNull(uri);
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("uri is not absolute");
        }
        if (!getScheme().equals(uri.getScheme())) {
            throw new IllegalArgumentException("bad scheme");
        }
    }
}
