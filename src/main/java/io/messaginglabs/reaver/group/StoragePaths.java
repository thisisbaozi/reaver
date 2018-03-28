package io.messaginglabs.reaver.group;

import io.messaginglabs.reaver.utils.FileSystemUtils;
import io.messaginglabs.reaver.utils.Parameters;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

public class StoragePaths {

    private final String path;

    public StoragePaths(String path) {
        Parameters.requireNotEmpty(path, "path");

        try {
            File file = FileSystemUtils.mkdirsIfNecessary(path);
            FileSystemUtils.hasRWPermission(file);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("can't store data in path(%s)", path), e);
        }

        if (path.endsWith(File.separator)) {
            this.path = path;
        } else {
            this.path = path + File.separator;
        }
    }

    public String metadataStoragePath() {
        return path + "meta" + File.separator;
    }

    public String logStoragePath() {
         return path + "log" + File.separator;
    }
}
