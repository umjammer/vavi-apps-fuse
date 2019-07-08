/*
 * http://blog.jcuff.net/2011/02/cli-java-based-google-authenticator.html
 */

package vavi.net.auth.totp;

import java.util.Timer;
import java.util.TimerTask;

import javax.xml.bind.DatatypeConverter;

import org.junit.jupiter.api.Test;

import vavi.net.auth.totp.Base32String;
import vavi.net.auth.totp.PinGenerator;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * display counter with dots (30 second refresh)
 *
 * $ java -cp ... vavi.net.totp.Main domain email
 *
 * @author jcuff@srv
 */
@PropsEntity(url = "file://${user.home}/.vavifuse/totp.properties")
public class Main {

    @Property(name = "{0}.{1}")
    transient String secret;

    public static void main(String[] args) throws Exception {
        final String domain = args[0];
        final String email = args[1];

        Main app = new Main();
        PropsEntity.Util.bind(app, domain, email);

        System.out.println("Authenticator Started!");
        System.out.println(email + ":" + domain);
        System.out.println(":----------------------------:--------:");
        System.out.println(":       Code Wait Time       :  Code  :");
        System.out.println(":----------------------------:--------:");

        new Timer().scheduleAtFixedRate(new TimerTask() {
            int count = 1;
            String previouscode = "";
            /* */
            public void run() {
                String newout = PinGenerator.computePin(app.secret, null);
                if (previouscode.equals(newout)) {
                    System.out.print(".");
                } else {
                    if (count <= 30) {
                        for (int i = count + 1; i <= 30; i++) {
                            System.out.print("+");
                        }
                    }
                    System.out.println(": " + newout + " :");
                    count = 0;
                }
                previouscode = newout;
                count++;
            }
        }, 0, 1 * 1000);
    }

    @Test
    void test01() throws Exception {
        byte[] keyBytes = Base32String.decode("N2STNZ6WB6V6DKMXWP6WYIAGNTRSDH2M");
        String secret = DatatypeConverter.printHexBinary(keyBytes).toLowerCase();
System.err.println(secret);
        assertEquals("6ea536e7d60fabe1a997b3fd6c20066ce3219f4c", secret);
    }
}
