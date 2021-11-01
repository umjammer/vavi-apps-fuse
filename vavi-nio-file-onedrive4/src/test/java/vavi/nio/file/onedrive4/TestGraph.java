/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import com.microsoft.graph.authentication.BaseAuthenticationProvider;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.httpcore.HttpClients;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCopyParameterSet;
import com.microsoft.graph.models.DriveItemCreateUploadSessionParameterSet;
import com.microsoft.graph.models.DriveItemUploadableProperties;
import com.microsoft.graph.models.ItemReference;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.tasks.LargeFileUploadResult;
import com.microsoft.graph.tasks.LargeFileUploadTask;

import vavi.net.auth.WithTotpUserCredential;
import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphLocalAppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphOAuth2;
import vavi.net.auth.web.microsoft.MicrosoftLocalUserCredential;
import vavi.nio.file.onedrive4.graph.MyLogger;
import vavi.util.properties.annotation.PropsEntity;

import okhttp3.OkHttpClient;


/**
 * TestGraph.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/04 umjammer initial version <br>
 */
public class TestGraph {

    /**
     * @param args 0: email
     */
    public static void main(String[] args) throws Exception {
        String email = args[0];

        TestGraph app = new TestGraph();
        app.auth(email);
        app.testUpload();
        app.testCopy();
    }

    /** */
    void auth(String email) throws IOException {

        OAuth2AppCredential appCredential = new MicrosoftGraphLocalAppCredential();
        PropsEntity.Util.bind(appCredential);

        WithTotpUserCredential userCredential = new MicrosoftLocalUserCredential(email);
        @SuppressWarnings("resource")
		String accessToken = new MicrosoftGraphOAuth2(appCredential, true).authorize(userCredential);

        BaseAuthenticationProvider authenticationProvider = new BaseAuthenticationProvider() {
            @Override
            public CompletableFuture<String> getAuthorizationTokenAsync(URL requestUrl) {
                if (this.shouldAuthenticateRequestWithUrl(requestUrl)) {
                    return CompletableFuture.completedFuture(accessToken);
                } else {
                    return CompletableFuture.completedFuture(null);
                }
            }
        };
        OkHttpClient httpClient = HttpClients.createDefault(authenticationProvider);
        client = GraphServiceClient.builder()
            .httpClient(httpClient)
            .logger(new MyLogger())
            .buildClient();
    }

    /** */
    private GraphServiceClient<?> client;

    /** */
    void testList() throws IOException {
        client.me().drive().root().children().buildRequest().get().getCurrentPage().forEach(e -> System.err.println(e.name));

        client.me().drive().root().itemWithPath(URLEncoder.encode("文書/Novels", "utf-8")).children().buildRequest().get().getCurrentPage().forEach(e -> System.err.println(e.name));

        client.me().drive().root().itemWithPath(URLEncoder.encode("文書/Novels/あ", "utf-8")).children().buildRequest().get().getCurrentPage().forEach(e -> System.err.println(e.name));
    }

    /** */
    void testUpload() throws IOException {
        testDelete("tmp/テスト.aiff");

        Path path = Paths.get("/System/Library/Sounds/Frog.aiff");

        UploadSession uploadSession = client.drive().root()
        		.itemWithPath("tmp/テスト.aiff")
        		.createUploadSession(DriveItemCreateUploadSessionParameterSet.newBuilder().withItem(new DriveItemUploadableProperties()).build())
        		.buildRequest()
        		.post();
        LargeFileUploadTask<DriveItem> chunkedUploadProvider = new LargeFileUploadTask<DriveItem>(
				uploadSession,
				client,
				Files.newInputStream(path),
				Files.size(path),
				DriveItem.class);
        LargeFileUploadResult<DriveItem> result = chunkedUploadProvider.upload(0, null, (current, max) -> {
            System.err.println(current + "/" + max);
        });
		System.err.println("done: " + result.responseBody);
    }

    /** */
    void testDelete(String name) throws IOException {
        try {
            client.drive().root().itemWithPath(name).buildRequest().delete();
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
        testDelete("tmp/フォルダー/コピー.aiff");

        DriveItem src = client.drive().root().itemWithPath("tmp/テスト.aiff").buildRequest().get();
        DriveItem dst = client.drive().root().itemWithPath("tmp/フォルダー").buildRequest().get();

        ItemReference ir = new ItemReference();
        ir.id = dst.id;
        client.drive()
			.items(src.id)
			.copy(DriveItemCopyParameterSet.newBuilder()
					.withName("コピー.aiff")
						.withParentReference(ir)
						.build())
				.buildRequest()
				.postAsync()
				.thenAccept(result -> {
					System.err.println("done: " + result);
				});
// progress
//        System.err.println(current + "/" + max);
    }
}

/* */
