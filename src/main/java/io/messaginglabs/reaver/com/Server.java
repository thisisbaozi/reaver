package io.messaginglabs.reaver.com;

import io.messaginglabs.reaver.config.Node;
import io.messaginglabs.reaver.utils.RefCount;

public interface Server extends RefCount {

    boolean join(Node node);

}
