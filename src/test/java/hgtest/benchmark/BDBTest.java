package hgtest.benchmark;

import java.io.File;
import java.util.UUID;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentStats;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;

public class BDBTest {
    private static Database openDb(String name, Environment env)
            throws Exception {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(true);
        Database db = env.openDatabase(null, name, dbConfig);
        return db;
    }

    private static void writeTo(Database db, Transaction tx) throws Exception {
        DatabaseEntry dbkey = new DatabaseEntry(UUID.randomUUID().toString()
                .getBytes());
        DatabaseEntry dbvalue = new DatabaseEntry(UUID.randomUUID().toString()
                .getBytes());
        db.put(tx, dbkey, dbvalue);
    }

    public static void main(String[] argv) {
        String dbLocation = "/tmp/bdbtest";

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);

        envConfig.setCacheSize(1024 * 1024 * 20);

        envConfig.setTransactional(true);
        // envConfig.setMultiversion(true);
        // envConfig.setTxnSnapshot(true);
        // envConfig.setTxnWriteNoSync(true);
        envConfig.setTxnNoSync(true);

        // envConfig.setMaxLockers(2000);
        // envConfig.setMaxLockObjects(20000);
        // envConfig.setMaxLocks(20000);
        System.out.println("cache=" + envConfig.getCacheSize());
        File envDir = new File(dbLocation);
        envDir.mkdirs();
        try {
            Environment env = new Environment(envDir, envConfig);

            Database sd = openDb("sd", env);
            Database sp = openDb("sp", env);
            Database tdb = openDb("tdb", env);
            Database vdb = openDb("vdb", env);
            Database idb = openDb("idb", env);
            Database pdb = openDb("pdb", env);

            long start = System.currentTimeMillis();
            for (int i = 0; i < 20000; i++) {
                Transaction tx = env.beginTransaction(null,
                        new TransactionConfig());
                writeTo(sd, tx);
                writeTo(sd, tx);
                writeTo(sp, tx);
                writeTo(sp, tx);
                writeTo(tdb, tx);
                writeTo(tdb, tx);
                writeTo(vdb, tx);
                writeTo(vdb, tx);
                writeTo(idb, tx);
                writeTo(pdb, tx);
                writeTo(pdb, tx);
                if (i % 1000 == 0)
                    System.out.println("At " + i + " -- "
                            + (System.currentTimeMillis() - start));
                tx.commit();
                // env.checkpoint(null);
                // System.out.println(i);
            }
            System.out.println("Done: " + (System.currentTimeMillis() - start));
            sd.close();
            sp.close();
            tdb.close();
            vdb.close();
            idb.close();
            pdb.close();
            env.checkpoint(null);
            EnvironmentStats stats = env.getStats(null);
            System.out.println("Cache stats: " + stats);
            env.close();
            System.out.println("Env closed/committed: "
                    + (System.currentTimeMillis() - start));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }
}
