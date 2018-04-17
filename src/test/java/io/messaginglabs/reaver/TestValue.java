package io.messaginglabs.reaver;

import io.messaginglabs.reaver.core.ValueType;
import io.messaginglabs.reaver.core.Value;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;

public class TestValue {

    @Test
    public void testValueHeader() throws Exception {
        int size = 1024 * 1024 * 4;
        int header = Value.combine(size, ValueType.APP_DATA);
        ValueType type = Value.parse(header);
        Assert.assertEquals(type, ValueType.APP_DATA);
        Assert.assertEquals(Value.parseSize(header), size);

        size = 1024 * 1024;
        header = Value.combine(size, ValueType.MEMBER_LEAVE);
        type = Value.parse(header);
        Assert.assertEquals(Value.parseSize(header), size);
        Assert.assertEquals(type, ValueType.MEMBER_LEAVE);
    }

    @Test
    public void testCodec() throws Exception {
        ByteBuffer value = MockUtils.wrap();
        ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer();
        Value.init(ValueType.APP_DATA, value, buffer);

        ValueType type = Value.parse(buffer);
        Assert.assertEquals(type, ValueType.APP_DATA);

        buffer = ByteBufAllocator.DEFAULT.directBuffer();
        Value.init(ValueType.MEMBER_JOIN, value, buffer);
        type = Value.parse(buffer);
        Assert.assertEquals(type, ValueType.MEMBER_JOIN);
    }
}
