package hgtest;

import java.io.File;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;

public class BDBTx {
    static boolean cmp(byte[] A, byte[] B) {
        if (A == null)
            return B == null;
        else if (B == null || A.length != B.length)
            return false;
        else
            for (int i = 0; i < A.length; i++) {
                if (A[i] != B[i])
                    return false;
            }
        return true;
    }

    static void checkData(Transaction tx, Database db, DatabaseEntry key,
            byte[] V) {
        DatabaseEntry data = new DatabaseEntry();
        try {
            if (db.get(tx, key, data, LockMode.DEFAULT) != OperationStatus.SUCCESS)
                throw new RuntimeException("No data found for key.");
            else if (!cmp(data.getData(), V))
                throw new RuntimeException("Value different than expected.");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] argv) {
        String databaseLocation = "c:/tmp/dblock";

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);

        envConfig.setCacheSize(20 * 1014 * 1024);

        envConfig.setTransactional(true);
        envConfig.setTxnWriteNoSync(true);

        File envDir = new File(databaseLocation);
        envDir.mkdirs();

        Environment env = null;
        int lockerId = 0;
        try {
            env = new Environment(envDir, envConfig);
            /*
             * lockerId = env.createLockerID(); byte [] lockObject = new byte[]
             * { 1, 2, 3, 4, 5 }; Lock lock = env.getLock(lockerId, false, new
             * DatabaseEntry(lockObject), LockRequestMode.READ);
             * System.out.println("Go lock " + lock);
             */

            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setAllowCreate(true);
            if (env.getConfig().getTransactional())
                dbConfig.setTransactional(true);

            Database db = env.openDatabase(null, "testdb", dbConfig);

            DatabaseEntry key = new DatabaseEntry(new byte[] { 1, 2, 3, 4, 5,
                    6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 });
            byte[] value0 = { 0, 1, 2 };

            Transaction tx = env
                    .beginTransaction(null, new TransactionConfig());
            db.put(null, key, new DatabaseEntry(value0));
            tx.commit();

            tx = env.beginTransaction(null, new TransactionConfig());
            DatabaseEntry data = new DatabaseEntry();

            checkData(tx, db, key, value0);

            byte[] value1 = { 23, 24, 23 };
            db.put(tx, key, new DatabaseEntry(value1));
            checkData(tx, db, key, value1);

            byte[] value2 = { 12, 4, 56, 7 };
            db.put(tx, key, new DatabaseEntry(value2));
            checkData(tx, db, key, value2);

            byte[] value3 = { 3, 2, 3, 45, 56, };
            db.put(tx, key, new DatabaseEntry(value3));
            checkData(tx, db, key, value3);

            tx.commit();

            tx = env.beginTransaction(null, new TransactionConfig());

            checkData(tx, db, key, value3);

            value1 = new byte[] { 23, 24, 23 };
            db.put(tx, key, new DatabaseEntry(value1));
            checkData(tx, db, key, value1);

            value2 = new byte[] { 12, 4, 56, 7 };
            db.put(tx, key, new DatabaseEntry(value2));
            checkData(tx, db, key, value2);

            value3 = new byte[] { 3, 2, 3, 45, 56, };
            db.put(tx, key, new DatabaseEntry(value3));
            checkData(tx, db, key, value3);

            tx.commit();

            db.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                env.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}