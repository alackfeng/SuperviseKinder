package com.alack.supervisekinder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.alack.supervisekinder.daemon.JobSchedulerService;
import com.alack.supervisekinder.daemon.KeepliveReceiver;
import com.alack.supervisekinder.daemon.KeepliveService;
import com.alack.supervisekinder.daemon.MonitorService;
import com.alack.supervisekinder.utils.Const;
import com.alack.supervisekinder.utils.LogManager;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by taurus on 2017/3/12.
 */

public class KinderApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final String TAG     = "KinderApplication";
    private LogManager          Log     = LogManager.getInstance();

    // singleton
    @SuppressLint("StaticFieldLeak")
    private static KinderApplication instance;

    private List<Activity>      activityList                = new LinkedList<>();
    @SuppressLint("StaticFieldLeak")
    private static Activity            mainActivity;
    private static final String PACKAGE_KEEPLIVE_ACTITVITY  = "com.alack.supervisekinder.KeepliveActivity";

    private List<Service>       serviceList                 = new LinkedList<>();

    public KeepliveReceiver mKeepReceiver;

    boolean bKeepliveRunning = false;

    //private JobScheduler        mJobScheduler;

    Intent mMonitorIntent;

    // system call begin
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onStart() - ");

        Log.init(this);
        //Log.startLogcatManager();
        registerActivityLifecycleCallbacks(this);
        instance = this;
        mainActivity = null;
        startBroadcastReceiver();
        startJobScheduler();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(TAG, "onLowMemory() - ");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "onConfigurationChanged() - ");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i(TAG, "onTerminate() - ");
        unregisterActivityLifecycleCallbacks(this);
        stopBroadcastReceiver();
    }

    @Override
    public void onTrimMemory(int level) {
        Log.i(TAG, "onTrimMemory() - " + level);
        super.onTrimMemory(level);
    }

    // ActivityLifecycleCallbacks
    @Override
    public void onActivityResumed(Activity activity) {
        Log.i(TAG, "LifeCycle - onActivityResumed() - " + activity.toString());
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.i(TAG, "LifeCycle - onActivityPaused() - " + activity.toString());
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        //Log.i(TAG, "LifeCycle - onActivityCreated() - " + activity.toString());
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        //Log.i(TAG, "LifeCycle - onActivityDestroyed() - " + activity.toString());
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        //Log.i(TAG, "LifeCycle - onActivitySaveInstanceState() - " + activity.toString());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        //Log.i(TAG, "LifeCycle - onActivityStarted() - " + activity.toString());
    }

    @Override
    public void onActivityStopped(Activity activity) {
        //Log.i(TAG, "LifeCycle - onActivityStopped() - " + activity.toString());
    }
    // system call over



    // instance
    public static KinderApplication getInstance() {
        if(instance == null) {
            LogManager.getInstance().e(TAG, "getInstance() - null, are you sure???");
            System.exit(0);
        }
        return instance;
    }
    public static void init(Activity activity) {
        if(mainActivity == null) {
            // huawei Honor 7 need, storage permission first
            mainActivity = activity;
            KinderApplication.getInstance().requestPermissionStorage();
            LogManager.getInstance().startLogcatManager();
        }

        mainActivity = activity;
    }
    public static void uninit() {
        LogManager.getInstance().stopLogcatManager();
        mainActivity = null;
    }
    public void addActivity(Activity activity) {
        Log.e(TAG, "KeepliveActivity - add");
        activityList.add(activity);
    }
    public void exit() {
        Log.e(TAG, "all Activity - exit ");
        for(Activity activity : activityList) {
            activity.finish();
        }
        System.exit(0);
    }
    public void exit(Activity activity) {
        activity.finish();
    }

    public void addService(Service service) {
        Log.i(TAG, "addService() - " + service.getClass().getCanonicalName());
        serviceList.add(service);
    }

    public void startRecService(Intent data, int resultCode) {
        Intent recorderService = new Intent(mainActivity, RecorderService.class);
        recorderService.setAction(Const.getScreenRecordingInit());
        recorderService.putExtra(Const.getRecorderIntentData(), data);
        recorderService.putExtra(Const.getRecorderIntentResult(), resultCode);
        mainActivity.startService(recorderService);
    }
    public void startMonitorService() {
        if(mMonitorIntent != null) {
            Log.i(TAG, "startMonitorService() already ...");
            return;
        }
        mMonitorIntent = new Intent(mainActivity, MonitorService.class);
        mainActivity.startService(mMonitorIntent);
    }

    public void startKeeplive() {
        if(bKeepliveRunning) {
            Log.e(TAG, "KeepliveActivity - is Yet Running...");
            return;
        }
        Intent keepIntent = new Intent(mainActivity, KeepliveActivity.class);
        //keepIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mainActivity.startActivity(keepIntent);
        bKeepliveRunning = true;
        Log.e(TAG, "KeepliveActivity - is Now Running...");

    }
    public void stopKeeplive() {

        for(Activity activity: activityList) {
            if(PACKAGE_KEEPLIVE_ACTITVITY.equals(activity.getClass().getCanonicalName())) {
                if(bKeepliveRunning) {
                    activityList.remove(activity);
                    activity.finish();
                    bKeepliveRunning = false;

                    Log.e(TAG, "KeepliveActivity - is Now Stopping...");
                }
                break;
            }
        }
    }

    private boolean isRunningForeground(Context context) {
        Log.i(TAG, "check isRunningForeground() ");
        boolean bRet = true;
        try {
            ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> serviceList = am.getRunningServices(100);
            Log.w(TAG, "isRunningForeground() SERVICE total exist: " + serviceList.size());
            //for(ActivityManager.RunningServiceInfo service : serviceList) {
                //Log.w(TAG, "-----SERVICE total exist: " + service.getClass().getCanonicalName());
            //}
        } catch (Exception e) {
            Log.e(TAG, "isRunningForeground() - ERROR - " + e.toString());
            bRet = false;
        }
        return bRet;
    }

    private boolean isActivityExist(Context context) {
        Log.i(TAG, "check isActivityExist() ");
        boolean bRet = true;
        try {
            if(mainActivity == null) {
                Log.e(TAG, "isActivityExist() - Context Activity main, ");
                bRet = false;
            }
            if(context == null) {
                Log.e(TAG, "isActivityExist() - Context Activity prim, ");
                bRet = false;
            }
            if(bRet) {
                Intent checkIntent = new Intent();
                checkIntent.setClass(mainActivity, context.getClass());
                List<ResolveInfo> reslist = context.getPackageManager().queryIntentActivities(checkIntent, 0);
                Log.w(TAG, "isActivityExist() total exist: " + reslist.size());
                //for(ResolveInfo info: reslist) {
                    // Log.w(TAG, "-----ACTIVITY total exist: " + info.getClass().getCanonicalName());
                //}
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "isActivityExist() - ERROR - " + e.toString());
            bRet = false;
        }
        return bRet;
    }

    private boolean isRunningApp() {
        Log.i(TAG, "checkApp() - process");
        boolean bRet = true;
        try {
            ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> processInfoList = am.getRunningAppProcesses();
            if(processInfoList.isEmpty()) {
                Log.e(TAG, "checkApp() - process NULL, sure???");
                bRet = false;
            }
            for(ActivityManager.RunningAppProcessInfo info: processInfoList) {
                Log.d(TAG, "checkApp() - process <" + info.pid + "> " + info.toString());
            }

            List<ActivityManager.RunningServiceInfo> serviceList = am.getRunningServices(100);
            Log.w(TAG, "checkApp() - service: total " + serviceList.size());
            for(ActivityManager.RunningServiceInfo serviceinfo : serviceList) {
                ComponentName service = serviceinfo.service;
                if("com.alack.supervisekinder".equals(service.getPackageName())) {
                    Log.d(TAG, "checkApp() - service: " + service.getPackageName() + "@" + service.getShortClassName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "isRunningApp() - ERROR - " + e.toString());
            bRet = false;
        }
        return bRet;
    }

    public boolean checkApp() {
        isActivityExist(mainActivity);
        isRunningForeground(mainActivity);
        isRunningApp();

        return true;
    }

    public void restart(Context cxt) {
        Log.i(TAG, "restart Context - " + cxt);

        try {
            if (cxt == null) {
                Log.e(TAG, "restart Context - Context is null");
                cxt = mainActivity;
                //return;
            }
            PackageManager pm = cxt.getPackageManager();
            if(pm == null) {
                Log.e(TAG, "restart Context - PackageManager is null");
                return;
            }
            Intent startIntent = pm.getLaunchIntentForPackage(cxt.getPackageName());
            if(startIntent == null) {
                Log.e(TAG, "restart Context - Intent is null");
                return;
            }
            startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            //create a pending intent so the application is restarted after System.exit(0) was called.
            // We use an AlarmManager to call this intent in 100ms
            int pendingIntentId = 223344;
            PendingIntent pendingIntent = PendingIntent.getActivity(cxt, pendingIntentId,
                    startIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) cxt.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);

            System.exit(0);
            Log.e(TAG, "restart Context - 100MS after - ");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "restart Context - ERROR - " + e.toString());
        }
    }


    public void startBroadcastReceiver() {
        if(mKeepReceiver != null) {
            Log.i(TAG, "startBroadcastReceiver() - already ...");
            return;
        }
        mKeepReceiver = new KeepliveReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        // not use filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.setPriority(2147483647);
        this.registerReceiver(mKeepReceiver, filter);
    }

    public void stopBroadcastReceiver() {
        mainActivity.unregisterReceiver(mKeepReceiver);
    }

    // record media permission begin
    public void permissionStorageAudio() {
        //requestPermissionStorage();
        requestPermissionAudio();
        //requestPermissionCamera();
    }
    private boolean requestPermissionStorage() {
        if(ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mainActivity, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, Const.getRequestCodeStorage());
        }
        return true;
    }

    private void requestPermissionAudio() {
        if(ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.RECORD_AUDIO}, Const.getRequestCodeAudio());
        }
    }
    // record media permission over

    public void notfiyKeeplive(Service keepService, String action) {
        try {
            Context cxt =
                    (keepService == null) ? this : keepService;

            Intent hideIntent = new Intent(cxt, KeepliveService.class);
            hideIntent.setAction(action);
            cxt.startService(hideIntent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "notfiyKeeplive() - ERROR - " + e.toString());
        }
    }
    public void startForeground(final int notficationID, String action, final Service innerService)
    {
        Log.i(TAG, "startForeground() Notifcation - " + notficationID + " VERSOIN " + Build.VERSION.SDK_INT + ", serverlist " + serviceList.size());

        for(Service service: serviceList) {
            String name = service.getClass().getCanonicalName();
            //Log.i(TAG, "startForeground() Notifcation - name (" + name + "),\n action (" + action + ")");
            if(name.equals(action)) {
                Log.i(TAG, "startForeground() Notifcation ---- " + Build.VERSION.SDK_INT + ", inner " + innerService);
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    service.startForeground(notficationID, new Notification());
                } else {
                    service.startForeground(notficationID, newNotifcation(service).build());
                    if(innerService != null) {
                        innerService.startForeground(notficationID, newNotifcation(service).build());
                        innerService.stopSelf();
                    }
                }
                Log.i(TAG, "startForeground() Notifcation - OK " + notficationID);
                break;
            }
        }
    }

    private NotificationCompat.Builder newNotifcation(Context s) {
        Intent notificationIntent = new Intent(s, s.getClass());
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(s, 0,
                notificationIntent, 0);

        return new NotificationCompat.Builder(s)
                .setContentTitle("Super Kinder")
                .setSmallIcon(R.mipmap.ic_kinder_launcher)
                .setTicker("Super Kinder .")
                .setContentText("Hello Kinder")
                .setContentIntent(pendingIntent)
                //.setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                //.setOngoing(true)
                .setVisibility(0);
    }

    public void startJobScheduler() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.e(TAG, "System version not implement JobScheduler-" + Build.VERSION.SDK_INT);
            return;
        }

        int intervalMillis = 60*1000;
        JobInfo.Builder builder = new JobInfo.Builder(Const.getJobschedulerServiceId(),
                //new ComponentName(mainActivity.getApplicationContext(), JobSchedulerService.class.getName()));
                new ComponentName(this, JobSchedulerService.class.getName()));
        builder.setPeriodic(intervalMillis);
        builder.setPersisted(true);

        JobScheduler mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(mJobScheduler.schedule(builder.build()) <= 0) {
            Log.e(TAG, "start JobScheduler failed!");
        } else
            Log.i(TAG, "start JobScheduler - interval :" + intervalMillis + "ms");
    }

    // restart main
    public void startMain() {
        Log.i(TAG, "startMain() Check MainActivity!");
        Context ctx = this; //getApplicationContext();
        Intent mainIntent = new Intent(ctx, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(mainIntent);
    }

}
