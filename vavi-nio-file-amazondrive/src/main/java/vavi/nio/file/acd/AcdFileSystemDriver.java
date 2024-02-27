/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.DoubleCachedFileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import org.yetiz.lib.acd.ACD;
import org.yetiz.lib.acd.ACDSession;
import org.yetiz.lib.acd.Entity.FolderInfo;
import org.yetiz.lib.acd.Entity.NodeInfo;
import org.yetiz.lib.acd.api.v1.Nodes;

import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;


/**
 * AcdFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class AcdFileSystemDriver extends DoubleCachedFileSystemDriver<NodeInfo> {

    private final ACD drive;
    private ACDSession session;

    public AcdFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final ACD drive,
            final ACDSession session,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.drive = drive;
        this.session = session;
        setEnv(env);
    }

    @Override
    protected String getFilenameString(NodeInfo entry) {
        return entry.getName();
    }

    @Override
    protected boolean isFolder(NodeInfo entry) {
        return entry.isFolder();
    }

    @Override
    protected NodeInfo getRootEntry(Path root) throws IOException {
        return Nodes.getRootFolder(session);
    }

    @Override
    protected NodeInfo getEntry(NodeInfo parentEntry, Path path)throws IOException {
        return Nodes.getFileMetadata(session, toPathString(path)); // TODO
    }

    @Override
    protected InputStream downloadEntryImpl(NodeInfo entry, Path path, Set<? extends OpenOption> options) throws IOException {
        return drive.getFile(entry.getId());
    }

    @Override
    protected OutputStream uploadEntry(NodeInfo parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        File temp = File.createTempFile("vavi-apps-fuse-", ".upload");

        return new AcdOutputStream(drive, temp, toFilenameString(path), (FolderInfo) parentEntry, file -> {
System.out.println("file: " + file.getName() + ", " + file.getCreationDate() + ", " + file.getContentProperties().getSize());
            updateEntry(path, file);
        });
    }

    @Override
    protected List<NodeInfo> getDirectoryEntries(NodeInfo dirEntry, Path dir) throws IOException {
        return drive.getList(dirEntry.getId());
    }

    @Override
    protected NodeInfo createDirectoryEntry(NodeInfo parentEntry, Path dir) throws IOException {
        // TODO: how to diagnose?
        return drive.createFolder(parentEntry.getId(), toFilenameString(dir));
    }

    @Override
    protected boolean hasChildren(NodeInfo dirEntry, Path dir) throws IOException {
        return !drive.getList(dirEntry.getId()).isEmpty();
    }

    @Override
    protected void removeEntry(NodeInfo entry, Path path) throws IOException {
        if (entry.isFolder()) {
            drive.removeFolder(entry.getId());
        } else {
            drive.removeFile(entry.getId());
        }
    }

    @Override
    protected NodeInfo copyEntry(NodeInfo sourceEntry, NodeInfo targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException {
        return null; // TODO
    }

    @Override
    protected NodeInfo moveEntry(NodeInfo sourceEntry, NodeInfo targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        return drive.renameFile(sourceEntry.getId(), toPathString(target)); // TODO
    }

    @Override
    protected NodeInfo moveFolderEntry(NodeInfo sourceEntry, NodeInfo targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        return drive.renameFolder(sourceEntry.getId(), toPathString(target)); // TODO
    }

    @Override
    protected NodeInfo renameEntry(NodeInfo sourceEntry, NodeInfo targetParentEntry, Path source, Path target) throws IOException {
        return drive.renameFile(sourceEntry.getId(), toFilenameString(target)); // TODO
    }

    @Override
    public void close() throws IOException {
        drive.destroy();
    }
}
