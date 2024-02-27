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
import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.net.auth.UserCredential;
import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.flickr.FlickrLocalAppCredential;
import vavi.net.auth.oauth2.flickr.FlickrOAuth2;
import vavi.net.auth.web.flickr.FlickrLocalUserCredential;


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

    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        // 1. user credential
        UserCredential userCredential = null;

        if (env.containsKey(FlickrFileSystemProvider.ENV_USER_CREDENTIAL)) {
            userCredential = (UserCredential) env.get(FlickrFileSystemProvider.ENV_USER_CREDENTIAL);
        }

        Map<String, String> params = getParamsMap(uri);
        if (userCredential == null && params.containsKey(FlickrFileSystemProvider.PARAM_ID)) {
            String email = params.get(FlickrFileSystemProvider.PARAM_ID);
            userCredential = new FlickrLocalUserCredential(email);
        }

        if (userCredential == null) {
            throw new NoSuchElementException("uri not contains a param " + FlickrFileSystemProvider.PARAM_ID + " nor " +
                                             "env not contains a param " + FlickrFileSystemProvider.ENV_USER_CREDENTIAL);
        }

        // 2. app credential
        OAuth2AppCredential appCredential = null;

        if (env.containsKey(FlickrFileSystemProvider.ENV_APP_CREDENTIAL)) {
            appCredential = (OAuth2AppCredential) env.get(FlickrFileSystemProvider.ENV_APP_CREDENTIAL);
        }

        if (appCredential == null) {
            appCredential = new FlickrLocalAppCredential(); // TODO use props
        }

        // 3. process
        Flickr flickr = new FlickrOAuth2(appCredential).authorize(userCredential);
        FlickrFileStore fileStore = new FlickrFileStore(flickr, factoryProvider.getAttributesFactory());
        return new FlickrFileSystemDriver(fileStore, factoryProvider, flickr, env);
    }
}
