package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.dsl.Commit;
import java.nio.ByteBuffer;

public interface Proposer extends Voter {

    Commit commit(ByteBuffer value);

}
