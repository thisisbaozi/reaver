package io.messaginglabs.jpaxos.com;

import io.messaginglabs.jpaxos.config.Node;
import io.messaginglabs.jpaxos.utils.RefCount;

public interface Server extends RefCount {

    boolean join(Node node);

}
