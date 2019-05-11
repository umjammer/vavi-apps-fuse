/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.totp;

import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


/**
 * PinGenerator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/02 umjammer initial version <br>
 */
public class PinGenerator {

    private PinGenerator() {
    }

    public static String computePin(String secret, Long counter) {
        if (secret == null || secret.length() == 0) {
            throw new IllegalArgumentException("Null or empty secret");
        }
        try {
            final byte[] keyBytes = Base32String.decode(secret);
            Mac mac = Mac.getInstance("HMACSHA1");
            mac.init(new SecretKeySpec(keyBytes, ""));
            PasscodeGenerator pcg = new PasscodeGenerator(mac);
            return pcg.generateTimeoutCode();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } catch (Base32String.DecodingException e) {
            throw new IllegalStateException(e);
        }
    }

}

/* */
