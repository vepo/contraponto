package dev.vepo.contraponto.markdown;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class MarkdownConverter {
    public static String convert(String markdown) {
        var parser = new MarkdownParser(new CommonTokenStream(new MarkdownLexer(CharStreams.fromString(markdown))));
        var tree = parser.document();
        var walker = new ParseTreeWalker();
        var listener = new MarkdownToHtmlListener();
        walker.walk(listener, tree);
        return listener.getHtml();
    }
}