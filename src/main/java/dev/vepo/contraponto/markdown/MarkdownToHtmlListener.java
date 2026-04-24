package dev.vepo.contraponto.markdown;

import dev.vepo.contraponto.markdown.MarkdownParser.BoldContext;
import dev.vepo.contraponto.markdown.MarkdownParser.HeadingContext;
import dev.vepo.contraponto.markdown.MarkdownParser.InlineCodeContext;
import dev.vepo.contraponto.markdown.MarkdownParser.ItalicContext;
import dev.vepo.contraponto.markdown.MarkdownParser.LinebreakContext;
import dev.vepo.contraponto.markdown.MarkdownParser.NewlineContext;
import dev.vepo.contraponto.markdown.MarkdownParser.ParagraphBreakContext;
import dev.vepo.contraponto.markdown.MarkdownParser.ParagraphContext;
import dev.vepo.contraponto.markdown.MarkdownParser.SpaceContext;
import dev.vepo.contraponto.markdown.MarkdownParser.TextContext;

public class MarkdownToHtmlListener extends MarkdownBaseListener {
    private final StringBuilder html;
    private BoldContext bold;
    private ItalicContext italic;

    public MarkdownToHtmlListener() {
        html = new StringBuilder();
        bold = null;
        italic = null;
    }

    public String getHtml() {
        return html.toString();
    }

    private void emit(String s) {
        html.append(s);
    }

    private String headingTag(MarkdownParser.HeadingContext ctx) {
        if (ctx.H1() != null) {
            return "h1";
        } else if (ctx.H2() != null) {
            return "h2";
        } else if (ctx.H3() != null) {
            return "h3";
        } else if (ctx.H4() != null) {
            return "h4";
        } else if (ctx.H5() != null) {
            return "h5";
        } else if (ctx.H6() != null) {
            return "h6";
        } else {
            throw new IllegalStateException("Unknown header!");
        }
    }

    @Override
    public void enterHeading(HeadingContext ctx) {
        emit("<%s>".formatted(headingTag(ctx)));
    }

    @Override
    public void exitHeading(HeadingContext ctx) {
        emit("</%s>\n".formatted(headingTag(ctx)));
    }

    @Override
    public void exitNewline(NewlineContext ctx) {
        emit(ctx.getText());
    }

    @Override
    public void enterParagraph(ParagraphContext ctx) {
        emit("<p>");
    }

    @Override
    public void exitLinebreak(LinebreakContext ctx) {
        emit("<br />");
    }

    @Override
    public void enterInlineCode(InlineCodeContext ctx) {
        emit("<code>");
    }

    @Override
    public void exitInlineCode(InlineCodeContext ctx) {
        emit("</code>");
    }

    @Override
    public void exitParagraphBreak(ParagraphBreakContext ctx) {
        emit("\n");
    }

    @Override
    public void exitSpace(SpaceContext ctx) {
        emit(" ");
    }

    @Override
    public void enterBold(BoldContext ctx) {
        if (bold == null) {
            emit("<strong>");
            bold = ctx;

        }
    }

    @Override
    public void exitBold(BoldContext ctx) {
        if (bold == ctx) {
            emit("</strong>");
            bold = null;
        }
    }

    @Override
    public void enterItalic(ItalicContext ctx) {
        if (italic == null) {
            emit("<em>");
            italic = ctx;
        }
    }

    @Override
    public void exitItalic(ItalicContext ctx) {
        if (italic == ctx) {
            emit("</em>");
            italic = null;
        }
    }

    @Override
    public void exitText(TextContext ctx) {
        emit(ctx.getText());
    }

    @Override
    public void exitParagraph(ParagraphContext ctx) {
        emit("</p>\n");
    }
}