package dev.vepo.contraponto.messaging;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class MessageTemplateExtensions {

    @TemplateExtension
    public static String url(MessageThread thread) {
        return MessageThreadPaths.thread(thread.getId());
    }

    private MessageTemplateExtensions() {
        throw new UnsupportedOperationException("Utility class");
    }
}
