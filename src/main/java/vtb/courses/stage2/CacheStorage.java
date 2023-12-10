package vtb.courses.stage2;

import lombok.Getter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.nanoTime;

public class CacheStorage {
    private final Map<Method, Map<String, TimedValue>> methodValues;

    public CacheStorage() {
        methodValues = new HashMap<>();
    }

    public boolean containsValue(Method method, String objectState) {
        return methodValues.containsKey(method) && methodValues.get(method).containsKey(objectState);
    }

    public void saveValue(Method method, String objectState, Object value, long ttl) {
        Map<String, TimedValue> stateMap;
        if (methodValues.containsKey(method)) {
            stateMap = methodValues.get(method);
        } else {
            methodValues.put(method, (stateMap = new HashMap<>()));
        }
        stateMap.put(objectState, new TimedValue(value, ttl));
    }

    public Object getCachedValue(Method method, String objectState) throws IllegalArgumentException {
        if (containsValue(method, objectState)) {
            TimedValue timedValue = methodValues.get(method).get(objectState);
            timedValue.setTime(nanoTime());
            return timedValue.getValue();
        } else {
            throw new IllegalArgumentException();
        }
    }
}


class TimedValue {
    @Getter
    @lombok.Setter
    private long time;
    @Getter @lombok.Setter
    private Object value;
    long ttl;
    boolean toDelete;
    public TimedValue(Object value, long ttl) {
        this.time = nanoTime();
        this.value = value;
        this.ttl = ttl;
        this.toDelete = false;
    }
}
