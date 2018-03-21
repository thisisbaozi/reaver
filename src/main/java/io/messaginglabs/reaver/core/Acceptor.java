package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.Prepare;
import io.messaginglabs.reaver.com.msg.PrepareReply;
import io.messaginglabs.reaver.com.msg.Propose;
import io.messaginglabs.reaver.com.msg.ProposeReply;

public interface Acceptor extends Voter {

    PrepareReply process(Prepare prepare);
    ProposeReply process(Propose propose);

}
