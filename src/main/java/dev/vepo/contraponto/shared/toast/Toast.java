package dev.vepo.contraponto.shared.toast;

import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

public interface Toast {
    public static class ToastResponseBuilder {

        private final ResponseBuilder builder;
        private String message;
        private Integer durationMs;
        private String typeName;

        public ToastResponseBuilder(ResponseBuilder builder) {
            this.builder = builder;
        }

        public ToastResponseBuilder message(String message) {
            this.message = message;
            builder.header("X-Toast-Message", message);
            return this;
        }

        public ToastResponseBuilder type(Type type) {
            this.typeName = switch (type) {
                case SUCCESS -> "Success";
                case ERROR -> "Error";
            };
            builder.header("X-Toast-Type", this.typeName);
            return this;
        }

        public ToastResponseBuilder duration(int durationMs) {
            this.durationMs = durationMs;
            builder.header("X-Toast-Duration", Integer.toString(durationMs));
            return this;
        }

        public Response build() {
            if (message != null) {
                int duration = durationMs != null ? durationMs : TOAST_DEFAULT_DURATION_MS;
                String type = typeName != null ? typeName : "Success";
                String triggerJson = buildTriggerJson(message, duration, type);
                // After main swaps, the request element may be detached so afterRequest
                // listeners on body
                // never run; HX-Trigger-After-Settle dispatches on body when needed.
                builder.header("HX-Trigger", triggerJson);
                builder.header("HX-Trigger-After-Settle", triggerJson);
            }
            return builder.build();
        }

        private static String buildTriggerJson(String message, int duration, String type) {
            String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
            return "{\"toast:show\":{\"message\":\"%s\",\"duration\":%d,\"type\":\"%s\"}}"
                                                                                          .formatted(escaped, duration, type);
        }

        public ToastResponseBuilder url(String url) {
            builder.header("HX-Push-Url", url);
            return this;
        }

        public ToastResponseBuilder page(TemplateInstance template) {
            builder.entity(template)
                   .type(MediaType.TEXT_HTML);
            return this;
        }
    }

    public enum Type {
        SUCCESS, ERROR;
    }

    public static final int TOAST_DEFAULT_DURATION_MS = 10_000;

    public static ToastResponseBuilder ok() {
        return new ToastResponseBuilder(Response.ok());
    }

    public static ToastResponseBuilder response(Status status) {
        return new ToastResponseBuilder(Response.status(status));
    }
}
