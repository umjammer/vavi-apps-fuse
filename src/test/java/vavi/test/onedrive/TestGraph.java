/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.test.onedrive;

import java.net.URLEncoder;

import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.concurrency.IProgressCallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.BaseRequest;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.http.HttpMethod;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.http.IStatefulResponseHandler;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.DriveItemCopyBody;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.ItemReference;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IDriveItemCopyRequest;

import vavi.net.auth.oauth2.BasicAppCredential;
import vavi.net.auth.oauth2.OAuth2;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphLocalAppCredential;
import vavi.nio.file.onedrive4.graph.CopyMonitorProvider;
import vavi.nio.file.onedrive4.graph.CopyMonitorResponseHandler;
import vavi.nio.file.onedrive4.graph.CopyMonitorResult;
import vavi.nio.file.onedrive4.graph.CopySession;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * TestGraph.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/04 umjammer initial version <br>
 */
@PropsEntity(url = "classpath:onedrive.properties")
public class TestGraph {

    @Property
    private String authenticatorClassName;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String email = "snaohide@hotmail.com";

        BasicAppCredential credential = new MicrosoftGraphLocalAppCredential();
        PropsEntity.Util.bind(credential, email);

        TestGraph app = new TestGraph();
        PropsEntity.Util.bind(app);

        String accesssToken = new OAuth2(credential, true, app.authenticatorClassName).getAccessToken(email);

        IGraphServiceClient client = GraphServiceClient.builder()
                .authenticationProvider(new IAuthenticationProvider() {
                    @Override
                    public void authenticateRequest(IHttpRequest request) {
                        request.addHeader("Authorization", "Bearer " + accesssToken);
                        }
                })
                .buildClient();

//        client.me().drive().root().children().buildRequest().get().getCurrentPage().forEach(e -> System.err.println(e.name));

//        client.me().drive().root().itemWithPath(URLEncoder.encode("文書/Novels", "utf-8")).children().buildRequest().get().getCurrentPage().forEach(e -> System.err.println(e.name));

//        client.me().drive().root().itemWithPath(URLEncoder.encode("文書/Novels/あ", "utf-8")).children().buildRequest().get().getCurrentPage().forEach(e -> System.err.println(e.name));

//        Path path = Paths.get(System.getenv("HOME"), "Music/0/rc.wav");
//        InputStream is = new FileInputStream(path.toFile());
//        UploadSession uploadSession = client.drive().root().itemWithPath(URLEncoder.encode("test/テスト.wav", "utf-8")).createUploadSession(new DriveItemUploadableProperties()).buildRequest().post();
//        ChunkedUploadProvider<DriveItem> chunkedUploadProvider = new ChunkedUploadProvider<>(uploadSession,
//                client, is, is.available(), DriveItem.class);
//        chunkedUploadProvider.upload(new IProgressCallback<DriveItem>() {
//                @Override
//                public void progress(final long current, final long max) {
//                    System.err.println(current + "/" + max);
//                }
//                @Override
//                public void success(final DriveItem result) {
//                    System.err.println("done");
//                }
//                @Override
//                public void failure(final ClientException ex) {
//                    throw new IllegalStateException(ex);
//                }
//            });

        try {
            client.drive().root().itemWithPath(URLEncoder.encode("test/フォルダー/コピー.wav", "utf-8")).buildRequest().delete();
        } catch (GraphServiceException e) {
            if (!e.getMessage().startsWith("Error code: itemNotFound")) {
                throw e;
            }
        }

        DriveItem src = client.drive().root().itemWithPath(URLEncoder.encode("test/テスト.wav", "utf-8")).buildRequest().get();
        DriveItem dst = client.drive().root().itemWithPath(URLEncoder.encode("test/フォルダー", "utf-8")).buildRequest().get();


        ItemReference ir = new ItemReference();
        ir.id = dst.id;
        IDriveItemCopyRequest request = client.drive().items(src.id).copy("コピー.wav", ir).buildRequest();
        BaseRequest.class.cast(request).setHttpMethod(HttpMethod.POST);
        DriveItemCopyBody body = new DriveItemCopyBody();
        body.name = "コピー.wav";
        body.parentReference = ir;
        CopyMonitorResponseHandler<DriveItem> handler = new CopyMonitorResponseHandler<>();
        CopySession copySession = client.getHttpProvider().<CopyMonitorResult, DriveItemCopyBody, CopyMonitorResult>send((IHttpRequest) request, CopyMonitorResult.class, body, (IStatefulResponseHandler) handler).getSession();
        CopyMonitorProvider<DriveItem> copyMonitorProvider = new CopyMonitorProvider<>(copySession, client, DriveItem.class);
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
}

/* */
