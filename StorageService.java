package com.spectocor.micor.android.storage;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.spectocor.micor.dummysender.Chunk;
import com.spectocor.micor.dummysender.SendingThread;
import com.spectocor.micor.dummytransfer.SendToServer;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// TODO: GC_FOR_ALLOC - need fix

public class StorageService extends Service {

    public static Queue<Chunk> queue = new ConcurrentLinkedQueue<>();
    public static boolean serviceOn = false;

    private static SendingThread t1;
    private static SendToServer t2;
    private static StorageThread t3;

    /**
     * Class used for the client Binder.
     */
    public class LocalBinder extends Binder {
        public StorageService getService() {
            return StorageService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    public static void log(String str) {
        Log.d("StorageService", str);
    }

    /**
     * Called by the system every time a client explicitly starts the service.
     *
     * @param intent  from the MainActivity.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to start. Use with stopSelfResult(int).
     * @return The return value indicates what semantics the system should use for the service's current started state.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            serviceOn = true;
            log("service started...");

            // thread to generate chunks
            t1 = new SendingThread(queue);
            t1.start();

            // thread to send chunks to server
            t2 = new SendToServer();
            t2.start();

            // TODO: storage thread
            // database location: INTERNAL / EXTERNAL
            // database path (if external): any String
            // encryption mode: AES256 / AES128/ DES
            // queue<Chunk> filled by the dummy sender
            t3 = new StorageThread("EXTERNAL", Environment.getExternalStorageDirectory() + File.separator + "Database" + File.separator, "AES256", queue, getApplicationContext());
            t3.start();

            log("START_STICKY");
            return START_STICKY;
        }
        log("killed");
        stopSelf();
        return START_NOT_STICKY;
    }

    /**
     * Called by the sender to notify StorageThread to poll data.
     */
    public static void processQueue() {
        t3.resume();
    }

    /**
     * Called when the first service is bound.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return The IBinder through which clients can call on to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        log("entering onBind()");
        return mBinder;
    }

    /**
     * Called when all clients have disconnected.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return true if you would like to have the service's onRebind(Intent) method later called when new clients bind to it.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        log("entering onUnbind()");
        return true;
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.
     */
    @Override
    public void onDestroy() {
        serviceOn = false;
        super.onDestroy();
    }
}
