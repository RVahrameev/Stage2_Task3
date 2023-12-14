package vtb.courses.stage2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

public class CachableTest {

    private Cachable proxyObject;

    public static class CachableClass implements Cachable{

        public int sourceData;
        public long calcData;
        public boolean cachableWasRunning;
        @Override
        @Cache(1000)
        public long cachableMethod() {
            cachableWasRunning = true;
            calcData = (long) sourceData * (long) sourceData;
            return calcData;
        }

        @Override
        @Mutator
        public void setterMethod(Object object){
            sourceData = (Integer)object;
        }

        @Override
        public void simpleMethod() {
            sourceData = sourceData / 2;
        }

        @Override
        public String toString() {
            return "CachableClass{" +
                    "sourceData=" + sourceData +
                    '}';
        }
    }

    @Test
    @DisplayName("Тестирование прокси класса и функционала кэширования")
    public void test(){
        System.out.println("Создаём объект, который будем кешировать. Т.е. создавать для него прокси");
        CachableClass cachableObject = new CachableClass();

        System.out.println("Сначала создаём прокси объект для нашего тестового кэшируемого объекта");
        Assertions.assertDoesNotThrow(() -> {proxyObject = new CacheInvocationHandler<Cachable>().cache(cachableObject);}, "Не удалось создать прокси для объекта cachableObject");

        System.out.println("Устанавливаем первоначальное значение, для проверки того что отработает первичный запуска кэшируемого метода");
        cachableObject.sourceData = 10;
        cachableObject.cachableWasRunning = false;

        System.out.println("Вызываем на прокси кэшируемый метод");
        Assertions.assertEquals(100, proxyObject.cachableMethod(), "Вызов cachableMethod 1 вернул не верное значение");

        System.out.println("Проверяем что кешируемый метод был вызван");
        Assertions.assertTrue(cachableObject.cachableWasRunning, "Вызов cachableMethod 1 не был произведён");

        System.out.println("Вызываем не кэшируемый метод, он должен отработать и поместить в sourceData - 5");
        Assertions.assertDoesNotThrow(() -> proxyObject.simpleMethod(), "Не удалось на прокси объекта вызвать не аннотированный метод simpleMethod");

        System.out.println("В результате вызова simpleMethod состояние объекта у нас изменилось, следовательно вызов кешируемого снова должен произойти");
        cachableObject.cachableWasRunning = false;
        Assertions.assertEquals(25, proxyObject.cachableMethod(), "Вызов cachableMethod 2 вернул не верное значение.");

        System.out.println("Проверяем что кешируемый метод был вызван");
        Assertions.assertTrue(cachableObject.cachableWasRunning, "Вызов cachableMethod 2 не был произведён");

        System.out.println("Меняем состояние на исходное значение, для проверки того что отработает кеш");
        cachableObject.sourceData = 10;
        cachableObject.cachableWasRunning = false;

        System.out.println("Вызываем на прокси кэшируемый метод. Должно вернуться значение из кеша");
        Assertions.assertEquals(100, proxyObject.cachableMethod(), "Вызов cachableMethod 3 вернул не верное значение. Кеш не сработал.");

        System.out.println("Проверяем что кешируемый метод НЕ был вызван");
        Assertions.assertFalse(cachableObject.cachableWasRunning, "Был совершен вызов cachableMethod 3. Кеш не сработал!");

        System.out.println("Устанавливаем новое значение sourceData через вызов setterMethod, которые считается меняющим состояние объекта");
        Assertions.assertDoesNotThrow(() -> proxyObject.setterMethod(20), "Не удалось на прокси объекта вызвать setterMethod(20)");

        System.out.println("Вызываем на прокси кэшируемый метод, в этот раз он должен отработать, т.к. ему предшествовал вызов setterMethod");
        cachableObject.cachableWasRunning = false;
        Assertions.assertEquals(400, proxyObject.cachableMethod(), "Вызов cachableMethod 4 вернул не верное значение.");

        System.out.println("Проверяем что кешируемый метод был вызван");
        Assertions.assertTrue(cachableObject.cachableWasRunning, "Вызов cachableMethod 4 не был произведён");

        System.out.println("Меняем состояние на одно из предыдущих, для проверки того что отработает кеш");
        cachableObject.sourceData = 5;
        cachableObject.cachableWasRunning = false;

        System.out.println("Вызываем на прокси кэшируемый метод, в этот раз он должен отработать, т.к. ему предшествовал вызов setterMethod");
        Assertions.assertEquals(25, proxyObject.cachableMethod(), "Вызов cachableMethod 5 вернул не верное значение.");

        System.out.println("Проверяем что кешируемый метод НЕ был вызван");
        Assertions.assertFalse(cachableObject.cachableWasRunning, "Был совершен вызов cachableMethod 5. Кеш не сработал!");

        System.out.println("Спим двойное время (2000), чтобы первое значение из кеша успело удалиться");
        try {
            Thread.sleep(2000);
        } catch (Exception e) {}

        System.out.println("Меняем состояние на исходное значение, которое уже должно удалиться, для проверки того что оно уже удалено из кеша");
        cachableObject.sourceData = 10;
        cachableObject.cachableWasRunning = false;

        System.out.println("Вызываем на прокси кэшируемый метод, в этот раз он должен отработать, т.к. в кеше значения уже нет");
        Assertions.assertEquals(100, proxyObject.cachableMethod(), "Вызов cachableMethod 6 вернул не верное значение.");

        System.out.println("Проверяем что кешируемый метод был вызван");
        Assertions.assertTrue(cachableObject.cachableWasRunning, "Вызов cachableMethod 5 не был произведён");

        System.out.println("Сразу же делаем повторный вызов, проверяем что теперь отработает кеш");
        cachableObject.cachableWasRunning = false;
        Assertions.assertEquals(100, proxyObject.cachableMethod(), "Вызов cachableMethod 7 вернул не верное значение.");

        System.out.println("Проверяем что кешируемый метод НЕ был вызван");
        Assertions.assertFalse(cachableObject.cachableWasRunning, "Был совершен вызов cachableMethod 5. Кеш не сработал!");
    }

    @Test
    @DisplayName("Объёмное тестирование массового создания и удаления")
    public void test2() {
        System.out.println("Создаём объект, который будем кешировать. Т.е. создавать для него прокси");
        CachableClass cachableObject = new CachableClass();

        System.out.println("Сначала создаём прокси объект для нашего тестового кэшируемого объекта");
        CacheInvocationHandler<Cachable> handler = new CacheInvocationHandler<>();
        Assertions.assertDoesNotThrow(() -> {proxyObject = handler.cache(cachableObject);}, "Не удалось создать прокси для объекта cachableObject");

        // Искуственно уменьшаем время задержки удаления
        cachableObject.sourceData = 1;
        proxyObject.cachableMethod();
        try {
            Field field = handler.getClass().getDeclaredField("methodMap");
            field.setAccessible(true);
            Map map = (Map)field.get(handler);
            for (Object hdl : map.values()) {
                Field ttl = hdl.getClass().getDeclaredField("cacheTTL");
                ttl.setAccessible(true);
                ttl.set(hdl, 100);
            }
        } catch (Exception e) {Assertions.fail("Не удалось установить время жизни кешируемых методов "+e.getMessage());}

        try {
            // Массовое порождение значений в кеше
            for (int i = 0; i <= 10000; i++) {
                cachableObject.sourceData = i;
                proxyObject.cachableMethod();
                // раз в 100 записей спим
                if ((i-1)%100 == 0)
                    Thread.sleep(10);
            }
            // Массовое вычитывание значений в кеше
            // Запускается встречным потоком к удалению элементов
            // Так что начиная с некоторого элемента снова начнёт формироваться кеш, так как прежний не долгоживущий кеш удалён
            // Таким образом корректность удаления кеша будет в том что начиная с некоторого момента, чтение из кеша прекратится
            boolean cacheDeleted = false;
            long prevRes = 100020001;
            for (int i = 10000; i > 0; i--) {
                cachableObject.sourceData = i;
                cachableObject.cachableWasRunning = false;
                long res = proxyObject.cachableMethod();
                // Проверяем корректность полученного значения
                Assertions.assertEquals(i+1+i, prevRes-res, "Для состояния "+i+" кешируемый метод вернул не верный ответ");
                prevRes = res;
                if (cachableObject.cachableWasRunning) {
                    cacheDeleted = true;
                }
                // с этого момента чтение кеша должно прекратиться
                if (cacheDeleted)
                    Assertions.assertTrue(cachableObject.cachableWasRunning, "Не корректно сработало удаление кеша, переход с кеша на некеш должен быть одноразовый");
                if ((i-1)%100 == 0)
                    Thread.sleep(10);
            }
            Assertions.assertTrue(cacheDeleted, "Сборщик мусора так ничего и не удалил из кэша");
        }
        catch (Exception e){}
    }
}
