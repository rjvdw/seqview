package com.example.seqview;

import java.util.List;

sealed interface Item {
    String name();

    long size();

    static Item parseDuLine(String line) {
        int i = line.indexOf('\t');
        return new File(
                line.substring(i).trim(),
                Long.parseLong(line.substring(0, i))
        );
    }
}

record Folder(String name, List<Item> children) implements Item {
    public Folder {
        children = List.copyOf(children);
    }

    @Override
    public long size() {
        return children
                .stream()
                .mapToLong(Item::size)
                .sum();
    }

    public List<Item> children() {
        return List.copyOf(children);
    }
}

record File(String name, long size) implements Item {
}
