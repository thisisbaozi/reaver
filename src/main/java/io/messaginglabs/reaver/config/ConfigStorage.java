package io.messaginglabs.reaver.config;

import java.io.Closeable;
import java.util.List;

public interface ConfigStorage extends Closeable {

    void init() throws Exception;
    void delete(int groupId);
    void write(int groupId, List<Config> configs) throws Exception;

}
