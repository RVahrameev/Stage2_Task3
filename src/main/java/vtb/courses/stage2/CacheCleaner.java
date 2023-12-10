package vtb.courses.stage2;

import java.util.HashSet;
import java.util.Set;

import static java.lang.System.nanoTime;

public class CacheCleaner extends Thread {
    private Set<CacheStorageItem> cacheStorages;
    // здесь будем расчитывать период запуска очистки
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
        if (nanoTime() - cacheStorageItem.lastProcessed > cacheStorageItem.cacheStorage.getMinTtl()) {
            cacheStorageItem.cacheStorage.clearTimeoutedValues();
        }
    }
    @Override
    public void run() {
        while (true) {
            cacheStorages.iterator().forEachRemaining(x -> clearCache(x));
            try {
                if (minTtl == Long.MAX_VALUE) {
                    Thread.sleep(1000);
                }
                Thread.sleep(minTtl);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    class CacheStorageItem {
        CacheStorage cacheStorage;
        long lastProcessed;

        public CacheStorageItem(CacheStorage cacheStorage) {
            this.cacheStorage = cacheStorage;
            this.lastProcessed = nanoTime();
        }
    }
}
