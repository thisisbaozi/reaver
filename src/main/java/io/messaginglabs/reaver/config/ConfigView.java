package io.messaginglabs.reaver.config;

import java.util.List;

public interface ConfigView {

    long proposalId();

    long time();

    List<String> members();
    String joined();
    String leaving();

}
