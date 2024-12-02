package com.imsproject.common.dataAccess;

import com.imsproject.common.dataAccess.abstracts.Cacheable;

import java.util.Collection;
import java.util.HashMap;

public class Cache <T extends Cacheable> {
    private final HashMap<String,T> map;

    public Cache() {
        map = new HashMap<>();
    }

    public void put(T object){
        map.put(object.getIdentifier(), object);
    }

    public void putAll(Collection<T> objects){
        for (T object : objects) {
            put(object);
        }
    }

    public T get(T object){
        return map.get(object.getIdentifier());
    }

    public void remove(T object){
        map.remove(object.getIdentifier());
    }

    public boolean contains(T object){
        return map.containsKey(object.getIdentifier());
    }

    public void clear(){
        map.clear();
    }
}
