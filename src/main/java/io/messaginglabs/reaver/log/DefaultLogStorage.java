package io.messaginglabs.reaver.log;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import java.io.IOException;

public class DefaultLogStorage extends AbstractReferenceCounted implements LogStorage {


    public DefaultLogStorage(String path) {

    }

    @Override
    public void init() throws Exception {

    }

    @Override public void close() throws IOException {

    }

    @Override protected void deallocate() {

    }

    @Override public ReferenceCounted touch(Object hint) {
        return null;
    }
}
