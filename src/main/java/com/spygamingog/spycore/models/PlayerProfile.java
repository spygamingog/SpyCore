package com.spygamingog.spycore.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
public class PlayerProfile {
    private final UUID uuid;
    private final String name;
    private final Map<String, Object> data = new HashMap<>();

    public void setData(String key, Object value) {
        data.put(key, value);
    }

    public Object getData(String key) {
        return data.get(key);
    }
    
    public <T> T getData(String key, Class<T> type) {
        Object val = data.get(key);
        if (type.isInstance(val)) {
            return type.cast(val);
        }
        return null;
    }
}
