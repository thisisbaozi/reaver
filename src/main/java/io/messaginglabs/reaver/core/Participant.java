package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.group.MultiPaxosGroup;
import java.util.concurrent.ExecutorService;

public interface Participant {


    ExecutorService executor();

    MultiPaxosGroup group();

}
