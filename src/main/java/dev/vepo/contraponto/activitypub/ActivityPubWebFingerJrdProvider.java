package dev.vepo.contraponto.activitypub;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces(ActivityPubWebFingerJrdProvider.JRD_JSON)
public class ActivityPubWebFingerJrdProvider implements MessageBodyWriter<WebFingerJrd> {

    static final String JRD_JSON = "application/jrd+json";

    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return WebFingerJrd.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(WebFingerJrd jrd,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream)
            throws IOException {
        JSON.writeValue(entityStream, jrd);
    }
}
