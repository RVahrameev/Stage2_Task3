package vtb.courses.stage2;

import lombok.Getter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.lang.System;

import static java.lang.System.nanoTime;

/**
 *  Класс <b>CacheInvocationHandler</b> перехватывает вызовы методов интерфейса T прокси-объекта
 *  Реализует логику кэширования вызовов методов помеченных аннотацией <b>@Cache</b>
 *  Делает перевызов интерфейсных методов, помеченных аннотацией <b>@Cache</b>, исходного объекта <b>cachableObject</b>
 *  только в том случае, если состояние объекта было изменено или объект находся в исходном состоянии.
 *  Изменение объекта определяется фиксацией вызовов интерфейсных методов помеченных аннотацией <b>@Setter</b>.
 *  Остальные интерфейсный методы перевызываются на исходном объекте без изменения логики работы.
 *  <p>
 *  Для того чтобы задать отслеживаемый объект, используется метод <b>cache()</b>
 *  <p>
 *  Идея позаимствована тут <a href="https://javarush.com/groups/posts/2281-dinamicheskie-proksi">https://javarush.com/groups/posts/2281-dinamicheskie-proksi</a>
 */
public class CacheInvocationHandler<T> implements InvocationHandler {
    private T cachedObject;
    private boolean cachedObjectChanged;
    private final HashMap<Method, HashMap<String, TimedValue>> lastValues;
    private final HashMap<Method, Method> methodMap;

    private static CacheCleaner cacheCleaner;

    public CacheInvocationHandler() {
        methodMap = new HashMap<>();
        lastValues = new HashMap<>();
        if (cacheCleaner == null) cacheCleaner = new CacheCleaner();
    }

    public T cache(T object) {
        this.cachedObject = object;
        // Первоначальное состояние = "Изменён", т.к. первый вызов метода обязательно должено отработать
        this.cachedObjectChanged = true;

        return (T) Proxy.newProxyInstance(
                object.getClass().getClassLoader(),
                object.getClass().getInterfaces(),
                this);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method objectMethod = getCachedObjectMethod(method);
        if (objectMethod != null) {
            if (objectMethod.isAnnotationPresent(Cache.class)) {
                System.out.println(objectMethod.getAnnotation(Cache.class).value());
                // если объект не менялся, пытаемся достать значение из кэша
                if (!cachedObjectChanged) {
                    String objectState = cachedObject.toString();
                    if (lastValues.containsKey(method) && lastValues.get(method).containsKey(objectState)) {
                        System.out.println("Cached object not changed, skip method " + method.getName() + " call!");
                        return getLastValue(method, objectState);
                    }
                }
                // в противном случае вызываем исходный метод и кешируем результат
                cachedObjectChanged = false;
                Object lastValue = method.invoke(cachedObject, args);
                saveValue(method, cachedObject.toString(), lastValue);
                return lastValue;
            } else if (objectMethod.isAnnotationPresent(Setter.class)) {
                System.out.println("Object state start to change!");
                cachedObjectChanged = true;
            }
            // Если дошли до этой точки, то просто вызываем на проксируемом объекте перехваченный метод
            System.out.println("Call native object method " + method.getName());
            return objectMethod.invoke(cachedObject, args);
        }
        return  null;
    }

    private void saveValue(Method method, String objectState, Object value) {
        if (!lastValues.containsKey(method)) {
            lastValues.put(method, new HashMap<>());
        }
        lastValues.get(method).put(objectState, new TimedValue(value));
    }

    /**
    * getCachedObjectMethod - по методу method прокси объекта,
    * возвращает соответствующий ему метод проксируемого объекта
     * */
    private Method getCachedObjectMethod(Method method) {
        Method objectMethod = null;
        // Чтобы каждый раз не заниматься сложными поисками соответствующего метода в проксируемом объекте
        // строим мапу методов прокси и проксируемого
        if (!methodMap.containsKey(method)) {
            try {
                objectMethod = cachedObject.getClass().getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {}
            methodMap.put(method, objectMethod);
        } else {
            objectMethod = methodMap.get(method);
        }
        return objectMethod;
    }

    private Object getLastValue(Method method, String objectState) {
        TimedValue timedValue = lastValues.get(method).get(objectState);
        timedValue.setTime(nanoTime());
        return timedValue.getValue();
    }
}

class TimedValue {
    @Getter @lombok.Setter
    long time;
    @Getter @lombok.Setter
    Object value;
    public TimedValue(Object value) {
        this.time = nanoTime();
        this.value = value;
    }
}

