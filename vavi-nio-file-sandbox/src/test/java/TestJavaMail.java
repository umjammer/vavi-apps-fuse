/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * TestJavaMail.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/08/10 umjammer initial version <br>
 */
@PropsEntity(url = "file://${user.home}/.vavifuse/credentials.properties")
public class TestJavaMail {

    @Property(name = "imap.host.{0}")
    String host;
    @Property(name = "imap.port.{0}")
    int port;
    @Property(name = "imap.password.{0}")
    String password;
    String email;
    String targetFolder;

    /**
     * @param args 0: email, 1: folder
     */
    public static void main(String[] args) throws Exception {
        TestJavaMail app = new TestJavaMail();
        app.email = args[0];
        app.targetFolder = args[1];
        PropsEntity.Util.bind(app, app.email);
        app.proceed();
    }

    void proceed() throws MessagingException, IOException, IOException {
        Properties props = System.getProperties();
        Session session = Session.getInstance(props, null);
//      session.setDebug(true);

        Store store = session.getStore("imaps");
        store.connect(host, port, email, password);

        Folder[] folders = store.getDefaultFolder().list();
        for (Folder f : folders) {
            System.err.println("+- " + f.getName());
        }

        Folder rootFolder = store.getFolder("[Gmail]");
        if (!rootFolder.exists()) {
            throw new IllegalArgumentException(targetFolder + " does not exist.");
        }

        for (Folder folder : rootFolder.list()) {
            System.err.println("sub folder: " + folder.getName());
            if (folder.getName().equals(targetFolder)) {

                Path dir = Paths.get("tmp", email, targetFolder);
                Files.createDirectories(dir);

                folder.open(Folder.READ_ONLY);
                UIDFolder uf = (UIDFolder) folder;
                for (Message message : folder.getMessages()) {
                    Path file = dir.resolve(uf.getUID(message) + ".eml");
                    if (!Files.exists(file)) {
                        System.err.printf("%s - %d\n", message.getSubject(), uf.getUID(message));
                        message.writeTo(Files.newOutputStream(file));
                    }
                }
                folder.close(false);
            }
        }

        store.close();
    }
}

/* */
