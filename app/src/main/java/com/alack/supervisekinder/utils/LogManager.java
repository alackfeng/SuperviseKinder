package com.alack.supervisekinder.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.lang.reflect.Field;


/**
 * Created by Administrator on 2017/3/8.
 */

public class LogManager implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "LogManager";
    @SuppressLint("StaticFieldLeak")
    private static LogManager INSTANCE      = null;
    private static String PATH_LOGCAT;
    private LogDumper mLogDumper            = null;
    private int mPId;
    private static boolean bStarting        = false;

    private SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // crash log
    private Thread.UncaughtExceptionHandler     mDefaultHandler;
    private Context                             mContext;
    private Map<String, String>                 infos   = new HashMap<>();
    private SimpleDateFormat                    formatter = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private String                              filePath = "";

    public void init(Context context) {
        if(context == null) {
            Log.e(TAG, "init() - Context is NULLL");
            //return;
        }
        if(mContext != null)
            mContext = context;
        else {
            mContext = context;
            filePath = Const.getExternalLocaldir();
            //Gets the system's default UncaughtException handler
            mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            //Set the CrashHandler as the default handler for the program
            Thread.setDefaultUncaughtExceptionHandler(this);
        }
        Log.i(TAG, "Init Crash log");
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if(!handleException(ex) && mDefaultHandler != null) {
            // If the user does not deal with the system is the default exception handler to handle
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Log.e("LogManager", "uncaughtException error : ", e);
            }

            // restart app
            //KinderApplication.getInstance().restart(null);
            // exit app
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        // Collect device parameter information
        collectDeviceInfo(mContext);
        //Save the log file
        String str = saveCrashInfo2File(ex);
        Log.e(TAG, str);
        return true;
    }

    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
                    PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null"
                        : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }

        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }

    }

    private String saveCrashInfo2File(Throwable ex) {

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append("[").append(key).append(", ").append(value).append("]\n");
        }
        sb.append("\n").append(getStackTraceString(ex));
        try {

            String time = formatter.format(new Date());
            String fileName = "Kinder_CRS_" + time + ".txt";
            //noinspection ConstantConditions
            File sdDir = mContext.getExternalFilesDir("logs").getAbsoluteFile();
            File file;
            if (!TextUtils.isEmpty(filePath)) {
                File files = new File(filePath);
                if (!files.exists()) {
                    //Create a directory
                    //noinspection ResultOfMethodCallIgnored
                    files.mkdirs();
                }
                file = new File(filePath + File.separator + fileName);
            } else {
                file = new File(sdDir + File.separator + fileName);
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.close();
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "saveCrashInfo2File(0 - ERROR " + e.toString());
        }
        return null;
    }

    public static String getStackTraceString(Throwable tr) {
        try {
            if (tr == null) {
                return "";
            }
            Throwable t = tr;
            while (t != null) {
                if (t instanceof UnknownHostException) {
                    return "";
                }
                t = t.getCause();
            }

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            tr.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            return "";
        }
    }


    public static LogManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LogManager();
        }
        return INSTANCE;
    }
    private LogManager() {
        mPId = android.os.Process.myPid();
    }

    private void setFolderPath(String folderPath) {

        File folder = new File(folderPath);
        if (!folder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
        }

        if (!folder.isDirectory()) {
            Log.i(TAG, "The logcat folder path is not a directory: " + folderPath);
            throw new IllegalArgumentException("The logcat folder path is not a directory: " + folderPath);
        }


        PATH_LOGCAT = folderPath.endsWith("/") ? folderPath : folderPath + "/";
    }

    public void start(String saveDirectoy) {

        if(bStarting) {
            Log.i("LogManager", "log manager been started YET!!!");
            return;
        }
        Log.d("LogManager", "start LogManager...");

        setFolderPath(saveDirectoy);

        if (mLogDumper == null)
            mLogDumper = new LogDumper(String.valueOf(mPId), PATH_LOGCAT);
        mLogDumper.start();

        bStarting = true;
    }

    public void stop() {
        if (mLogDumper != null) {
            mLogDumper.stopLogs();
            mLogDumper = null;
            bStarting = false;
        }
    }

    private class LogDumper extends Thread {
        private Process logcatProc;
        private BufferedReader mReader = null;
        private boolean mRunning = true;
        String cmds = null;
        private String mPID;
        private FileOutputStream out = null;

        public LogDumper(String pid, String dir) {
            mPID = pid;
            try {
                out = new FileOutputStream(new File(dir, "kinder-" +  simpleDateFormat1.format(new Date()) + ".log"), true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


            /**
             *
             * log level：*:v , *:d , *:w , *:e , *:f , *:s
             *
             * Show the current mPID process level of E and W log.
             *
             * */

            // cmds = "logcat *:e *:w | grep \"(" + mPID + ")\"";
            cmds = "logcat  | grep \"(" + mPID + ")\"";//show log of all level
            // cmds = "logcat -s way";//Print label filtering information

            //cmds = "logcat *:d *:e *:i | grep \"(" + mPID + ")\"";

        }

        public void stopLogs() {
            mRunning = false;
        }

        @Override
        public void run() {
            try {

                logcatProc = Runtime.getRuntime().exec(cmds);
                mReader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()), 1024);
                String line;
                while (mRunning && (line = mReader.readLine()) != null) {
                    if (!mRunning) {
                        break;
                    }
                    if (line.length() == 0) {
                        continue;
                    }
                    if (out != null && line.contains(mPID)) {
                        out.write((simpleDateFormat2.format(new Date()) + "  " + line + "\n").getBytes());
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (logcatProc != null) {
                    logcatProc.destroy();
                    logcatProc = null;
                }
                if (mReader != null) {
                    try {
                        mReader.close();
                        mReader = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    out = null;
                }

            }

        }
    }

    public void v(String tag, String msg) {
        Log.v(tag, "========:" + msg);
    }
    public void d(String tag, String msg) {
        Log.d(tag, "========:" + msg);
    }
    public void i(String tag, String msg) {
        Log.i(tag, "========:" + msg);
    }
    public void w(String tag, String msg) {
        Log.w(tag, "========:" + msg);
    }
    public void e(String tag, String msg) {
        Log.e(tag, "========:" + msg);
    }



    public static void startLogcatManager() {
        String folderPath = Const.getExternalLocaldir() + "/";
        LogManager.getInstance().start(folderPath);
    }

    public static void stopLogcatManager() {
        LogManager.getInstance().stop();
    }

}
