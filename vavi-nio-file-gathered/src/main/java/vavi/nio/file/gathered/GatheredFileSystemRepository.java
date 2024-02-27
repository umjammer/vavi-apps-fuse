/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.gathered;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;


/**
 * GatheredFsFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/06 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class GatheredFileSystemRepository extends FileSystemRepositoryBase {

    public GatheredFileSystemRepository() {
        super("gatheredfs", new GatheredFileSystemFactoryProvider());
    }

    /**
     * @param env ENV_FILESYSTEMS must be set.
     * <pre>
     * { "id", FileSystem }
     * </pre>
     */
    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        if (!env.containsKey(GatheredFileSystemProvider.ENV_FILESYSTEMS)) {
            throw new NoSuchElementException(GatheredFileSystemProvider.ENV_FILESYSTEMS);
        }
        Map<String, FileSystem> fileSystems = (Map<String, FileSystem>) env.get(GatheredFileSystemProvider.ENV_FILESYSTEMS);

        GatheredFileStore fileStore = new GatheredFileStore(factoryProvider.getAttributesFactory());
        return new GatheredFileSystemDriver(fileStore, factoryProvider, fileSystems, env);
    }
}
