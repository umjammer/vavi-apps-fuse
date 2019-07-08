/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.yetiz.lib.acd.ACD;
import org.yetiz.lib.acd.ACDSession;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;


/**
 * AcdFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class AcdFileSystemRepository extends FileSystemRepositoryBase {

    public AcdFileSystemRepository() {
        super("acd", new AcdFileSystemFactoryProvider());
    }

    /** */
    private transient ACD drive;
    /** */
    private transient ACDSession session;

    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        if (!env.containsKey(AcdFileSystemProvider.ENV_ID)) {
            throw new NoSuchElementException(AcdFileSystemProvider.ENV_ID);
        }
        String email = (String) env.get(AcdFileSystemProvider.ENV_ID);

        final AcdFileStore fileStore = new AcdFileStore(drive, factoryProvider.getAttributesFactory());
        return new AcdFileSystemDriver(fileStore, factoryProvider, drive, session, env);
    }
}
