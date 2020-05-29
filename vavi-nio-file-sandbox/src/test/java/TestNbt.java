/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import net.querz.nbt.NBTUtil;
import net.querz.nbt.Tag;


/**
 * TestNbt.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/08 umjammer initial version <br>
 */
public class TestNbt {

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        Tag<?> tag = NBTUtil.readTag(args[0]);
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(new JsonParser().parse(tag.toString())));
    }
}

/* */
