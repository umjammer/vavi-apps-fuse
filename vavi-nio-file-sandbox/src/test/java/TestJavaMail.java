/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

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
     * @param args
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

                File dir = new File("tmp" + File.separator + email + File.separator + targetFolder);
                dir.mkdirs();

                folder.open(Folder.READ_ONLY);
                UIDFolder uf = (UIDFolder) folder;
                for (Message message : folder.getMessages()) {
                    File file = new File(dir, uf.getUID(message) + ".eml");
                    if (!file.exists()) {
                        System.err.printf("%s - %d\n", message.getSubject(), uf.getUID(message));
                        message.writeTo(new FileOutputStream(file));
                    }
                }
                folder.close(false);
            }
        }

        store.close();
    }
}

/* */
