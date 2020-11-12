/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import vavi.net.auth.oauth2.microsoft.MicrosoftLocalAppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftOAuth2;
import vavi.net.auth.web.microsoft.MicrosoftLocalUserCredential;
import vavi.util.Debug;

import static vavi.net.auth.oauth2.OAuth2AppCredential.wrap;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveFactory;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.OneDriveScope;
import de.tuberlin.onedrivesdk.networking.OneDriveAuthenticationException;


/**
 * WebHookTest3. onedrive, using now construction libraries.
 *
 * @depends "file://${HOME}.vavifuse/onedrive/?"
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 * @see "https://stackoverflow.com/a/25794109/6102938"
 */
public class WebHookTest3 {

    static String email = System.getenv("MICROSOFT_TEST_ACCOUNT");

    /**
     * @param args 0: email
     */
    public static void main(String[] args) throws Exception {
        WebHookTest3 app = new WebHookTest3();
        app.test();
    }

    void test() throws Exception {
        MicrosoftLocalUserCredential userCredential = new MicrosoftLocalUserCredential(email);
        MicrosoftLocalAppCredential appCredential = new MicrosoftLocalAppCredential();

        OneDriveSDK client = null;
        try {
            client = OneDriveFactory.createOneDriveSDK(appCredential.getClientId(),
                                                       appCredential.getClientSecret(),
                                                       appCredential.getRedirectUrl(),
                                                       OneDriveScope.OFFLINE_ACCESS); // TODO out source
            String url = client.getAuthenticationURL();

            MicrosoftOAuth2 oauth2 = new MicrosoftOAuth2(wrap(appCredential, url), userCredential.getId());
            String code = null;
            String refreshToken = oauth2.readRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                code = oauth2.authorize(userCredential);
                client.authenticate(code);
            } else {
                try {
                    client.authenticateWithRefreshToken(refreshToken);
                } catch (OneDriveAuthenticationException e) {
Debug.println("refreshToken: timeout?");
                    code = oauth2.authorize(userCredential);
                    client.authenticate(code);
                }
            }

        } catch (OneDriveException e) {
            throw new IOException(e);
        }

        OneDriveWatchService service = new OneDriveWatchService(client);
Debug.println("WEBSOCKET: start: " + service);
        try {
            URI uri = URI.create("onedrive1:///?id=" + email);
            FileSystem fs = FileSystems.newFileSystem(uri, Collections.EMPTY_MAP);

            Path tmpDir = fs.getPath("tmp");
            if (!Files.exists(tmpDir)) {
System.out.println("rmdir " + tmpDir);
                Files.createDirectory(tmpDir);
            }
            Path remote = tmpDir.resolve("Test+Watch");
            if (Files.exists(remote)) {
System.out.println("rm " + remote);
                Files.delete(remote);
            }
            Path source = Paths.get("src/test/resources", "Hello.java");
System.out.println("cp " + source + " " + remote);
            Files.copy(source, remote);

System.out.println("rm " + remote);
            Files.delete(remote);

            Thread.sleep(10000);

            fs.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            service.close();
        }
Debug.println("APP: done");
        OneDriveWatchService.dispose();
//Thread.getAllStackTraces().keySet().forEach(System.err::println);
    }
}
