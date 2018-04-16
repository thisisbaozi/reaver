package io.messaginglabs.reaver.core;

import io.messaginglabs.reaver.com.msg.AcceptorReply;
import io.messaginglabs.reaver.com.msg.Proposing;

public interface Acceptor extends Participant {

    AcceptorReply process(Proposing propose);

}
