package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.utils.FileSystemUtils;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

public class SimpleConfigStorage implements ConfigStorage {

    private static final String TMP_FILE_SUFFIX = ".tmp";

    /*
     * config files are created under this directory
     */
    private final String dir;

    public SimpleConfigStorage(String dir) {
        Objects.requireNonNull(dir, "dir");

        try {
            File file = FileSystemUtils.mkdirsIfNecessary(dir);
            FileSystemUtils.hasRWPermission(file);
        } catch (IOException cause) {
            throw new UncheckedIOException("void dir: "  + dir, cause);
        }

        if (dir.endsWith(File.separator)) {
            this.dir = dir;
        } else {
            this.dir = dir + File.separator;
        }
    }

    @Override
    public void init() throws Exception {

    }

    private String toPath(int groupId) {
        return String.format("%s%d", dir, groupId);
    }

    private String toTmpPath(int groupId) {
        return String.format("%s%d%d", dir, groupId, TMP_FILE_SUFFIX);
    }

    @Override
    public void delete(int groupId) {

    }

    @Override
    public void write(int groupId, List<Config> configs) throws Exception {
        Objects.requireNonNull(configs, "configs");

        String tmp = toTmpPath(groupId);
        String formal = toPath(groupId);

        /*
         * two phases:
         *
         * 0. write configs to temporary file
         * 1. move(atomic) temporary file to formal file
         */
        write(tmp, configs);
        Files.move(Paths.get(tmp), Paths.get(formal), StandardCopyOption.ATOMIC_MOVE);
    }

    private void write(String path, List<Config> configs) throws IOException {

    }

    private String serialize(Config config) {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
