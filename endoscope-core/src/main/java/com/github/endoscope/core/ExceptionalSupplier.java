package com.github.endoscope.core;

public interface ExceptionalSupplier<T> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws Exception;
}