package dev.vepo.contraponto.shared.toast;

import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

public interface Toast {
    public enum Type {
        SUCCESS, ERROR;
    }

    public static class ToastResponseBuilder {

        private final ResponseBuilder builder;

        public ToastResponseBuilder(ResponseBuilder builder) {
            this.builder = builder;
        }

        public ToastResponseBuilder message(String message) {
            builder.header("X-Toast-Message", message);
            return this;
        }

        public ToastResponseBuilder type(Type type) {
            builder.header("X-Toast-Type", switch (type) {
                case SUCCESS -> "Success";
                case ERROR -> "Error";
            });
            return this;
        }

        public ToastResponseBuilder duration(int durationMs) {
            builder.header("X-Toast-Duration", Integer.toString(durationMs));
            return this;
        }

        public Response build() {
            return builder.build();
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

    public static ToastResponseBuilder response(Status status) {
        return new ToastResponseBuilder(Response.status(status));
    }

    public static ToastResponseBuilder ok() {
        return new ToastResponseBuilder(Response.ok());
    }
}
