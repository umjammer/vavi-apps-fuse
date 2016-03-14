
package vavi.nio.file.onedrive.test;

import com.github.fge.filesystem.provider.FileSystemProviderBase;
import com.github.fge.filesystem.provider.FileSystemRepository;


public final class OneDriveFileSystemProvider extends FileSystemProviderBase {

    public OneDriveFileSystemProvider(final FileSystemRepository repository) {
        super(repository);
    }
}
