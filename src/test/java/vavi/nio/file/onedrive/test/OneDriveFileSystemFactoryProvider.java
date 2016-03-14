
package vavi.nio.file.onedrive.test;

import com.github.fge.filesystem.provider.FileSystemFactoryProvider;


public final class OneDriveFileSystemFactoryProvider extends FileSystemFactoryProvider {

    public OneDriveFileSystemFactoryProvider() {
        setAttributesFactory(new OneDriveFileAttributesFactory());
        setOptionsFactory(new OneDriveFileSystemOptionsFactory());
    }
}
