package vtb.courses.stage2;

import java.util.HashSet;
import java.util.Set;

import static java.lang.System.nanoTime;

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
            // запускаем процесс очистки кеша
            cacheStorages.iterator().forEachRemaining(this::clearCache);
            System.out.println("Thread sleep " + minTtl);
            // спим до начала следующей итерации
            try {
                if (minTtl == Long.MAX_VALUE) {
                    Thread.sleep(1000);
                } else {
                    Thread.sleep(minTtl);
                }
            } catch (InterruptedException e) {
                break;
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
