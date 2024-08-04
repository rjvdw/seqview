package com.example.seqview;

import java.text.NumberFormat;

class OutputBuilder {

    private static final NumberFormat SIZE_FORMATTER = NumberFormat.getInstance();

    private final StringBuilder sb = new StringBuilder();
    private final int width;
    private final int height;

    OutputBuilder(int width, int height) {
        this.width = width;
        this.height = height;
    }

    void take(Item item, Position position) {
        sb
                .append("<div title=\"")
                .append(item.name())
                .append(" (size=")
                .append(SIZE_FORMATTER.format(item.size()))
                .append(")\" class=\"file-block\" style=\"left:")
                .append(position.x())
                .append("px;top:")
                .append(position.y())
                .append("px;width:")
                .append(position.width())
                .append("px;height:")
                .append(position.height())
                .append("px\" data-depth=\"")
                .append(position.depth())
                .append("\"></div>\n");
    }

    @Override
    public String toString() {
        // language=HTML
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8"/>
                    <title>Output</title>
                    <style>
                        :root, body {
                            width: 100%%;
                            height: 100%%;
                            margin: 0;
                            padding: 0;
                        }
                        body {
                            display: grid;
                            place-content: center;
                        }
                        .viewer {
                            position: relative;
                            width: %spx;
                            height: %spx;
                        }
                        .file-block {
                            background: linear-gradient(135deg, #ddd 0%%, #222 100%%);
                            position: absolute;
                        }
                    </style>
                </head>
                <body>
                <div class="viewer">
                %s</div>
                </body>
                </html>
                """.formatted(width, height, sb.toString());
    }
}
