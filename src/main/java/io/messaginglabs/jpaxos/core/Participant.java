package io.messaginglabs.jpaxos.core;

import io.messaginglabs.jpaxos.group.MultiPaxosGroup;
import java.util.concurrent.ExecutorService;

public interface Participant {


    ExecutorService executor();

    MultiPaxosGroup group();

}
