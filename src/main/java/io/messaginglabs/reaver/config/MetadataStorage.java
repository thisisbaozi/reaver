package io.messaginglabs.reaver.config;

import java.io.Closeable;
import java.util.List;

public interface MetadataStorage extends Closeable {

    void init() throws Exception;
    void delete(int groupId);
    void write(int groupId, List<PaxosConfig> configs) throws Exception;

    List<PaxosConfig> fetch(int groupId);
}
