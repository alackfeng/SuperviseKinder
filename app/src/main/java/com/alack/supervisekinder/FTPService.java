package com.alack.supervisekinder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.alack.supervisekinder.utils.Const;
import com.alack.supervisekinder.utils.LogManager;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * Created by Administrator on 2017/3/9.
 */

public class FTPService extends Service {
    private static final String     TAG = "FTPService";
    private LogManager              Log = LogManager.getInstance();

    @SuppressWarnings("FieldCanBeLocal")
    private String                  mHost       = "192.168.106.207";
    @SuppressWarnings("FieldCanBeLocal")
    private int                     mPort       = 21;
    @SuppressWarnings("FieldCanBeLocal")
    private String                  mUsername   = "Administrator";
    @SuppressWarnings("FieldCanBeLocal")
    private String                  mPassword   = "taurus";

    @SuppressWarnings("FieldCanBeLocal")
    private String                  ftpRoot     = "ftp/";
    private FTPClient               ftpClient   = null;
    private String                  mImgPath     = null;

    private boolean                 bConnected  = false;
    private boolean                 bUploadNow  = false;

    @SuppressWarnings("FieldCanBeLocal")
    private int                     mFileCount  = 5;
    @SuppressWarnings("FieldCanBeLocal")
    private int                     mTryCount   = 10;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()...thread id : " + Thread.currentThread().getId());
        mImgPath = Const.getExternalLocaldir() + "/upload/";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy() :");
        try {
            if (ftpClient != null) {
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "onDestroy() - " + e.toString());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand() intent action:" + intent.getAction());

        // connect to ftp server
        if(intent.getAction().equals(Const.getTaskUploadfileStart())) {
            Log.i(TAG, "click connect_button");
            if(ftpClient == null || !bConnected) {
                try {
                    ftpConnect connect = new ftpConnect(mHost, mPort, mUsername, mPassword);
                    connect.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "ftpConnect start() - ERROR - " + e.toString());
                }

            }
            bUploadNow = true;
            if(selectUploadFile()) {
                Log.d(TAG, "Now upload !!!");
            } else {
                Log.d(TAG, "NOT FOUND upload file!!!");
            }
        } else if(intent.getAction().equals(Const.getTaskUploadfileStop())) {
            Log.i(TAG, "getTask Upload fileStop()");
        }

        return START_STICKY;
    }

    public boolean selectUploadFile() {
        Log.i(TAG, "selectUploadFile() select path - " + mImgPath);
        File path = new File(mImgPath);
        if(!path.exists()) {
            Log.e(TAG, mImgPath + " - path is not,why???");
            return false;
        }
        File[] files = path.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String f = pathname.getName();
                //Log.d(TAG, "file name :" + f);
                return !pathname.isDirectory() && f.endsWith(".mp4");
            }
        });
        if(files.length <= 0) {
            Log.e(TAG, mImgPath + " - path not files");
            return false;
        }
        uploadFile(files, mFileCount);
        return true;
    }

    public void uploadFile(File[] filePath, int max) {
        // upload file
        Log.i(TAG, "click upload_button");
        try {
            ftpUpload upload = new ftpUpload(filePath, max);
            upload.start();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "ftpUpload uploadFile() ERROR - " + e.toString());
        }

    }

    class ftpConnect extends Thread {
        private String host;
        private int port;
        private String username;
        private String password;

        public ftpConnect(String host, int port, String username, String password) {
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        @Override
        public void run() {
            ftpClient = new FTPClient();
            try {
                Log.d(TAG, "ftpClient connecting... now");
                ftpClient.connect(this.host, this.port);
                ftpClient.login(this.username, this.password);
                int reply = ftpClient.getReplyCode();
                Log.d(TAG, "ftpClient login... reply: " + reply);
                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftpClient.disconnect();
                    Log.e(TAG, "ftp login failed!");
                    bConnected = false;
                } else {
                    Log.i(TAG, "ftp login successfully!");
                    bConnected = true;
                }
                ftpClient.changeWorkingDirectory(ftpRoot);
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            } catch (Exception e) {
                //e.printStackTrace();
                Log.d(TAG, "FTPClient Connect ERROR: " + e.toString());
                bConnected = false;
                bUploadNow = false; // close ftpUpload run()
                ftpClient = null;
            }
        }
    }

    class ftpUpload extends Thread {
        FileInputStream fis;
        int mMax = 10;
        File[] mFiles;
        int mLen = 0;

        public ftpUpload(File[] files, int max) {
            this.mMax = max;
            this.mFiles = files;
        }

        @Override
        public void run() {
            try {
                Log.d(TAG, "ftpUpload Thread :" + Thread.currentThread().getId() + ", state :" + bUploadNow);
                mTryCount = 10;
                while(bUploadNow) {
                    if (ftpClient == null || !bConnected) {
                        Thread.sleep(1000);
                        if(0 < mTryCount--) {
                            Log.d(TAG, "ftpUpload Thread : try again - " + mTryCount);
                            continue;
                        } else {
                            Log.d(TAG, "ftpUpload Thread : try again failed- " + mTryCount);
                            bUploadNow = false;
                            continue;
                        }
                    }
                    mLen = mFiles.length < mMax ? mFiles.length : mMax;
                    Log.d(TAG, "ftpUpload Thread : up file success at count - " + mLen);

                    for(int i=0; i<mLen; i++) {
                        File f = mFiles[i];
                        if(!f.exists()) {
                            continue;
                        }
                        //
                        Log.d(TAG, "Upload file <" + f.getPath() + ">(" + f.getName() + ")");
                        boolean bErr = false;
                        try {
                            this.fis = new FileInputStream(f.getPath());
                            ftpClient.storeFile(f.getName(), fis);
                            int reply = ftpClient.getReplyCode();
                            if (!FTPReply.isPositiveCompletion(reply)) {
                                Log.e(TAG, "upload failed!");
                            } else {
                                Log.i(TAG, "upload successfully!");
                                //noinspection ResultOfMethodCallIgnored
                                f.delete();
                            }
                        } catch (FileNotFoundException e) {
                            Log.e(TAG, "ftpUpload - FileNotFoundException ERROR - " + e.toString());
                            e.printStackTrace();
                            bErr = true;
                        } catch (IOException e) {
                            Log.e(TAG, "ftpUpload - IOException ERROR - " + e.toString());
                            e.printStackTrace();
                            bErr = true;
                        } catch ( Exception e) {
                            Log.e(TAG, "ftpUpload - Exception ERROR - " + e.toString());
                            e.printStackTrace();
                            bErr = true;
                        }
                        if(fis != null)
                            fis.close();
                        if(bErr)
                            break;
                    }
                    bUploadNow = false;
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                bUploadNow = false;
                try {
                    if (ftpClient != null) {
                        ftpClient.disconnect();
                        ftpClient = null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "ftpUpload - ftpClient disconnect ERROR - " + e.toString());
                }
            }
        }
    }
}
