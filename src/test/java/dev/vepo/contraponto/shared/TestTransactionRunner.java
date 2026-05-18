package dev.vepo.contraponto.shared;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TestTransactionRunner {

    @Transactional
    public void run(Runnable code) {
        code.run();
    }

    @Transactional
    public <T> T run(Supplier<T> code) {
        return code.get();
    }
}
