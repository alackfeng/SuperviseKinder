package com.alack.supervisekinder;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;


import com.alack.supervisekinder.utils.Const;
import com.alack.supervisekinder.utils.LogManager;

import java.io.File;
import java.io.IOException;

/**
 * Created by Administrator on 2017/3/9.
 */

public class RecorderService extends Service {

    private static final String TAG     = "RecorderService";
    private LogManager          Log     = LogManager.getInstance();


    @SuppressWarnings("FieldCanBeLocal")
    private int          WIDTH        = 720, HEIGHT = 1080, FPS = 25, DENSITY_DPI = 480;
    @SuppressWarnings("FieldCanBeLocal")
    private int          BITRATE      = 3 * 1024 * 1024;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean      bRecAudio    = true;

    //private WindowManager       mWindowManager;
    private MediaProjection     mMediaProejction;
    private MediaRecorder       mMediaRecorder;
    private VirtualDisplay      mVirtualDisplay;
    private Intent              mData;
    private int                 mResult;
    private String              mOutputFile;

    private static boolean      bRunning            = false;

    // private FloatControlService mFloatControlSrv;
    // private boolean             bFloatingControls   = false;
    private boolean             bIsBound            = false;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean             bForeground         = true;


    public class RecorderBinder extends Binder {
        public RecorderService getRecorderService() {
            return RecorderService.this;
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "serviceConnection::onServiceConnected()");
            bIsBound = true;
            /*
            if(bFloatingControls) {
                FloatControlService.ServiceBinder binder = (FloatControlService.ServiceBinder)service;
                mFloatControlSrv = binder.getService();
                mFloatControlSrv.setRecordingState(Const.RecordingState.RECORDING);
            }*/
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "serviceConnection::onServiceDisconnected()");
            bIsBound = false;
            //if(bFloatingControls)
            //    mFloatControlSrv = null;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return new RecorderBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand() intent action: " + intent.getAction() + ", flags: " + flags);
        String action = intent.getAction();
        if(action.equals(Const.getScreenRecordingInit())) {

            try {
                Log.e(TAG, "Init Media getScreenRecordingInit - ");
                mData = intent.getParcelableExtra(Const.getRecorderIntentData());
                mResult = intent.getIntExtra(Const.getRecorderIntentResult(), Activity.RESULT_OK);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Init Media getScreenRecordingInit ERROR - " + e.toString());
            }

        } else if (action.equals(Const.getScreenRecordingStop())) {
            //TOAST("SCREEN_RECORDING_STOP!!!");
            Log.w(TAG, "SCREEN_RECORDING_STOP!!!");
            if (bIsBound) {
                unbindService(serviceConnection);
            }
            if(isRunning()) {
                stopRecorder();
            }
            else {
                Log.d(TAG, "SCREEN_RECORDING_STOP is not running...,No need Stop");
            }


        } else if(action.equals(Const.getScreenRecordingStart())) {
            //TOAST("SCREEN_RECORDING_START!!!");
            Log.w(TAG, "SCREEN_RECORDING_START!!!");
            if(!isRunning()) {
                if(mData == null || mResult != Activity.RESULT_OK) {
                    Log.e(TAG, "Media mData or mResult is not correct,why ?");
                    KinderApplication.getInstance().notfiyKeeplive(this, Intent.ACTION_MAIN);
                } else {
                    Log.d(TAG, "SCREEN_RECORDING_START is running...data:result - " + mData.toString() + ":" + mResult);
                    startRecorder();
                }
            } else {
                Log.d(TAG, "SCREEN_RECORDING_START is running...,No need Start");
            }
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()...thread id :" + Thread.currentThread().getId());
        //HandlerThread serviceThread = new HandlerThread("rec_service_thread", Process.THREAD_PRIORITY_BACKGROUND);
        //serviceThread.start();
        // background/
        KinderApplication.getInstance().addService(this);
        startForeGround(false);
        bRunning = false;
        mData = null;
        mResult = Activity.RESULT_CANCELED;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy().");
    }

    public boolean isRunning() {
        return bRunning;
    }

    public void getRecorderConfig() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager mWindowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        DENSITY_DPI = metrics.densityDpi;
        WIDTH       = 360; //metrics.widthPixels;
        HEIGHT      = 480; //metrics.heightPixels;
        mOutputFile = "Kinder_" + Const.getRecFileName() + ".mp4";

        Log.i(TAG, "Recorder Metrics : " + DENSITY_DPI + "(" + WIDTH + "*" + HEIGHT + "), File: " + mOutputFile);
    }

    public boolean startRecorder() {

        if(bRunning) {
            Log.i(TAG, "startRecorder() running..." + mMediaProejction);
            return false;
        }
        Log.i(TAG, "startRecorder()");
        try {

            getRecorderConfig();
            mMediaRecorder = new MediaRecorder();
            if(!initRecorder()) {
                throw new Exception("Init Recorder Error"); //return false;
            }

            MediaProjectionManager mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            mMediaProejction = mProjectionManager.getMediaProjection(mResult, mData);
            if(mMediaProejction == null) {
                Log.e(TAG, "mMediaProejction is NULL, why???");
            }

            createVirtualDisplay();

            /*
            if(bFloatingControls) {
                Intent floatControlIntent = new Intent(this, FloatControlService.class);
                startService(floatControlIntent);
                bindService(floatControlIntent, serviceConnection, BIND_AUTO_CREATE);
            } */

            mMediaRecorder.start();

            bRunning = true;



        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.e(TAG, "startRecorder() - ERROR -  " + e.toString());
            bRunning = false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startRecorder() - ERROR - : " + e.toString());
            bRunning = false;
        }

        return true;
    }

    public boolean stopRecorder() {

        if (!bRunning) {
            return false;
        }
        Log.i(TAG, "stopRecorder()");
        bRunning = false;
        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mVirtualDisplay.release();
            mMediaProejction.stop();

            // tmp file rename
            Const.fileRename(mOutputFile);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "stopRecorder() - " + e.toString());
        }
        return true;
    }

    private void createVirtualDisplay() {
        Log.i(TAG, "createVirtualDisplay()");
        mVirtualDisplay =  mMediaProejction.createVirtualDisplay("MainScreen",
                WIDTH, HEIGHT, DENSITY_DPI, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null, null);
    }

    private boolean initRecorder() {
        Log.i(TAG, "initRecorder()");
        try {

            if(bRecAudio)
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setOutputFile(getsaveDirectory() + mOutputFile + ".tmp");
            mMediaRecorder.setVideoSize(WIDTH, HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            if(bRecAudio)
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncodingBitRate(BITRATE);
            mMediaRecorder.setVideoFrameRate(FPS);

            mMediaRecorder.prepare();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "initRecorder() - ERROR - " + e.toString());
            KinderApplication.getInstance().startMain();
            return false;
        }
        return true;
    }

    public String getsaveDirectory() {
        String rootDir = Const.getExternalLocaldir() + "/";
        Log.i(TAG, "getsaveDirectory() - " + rootDir);

        File file = new File(rootDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return null;
            }
        }

        return rootDir;
    }

    private void startForeGround(boolean bFore) {

        if(!bForeground)
            return;

        Log.i(TAG, "startForeGround() :" + bFore);
        KinderApplication.getInstance().notfiyKeeplive(this, Const.getNotificationHideRecord());
    }

    private void TOAST(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}

