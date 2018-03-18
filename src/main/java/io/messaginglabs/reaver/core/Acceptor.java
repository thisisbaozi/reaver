package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.Accept;
import io.messaginglabs.reaver.com.AcceptReply;
import io.messaginglabs.reaver.com.msg.Prepare;
import io.messaginglabs.reaver.com.PrepareReply;

public interface Acceptor extends Voter {

    PrepareReply process(Prepare prepare);
    AcceptReply process(Accept accept);

}
