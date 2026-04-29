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

public final class AsciiDoctor implements Renderer {

    static {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    }

    private static final Logger logger = LoggerFactory.getLogger(AsciiDoctor.class);
    private static final AtomicReference<AsciiDoctor> instance = new AtomicReference<>();

    public static AsciiDoctor getInstance() {
        return instance.updateAndGet(current -> current == null ? new AsciiDoctor() : current);
    }

    private final Queue<ScriptEngine> idleEngines = new ConcurrentLinkedQueue<>();
    private final Semaphore semaphore;

    private final int maxPoolSize;

    private AsciiDoctor() {
        maxPoolSize = Integer.getInteger("asciidoctor.pool.maxSize", Runtime.getRuntime().availableProcessors());
        semaphore = new Semaphore(maxPoolSize);
        logger.info("Reactive AsciiDoctor engine pool (max concurrency = {})", maxPoolSize);
    }

    private ScriptEngine createEngine() {
        try {
            ScriptEngine engine = GraalJSScriptEngine.create();

            // 1. Set up a global object to mimic a basic browser environment
            engine.eval("var global = this; var window = this;");

            // 2. Load Asciidoctor.js (browser UMD build)
            var scriptBytes = AsciiDoctor.class.getResourceAsStream("/META-INF/resources/js/third-party/asciidoctor.min.js")
                                               .readAllBytes();
            String asciidoctorScript = new String(scriptBytes);
            if (asciidoctorScript.isEmpty()) {
                throw new RuntimeException("Asciidoctor script is empty – check the file path");
            }
            logger.debug("Loaded Asciidoctor.js, length: {} bytes", asciidoctorScript.length());
            engine.eval(asciidoctorScript);

            // 3. Define the rendering function (uses the global Asciidoctor factory)
            engine.eval("""
                        const asciidoctor = Asciidoctor();
                        function renderAsciiDocAsHTML(content) {
                            return asciidoctor.convert(content, {
                                safe: 'safe',
                                attributes: { showtitle: true }
                            });
                        }
                        """);
            logger.debug("Created new GraalJS engine for AsciiDoctor");
            return engine;
        } catch (IOException | ScriptException e) {
            logger.error("Failed to create or initialise GraalJS engine for AsciiDoctor", e);
            throw new RuntimeException("Could not create AsciiDoctor engine", e);
        }
    }

    @Override
    public String render(String content) {
        ScriptEngine engine = null;
        boolean permitAcquired = false;
        try {
            permitAcquired = semaphore.tryAcquire(5, TimeUnit.SECONDS);
            if (!permitAcquired) {
                throw new RuntimeException("Timeout waiting for an engine slot (max concurrency = " + maxPoolSize + ")");
            }

            engine = idleEngines.poll();
            if (engine == null) {
                engine = createEngine();
            }

            return (String) ((Invocable) engine).invokeFunction("renderAsciiDocAsHTML", content);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for an engine", e);
        } catch (NoSuchMethodException | ScriptException e) {
            logger.error("Failed to render AsciiDoc content", e);
            return content;
        } finally {
            if (engine != null) {
                idleEngines.offer(engine);
            }
            if (permitAcquired) {
                semaphore.release();
            }
        }
    }
}