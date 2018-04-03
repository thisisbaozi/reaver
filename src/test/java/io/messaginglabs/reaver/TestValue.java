package io.messaginglabs.reaver;

import io.messaginglabs.reaver.core.ValueType;
import io.messaginglabs.reaver.core.ValueUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;

public class TestValue {

    @Test
    public void testValueHeader() throws Exception {
        int size = 1024 * 1024 * 4;
        int header = ValueUtils.combine(size, ValueType.APP_DATA);
        ValueType type = ValueUtils.parse(header);
        Assert.assertEquals(type, ValueType.APP_DATA);
        Assert.assertEquals(ValueUtils.parseSize(header), size);

        size = 1024 * 1024;
        header = ValueUtils.combine(size, ValueType.REMOVE_MEMBER);
        type = ValueUtils.parse(header);
        Assert.assertEquals(ValueUtils.parseSize(header), size);
        Assert.assertEquals(type, ValueType.REMOVE_MEMBER);
    }

    @Test
    public void testCodec() throws Exception {
        ByteBuffer value = MockUtils.makeValue();
        ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer();
        ValueUtils.init(ValueType.APP_DATA, value, buffer);

        ValueType type = ValueUtils.parse(buffer);
        Assert.assertEquals(type, ValueType.APP_DATA);
    }
}
