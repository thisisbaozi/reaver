package io.messaginglabs.reaver.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class FileSystemUtils {

    private FileSystemUtils() {

    }

    public static File mkdirsIfNecessary(String dir) throws IOException {
        Parameters.requireNotEmpty(dir, "path");

        File f = new File(dir);
        if (f.isFile()) {
            throw new IOException(String.format("%s is file, expect it's dir", dir));
        }

        if (!f.exists()) {
            if (!f.mkdirs()) {
                throw new IOException(String.format("can't mkdirs for path(%s)", dir));
            }
        }

        return f;
    }

    public static void hasRWPermission(File file) throws IOException {
        Objects.requireNonNull(file, "file");

        if (!file.canRead()) {
            throw new IOException(String.format("not allowed to read files from file(%s)", file.getAbsolutePath()));
        }
        if (!file.canWrite()) {
            throw new IOException(String.format("not allowed to write data to file(%s)", file.getAbsolutePath()));
        }
    }

}
