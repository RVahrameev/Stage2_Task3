package vtb.courses.stage2;

import lombok.Getter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.lang.System;
import java.util.Map;

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
    private final CacheStorage lastValues;
    private final Map<Method, Method> methodMap;

    private static CacheCleaner cacheCleaner;

    public CacheInvocationHandler() {
        methodMap = new HashMap<>();
        lastValues = new CacheStorage();
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
                Object lastValue;
                // если объект не менялся, пытаемся достать значение из кэша
                if (!cachedObjectChanged) {
                    try {
                        lastValue = lastValues.getCachedValue(method, cachedObject.toString());
                        System.out.println("Cached object not changed, skip method " + method.getName() + " call!");
                        return lastValue;
                    } catch (IllegalArgumentException e) {}
                }
                // в противном случае вызываем исходный метод и кешируем результат
                cachedObjectChanged = false;
                lastValue = method.invoke(cachedObject, args);
                lastValues.saveValue(method, cachedObject.toString(), lastValue);
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
}

