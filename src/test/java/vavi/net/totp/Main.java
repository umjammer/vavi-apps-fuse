/*
 * http://blog.jcuff.net/2011/02/cli-java-based-google-authenticator.html
 */

package vavi.net.totp;

import java.util.Timer;
import java.util.TimerTask;

import vavi.net.totp.PinGenerator;


/**
 * display counter with dots (30 second refresh)
 * 
 * $ java -classpath ./ vavi.net.totp.Main base32_encoded_secret
 * 
 * @author jcuff@srv
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("\nAuthenticator Started!");
        System.out.println(":----------------------------:--------:");
        System.out.println(":       Code Wait Time       :  Code  :");
        System.out.println(":----------------------------:--------:");

        final String secret = args[0];

        new Timer().scheduleAtFixedRate(new TimerTask() {
            int count = 1;
            String previouscode = "";
            /* */
            public void run() {
                String newout = PinGenerator.computePin(secret, null);
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
