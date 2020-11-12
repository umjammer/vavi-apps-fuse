/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.rainerhahnekamp.sneakythrow.Sneaky;


/**
 * Del.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/01 umjammer initial version <br>
 */
public class Del {

    public static void main(String[] args) throws Exception {

        Path trash = Paths.get("./Trash");
//        Files.list(trash).forEach(p -> Sneaky.sneaked(Files::delete));
        Files.list(trash).forEach(System.out::println);
    }
}