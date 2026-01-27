package com.spygamingog.spycore.api;

public interface DataService {
    void save(String key, Object value);
    Object load(String key);
    <T> T load(String key, Class<T> type);
    void delete(String key);
}
