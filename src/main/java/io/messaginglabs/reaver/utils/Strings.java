package io.messaginglabs.reaver.utils;

import java.nio.charset.Charset;

public final class Strings {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static byte[] UTF8Bytes(String src) {
        return src.getBytes(UTF8);
    }

}
