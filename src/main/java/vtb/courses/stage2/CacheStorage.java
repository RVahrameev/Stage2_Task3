package vtb.courses.stage2;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.nanoTime;

/** Класс CacheStorage собственно реализует кеш
 * <p>   saveValue - помещает новое значение в кеш
 * <p>   getCachedValue - извлекает значение из кеша
 * <p>   clearTimeoutedValues - чистит кеш. вызывается сборщиком мусора
 */

public class CacheStorage {
    private final Map<Method, Map<String, TimedValue>> methodValues;
    @Getter
    private long minTtl = Long.MAX_VALUE;

    public CacheStorage() {
        methodValues = new HashMap<>();
    }

    // Помещает новое значение в кэш
    public void saveValue(Method method, String objectState, Object value, long ttl) {
        System.out.println("saveValue " + objectState + " - " + value + " - " + ttl);
        Map<String, TimedValue> stateMap;
        stateMap = methodValues.get(method);
        if (stateMap == null){
            stateMap = new ConcurrentHashMap<>();
        }
        methodValues.put(method, stateMap);
        stateMap.put(objectState, new TimedValue(value, ttl));
        if (ttl != 0) {
            minTtl = Long.min(minTtl, ttl);
        }

    }

    public Object getCachedValue(Method method, String objectState) throws IllegalArgumentException {
        Map<String, TimedValue> stateCache;
        TimedValue timedValue;
        if ((stateCache = methodValues.get(method)) != null &&
                (timedValue = stateCache.get(objectState)) != null)
        {
            timedValue.setTime(nanoTime());
            return timedValue.getValue();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void clearTimeoutedValues() {
        Map<String, TimedValue> stateMap;
        for (Method method: methodValues.keySet()) {
            for (String state: (stateMap = methodValues.get(method)).keySet()) {
                TimedValue timedValue = stateMap.get(state);
                if (timedValue.getTtl() != 0 && (nanoTime() - timedValue.getTime() > 1000000L * timedValue.getTtl())) {
                    stateMap.remove(state);
                }
            }
        }
    }

    class TimedValue {
        @Getter @Setter
        private long time;
        @Getter
        private final Object value;
        @Getter
        private final long ttl;
        boolean toDelete;
        public TimedValue(Object value, long ttl) {
            this.time = nanoTime();
            this.value = value;
            this.ttl = ttl;
            this.toDelete = false;
        }
    }
}


