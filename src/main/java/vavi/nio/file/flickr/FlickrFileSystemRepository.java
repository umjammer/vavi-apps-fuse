/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.flickr;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.REST;
import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.net.auth.oauth2.BasicAppCredential;


/**
 * FlickrFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class FlickrFileSystemRepository extends FileSystemRepositoryBase {

    public FlickrFileSystemRepository() {
        super("flickr", new FlickrFileSystemFactoryProvider());
    }

    /** */
    private transient Flickr flickr;

    /** */
    private BasicAppCredential appCredential;

    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        if (!env.containsKey(FlickrFileSystemProvider.ENV_ID)) {
            throw new NoSuchElementException(FlickrFileSystemProvider.ENV_ID);
        }
        String email = (String) env.get(FlickrFileSystemProvider.ENV_ID);

        if (!env.containsKey(FlickrFileSystemProvider.ENV_CREDENTIAL)) {
            throw new NoSuchElementException(FlickrFileSystemProvider.ENV_CREDENTIAL);
        }
        appCredential = BasicAppCredential.class.cast(env.get(FlickrFileSystemProvider.ENV_CREDENTIAL));

        flickr = new Flickr(appCredential.getClientId(), appCredential.getClientSecret(), new REST());

        FlickrFileStore fileStore = new FlickrFileStore(flickr, factoryProvider.getAttributesFactory());
        return new FlickrFileSystemDriver(fileStore, factoryProvider, flickr, env);
    }
}
