package io.messaginglabs.jpaxos.core;

import io.messaginglabs.jpaxos.dsl.Commit;
import java.nio.ByteBuffer;

public interface Proposer extends Voter {

    Commit commit(ByteBuffer value);

}
