package io.messaginglabs.reaver.config;

public interface GroupConfigs {


    Config apply(UnifiedBoot boot);
    Config apply(AddNode event);
    Config find(long sequence);

}
