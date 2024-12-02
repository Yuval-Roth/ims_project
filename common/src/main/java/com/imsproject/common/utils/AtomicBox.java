package com.imsproject.common.utils;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicBox<T>{
    private final AtomicReference<T> value;

    private AtomicBox(T value){
        this.value = new AtomicReference<>(value);
    }

    public T get(){
        return value.get();
    }

    public boolean set(T value){
        return this.value.compareAndSet(this.value.get(), value);
    }

    public static <T> AtomicBox<T> of(T value){
        return new AtomicBox<>(value);
    }

    public static <T> AtomicBox<T> empty(){
        return new AtomicBox<>(null);
    }
}
