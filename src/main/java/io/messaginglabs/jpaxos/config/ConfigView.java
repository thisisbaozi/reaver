package io.messaginglabs.jpaxos.config;

import java.util.List;

public interface ConfigView {

    long proposalId();

    long time();

    List<String> members();
    String joined();
    String leaving();

}
