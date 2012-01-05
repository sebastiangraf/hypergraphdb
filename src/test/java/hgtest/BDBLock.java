package hgtest;

import java.io.File;
import java.util.concurrent.locks.Lock;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class BDBLock {
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

            byte[] lockObject = new byte[] { 1, 2, 3, 4, 5 };

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