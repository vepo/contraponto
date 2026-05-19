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
        private String i18nKey;
        private String i18nParamsJson;
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

        public ToastResponseBuilder i18nKey(String key) {
            return i18nKey(key, null);
        }

        /**
         * {@code defaultPtBrMessage} is shown when locale is pt-BR (no JSON bundle).
         */
        public ToastResponseBuilder i18nKey(String key, String defaultPtBrMessage) {
            this.i18nKey = key;
            this.message = defaultPtBrMessage;
            builder.header("X-Toast-I18n-Key", key);
            if (defaultPtBrMessage != null) {
                builder.header("X-Toast-Message", defaultPtBrMessage);
            }
            return this;
        }

        public ToastResponseBuilder i18nParams(String jsonParams) {
            this.i18nParamsJson = jsonParams;
            builder.header("X-Toast-I18n-Params", jsonParams);
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
            if (message != null || i18nKey != null) {
                int duration = durationMs != null ? durationMs : TOAST_DEFAULT_DURATION_MS;
                String type = typeName != null ? typeName : "Success";
                String triggerJson = buildTriggerJson(message, i18nKey, i18nParamsJson, duration, type);
                builder.header("HX-Trigger", triggerJson);
                builder.header("HX-Trigger-After-Settle", triggerJson);
            }
            return builder.build();
        }

        private static String buildTriggerJson(String message, String i18nKey, String i18nParamsJson,
                                               int duration, String type) {
            if (i18nKey != null) {
                String escapedKey = escapeJson(i18nKey);
                String paramsPart = "";
                if (i18nParamsJson != null && !i18nParamsJson.isBlank()) {
                    paramsPart = ",\"i18nParams\":" + i18nParamsJson;
                }
                String messagePart = "";
                if (message != null) {
                    messagePart = ",\"message\":\"" + escapeJson(message) + "\"";
                }
                return "{\"toast:show\":{\"i18nKey\":\"%s\"%s%s,\"duration\":%d,\"type\":\"%s\"}}"
                                                                                                  .formatted(escapedKey, messagePart, paramsPart, duration,
                                                                                                             type);
            }
            String escaped = escapeJson(message);
            return "{\"toast:show\":{\"message\":\"%s\",\"duration\":%d,\"type\":\"%s\"}}"
                                                                                          .formatted(escaped, duration, type);
        }

        private static String escapeJson(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
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
