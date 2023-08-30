/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.nio.file.Util;


/**
 * OneDriveEasyFSTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-11-23 nsano initial version <br>
 */
public class OneDriveEasyFSTest {

    int c = 0;

    // nio file filesystem convert nfd automatically, so we cant rename via java filesystem
    @Test
    @DisplayName("nfd -> nfc")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
        OneDriveEasyFS efs = new OneDriveEasyFS("snaohide@hotmail.com");
        efs.walk(efs.getRootEntry(), p -> {
            try {
                String a = p.name;
                String b = Util.toNormalizedString(a);
                c++;
                if (!a.equals(b)) {
                    System.err.println("\n" + a + ", " + b);
//                    efs.renameEntry(p, b);
                } else {
                    System.err.print(".");
                    if (c % 100 == 0) {
                        System.err.println();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
