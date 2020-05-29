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

import vavi.net.auth.UserCredential;
import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.amazon.AmazonLocalAppCredential;
import vavi.net.auth.oauth2.amazon.AmazonOAuth2;
import vavi.net.auth.web.amazon.AmazonLocalUserCredential;


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

    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        // 1. user credential
        UserCredential userCredential = null;

        if (env.containsKey(AcdFileSystemProvider.ENV_USER_CREDENTIAL)) {
            userCredential = UserCredential.class.cast(env.get(AcdFileSystemProvider.ENV_USER_CREDENTIAL));
        }

        Map<String, String> params = getParamsMap(uri);
        if (userCredential == null && params.containsKey(AcdFileSystemProvider.PARAM_ID)) {
            String email = params.get(AcdFileSystemProvider.PARAM_ID);
            userCredential = new AmazonLocalUserCredential(email);
        }

        if (userCredential == null) {
            throw new NoSuchElementException("uri not contains a param " + AcdFileSystemProvider.PARAM_ID + " nor " +
                                             "env not contains a param " + AcdFileSystemProvider.ENV_USER_CREDENTIAL);
        }

        // 2. app credential
        OAuth2AppCredential appCredential = null;

        if (env.containsKey(AcdFileSystemProvider.ENV_APP_CREDENTIAL)) {
            appCredential = OAuth2AppCredential.class.cast(env.get(AcdFileSystemProvider.ENV_APP_CREDENTIAL));
        }

        if (appCredential == null) {
            appCredential = new AmazonLocalAppCredential(); // TODO use props
        }

        // 3. process
        ACDSession session = new AmazonOAuth2(appCredential).authorize(userCredential);
        ACD drive = new ACD(session);
        AcdFileStore fileStore = new AcdFileStore(drive, factoryProvider.getAttributesFactory());
        return new AcdFileSystemDriver(fileStore, factoryProvider, drive, session, env);
    }
}
