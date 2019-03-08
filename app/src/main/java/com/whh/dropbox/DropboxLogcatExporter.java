package com.whh.dropbox;

import android.content.Context;
import android.os.DropBoxManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Provides an integrated dropbox logcat output
 */
public class DropboxLogcatExporter {

    private volatile static DropboxLogcatExporter sInstance;

    private static final String TAG = DropboxLogcatExporter.class.getSimpleName();

    private DropboxLogcatExporter() {
    }

    public static DropboxLogcatExporter getInstance() {
        if (sInstance == null) {
            synchronized (DropboxLogcatExporter.class) {
                if (sInstance == null) {
                    sInstance = new DropboxLogcatExporter();
                }
            }
        }
        return sInstance;
    }

    /**
     * Outputs the dropbox logcat to the specified file or dir，donnot do this in the main thread
     * @param context nonnull context
     * @param pathname absolute path of file or directory for the logcat，if give a directory, default file name is "dropbox.log"
     * @param maxBytes of string to return with {@link DropBoxManager.Entry#getText(int)}
     * @return the absolute path of dropbox-logcat-file
     */
    public String output(@NonNull Context context, String pathname, int maxBytes) {
        //1、get dropbox manager
        DropBoxManager dropBoxManager = (DropBoxManager) context.getApplicationContext().getSystemService(Context.DROPBOX_SERVICE);
        if (dropBoxManager == null) {
            return null;
        }

        File file = new File(pathname);
        if (file.isDirectory()) {
            file = new File(pathname, "dropbox.log");
        }
        if (file.exists()) {
            file.delete();
        }
        file.getParentFile().mkdirs();
        try {
            if (!file.createNewFile()) {
                Log.e(TAG, "create file " + file.getAbsolutePath() + " failed");
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsolutePath(), true))){
            long startTime = 0L;
            DropBoxManager.Entry entry = null;
            //2、get entry by startTime
            while ((entry = dropBoxManager.getNextEntry(null, startTime)) != null) {
                //3、write the tag info and entry info into file
                writer.write(entry.getTag() + ":\r\n" + entry.getText(maxBytes) + "\r\n\r\n");
                //4、reset startTime for the next entry
                startTime = entry.getTimeMillis();
                //5、donnot forget to close the entry after you finished your task
                entry.close();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return file.getAbsolutePath();
    }

}
