/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.concurrency.ChunkedUploadProvider;
import com.microsoft.graph.concurrency.IProgressCallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.BaseRequest;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.http.HttpMethod;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.http.IStatefulResponseHandler;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.DriveItemCopyBody;
import com.microsoft.graph.models.extensions.DriveItemUploadableProperties;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.ItemReference;
import com.microsoft.graph.models.extensions.ThumbnailSet;
import com.microsoft.graph.models.extensions.UploadSession;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IDriveItemCopyRequest;
import com.microsoft.graph.requests.extensions.IThumbnailSetCollectionPage;

import vavi.net.auth.WithTotpUserCredential;
import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphLocalAppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphOAuth2;
import vavi.net.auth.web.microsoft.MicrosoftLocalUserCredential;
import vavi.nio.file.onedrive4.graph.LraMonitorProvider;
import vavi.nio.file.onedrive4.graph.LraMonitorResponseHandler;
import vavi.nio.file.onedrive4.graph.LraMonitorResult;
import vavi.nio.file.onedrive4.graph.LraSession;
import vavi.nio.file.onedrive4.graph.ThumbnailUploadProvider;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.properties.annotation.PropsEntity;


/**
 * TestGraph.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/04 umjammer initial version <br>
 */
public class TestGraph {

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod", "(" +
                           "sun\\.util\\.logging\\.\\w*Log\\w*#\\w+" + "|" +
                           "jdk\\.internal\\.event\\.EventHelper#log\\w+" +
                           ")");
    }

    /**
     * @param args 0: email
     */
    public static void main(String[] args) throws Exception {
//        String email = args[0];
        String email = "snaohide@hotmail.com";

        TestGraph app = new TestGraph();
        app.auth(email);
//        app.testCopy();
        app.testThumbnail();
        app.close();
    }

    void close() {
        auth2.close();
    }

    MicrosoftGraphOAuth2 auth2;

    /** @after {@link #client} */
    void auth(String email) throws IOException {

        OAuth2AppCredential appCredential = new MicrosoftGraphLocalAppCredential();
        PropsEntity.Util.bind(appCredential);

        WithTotpUserCredential userCredential = new MicrosoftLocalUserCredential(email);
        auth2 = new MicrosoftGraphOAuth2(appCredential, true);
        String accesssToken = auth2.authorize(userCredential);

        client = GraphServiceClient.builder()
                .authenticationProvider(new IAuthenticationProvider() {
                    @Override
                    public void authenticateRequest(IHttpRequest request) {
                        request.addHeader("Authorization", "Bearer " + accesssToken);
                        }
                })
                .buildClient();
    }

    /** */
    private IGraphServiceClient client;

    /** */
    void testList() throws IOException {
        client.me().drive().root().children().buildRequest().get().getCurrentPage().forEach(e -> System.err.println(e.name));

        client.me().drive().root().itemWithPath(URLEncoder.encode("文書/Novels", "utf-8")).children().buildRequest().get().getCurrentPage().forEach(e -> System.err.println(e.name));

        client.me().drive().root().itemWithPath(URLEncoder.encode("文書/Novels/あ", "utf-8")).children().buildRequest().get().getCurrentPage().forEach(e -> System.err.println(e.name));
    }

    /** */
    void testUpload() throws IOException {
        testDelete("test/テスト.wav");

        Path path = Paths.get(System.getenv("HOME"), "Music/0/rc.wav");
        InputStream is = new FileInputStream(path.toFile());
        UploadSession uploadSession = client.drive().root().itemWithPath(URLEncoder.encode("test/テスト.wav", "utf-8")).createUploadSession(new DriveItemUploadableProperties()).buildRequest().post();
        ChunkedUploadProvider<DriveItem> chunkedUploadProvider = new ChunkedUploadProvider<>(uploadSession,
                client, is, is.available(), DriveItem.class);
        chunkedUploadProvider.upload(new IProgressCallback<DriveItem>() {
                @Override
                public void progress(final long current, final long max) {
                    System.err.println(current + "/" + max);
                }
                @Override
                public void success(final DriveItem result) {
                    System.err.println("done");
                }
                @Override
                public void failure(final ClientException ex) {
                    throw new IllegalStateException(ex);
                }
            });
    }

    /** */
    void testDelete(String name) throws IOException {
        try {
            client.drive().root().itemWithPath(URLEncoder.encode(name, "utf-8")).buildRequest().delete();
        } catch (GraphServiceException e) {
            if (!e.getMessage().startsWith("Error code: itemNotFound")) {
                throw e;
            } else {
                System.err.println("file not found:" + name);
            }
        }
    }

    /** */
    void testCopy() throws IOException {
        testDelete("test/フォルダー/コピー.wav");

        DriveItem src = client.drive().root().itemWithPath(URLEncoder.encode("test/テスト.wav", "utf-8")).buildRequest().get();
        DriveItem dst = client.drive().root().itemWithPath(URLEncoder.encode("test/フォルダー", "utf-8")).buildRequest().get();


        ItemReference ir = new ItemReference();
        ir.id = dst.id;
        IDriveItemCopyRequest request = client.drive().items(src.id).copy("コピー.wav", ir).buildRequest();
        BaseRequest.class.cast(request).setHttpMethod(HttpMethod.POST);
        DriveItemCopyBody body = new DriveItemCopyBody();
        body.name = "コピー.wav";
        body.parentReference = ir;
        LraMonitorResponseHandler<DriveItem> handler = new LraMonitorResponseHandler<>();
        @SuppressWarnings({ "unchecked", "rawtypes" }) // TODO
        LraSession copySession = client.getHttpProvider().<LraMonitorResult, DriveItemCopyBody, LraMonitorResult>send((IHttpRequest) request, LraMonitorResult.class, body, (IStatefulResponseHandler) handler).getSession();
        LraMonitorProvider<DriveItem> copyMonitorProvider = new LraMonitorProvider<>(copySession, client, DriveItem.class);
        copyMonitorProvider.monitor(new IProgressCallback<DriveItem>() {
                @Override
                public void progress(final long current, final long max) {
                    System.err.println(current + "/" + max);
                }
                @Override
                public void success(final DriveItem result) {
                    System.err.println("done: " + result.getRawObject());
                }
                @Override
                public void failure(final ClientException ex) {
                    ex.printStackTrace();
                }
            });
    }

    /** */
    void testThumbnail() throws Exception {
        Path p = Paths.get(TestGraph.class.getResource("/duke.jpg").toURI());
        byte[] b = Files.readAllBytes(p);

        DriveItem sourceEntry = client.drive().root().itemWithPath("tmp/test.zip").buildRequest().get();

        ThumbnailUploadProvider provider = new ThumbnailUploadProvider(sourceEntry, client);
        provider.upload(b);
Debug.println("upload done");

        Thread.sleep(3000);

        IThumbnailSetCollectionPage page = client.drive().items(sourceEntry.id)
            .thumbnails()
            .buildRequest(Arrays.asList(new QueryOption("select", "source")))
            .get();
        ThumbnailSet set = page.getCurrentPage().get(0);
Debug.println("set: " + StringUtil.paramString(set));
Debug.println("thumbnail url: " + set.source.url);

        Path dir = Paths.get("tmp");
        Path out = dir.resolve("thumbnail.jpg");

        Files.copy(new URL(set.source.url).openStream(), out, StandardCopyOption.REPLACE_EXISTING);
    }
}

/* */
