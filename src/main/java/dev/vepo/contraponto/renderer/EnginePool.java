package dev.vepo.contraponto.renderer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnginePool {
    @FunctionalInterface
    public interface EngineFunction<T> {
        T apply(ScriptEngine engine) throws NoSuchMethodException, ScriptException;

    }

    private static final Logger logger = LoggerFactory.getLogger(EnginePool.class);

    private final Supplier<ScriptEngine> engineSupplier;
    private final Queue<ScriptEngine> idleEngines;
    private final Semaphore semaphore; // limits number of engines borrowed at the same time
    private final int maxPoolSize;

    public EnginePool(int maxPoolSize, Supplier<ScriptEngine> engineSupplier) {
        this.maxPoolSize = maxPoolSize;
        this.engineSupplier = engineSupplier;
        this.semaphore = new Semaphore(maxPoolSize);
        this.idleEngines = new ConcurrentLinkedQueue<>();
        logger.info("Reactive engine pool (max concurrency = {})", maxPoolSize);
    }

    public <T> T withEngine(EngineFunction<T> fn) throws NoSuchMethodException, ScriptException {
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
                engine = engineSupplier.get();
            }

            // 3. Perform rendering
            return fn.apply(engine);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RendererException("Interrupted while waiting for an engine", e);
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
