/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.flickr;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.REST;
import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * FlickrFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
@PropsEntity(url = "file://${user.home}/.vavifuse/flickr.properties")
public final class FlickrFileSystemRepository extends FileSystemRepositoryBase {

    @Property(name = "flickr.clientId")
    private String clientId;
    @Property(name = "flickr.clientSecret")
    private transient String clientSecret;
    @Property(name = "flickr.redirectUrl")
    private String redirectUrl;

    public FlickrFileSystemRepository() {
        super("flickr", new FlickrFileSystemFactoryProvider());
    }

    /** */
    private transient Flickr flickr;

    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        final String email = (String) env.get("email");
        if (email == null)
            throw new IllegalArgumentException("email not found");

        PropsEntity.Util.bind(this);

        flickr = new Flickr(clientId, clientSecret, new REST());

        final FlickrFileStore fileStore = new FlickrFileStore(flickr, factoryProvider.getAttributesFactory());
        return new FlickrFileSystemDriver(fileStore, factoryProvider, flickr, env);
    }
}
