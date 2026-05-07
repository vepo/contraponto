package dev.vepo.contraponto.renderer;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;

public final class Markdown implements Renderer {

    static {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    }

    private static final Logger logger = LoggerFactory.getLogger(Markdown.class);
    private static final AtomicReference<Markdown> instance = new AtomicReference<>();

    public static Markdown getInstance() {
        return instance.updateAndGet(current -> current == null ? new Markdown() : current);
    }

    private EnginePool pool;

    private Markdown() {
        pool = new EnginePool(Integer.getInteger("markdown.pool.maxSize", Runtime.getRuntime().availableProcessors()), this::createEngine);
    }

    private ScriptEngine createEngine() {
        try {
            ScriptEngine engine = GraalJSScriptEngine.create();
            var scriptBytes = Markdown.class.getResourceAsStream("/META-INF/resources/js/third-party/marked.min.js")
                                            .readAllBytes();
            engine.eval(new String(scriptBytes));
            engine.eval("""
                        function renderMarkdownAsHTML(content) {
                            marked.setOptions({ breaks: true, gfm: true, headerIds: false, mangle: false });
                            return marked.parse(content);
                        }
                        """);
            logger.debug("Created new GraalJS engine");
            return engine;
        } catch (IOException | ScriptException e) {
            logger.error("Failed to create GraalJS engine", e);
            throw new RendererException("Could not create Markdown engine", e);
        }
    }

    @Override
    public String render(String content) {
        try {
            return pool.withEngine(engine -> (String) ((Invocable) engine).invokeFunction("renderMarkdownAsHTML", content));
        } catch (NoSuchMethodException | ScriptException e) {
            logger.error("Failed to render Markdown content", e);
            return content;
        }
    }
}