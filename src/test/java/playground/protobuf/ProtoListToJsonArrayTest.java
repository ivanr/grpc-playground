package playground.protobuf;

import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProtoListToJsonArrayTest {

    @Test
    public void test() throws IOException {

        List<Item> input = new ArrayList<>();
        input.add(Item.newBuilder()
                .setName("1")
                .build());
        input.add(Item.newBuilder()
                .setName("2")
                .build());

        String json = protoListToJson(input);
        System.out.println(json);

        List<Item> output = jsonArrayToProtoList(json, ItemList.newBuilder())
                .build()
                .getItemsList();
    }

    public static String protoListToJson(List<? extends MessageOrBuilder> list) throws InvalidProtocolBufferException {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        int counter = 0;
        for (MessageOrBuilder item : list) {
            if (counter++ != 0) {
                sb.append(",");
            }

            sb.append(JsonFormat.printer().print(item));
        }

        sb.append("]");
        return sb.toString();
    }

    private static <T extends Message.Builder> T jsonArrayToProtoList(String json, T parentBuilder) throws InvalidProtocolBufferException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"");
        sb.append(parentBuilder.getDescriptorForType().getFields().iterator().next().getName());
        sb.append("\":");
        sb.append(json);
        sb.append("}");
        String wrappedJson = sb.toString();

        JsonFormat.parser().merge(wrappedJson, parentBuilder);

        return parentBuilder;
    }
}

