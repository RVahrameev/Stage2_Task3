package vtb.courses.stage2;

import java.util.HashSet;
import java.util.Set;

import static java.lang.System.nanoTime;

/** Класс CacheCleaner собственно реализует поток сборки мусора
 *  Поток автоматически запускается при создании объекта
 *  Стратегия сборки мусора - интеравальная сборка,
 *  т.к. нет ни каких оснований строить предположения об интенсивности использования кэша
 *  Длина интервала определяется как минимальное время жизни объектов в очищаемых кэшах
 *  При этом каждый из очищаемых кешей чистится не чаще чем минимальное время жизни его объектов
 */

public class CacheCleaner extends Thread {
    private final Set<CacheStorageItem> cacheStorages;

    // здесь будем расчитывать минимальный период запуска цикла очистки
    private long minTtl = Long.MAX_VALUE;

    public CacheCleaner() {
        cacheStorages = new HashSet<>();
        this.start();
    }

    public void addCacheStorage(CacheStorage cacheStorage) {
        cacheStorages.add(new CacheStorageItem(cacheStorage));
        minTtl = Long.min(minTtl, cacheStorage.getMinTtl());
    }

    private void clearCache(CacheStorageItem cacheStorageItem) {
        minTtl = Long.min(minTtl, cacheStorageItem.cacheStorage.getMinTtl());
        if (nanoTime() - cacheStorageItem.lastProcessed > 1000000L * cacheStorageItem.cacheStorage.getMinTtl()) {
            cacheStorageItem.cacheStorage.clearTimeoutedValues();
            cacheStorageItem.lastProcessed = nanoTime();
        }
    }

    @Override
    public void run() {
        while (true) {
            // запускаем процесс очистки кеша и фиксируем его длительность
            long startClearing = nanoTime();
            cacheStorages.iterator().forEachRemaining(this::clearCache);

            // определяем сколько будем спать, с корректировкой на время затраченное на очистку
            long timeToSleep;
            if (minTtl == Long.MAX_VALUE) {
                timeToSleep = 1000;
            } else {
                timeToSleep = minTtl;
            }
            timeToSleep = timeToSleep - (nanoTime() - startClearing)/1000000L;

            // спим до начала следующей итерации, если осталось время
            if (timeToSleep > 0) {
                System.out.println("Thread sleep " + timeToSleep);
                try {
                    Thread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    class CacheStorageItem {
        private final CacheStorage cacheStorage;
        private long lastProcessed;

        public CacheStorageItem(CacheStorage cacheStorage) {
            this.cacheStorage = cacheStorage;
            this.lastProcessed = nanoTime();
        }
    }
}
