package com.example.seqview;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: $0 input-file width=800 height=600");
            System.exit(1);
        }

        Path inputFile = Path.of(args[0]);
        int width = args.length > 1 ? Integer.parseInt(args[1]) : 800;
        int height = args.length > 2 ? Integer.parseInt(args[2]) : 600;

        Item root = buildHierarchy("/", parseDuOutput(inputFile));
        OutputBuilder outputBuilder = new OutputBuilder(width, height);
        constructFileBlocks(root, Position.initial(width, height), outputBuilder);
        System.out.println(outputBuilder);
    }

    /**
     * Parse the output of the `du` command into a map.
     *
     * @param inputFile The file with the output of `du`.
     * @return A map which maps directory paths to the list of their children.
     * @throws IOException When the input file could not be read.
     */
    private static Map<String, List<Item>> parseDuOutput(Path inputFile) throws IOException {
        List<String> lines = Files.readAllLines(inputFile, StandardCharsets.UTF_8);
        Map<String, List<Item>> items = new HashMap<>();

        for (String line : lines) {
            Item item = Item.parseDuLine(line);
            if (!item.name().equals("/")) {
                int lastSlash = item.name().lastIndexOf('/');
                items.computeIfAbsent(
                        lastSlash == 0 ? "/" : item.name().substring(0, lastSlash),
                        ignored -> new ArrayList<>()
                ).add(item);
            }
        }

        return items;
    }

    /**
     * Construct an item hierarchy from a map of items.
     *
     * @param root      The root path from which to start.
     * @param hierarchy A map which maps directory paths to the list of their children.
     * @return The item hierarchy that was constructed.
     */
    private static Item buildHierarchy(String root, Map<String, List<Item>> hierarchy) {
        List<Item> children = hierarchy.get(root)
                .stream()
                .map(item -> hierarchy.containsKey(item.name())
                        ? buildHierarchy(item.name(), hierarchy)
                        : item
                )
                .toList();

        return new Folder(root, children);
    }

    /**
     * Recursively constructs file blocks which dimensions correspond with their sizes relative to each other.
     *
     * @param root          The root item from which to find files.
     * @param position      The position of the root item.
     * @param outputBuilder The output builder which collects the file blocks.
     */
    private static void constructFileBlocks(Item root, Position position, OutputBuilder outputBuilder) {
        if (position.width() == 0 || position.height() == 0) {
            // file is too small to be shown
            return;
        }

        switch (root) {
            case File file -> outputBuilder.take(file, position);
            case Folder f -> {
                List<Item> children = f.children()
                        .stream()
                        .sorted(Comparator.comparingLong(Item::size).reversed())
                        .toList();

                Tree tree = partition(children);
                traverse(tree, position, outputBuilder);
            }
        }
    }

    /**
     * Recursively traveres a binary tree with files.
     *
     * @param tree          The binary tree to traverse.
     * @param position      The position of the root item.
     * @param outputBuilder The output builder which collects the file blocks.
     */
    private static void traverse(Tree tree, Position position, OutputBuilder outputBuilder) {
        int x = position.x();
        int y = position.y();
        int width = position.width();
        int height = position.height();
        int depth = position.depth();

        switch (tree) {
            case Node(Tree left, Tree right) -> {
                double ratio = ((double) left.size()) / ((double) (left.size() + right.size()));
                if (width > height) {
                    int w1 = (int) (width * ratio);
                    int w2 = width - w1;
                    traverse(left, new Position(x, y, w1, height, depth), outputBuilder);
                    traverse(right, new Position(x + w1, y, w2, height, depth), outputBuilder);
                } else {
                    int h1 = (int) (height * ratio);
                    int h2 = height - h1;
                    traverse(left, new Position(x, y, width, h1, depth), outputBuilder);
                    traverse(right, new Position(x, y + h1, width, h2, depth), outputBuilder);
                }
            }
            case Leaf(Item item) -> {
                constructFileBlocks(item, new Position(x, y, width, height, depth + 1), outputBuilder);
            }
        }
    }

    /**
     * Recursively partition a list of items into a binary tree in such a way that for every node the total size of the
     * children is (roughly) the same.
     *
     * @param items The list of items to partition.
     * @return A binary tree containing all elements from the list.
     */
    private static Tree partition(List<Item> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Cannot partition empty list");
        }
        if (items.size() == 1) {
            return new Leaf(items.getFirst());
        }

        List<Item> left = new ArrayList<>();
        long leftSize = 0;
        List<Item> right = new ArrayList<>();
        long rightSize = 0;

        for (Item item : items) {
            // Add the item to the smallest subset. If both subsets are equal in size, add the item to the subset with
            // the fewest items. This helps with the edge case where there are only files with size 0.
            if (leftSize < rightSize) {
                left.add(item);
                leftSize += item.size();
            } else if (leftSize > rightSize) {
                right.add(item);
                rightSize += item.size();
            } else if (left.size() > right.size()) {
                right.add(item);
                rightSize += item.size();
            } else {
                left.add(item);
                leftSize += item.size();
            }
        }

        return new Node(
                partition(left),
                partition(right)
        );
    }
}
