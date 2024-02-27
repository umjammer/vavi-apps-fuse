/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import vavi.net.auth.WithTotpUserCredential;
import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphLocalAppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphOAuth2;
import vavi.net.auth.web.microsoft.MicrosoftLocalUserCredential;
import vavi.nio.file.EasyFS;
import vavi.util.properties.annotation.PropsEntity;


/**
 * OneDriveEasyFS.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-11-23 nsano initial version <br>
 */
public class OneDriveEasyFS implements EasyFS<DriveItem> {

    /** */
    private final MicrosoftGraphOAuth2 auth2;
    /** */
    private final IGraphServiceClient client;

    /** */
    public OneDriveEasyFS(String email) throws IOException {

        OAuth2AppCredential appCredential = new MicrosoftGraphLocalAppCredential();
        PropsEntity.Util.bind(appCredential);

        WithTotpUserCredential userCredential = new MicrosoftLocalUserCredential(email);
        auth2 = new MicrosoftGraphOAuth2(appCredential, true);
        String accesssToken = auth2.authorize(userCredential);

        client = GraphServiceClient.builder()
                .authenticationProvider(request -> request.addHeader("Authorization", "Bearer " + accesssToken))
                .buildClient();
    }

    @Override
    public boolean isFolder(DriveItem entry) {
        return entry.folder != null;
    }

    @Override
    public DriveItem getRootEntry() throws IOException {
        return client.drive().root().buildRequest().get();
    }

    @Override
    public List<DriveItem> getDirectoryEntries(DriveItem dirEntry) throws IOException {
        List<DriveItem> list = new ArrayList<>(dirEntry.folder.childCount);

        IDriveItemCollectionPage pages = client.drive().items(dirEntry.id).children().buildRequest().get();
        while (pages != null) {
            list.addAll(pages.getCurrentPage());
            pages = pages.getNextPage() != null ? pages.getNextPage().buildRequest().get() : null;
        }

        return list;
    }

    @Override
    public DriveItem renameEntry(DriveItem sourceEntry, String name) throws IOException {
        DriveItem preEntry = new DriveItem();
        preEntry.name = name;
        return client.drive().items(sourceEntry.id).buildRequest().patch(preEntry);
    }

    @Override
    public void walk(DriveItem dirEntry, Consumer<DriveItem> task) throws Exception {
        List<DriveItem> list = getDirectoryEntries(dirEntry);
        for (DriveItem item : list) {
            if (isFolder(item)) {
                walk(item, task);
            } else {
                task.accept(item);
            }
        }
    }
}
