package org.hypergraphdb.storage;

import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.EnvironmentConfig;

public class BDBConfig {
    public static final long DEFAULT_STORE_CACHE = 20 * 1024 * 1024; // 20MB
    public static final int DEFAULT_NUMBER_OF_STORAGE_CACHES = 1;

    private EnvironmentConfig envConfig;
    private DatabaseConfig dbConfig;
    private boolean storageMVCC = true;

    private void resetDefaults() {
        envConfig.setAllowCreate(true);
        envConfig.setCacheSize(DEFAULT_STORE_CACHE);

        dbConfig.setAllowCreate(true);
    }

    public BDBConfig() {
        envConfig = new EnvironmentConfig();
        dbConfig = new DatabaseConfig();
        resetDefaults();
    }

    public EnvironmentConfig getEnvironmentConfig() {
        return envConfig;
    }

    public DatabaseConfig getDatabaseConfig() {
        return dbConfig;
    }

    public void configureTransactional() {
        envConfig.setTransactional(true);

        envConfig.setTxnWriteNoSync(true);
        dbConfig.setTransactional(true);
    }

    public boolean isStorageMVCC() {
        return storageMVCC;
    }

    public void setStorageMVCC(boolean storageMVCC) {
        this.storageMVCC = storageMVCC;
    }
}