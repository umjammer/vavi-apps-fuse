/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.hfs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.catacombae.dmg.encrypted.ReadableCEncryptedEncodingStream;
import org.catacombae.dmg.sparseimage.ReadableSparseImageStream;
import org.catacombae.dmg.sparseimage.SparseImageRecognizer;
import org.catacombae.dmg.udif.UDIFDetector;
import org.catacombae.dmg.udif.UDIFRandomAccessStream;
import org.catacombae.io.ReadableConcatenatedStream;
import org.catacombae.io.ReadableFileStream;
import org.catacombae.io.ReadableRandomAccessStream;
import org.catacombae.io.ReadableRandomAccessSubstream;
import org.catacombae.io.SynchronizedReadableRandomAccessStream;
import org.catacombae.storage.fs.FileSystemHandlerFactory;
import org.catacombae.storage.fs.FileSystemHandlerFactory.StandardAttribute;
import org.catacombae.storage.fs.FileSystemMajorType;
import org.catacombae.storage.fs.hfscommon.HFSCommonFileSystemHandler;
import org.catacombae.storage.fs.hfscommon.HFSCommonFileSystemRecognizer;
import org.catacombae.storage.fs.hfscommon.HFSCommonFileSystemRecognizer.FileSystemType;
import org.catacombae.storage.io.ReadableStreamDataLocator;
import org.catacombae.storage.ps.Partition;
import org.catacombae.storage.ps.PartitionSystemDetector;
import org.catacombae.storage.ps.PartitionSystemHandler;
import org.catacombae.storage.ps.PartitionSystemHandlerFactory;
import org.catacombae.storage.ps.PartitionSystemType;
import org.catacombae.storage.ps.PartitionType;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.util.Debug;


/**
 * HfsFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/06 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class HfsFileSystemRepository extends FileSystemRepositoryBase {

    public HfsFileSystemRepository() {
        super("hfs", new HfsFileSystemFactoryProvider());
    }

    /**
     * @param uri "hfs:/tmp/dmg/exam.dmg!/img/sample.png"
     */
    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        try {
            String[] rawSchemeSpecificParts = uri.getRawSchemeSpecificPart().split("!");
            URI file = new URI(rawSchemeSpecificParts[0]);
            if (!"file".equals(file.getScheme())) {
                // currently only support "file"
                throw new IllegalArgumentException(file.toString());
            }
            // TODO virtual relative directory from rawSchemeSpecificParts[1]

Debug.println("file: " + Paths.get(file).toAbsolutePath());

            HFSCommonFileSystemHandler fsHandler = loadFSWithUDIFAutodetect(Paths.get(file).toAbsolutePath().toString());

            HfsFileStore fileStore = new HfsFileStore(fsHandler, factoryProvider.getAttributesFactory());
            return new HfsFileSystemDriver(fileStore, factoryProvider, fsHandler, env);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /* ad-hoc hack for ignoring checking opacity */
    protected void checkURI(@Nullable final URI uri) {
        Objects.requireNonNull(uri);
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("uri is not absolute");
        }
        if (!getScheme().equals(uri.getScheme())) {
            throw new IllegalArgumentException("bad scheme");
        }
    }

    /** */
    private HFSCommonFileSystemHandler loadFSWithUDIFAutodetect(String filename) {

        ReadableRandomAccessStream fsFile = new ReadableFileStream(filename);

        try {
Debug.println(Level.FINER, "Trying to detect CEncryptedEncoding structure...");
            if (ReadableCEncryptedEncodingStream.isCEncryptedEncoding(fsFile)) {

String password = ""; // TODO env
Debug.println(Level.FINE, "CEncryptedEncoding structure found! Creating filter stream...");
                char[] res = password.toCharArray();
                try {
                    ReadableCEncryptedEncodingStream stream = new ReadableCEncryptedEncodingStream(fsFile, res);

                    try {
                        stream.read(new byte[512]);

                        fsFile = stream;
                    } catch (Exception e) {
                        Throwable cause = e.getCause();

                        if (!(cause instanceof InvalidKeyException)) {
                            throw e;
                        }

Debug.println(
 "Unsupported AES key size: " +
 "If you were trying to load an AES-256 encrypted image and\n" +
 "are using Sun/Oracle's Java Runtime Environment, then \n" +
 "please check if you have installed the Java Cryptography\n" +
 "Extension (JCE) Unlimited Strength Jurisdiction Policy\n" +
 "Files, which are required for AES-256 support in Java.");
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Reading encrypted disk image...: " + "Incorrect password.");
                }
            } else {
Debug.println(Level.FINER, "CEncryptedEncoding structure not found. Proceeding...");
            }
        } catch (Exception e) {
Debug.println(Level.FINER, "Non-critical exception while trying to detect CEncryptedEncoding structure:");
e.printStackTrace();
        }

        try {
Debug.println(Level.FINE, "Detecting sparseimage structure...");
            if (SparseImageRecognizer.isSparseImage(fsFile)) {
Debug.println(Level.FINE, "sparseimage structure found! Creating filter stream...");

                try {
                    ReadableSparseImageStream stream = new ReadableSparseImageStream(fsFile);
                    fsFile = stream;
                } catch (Exception e) {
Debug.println("Exception while creating readable sparseimage stream:");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
Debug.println("Non-critical exception while trying to detect sparseimage structure:");
e.printStackTrace();
        }

        try {
Debug.println(Level.FINER, "Trying to detect UDIF structure...");
            if (UDIFDetector.isUDIFEncoded(fsFile)) {
Debug.println(Level.FINE, "UDIF structure found! Creating filter stream...");
                UDIFRandomAccessStream stream = null;
                try {
                    stream = new UDIFRandomAccessStream(fsFile);
                } catch (Exception e) {
e.printStackTrace();
                    if (e.getMessage().startsWith("java.lang.RuntimeException: No handler for block type")) {
                        throw new IllegalArgumentException(
                                "UDIF file contains unsupported block types!\n" +
                                "(The file was probably created with BZIP2 or ADC " +
                                "compression, which is unsupported currently)");
                    } else {
                        throw new IllegalArgumentException("UDIF file unsupported or damaged!");
                    }
                }
                if (stream != null) {
                    fsFile = stream;
                }
            } else {
Debug.println(Level.FINER, "UDIF structure not found. Proceeding...");
            }
        } catch(Exception e) {
Debug.println("Non-critical exception while trying to detect UDIF structure:");
e.printStackTrace();
        }

        SynchronizedReadableRandomAccessStream syncStream = new SynchronizedReadableRandomAccessStream(fsFile);

        try {
            return loadFS(syncStream);
        } finally {
            syncStream.close();
        }
    }

    /** */
    private HFSCommonFileSystemHandler loadFS(SynchronizedReadableRandomAccessStream syncStream) {
        long fsOffset;
        long fsLength;

        // Detect partition system
        PartitionSystemType[] matchingTypes = PartitionSystemDetector.detectPartitionSystem(syncStream, false);

        if (matchingTypes.length > 1) {
            String message = "Multiple partition system types detected:";
            for (PartitionSystemType type : matchingTypes) {
                message += "\n" + type;
            }
            throw new IllegalArgumentException(message);

        } else if (matchingTypes.length == 1) {
            PartitionSystemType psType = matchingTypes[0];
            PartitionSystemHandlerFactory psFact = psType.createDefaultHandlerFactory();

            if (psFact == null) {
                throw new IllegalArgumentException("Unsupported partition system: " +
                        "Can't find handler for " +
                        "partition system type " + psType);
            }

            PartitionSystemHandler psHandler =
                    psFact.createHandler(new ReadableStreamDataLocator(new ReadableRandomAccessSubstream(syncStream)));

            Partition[] partitions;
            try {
                partitions = psHandler.getPartitions();
            } finally {
                psHandler.close();
            }

            if (partitions.length == 0) {
                // Proceed to detect file system
                fsOffset = 0;
                try {
                    fsLength = syncStream.length();
                } catch(Exception e) {
                    e.printStackTrace();
                    fsLength = -1;
                }
            } else {
                // Search for suitable partitions, and make the first one that
                // was found the default value for the dialog box.
                int defaultSelection = 0;
                for (int i = 0; i < partitions.length; ++i) {
                    Partition p = partitions[i];
                    PartitionType pt = p.getType();
                    if (pt == PartitionType.APPLE_HFS_CONTAINER ||
                        pt == PartitionType.APPLE_HFSX) {
                        defaultSelection = i;
                        break;
                    }
                }
Debug.println("patitions: " + partitions.length + ", default: " + defaultSelection); // TODO env

                // Prompt user to choose a partition to load.
                Partition selectedPartition = partitions[defaultSelection];
                PartitionType pt = selectedPartition.getType();

                if (pt != PartitionType.APPLE_HFS_CONTAINER &&
                    pt != PartitionType.APPLE_HFSX) {
                    throw new IllegalArgumentException("Unknown partition type: " +
                            "Can't find handler for partition type \"" +
                            selectedPartition.getType() + "\"");
                }

                // A selection was made.
                fsOffset = selectedPartition.getStartOffset();
                fsLength = selectedPartition.getLength();
            }
        } else {
            fsOffset = 0;
            fsLength = syncStream.length();
        }

        // Detect HFS file system
        FileSystemType fsType = HFSCommonFileSystemRecognizer.detectFileSystem(syncStream, fsOffset);

        switch (fsType) {
        case HFS:
        case HFS_WRAPPED_HFS_PLUS:
        case HFS_PLUS:
        case HFSX:

            final FileSystemMajorType fsMajorType;
            switch (fsType) {
            case HFS:
                fsMajorType = FileSystemMajorType.APPLE_HFS;
                break;
            case HFS_PLUS:
            case HFS_WRAPPED_HFS_PLUS:
                fsMajorType = FileSystemMajorType.APPLE_HFS_PLUS;
                break;
            case HFSX:
                fsMajorType = FileSystemMajorType.APPLE_HFSX;
                break;
            default:
                throw new IllegalArgumentException("Unhandled type: " + fsType);
            }

boolean cachingEnabled = true; // TODO env
            FileSystemHandlerFactory factory = fsMajorType.createDefaultHandlerFactory();
            if (factory.isSupported(StandardAttribute.CACHING_ENABLED)) {
                factory.getCreateAttributes().setBooleanAttribute(
                        StandardAttribute.CACHING_ENABLED,
                        cachingEnabled);
            }

            ReadableRandomAccessStream stage1;
            if (fsLength > 0) {
                stage1 = new ReadableConcatenatedStream(
                        new ReadableRandomAccessSubstream(syncStream), fsOffset,
                        fsLength);
            } else if (fsOffset == 0) {
                stage1 = new ReadableRandomAccessSubstream(syncStream);
            } else {
                throw new IllegalStateException("length undefined and offset " +
                        "!= 0 (fsLength=" + fsLength + " fsOffset=" +
                        fsOffset + ")");
            }

            ReadableStreamDataLocator fsDataLocator = new ReadableStreamDataLocator(stage1);

            return (HFSCommonFileSystemHandler) factory.createHandler(fsDataLocator);

        default:
            throw new IllegalArgumentException("Unsupported file system type: Invalid HFS type.\n" +
                    "hfs supports:\n" +
                    "    " + FileSystemType.HFS_PLUS + "\n" +
                    "    " + FileSystemType.HFSX + "\n" +
                    "    " + FileSystemType.HFS_WRAPPED_HFS_PLUS + "\n" +
                    "    " + FileSystemType.HFS + "\n" +
                    "\nDetected type is (" + fsType + ").");
        }
    }
}
