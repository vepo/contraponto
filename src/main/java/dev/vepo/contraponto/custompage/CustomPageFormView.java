package dev.vepo.contraponto.custompage;

import java.util.List;

import dev.vepo.contraponto.blog.Blog;

public record CustomPageFormView(CustomPage page,
                                 String slugPath,
                                 String publicUrl,
                                 List<Blog> blogs,
                                 boolean editorView,
                                 boolean applicationScope) {}
