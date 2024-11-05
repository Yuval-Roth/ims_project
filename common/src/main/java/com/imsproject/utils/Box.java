package com.imsproject.utils;

public class Box<T>{
    private T value;

    private Box(T value){
        this.value = value;
    }

    public T get(){
        return value;
    }

    public void set(T value){
        this.value = value;
    }

    public static <T> Box<T> of(T value){
        return new Box<>(value);
    }

    public static <T> Box<T> empty(){
        return new Box<>(null);
    }
}
