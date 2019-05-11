/*
 * http://blog.jcuff.net/2011/02/cli-java-based-google-authenticator.html
 */

package vavi.net.totp;

import java.util.Timer;
import java.util.TimerTask;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


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
        System.out.println("\nAuthenticator Started!");
        System.out.println(":----------------------------:--------:");
        System.out.println(":       Code Wait Time       :  Code  :");
        System.out.println(":----------------------------:--------:");

        final String domain = args[0];
        final String email = args[1];

        Main app = new Main();
        PropsEntity.Util.bind(app, domain, email);

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
}
