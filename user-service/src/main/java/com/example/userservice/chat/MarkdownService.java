package com.example.userservice.chat;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

@Service
public class MarkdownService {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    public String toHtml(String markdownOrHtml) {
        if (markdownOrHtml == null || markdownOrHtml.isBlank()) {
            return "";
        }

        // Heuristic: if it already looks like HTML, return as-is
        String trimmed = markdownOrHtml.trim();
        if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            return trimmed;
        }

        Node document = parser.parse(markdownOrHtml);
        return renderer.render(document);
    }
}


