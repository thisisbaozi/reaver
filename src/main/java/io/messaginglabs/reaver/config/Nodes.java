package io.messaginglabs.reaver.config;

import java.util.List;

public class Nodes {


    public static String dump(List<Node> nodes) {
        StringBuilder str = new StringBuilder("nodes(");

        int size = nodes.size();
        for (int i = 0; i < size; i++) {
            str.append(nodes.get(i).toString());
            if (i < size - 1) {
                str.append(",");
            }
        }

        str.append(")");
        return str.toString();
    }

}
