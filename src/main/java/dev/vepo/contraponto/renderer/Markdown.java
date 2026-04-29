package dev.vepo.contraponto.renderer;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
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

    private final Queue<ScriptEngine> idleEngines = new ConcurrentLinkedQueue<>();
    private final Semaphore semaphore; // limits number of engines borrowed at the same time

    private final int maxPoolSize;

    private Markdown() {
        maxPoolSize = Integer.getInteger("markdown.pool.maxSize", Runtime.getRuntime().availableProcessors());
        semaphore = new Semaphore(maxPoolSize);
        logger.info("Reactive Markdown engine pool (max concurrency = {})", maxPoolSize);
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
        ScriptEngine engine = null;
        boolean permitAcquired = false;
        try {
            // 1. Acquire a permit to borrow an engine (up to max concurrency)
            permitAcquired = semaphore.tryAcquire(5, TimeUnit.SECONDS);
            if (!permitAcquired) {
                throw new RendererException("Timeout waiting for an engine slot (max concurrency = " + maxPoolSize + ")");
            }

            // 2. Try to get an idle engine; if none, create a new one
            engine = idleEngines.poll();
            if (engine == null) {
                engine = createEngine();
            }

            // 3. Perform rendering
            return (String) ((Invocable) engine).invokeFunction("renderMarkdownAsHTML", content);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RendererException("Interrupted while waiting for an engine", e);
        } catch (NoSuchMethodException | ScriptException e) {
            logger.error("Failed to render Markdown content", e);
            return content;
        } finally {
            // 4. Return the engine to the idle pool for reuse (if it exists)
            if (engine != null) {
                idleEngines.offer(engine);
            }
            // 5. Release the permit – another thread can now borrow an engine
            if (permitAcquired) {
                semaphore.release();
            }
        }
    }
}