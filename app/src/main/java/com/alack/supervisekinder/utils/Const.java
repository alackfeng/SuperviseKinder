package com.alack.supervisekinder.utils;

import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Administrator on 2017/3/9.
 */

public class Const {
    static final int REQUEST_CODE_STORAGE               = 1000;
    static final int REQUEST_CODE_AUDIO                 = 1001;
    static final int REQUEST_CODE_SCREEN_RECORD         = 1003;
    static final int SCRENN_RECORD_NOTIFICATION_RECORD  = 1004;
    static final int SCRENN_RECORD_NOTIFICATION_MONITOR = 1005;
    static final int JOBSCHEDULER_SERVICE_ID            = 9999;


    // service foreground Notification
    static final String NOTIFICATION_HIDE_MONITOR       = "com.alack.supervisekinder.daemon.MonitorService";
    static final String NOTIFICATION_HIDE_RECORD        = "com.alack.supervisekinder.RecorderService";

    // wifi state
    static final String CONNECTIVITY_ACTION_START = "com.alack.supervisekinder.net.conn.CONNECTIVITY_CHANGE.start";
    static final String CONNECTIVITY_ACTION_END = "com.alack.supervisekinder.net.conn.CONNECTIVITY_CHANGE.end";

    // recording
    static final String SCREEN_RECORDING_START          = "com.alack.supervisekinder.services.action.startrecording";
    static final String SCREEN_RECORDING_STOP           = "com.alack.supervisekinder.services.action.stoprecording";
    static final String SCREEN_RECORDING_INIT           = "com.alack.supervisekinder.services.action.initrecording";

    // task item
    static final String TASK_UPLOADFILE_START           = "com.alack.supervisekinder.services.action.uploadfilestart";
    static final String TASK_UPLOADFILE_STOP            = "com.alack.supervisekinder.services.action.uploadfilestop";
    static final String TASK_EMPTY_NOTHING              = "com.alack.supervisekinder.services.action.nothing";
    static final String TASK_CHECK_MAINACTIVITY         = "com.alack.supervisekinder.services.action.mainactivitystart";


    // record path
    static final String SCREEN_RECORD_PATH              = "1ASuperviseKinder";

    // record media surface
    static final String RECORDER_INTENT_DATA            = "recorder_intent_data";
    static final String RECORDER_INTENT_RESULT          = "recorder_intent_result";


    // recording
    public static String getScreenRecordingStart() {
        return SCREEN_RECORDING_START;
    }
    public static String getScreenRecordingInit() {
        return SCREEN_RECORDING_INIT;
    }
    public static String getScreenRecordingStop() {
        return SCREEN_RECORDING_STOP;
    }


    public static int getJobschedulerServiceId() {
        return JOBSCHEDULER_SERVICE_ID;
    }
    public static int getRequestCodeAudio() {
        return REQUEST_CODE_AUDIO;
    }

    public static int getRequestCodeStorage() {
        return REQUEST_CODE_STORAGE;
    }
    public static int getRequestCodeScreenRecord() {
        return REQUEST_CODE_SCREEN_RECORD;
    }

    // record path
    public static String getScreenRecordPath() {
        return SCREEN_RECORD_PATH;
    }

    // record media surface
    public static String getRecorderIntentData() {
        return RECORDER_INTENT_DATA;
    }
    public static String getRecorderIntentResult() {
        return RECORDER_INTENT_RESULT;
    }

    // task item
    public static String getTaskUploadfileStart() {
        return TASK_UPLOADFILE_START;
    }
    public static String getTaskUploadfileStop() {
        return TASK_UPLOADFILE_STOP;
    }
    public static String getTaskEmptyNothing() {
        return TASK_EMPTY_NOTHING;
    }
    public static String getTaskCheckMainactivity() {
        return TASK_CHECK_MAINACTIVITY;
    }

    // service foreground Notification
    public static String getNotificationHideMonitor() {
        return NOTIFICATION_HIDE_MONITOR;
    }
    public static String getNotificationHideRecord() {
        return NOTIFICATION_HIDE_RECORD;
    }

    // wifi state
    public static String getConnectivityActionEnd() {
        return CONNECTIVITY_ACTION_END;
    }
    public static String getConnectivityActionStart() {
        return CONNECTIVITY_ACTION_START;
    }

    // service notifcation id
    public static int getScrennRecordNotificationRecord() {
        return SCRENN_RECORD_NOTIFICATION_RECORD;
    }
    public static int getScrennRecordNotificationMonitor() {
        return SCRENN_RECORD_NOTIFICATION_MONITOR;
    }

    // file path
    public static String getRecFileName() {
        return  new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }
    public static String getUploadPath() {
        String path = getExternalLocaldir() + "/upload/";
        File file = new File(path);
        if(!file.exists())
            if(!file.mkdirs())
                return null;
        return path;
    }
    public static boolean fileRename(String newPath) {
        String upload;
        File file = new File(getExternalLocaldir() + "/" + newPath + ".tmp");
        return file.exists() && null != (upload = getUploadPath()) && file.renameTo(new File(upload + newPath));

    }
    public static String getExternalLocaldir() {
        String path;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {// save in SD card first
            path = Environment.getExternalStorageDirectory() + File.separator + getScreenRecordPath();
        } else {
            path = Environment.getExternalStorageDirectory() + File.separator + getScreenRecordPath();
        }
        return path;
    }
}

