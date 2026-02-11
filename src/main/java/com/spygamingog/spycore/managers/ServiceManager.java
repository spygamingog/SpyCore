package com.spygamingog.spycore.managers;

import com.spygamingog.spycore.SpyCore;
import java.util.HashMap;
import java.util.Map;

public class ServiceManager {
    private final SpyCore plugin;
    private final Map<Class<?>, Object> services = new HashMap<>();

    public ServiceManager(SpyCore plugin) {
        this.plugin = plugin;
    }

    public <T> void registerService(Class<T> serviceClass, T implementation) {
        services.put(serviceClass, implementation);
        plugin.getLogger().info("Registered service: " + serviceClass.getSimpleName());
    }

    public <T> T getService(Class<T> serviceClass) {
        Object service = services.get(serviceClass);
        if (serviceClass.isInstance(service)) {
            return serviceClass.cast(service);
        }
        return null;
    }
}
