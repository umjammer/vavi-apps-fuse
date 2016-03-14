
package vavi.nio.file.onedrive.test;

import com.github.fge.filesystem.attributes.FileAttributesFactory;

import de.tuberlin.onedrivesdk.common.OneItem;


public final class OneDriveFileAttributesFactory extends FileAttributesFactory {

    public OneDriveFileAttributesFactory() {
        setMetadataClass(OneItem.class);
        addImplementation("basic", OneDriveBasicFileAttributesProvider.class);
    }
}
