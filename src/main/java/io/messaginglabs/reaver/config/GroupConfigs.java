package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.com.msg.Message;
import io.messaginglabs.reaver.core.ChosenValue;

public interface GroupConfigs {


    Config apply(UnifiedBoot boot);
    Config apply(AddNode event);
    Config find(long sequence);

}
