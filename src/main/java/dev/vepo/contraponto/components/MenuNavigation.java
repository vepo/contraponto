package dev.vepo.contraponto.components;

public record MenuNavigation(String myBlogUrl,
                             boolean myBlogUsesHtmx,
                             String writeUrl,
                             boolean writeUsesHtmx,
                             String writingUrl,
                             boolean writingUsesHtmx,
                             String readingUrl,
                             boolean readingUsesHtmx,
                             String manageUrl,
                             boolean manageUsesHtmx,
                             String accountUrl,
                             boolean accountUsesHtmx,
                             String editorUrl,
                             boolean editorUsesHtmx,
                             String administrationUrl,
                             boolean administrationUsesHtmx) {}
