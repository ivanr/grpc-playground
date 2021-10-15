package playground.protobuf;

import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

public class NullTest {

    // You can't set a NULL value on an object.
    @Test(expected = NullPointerException.class)
    public void testSetNullMessage() {
        Null t = Null.newBuilder()
                .setVoid((Void) null)
                .build();
    }

    // You can't create a StringValue from null.
    @Test(expected = NullPointerException.class)
    public void testSetNullStringValue() {
        Null t = Null.newBuilder()
                .setStringValue(StringValue.of(null))
                .build();
    }

    // You can't set a NULL value even on an optional field.
    @Test(expected = NullPointerException.class)
    public void testSetNullOptional() {
        Null t = Null.newBuilder()
                .setOptional(null)
                .build();
    }

    @Test
    @SneakyThrows
    public void testGets() {
        // Create an object on which we don't set any fields.
        Null t = Null.newBuilder()
                .build();

        // We can check if the optional field is set.
        Assert.assertFalse(t.hasOptional());
        Assert.assertEquals("", t.getOptional());

        // We can't check if the optional field is not set because the field doesn't have the hasN() method.
        Assert.assertEquals("", t.getNotOptional());

        // We can check if the message is set.
        Assert.assertFalse(t.hasVoid());
        Assert.assertNotNull(t.getVoid());

        // We can also check if the StringValue is set; it's just a message.
        Assert.assertFalse(t.hasStringValue());
        Assert.assertEquals("", t.getStringValue().getValue());

        /*

        Optional fields and messages that are not set behave as
        expected when a protocol buffer is serialised as JSON: a field
        that's not set will not appear in output.

        {
          "notOptional": ""
        }

         */
        Assert.assertEquals("{\n" +
                "  \"notOptional\": \"\"\n" +
                "}", JsonFormat.printer()
                .includingDefaultValueFields().print(t));
    }
}
