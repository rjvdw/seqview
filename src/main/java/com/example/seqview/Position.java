package com.example.seqview;

record Position(int x, int y, int width, int height, int depth) {
    static Position initial(int width, int height) {
        return new Position(0, 0, width, height, 0);
    }
}
