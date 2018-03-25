package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.Prepare;
import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.Propose;

public interface Acceptor extends Participant {

    AcceptorReply process(Prepare prepare);
    AcceptorReply process(Propose propose);

}
