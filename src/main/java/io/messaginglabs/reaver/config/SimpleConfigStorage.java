package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.utils.FileSystemUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleConfigStorage implements ConfigStorage {

    private static final String TMP_FILENAME_SUFFIX = ".tmp";
    private static final String FILENAME_PREFIX = "group-config-";

    private static final Logger logger = LoggerFactory.getLogger(SimpleConfigStorage.class);

    /*
     * config files are created under this directory
     */
    private final String dir;
    private int num;

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

        this.num = 0;
    }

    @Override
    public void init() throws Exception {
        File[] files = new File(this.dir).listFiles();
        if (files == null || files.length == 0) {
            return ;
        }

        /*
         * ignores temporary files
         */
        this.num = (int)Arrays.stream(files).filter(file -> !file.getName().startsWith(FILENAME_PREFIX)).filter(file -> file.getName().endsWith(TMP_FILENAME_SUFFIX)).count();
    }

    private String toPath(int groupId) {
        return dir + FILENAME_PREFIX + groupId;
    }

    private String toTmpPath(int groupId) {
        return dir + FILENAME_PREFIX + groupId + TMP_FILENAME_SUFFIX;
    }

    @Override
    public void delete(int groupId) {

    }

    @Override
    public void write(int groupId, List<Config> configs) throws Exception {
        Objects.requireNonNull(configs, "configs");

        String tmp = toTmpPath(groupId);
        Path formal = Paths.get(toPath(groupId));

        // it's a new config?
        boolean newConfig = !formal.toFile().exists();

        /*
         * two phases:
         *
         * 0. write configs to temporary file
         * 1. move(atomic) temporary file to formal file
         */
        write(tmp, configs);
        Files.move(Paths.get(tmp), formal, StandardCopyOption.ATOMIC_MOVE);

        // warning if there're too many config files.
        if (newConfig) {
            num++;
        }
        if (num >= 64 && logger.isWarnEnabled()) {
            logger.warn("too many config files({}) under directory({})", num, dir);
        }
    }

    private void write(String path, List<Config> configs) throws IOException {
        BufferedWriter bWriter = null;
        FileWriter writer = new FileWriter(path);
        try {
            bWriter = new BufferedWriter(writer);
            for (Config cfg : configs) {
                String str = serialize(cfg);
                bWriter.write(str);
                bWriter.newLine();
            }

            // todo: checksum?

            bWriter.flush();
        } finally {
            if (bWriter != null) {
                bWriter.close();
            }
        }
    }

    private String serialize(Config config) {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
