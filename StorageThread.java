package com.spectocor.micor.android.storage;

import android.content.Context;
import android.util.Log;

import com.spectocor.micor.dummysender.Chunk;
import com.spectocor.micor.core.encryption.Aes128Cbc;
import com.spectocor.micor.core.encryption.Aes256Cbc;
import com.spectocor.micor.core.encryption.Des;
import com.spectocor.micor.core.encryption.Encryptor;
import com.spectocor.micor.sync.ChunkInfo;
import com.spectocor.micor.sync.QueueHandler;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Queue;

public class StorageThread implements Runnable {
    private Queue<Chunk> queue;
    private String db_mode;
    private String db_path;
    private String encryption_mode;
    private Context context;

    private Thread thread;
    private static boolean suspended;
    private final Object monitor = new Object();

    private ChunkDbOpenHelper helper;
    private Encryptor<Encryptor.CipherTextIv> aes256Encryptor = new Aes256Cbc();
    private Encryptor<Encryptor.CipherTextIv> aes128Encryptor = new Aes128Cbc();
    private Encryptor<byte[]> desEncryptor = new Des();
    QueueHandler queueHandler = new QueueHandler();

    /**
     * The constructor.
     *
     * @param q The queue of chunks.
     */
    public StorageThread(String db_m, String db_p, String enc_m, Queue<Chunk> q, Context ctxt) {

        this.db_mode = db_m;
        this.db_path = db_p;
        this.encryption_mode = enc_m;
        this.queue = q;
        this.context = ctxt;
    }

    /**
     * The logger.
     *
     * @param str String to be logged.
     */
    public static void log(String str) {
        Log.d("StorageThread", str);
    }

    /**
     * The function to start this thread.
     */
    public void start() {
        this.thread = new Thread(this, "StorageThread");
        this.thread.start();
    }

    /**
     * Retrieve chunks from the queue after being notified by the service (notified by the sender).
     * Call the storage function afterwards.
     */
    @Override
    public void run() {
        suspended = false;

        ChunkDbOpenHelper.switchDbMode(this.db_mode, this.db_path);
        this.helper = new ChunkDbOpenHelper(this.context);

        try {
            while (true) {
                synchronized (monitor) {
                    while (suspended) {
                        log("suspended");
                        monitor.wait();
                    }
                }
                for (int i = 0; i < queue.size(); i++) {
                    Chunk chunk = queue.poll();
                    store(chunk);
                }
                suspend();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log("exiting");
    }

    /**
     * The function for storage to CHUNCK_DB and queues.
     *
     * @param ch The chunk to be stored
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void store(Chunk ch) throws GeneralSecurityException, IOException, ClassNotFoundException {
        log("storing...");

        byte[] blob = ch.getChunkBlob();
        byte[] encryptedBlob = doEncryption(this.encryption_mode, blob);
        ch.setChunkBlob(encryptedBlob);
        this.helper.addChunk(ch);

        ChunkInfo chunkInfo = new ChunkInfo(ch.getChunkId(), ch.getChunkStartTime());
        queueHandler.enqueue(chunkInfo);

//        // test: print part of the retrieved blob data
//        aes256Encryptor.generateKey();
//        byte[] encryptedData = this.helper.findBlobById(ch.getChunkId());
//        byte[] plainText = aes256Encryptor.decrypt(encryptedData);
//        ByteArrayInputStream bIn = new ByteArrayInputStream(plainText);
//        ObjectInputStream oIn = new ObjectInputStream(bIn);
//        Object obj = oIn.readObject();
//        bIn.close();
//        oIn.close();
//        Signal[] sigs;
//        if (obj instanceof Signal[]) {
//            sigs = (Signal[]) obj;
//            System.out.println("======decrypted signal[]========");
//            System.out.println(sigs[0].getData() + " " + sigs[0].getTs() + " " + sigs[sigs.length - 1].getData() + " " + sigs[sigs.length - 1].getTs());
//        } else {
//            log("retrieval from db error");
//        }

        log("stored chunk " + ch.getChunkId());
    }

    private byte[] doEncryption(String mode, byte[] contentBytes) throws GeneralSecurityException, IOException, ClassNotFoundException {
        log("encrypting in " + mode + "...");
        switch (mode) {
            case "AES256": {
                aes256Encryptor.generateKey();
                byte[] encryptedData = aes256Encryptor.encrypt(contentBytes);

                return encryptedData;
            }
            case "AES128": {
                aes128Encryptor.generateKey();
                byte[] encryptedData = aes128Encryptor.encrypt(contentBytes);

                return encryptedData;
            }
            case "DES": {
                desEncryptor.generateKey();
                byte[] encryptedData = desEncryptor.encrypt(contentBytes);

                return encryptedData;
            }
            default:
                break;
        }
        return null;
    }

    /**
     * The function to pause this thread if it's running.
     */
    public void suspend() {
        synchronized (monitor) {
            suspended = true;
            monitor.notifyAll();
        }
    }

    /**
     * The function to resume this thread if it's paused.
     */
    public void resume() {
        log("resuming");
        synchronized (monitor) {
            suspended = false;
            monitor.notifyAll();
        }
    }

}
