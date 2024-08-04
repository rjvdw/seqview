package com.example.seqview;

sealed interface Tree {
    long size();
}

record Node(Tree left, Tree right) implements Tree {
    @Override
    public long size() {
        return left().size() + right().size();
    }
}

record Leaf(Item item) implements Tree {
    @Override
    public long size() {
        return item().size();
    }
}
