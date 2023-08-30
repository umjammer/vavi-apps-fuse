/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.drive.Drive;

import vavi.net.auth.WithTotpUserCredential;
import vavi.net.auth.oauth2.google.GoogleOAuth2AppCredential;
import vavi.net.auth.oauth2.google.GoogleLocalOAuth2AppCredential;
import vavi.net.auth.oauth2.google.GoogleOAuth2;
import vavi.net.auth.web.google.GoogleLocalUserCredential;
import vavi.util.Debug;


/**
 * WebHookTest3. google drive, using libraries now construction.
 *
 * @depends "file://${HOME}.vavifuse/googledrive/?"
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
public class WebHookTest3 {

    static String email = System.getenv("GOOGLE_TEST_ACCOUNT");

    /**
     * @param args 0: email
     *
     * @see "https://developers.google.com/drive/api/v3/reference/changes/watch"
     * @see "https://stackoverflow.com/a/43793313/6102938"
     *  TODO needs domain authorize
     */
    public static void main(String[] args) throws Exception {
        WebHookTest3 app = new WebHookTest3();
        app.test();
    }

    void test() throws Exception {
        WithTotpUserCredential userCredential = new GoogleLocalUserCredential(email);
        GoogleOAuth2AppCredential appCredential = new GoogleLocalOAuth2AppCredential("googledrive");

        Credential credential = new GoogleOAuth2(appCredential).authorize(userCredential);
        Drive driveService = new Drive.Builder(GoogleOAuth2.getHttpTransport(), GoogleOAuth2.getJsonFactory(), credential)
                .setHttpRequestInitializer(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest httpRequest) throws IOException {
                        credential.initialize(httpRequest);
                        httpRequest.setConnectTimeout(30 * 1000);
                        httpRequest.setReadTimeout(30 * 1000);
                    }
                })
                .setApplicationName(appCredential.getClientId())
                .build();

        GoogleDriveWatchService service = new GoogleDriveWatchService(driveService);
Debug.println("WEBSOCKET: start: " + service);
        try {
            URI uri = URI.create("googledrive:///?id=" + email);
            FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

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

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            service.close();
        }
Debug.println("APP: done");
        GoogleDriveWatchService.dispose();
//Thread.getAllStackTraces().keySet().forEach(System.err::println);
    }
}
