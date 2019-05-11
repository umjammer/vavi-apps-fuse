package vavi.net.fuse.googledrive;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


@PropsEntity(url = "file://${user.home}/.vavifuse/googledrive.properties")
public class DriveCommandLine {

    @Property(name = "googledrive.clientId")
    private String clientId;
    @Property(name = "googledrive.clientSecret")
    private String clientSecret;

    @Property(name = "googledrive.redirectUrl")
    private String redirectUrl;

    /**
     * @param args 0: email
     */
    public static void main(String[] args) throws IOException {

        String email = args[0];

        DriveCommandLine app = new DriveCommandLine();
        PropsEntity.Util.bind(app, email);

        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                app.clientId,
                app.clientSecret,
                Arrays.asList(DriveScopes.DRIVE)
                ).setAccessType("online").setApprovalPrompt("auto").build();

        String url = flow.newAuthorizationUrl().setRedirectUri(app.redirectUrl).build();

        String code = new GoogleDriveFxGetter(email).get(url);

        GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(app.redirectUrl).execute();
        GoogleCredential credential = new GoogleCredential().setFromTokenResponse(response);

        // 新規認証APIクライアントを作成
        Drive service = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName("vavifuse").build();

        //
        FileList result = service.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.size() == 0) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        }

        // ファイルを挿入
/*
        File body = new File();
        body.setTitle("My document");
        body.setDescription("A test document");
        body.setMimeType("text/plain");

        java.io.File fileContent = new java.io.File("サンプルurl/document.txt");
        FileContent mediaContent = new FileContent("text/plain", fileContent);

        File file = service.files().insert(body, mediaContent).execute();
        System.out.println("File ID: " + file.getId());
*/
    }
}
