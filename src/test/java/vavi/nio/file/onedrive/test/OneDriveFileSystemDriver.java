
package vavi.nio.file.onedrive.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.json.simple.parser.ParseException;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.OneItem;
import de.tuberlin.onedrivesdk.downloadFile.OneDownloadFile;
import de.tuberlin.onedrivesdk.file.OneFile;
import de.tuberlin.onedrivesdk.folder.OneFolder;
import de.tuberlin.onedrivesdk.uploadFile.OneUploadFile;


@ParametersAreNonnullByDefault
public final class OneDriveFileSystemDriver extends UnixLikeFileSystemDriverBase {

    private final OneDriveSDK client;

    public OneDriveFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final OneDriveSDK client) {
        super(fileStore, provider);
        this.client = client;
    }

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        // TODO: need a "shortcut" way for that; it's quite common
        final String target = path.toRealPath().toString();
        final OneItem entry;

        try {
            entry = getOneItem(target);
        } catch (OneDriveException e) {
            throw OneDriveIOException.wrap(e);
        }

        // TODO: metadata driver
        if (OneFolder.class.isInstance(entry))
            throw new IsDirectoryException(target);

        final OneDownloadFile downloader;

        try {
            downloader = OneFile.class.cast(target).download(new File("temp"));
            downloader.startDownload();
        } catch (OneDriveException e) {
            throw new OneDriveIOException(e);
        }

        return new OneDriveInputStream(downloader);
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        // TODO: need a "shortcut" way for that; it's quite common
        final String target = path.toRealPath().toString();
        final OneFile entry;

        try {
            entry = client.getFileByPath(target);
        } catch (OneDriveException e) {
            throw new OneDriveIOException(e);
        }

        // TODO: metadata
        if (OneFolder.class.isInstance(entry))
            throw new IsDirectoryException(target);

        final OneUploadFile uploader;
        try {
            uploader = entry.getParentFolder().uploadFile(new File(target));
            uploader.startUpload();
        } catch (OneDriveException e) {
            throw new OneDriveIOException(e);
        }

        return new OneDriveOutputStream(uploader);
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        // TODO: need a "shortcut" way for that; it's quite common
        final String target = dir.toRealPath().toString();
        final OneItem dirent;
        try {
            dirent = getOneItem(target);
        } catch (OneDriveException e) {
            throw new OneDriveIOException(e);
        }

        if (!OneFolder.class.isInstance(dirent))
            throw new NotDirectoryException(target);

        final List<OneItem> children;
        try {
            children = client.getFolderByPath(target).getChildren();
        } catch (OneDriveException e) {
            throw OneDriveIOException.wrap(e);
        }
        final List<Path> list = new ArrayList<>(children.size());

        for (final OneItem child : children)
            list.add(dir.resolve(child.getName()));

        //noinspection AnonymousInnerClassWithTooManyMethods
        return new DirectoryStream<Path>() {
            private final AtomicBoolean alreadyOpen = new AtomicBoolean(false);

            @Override
            public Iterator<Path> iterator() {
                // required by the contract
                if (alreadyOpen.getAndSet(true))
                    throw new IllegalStateException();
                return list.iterator();
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        // TODO: need a "shortcut" way for that; it's quite common
        final String parent = dir.toRealPath().getParent().toString();
        final String target = dir.toRealPath().getFileName().toString();

        try {
            // TODO: how to diagnose?
            if (client.getFolderByPath(parent).createFolder(target) == null)
                throw new OneDriveIOException("cannot create directory??");
        } catch (OneDriveException e) {
            throw OneDriveIOException.wrap(e);
        }
    }

    @Override
    public void delete(final Path path) throws IOException {
        // TODO: need a "shortcut" way for that; it's quite common
        final String target = path.toRealPath().toString();

        final OneItem entry;

        try {
            entry = getOneItem(target);
        } catch (OneDriveException e) {
            throw OneDriveIOException.wrap(e);
        }

        // TODO: metadata!
        if (OneFolder.class.isInstance(entry)) {
            final List<OneItem> list;

            try {
                list = client.getFolderByPath(target).getChildren();
            } catch (OneDriveException e) {
                throw OneDriveIOException.wrap(e);
            }

            if (list.size() > 0)
                throw new DirectoryNotEmptyException(target);
        }

        try {
            entry.delete();
        } catch (OneDriveException e) {
            throw OneDriveIOException.wrap(e);
        }
    }

    private OneItem getOneItem(String path) throws OneDriveException, IOException {
        OneItem item;
        try {
            item = OneItem.class.cast(client.getFileByPath(path));
        } catch (ClassCastException e) {
            item = OneItem.class.cast(client.getFolderByPath(path));
        }
        return item;
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        final String srcpath = source.toRealPath().toString();
        final String dstpath = source.toRealPath().toString();

        OneItem dstentry;
        
        try {
            dstentry = getOneItem(dstpath);
        } catch (OneDriveException e) {
            throw OneDriveIOException.wrap(e);
        }

        if (OneFolder.class.isInstance(dstentry)) {
            final List<OneItem> list;

            try {
                list = client.getFolderByPath(dstpath).getChildren();
            } catch (OneDriveException e) {
                throw OneDriveIOException.wrap(e);
            }

            if (list.size() > 0)
                throw new DirectoryNotEmptyException(dstpath);
        }
        // TODO: unknown what happens when a copy operation is performed
        try {
            dstentry.delete();
            dstentry = OneItem.class.cast(dstentry.getParentFolder());
        } catch (OneDriveException e) {
            throw OneDriveIOException.wrap(e);
        }

        final OneItem srcentry;
        try {
            srcentry = getOneItem(srcpath);
        } catch (OneDriveException e) {
            throw OneDriveIOException.wrap(e);
        }

        try {
            // TODO: how to diagnose?
            if (OneFile.class.isInstance(srcentry)) {
                if (OneFile.class.cast(srcentry).copy(OneFolder.class.cast(dstentry)) == null)
                    throw new OneDriveIOException("cannot copy??");
            } else if (OneFolder.class.isInstance(srcentry)) {
                throw new UnsupportedOperationException("source can not be a folder");
            }
        } catch (OneDriveException | ParseException | InterruptedException e) {
            throw new OneDriveIOException(e);
        }
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        final String srcpath = source.toRealPath().toString();
        final String dstpath = source.toRealPath().toString();

        OneItem dstentry;

        try {
            dstentry = getOneItem(dstpath);
        } catch (OneDriveException e) {
            throw OneDriveIOException.wrap(e);
        }

        if (OneFolder.class.isInstance(dstentry)) {
            final List<OneItem> list;

            try {
                list = client.getFolderByPath(dstpath).getChildren();
            } catch (OneDriveException e) {
                throw OneDriveIOException.wrap(e);
            }

            if (list.size() > 0)
                throw new DirectoryNotEmptyException(dstpath);
        }
        // TODO: unknown what happens when a move operation is performed
        // and the target already exists
        try {
            dstentry.delete();
            dstentry = OneItem.class.cast(dstentry.getParentFolder());
        } catch (OneDriveException e) {
            throw OneDriveIOException.wrap(e);
        }

        final OneItem srcentry;
        try {
            srcentry = getOneItem(srcpath);
        } catch (OneDriveException e) {
            throw OneDriveIOException.wrap(e);
        }

        try {
            // TODO: how to diagnose?
            if (OneFile.class.isInstance(srcentry)) {
                if (OneFile.class.cast(srcentry).move(OneFolder.class.cast(dstentry)) == null)
                    throw new OneDriveIOException("cannot copy??");
            } else if (OneFolder.class.isInstance(srcentry)) {
                throw new UnsupportedOperationException("source can not be a folder");
            }
        } catch (OneDriveException | ParseException | InterruptedException e) {
            throw new OneDriveIOException(e);
        }
    }

    /**
     * Check access modes for a path on this filesystem
     * <p>
     * If no modes are provided to check for, this simply checks for the
     * existence of the path.
     * </p>
     *
     * @param path the path to check
     * @param modes the modes to check for, if any
     * @throws IOException filesystem level error, or a plain I/O error
     * @see FileSystemProvider#checkAccess(Path, AccessMode...)
     */
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        final String target = path.toRealPath().toString();

        final OneItem entry;

        try {
            entry = getOneItem(target);
        } catch (OneDriveException e) {
            throw new NoSuchFileException(target);
        }

        if (!OneFile.class.isInstance(entry))
            return;

        // TODO: assumed; not a file == directory
        for (final AccessMode mode : modes)
            if (mode == AccessMode.EXECUTE)
                throw new AccessDeniedException(target);
    }

    @Override
    public void close() throws IOException {
        // TODO: what to do here? OneDriveClient does not implement Closeable :(
    }

    @Nonnull
    @Override
    public Object getPathMetadata(final Path path) throws IOException {
        try {
            return client.getFileById(path.toRealPath().toString());
        } catch (OneDriveException e) {
            throw OneDriveIOException.wrap(e);
        }
    }
}
