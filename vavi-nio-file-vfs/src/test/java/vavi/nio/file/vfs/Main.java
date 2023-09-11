/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

import java.net.URI;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.TimeZone;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

import static vavi.nio.file.Base.testAll;

import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
import vavi.util.Debug;


/**
 * Commons VFS JavaFS.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/03 umjammer initial version <br>
 */
public class Main {

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod",
                "(" +
                "org\\.slf4j\\.impl\\.JDK14LoggerAdapter#(log|info)" +
                "|" +
                "sun\\.util\\.logging\\.LoggingSupport#log" +
                "|" +
                "sun\\.util\\.logging\\.PlatformLogger#fine" +
                "|" +
                "jdk\\.internal\\.event\\.EventHelper#logX509CertificateEvent" +
                "|" +
                "sun\\.util\\.logging\\.PlatformLogger.JavaLoggerProxy#doLog" +
                ")");
    }

    /**
     * environment variable
     * <ul>
     * <li> TEST_SFTP_ACCOUNT
     * <li> TEST_SFTP_PASSPHRASE
     * <li> TEST_SFTP_HOST
     * <li> TEST_SFTP_KEYPATH
     * <li> TEST_SFTP_PATH
     * </ul>
     */
    @Test
    @EnabledIfEnvironmentVariables({
            @EnabledIfEnvironmentVariable(named = "TEST_SFTP_ACCOUNT", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SFTP_PASSPHRASE", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SFTP_HOST", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SFTP_KEYPATH", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SFTP_PATH", matches = ".+"),
    })
    void test01() throws Exception {
        String username = URLEncoder.encode(System.getenv("TEST_SFTP_ACCOUNT"), "utf-8");
        String passPhrase = URLEncoder.encode(System.getenv("TEST_SFTP_PASSPHRASE"), "utf-8");
        String host = System.getenv("TEST_SFTP_HOST");
        String keyPath = URLEncoder.encode(System.getenv("TEST_SFTP_KEYPATH"), "utf-8");
        String path = System.getenv("TEST_SFTP_PATH");

        URI uri = URI.create(String.format("vfs:sftp://%s@%s%s?keyPath=%s&passphrase=%s", username, host, path, keyPath, passPhrase));

        testAll(new VfsFileSystemProvider().newFileSystem(uri, Collections.emptyMap()));
    }

    /**
     * TODO doesn't work
     * bug https://github.com/apache/commons-vfs/pull/81
     *
     * environment variable
     * <ul>
     * <li> TEST_WEBDAV_ACCOUNT
     * <li> TEST_WEBDAV_PASSWORD
     * <li> TEST_WEBDAV_HOST
     * <li> TEST_WEBDAV_PORT
     * <li> TEST_WEBDAV_PATH
     * </ul>
     */
    @Test
    @Disabled
    @EnabledIfEnvironmentVariables({
            @EnabledIfEnvironmentVariable(named = "TEST_WEBDAV_ACCOUNT", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_WEBDAV_PASSWORD", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_WEBDAV_HOST", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_WEBDAV_PORT", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_WEBDAV_PATH", matches = ".+"),
    })
    void test02() throws Exception {
        String username = URLEncoder.encode(System.getenv("TEST_WEBDAV_ACCOUNT"), "utf-8");
        String password = System.getenv("TEST_WEBDAV_PASSWORD");
        String host = System.getenv("TEST_WEBDAV_HOST");
        String port = System.getenv("TEST_WEBDAV_PORT");
        String path = System.getenv("TEST_WEBDAV_PATH");

        URI uri = URI.create(String.format("vfs:webdav4s://%s:%s@%s:%s%s", username, password, host, port, path));

        testAll(new VfsFileSystemProvider().newFileSystem(uri, Collections.emptyMap()));
    }

    /**
     * <p>
     * TODO doesn't work
     *  * sandbox/vfs2-cifs ... where are you?
     *  * github:mikhasd/commons-vfs2-smb ... now
     * </p>
     * <p>
     * why smb spec. is so complicated! even sftp is secure than smb, it's works fine.
     * f*ck smb, get out away.
     * </p>
     * environment variable
     * <ul>
     * <li> TEST_SMB_ACCOUNT
     * <li> TEST_SMB_PASSWORD
     * <li> TEST_SMB_HOST
     * <li> TEST_SMB_DOMAIN
     * <li> TEST_SMB_PATH
     * </ul>
     */
    @Test
    @EnabledIfEnvironmentVariables({
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_ACCOUNT", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_PASSWORD", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_HOST", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_DOMAIN", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_PATH", matches = ".+"),
    })
    void test03() throws Exception {
        String username = System.getenv("TEST_SMB_ACCOUNT");
        String password = System.getenv("TEST_SMB_PASSWORD");
        String host = System.getenv("TEST_SMB_HOST");
        String domain = System.getenv("TEST_SMB_DOMAIN");
        String path = System.getenv("TEST_SMB_PATH");

        URI uri = URI.create(String.format("vfs:smb://%s:%s@%s%s?domain=%s", username, password, host, path, domain));

        testAll(new VfsFileSystemProvider().newFileSystem(uri, Collections.emptyMap()));
    }

    /**
     * <p>
     * github:vbauer/commons-vfs2-cifs ... use jcifs-ng, works fine!
     * </p>
     * <p>
     * environment variable
     * <ul>
     * <li> TEST_SMB_ACCOUNT
     * <li> TEST_SMB_PASSWORD
     * <li> TEST_SMB_HOST
     * <li> TEST_SMB_DOMAIN
     * <li> TEST_SMB_PATH
     * </ul>
     * </p>
     */
    @Test
    @EnabledIfEnvironmentVariables({
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_ACCOUNT", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_PASSWORD", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_HOST", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_DOMAIN", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_PATH", matches = ".+"),
    })
    void test04() throws Exception {
        String username = System.getenv("TEST_SMB_ACCOUNT");
        String password = System.getenv("TEST_SMB_PASSWORD");
        String host = System.getenv("TEST_SMB_HOST");
        String domain = System.getenv("TEST_SMB_DOMAIN");
        String path = System.getenv("TEST_SMB_PATH");

        URI uri = URI.create(String.format("vfs:cifs://%s:%s@%s%s?domain=%s", username, password, host, path, domain));

        testAll(new VfsFileSystemProvider().newFileSystem(uri, Collections.emptyMap()));
    }

    /**
     * works, thaks jcifs-ng
     * <p>
     * TODO 2022-04-22 works but got exception
     * @see "https://gist.github.com/umjammer/58a5fc48f4620837bae008bae9440f16"
     */
    @Test
    @EnabledIfEnvironmentVariables({
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_ACCOUNT", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_PASSWORD", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_HOST", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_DOMAIN", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_PATH", matches = ".+"),
    })
    void test_cifs() throws Exception {
        String username = System.getenv("TEST_SMB_ACCOUNT");
        String password = System.getenv("TEST_SMB_PASSWORD");
        String host = System.getenv("TEST_SMB_HOST");
        String domain = System.getenv("TEST_SMB_DOMAIN");
        String path = System.getenv("TEST_SMB_PATH");

        String url = "cifs://" + host + path;
Debug.println(url);
        NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator(domain, username, password);
        CIFSContext context = SingletonContext.getInstance().withCredentials(auth);
        SmbFile smbFile = new SmbFile(url, context);
Debug.println(smbFile.getPath() + ", " +
            LocalDateTime.ofInstant(Instant.ofEpochMilli(smbFile.getLastModified()), TimeZone.getDefault().toZoneId()));
        smbFile.close();
        context.close();
    }

    /**
     * smbj
     * - works with smbj c9ab3d8
     */
    @Test
    @EnabledIfEnvironmentVariables({
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_ACCOUNT", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_PASSWORD", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_HOST", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_DOMAIN", matches = ".+"),
            @EnabledIfEnvironmentVariable(named = "TEST_SMB_PATH", matches = ".+"),
    })
    void test_smbj() throws Exception {
        String username = System.getenv("TEST_SMB_ACCOUNT");
        String password = System.getenv("TEST_SMB_PASSWORD");
        String host = System.getenv("TEST_SMB_HOST");
        String domain = System.getenv("TEST_SMB_DOMAIN");
        String path = System.getenv("TEST_SMB_PATH");

        try (SMBClient client = new SMBClient();
             Connection connection = client.connect(host)) {

            AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), domain);
            Session session = connection.authenticate(ac);

            // Connect to Share
            String[] pe = path.split("/");
            try (DiskShare share = (DiskShare) session.connectShare(pe[1])) {
                for (FileIdBothDirectoryInformation f : share.list(pe[2])) {
                    System.out.println("File : " + f.getFileName());
                }
            }
        }
    }
}