/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import com.github.fge.filesystem.driver.DoubleCachedFileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemProviderBase;


/**
 * GoogleDriveFileSystemProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
public final class GoogleDriveFileSystemProvider extends FileSystemProviderBase {

    /**
     * uri parameter: "id"
     * @see java.nio.file.FileSystems#newFileSystem(java.net.URI, java.util.Map)
     */
    public static final String PARAM_ID = "id";

    /**
     * {@link vavi.net.auth.UserCredential}: user credential
     * @see java.nio.file.FileSystems#newFileSystem(java.net.URI, java.util.Map)
     */
    public static final String ENV_USER_CREDENTIAL = "user_credential";

    /**
     * {@link vavi.net.auth.AppCredential}: application credential
     * @see java.nio.file.FileSystems#newFileSystem(java.net.URI, java.util.Map)
     */
    public static final String ENV_APP_CREDENTIAL = "app_credential";

    /**
     * boolean: true: use system file watcher
     * @see java.nio.file.FileSystems#newFileSystem(java.net.URI, java.util.Map)
     */
    public static final String ENV_USE_SYSTEM_WATCHER = "use_system_watcher";

    /**
     * boolean: false: disable to normalize filename
     * @see java.nio.file.FileSystems#newFileSystem(java.net.URI, java.util.Map)
     */
    public static final String ENV_NORMALIZE_FILENAME = "normalize_filename";

    /**
     * boolean: true: disable file cache
     * @see java.nio.file.FileSystems#newFileSystem(java.net.URI, java.util.Map)
     */
    public static final String ENV_DISABLED_FILE_CACHE = DoubleCachedFileSystemDriver.ENV_DISABLED_FILE_CACHE;

    public GoogleDriveFileSystemProvider() {
        super(new GoogleDriveFileSystemRepository());
    }
}
