/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.watch.webhook.websocket;

import java.io.InputStream;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;


/**
 * JsonCodec.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/02 umjammer initial version <br>
 */
public abstract class JsonCodec {

    protected static Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return false;
        }
        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return InputStream.class.equals(clazz);
        }
    }).create();

    protected static abstract class JsonEncoder<T> implements Encoder.Text<T> {
        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public String encode(T notification) throws EncodeException {
            return gson.toJson(notification);
        }

        @Override
        public void destroy() {
        }
    }

    protected static abstract class JsonDecoder<T> implements Decoder.Text<T> {

        protected abstract Class<T> getType();

        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public boolean willDecode(String s) {
            try {
                gson.fromJson(s, getType());
                return true;
            } catch (JsonSyntaxException e) {
                return false;
            }
        }

        @Override
        public T decode(String s) throws DecodeException {
            try {
                return gson.fromJson(s, getType());
            } catch (JsonSyntaxException e) {
                throw new DecodeException(s, e.getMessage(), e);
            }
        }

        @Override
        public void destroy() {
        }
    }
}

/* */
