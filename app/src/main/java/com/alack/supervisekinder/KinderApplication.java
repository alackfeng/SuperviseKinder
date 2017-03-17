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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
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
    private static Activity     mainActivity;
    private static final String PACKAGE_KEEPLIVE_MONITOR    = "com.alack.supervisekinder.daemon.MonitorService";
    private static final String PACKAGE_KEEPLIVE_ACTIVITY   = "com.alack.supervisekinder.KeepliveActivity";
    private static final String PACKAGE_NAME                = "com.alack.supervisekinder";


    private List<Service>       serviceList                 = new LinkedList<>();

    private KeepliveReceiver mKeepReceiver;

    boolean bKeepliveRunning = false;

    //private JobScheduler        mJobScheduler;

    //Intent mMonitorIntent;

    // system call begin
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onStart() - ");

        Log.init(this);
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
            System.exit(1);
        }
        return instance;
    }
    public static void init(Activity activity) {
        if(mainActivity == null) {
            // huawei Honor 7 need, storage permission first
            mainActivity = activity;
            KinderApplication.getInstance().requestPermissionStorage();
            LogManager.getInstance().startLogcatManager();
        } else {
            mainActivity = activity;
        }
    }
    public static void uninit() {
        mainActivity = null;
        LogManager.getInstance().stopLogcatManager();
    }
    public void addActivity(Activity activity) {
        Log.i(TAG, "addActivity() - add - " + activity.getClass().getCanonicalName());
        activityList.add(activity);
    }
    public void removeActivity(Activity activity) {
        Log.i(TAG, "removeActivity() - remove - " + activity.getClass().getCanonicalName());
        if(activityList.isEmpty())
            return;
        for(Activity activity1: activityList) {
            if(activity1.equals(activity)) {
                activityList.remove(activity1);
                break;
            }
        }
    }
    public void exit() {
        Log.e(TAG, "exit() - all Activity - exit ");
        for(Activity activity : activityList) {
            activity.finish();
        }
        System.exit(0);
    }

    public void addService(Service service) {
        Log.i(TAG, "addService() - " + service.getClass().getCanonicalName());
        serviceList.add(service);
    }
    public void removeService(Service service) {
        Log.i(TAG, "removeService() - " + service.getClass().getCanonicalName());
        if(serviceList.isEmpty())
            return;
        for(Service service1: serviceList) {
            if(service1.equals(service)) {
                serviceList.remove(service1);
                break;
            }
        }
    }

    public void startRecService(Intent data, int resultCode) {
        try {
            Log.i(TAG, "startMonitorService() - action -) " + Const.getScreenRecordingInit());
            Context cxt = ((mainActivity != null) ? mainActivity : this);

            Intent recorderService = new Intent(cxt, RecorderService.class);
            recorderService.setAction(Const.getScreenRecordingInit());
            recorderService.putExtra(Const.getRecorderIntentData(), data);
            recorderService.putExtra(Const.getRecorderIntentResult(), resultCode);
            cxt.startService(recorderService);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startRecService() - ERROR - " + e.toString());
        }

    }
    public void startMonitorService(String action, String param) {
        try {
            Log.i(TAG, "startMonitorService() - action -) " + action);
            Context cxt = ((mainActivity != null) ? mainActivity : this);

            Intent mMonitorIntent = new Intent(cxt, MonitorService.class);
            if(action != null) {
                mMonitorIntent.setAction(action);
                if(param != null)
                    mMonitorIntent.putExtra(action, param);
            }
            cxt.startService(mMonitorIntent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startMonitorService() - ERROR " + e.toString());
        }
    }

    public boolean startKeeplive() {
        boolean bRet = true;

        if(bKeepliveRunning) {
            Log.e(TAG, "startKeeplive() - KeepliveActivity - is Yet Running...");
            return true;
        }

        try {
            if(mainActivity == null) {
                Log.e(TAG, "startKeeplive() - Context mainActivity is null, why? ");
                bRet = false;
            }
            if(bRet) {
                Intent keepIntent = new Intent(mainActivity, KeepliveActivity.class);
                mainActivity.startActivity(keepIntent);
                bKeepliveRunning = true;
                Log.e(TAG, "startKeeplive() - KeepliveActivity - is Now Running...");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startKeeplive() - ERROR - " + e.toString());
            bRet = false;
        }
        return bRet;
    }
    public boolean stopKeeplive() {
        boolean bRet = true;
        if(!bKeepliveRunning) {
            Log.e(TAG, "stopKeeplive() - KeepliveActivity - is NO Yet Running...");
            return true;
        }

        try {
            Activity keepActivity = null;

            for(Activity activity: activityList) {
                if(PACKAGE_KEEPLIVE_ACTIVITY.equals(activity.getClass().getCanonicalName())) {
                    keepActivity = activity;
                    activityList.remove(activity);
                    break;
                }
            }
            if(bKeepliveRunning && keepActivity != null) {
                keepActivity.finish();
                bKeepliveRunning = false;
                Log.e(TAG, "stopKeeplive() - KeepliveActivity - is Now Stopping...");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "stopKeeplive() - ERROR - " + e.toString());
            bKeepliveRunning = false;
            bRet = false;
        }
        return bRet;

    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isServiceWorking(Context context, String serviceName) {
        Log.i(TAG, "isServiceWorking() - ");
        boolean bRet = false;
        try {
            Log.i(TAG, "isServiceWorking() Context - set -) " + context + " -) " + mainActivity + " -) " + this);
            Context cxt = (context != null) ? context : (mainActivity != null ? mainActivity : this);

            ActivityManager activityMgr = (ActivityManager)cxt.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> serviceList = activityMgr.getRunningServices(100);

            if(serviceList.isEmpty()) {
                Log.e(TAG, "isServiceWorking() - ACTIVITY_SERVICE is NO!!, why??? " + serviceName);
                bRet = false;
            }

            for(int i=0; i<serviceList.size(); i++) {
                String name = serviceList.get(i).service.getClassName();
                if (name.equals(serviceName)) {
                    Log.e(TAG, "isServiceWorking() - service <" + name + "> Yet at.");
                    bRet = true;
                    break;
                }
            }
            Log.e(TAG, "isServiceWorking() - service <" + serviceName + "> not Yet at.");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "isServiceWorking(0 - ERROR - " + e.toString());
            bRet = false;
        }
        return bRet;
    }

    private boolean isRunningForeground(Context context) {
        Log.i(TAG, "isRunningForeground() - ");
        boolean bRet = true;
        try {
            Log.i(TAG, "isRunningForeground() Context - set -) " + context + " -) " + mainActivity + " -) " + this);
            Context cxt = (context != null) ? context : (mainActivity != null ? mainActivity : this);

            ActivityManager am = (ActivityManager)cxt.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> serviceList = am.getRunningServices(100);
            Log.i(TAG, "isRunningForeground() SERVICE total : " + serviceList.size());
            if(serviceList.isEmpty()) {
                Log.e(TAG, "isRunningForeground() - processInfo NULL, sure???");
                bRet = false;
            }

            for(ActivityManager.RunningServiceInfo service : serviceList) {
                if(PACKAGE_NAME.equals(service.service.getPackageName())) {
                    Log.i(TAG, "isRunningForeground() - SERVICE :< " + service.foreground +  "> " + service.service.getClassName());
                }
                //service.foreground
            }
        } catch (Exception e) {
            Log.e(TAG, "isRunningForeground() - ERROR - " + e.toString());
            bRet = false;
        }
        return bRet;
    }

    private boolean isRunningActivity(Context context) {
        Log.i(TAG, "isRunningActivity() - ");
        boolean bRet = true;
        try {

            Log.i(TAG, "isRunningActivity() Context - set -) " + context + " -) " + mainActivity + " -) " + this);
            Context cxt = (context != null) ? context : (mainActivity != null ? mainActivity : this);

            Intent checkIntent = new Intent();
            checkIntent.setClass(cxt, MainActivity.class); //MainActivity
            PackageManager pm = cxt.getPackageManager();
            if(pm == null) {
                Log.e(TAG, "isRunningActivity() - PackageManager NULL,why ?");
                bRet = false;
            }
            if(bRet) {
                List<ResolveInfo> resolveInfos = pm.queryIntentActivities(checkIntent, 0);
                Log.i(TAG, "isRunningActivity() ACTIVITY total : " + resolveInfos.size());

                for(ResolveInfo resolveInfo: resolveInfos) {
                    Log.i(TAG, "isRunningActivity() - ACTIVITY : " + resolveInfo.toString() + " >> " + resolveInfo.activityInfo.getClass().getCanonicalName());
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "isRunningActivity() - ERROR - " + e.toString());
            bRet = false;
        }
        return bRet;
    }

    private boolean isRunningApp(Context context) {
        Log.i(TAG, "isRunningApp() - process");
        boolean bRet = true;
        try {
            Log.i(TAG, "isRunningForeground() Context - set -) " + context + " -) " + mainActivity + " -) " + this);
            Context cxt = (context != null) ? context : (mainActivity != null ? mainActivity : this);

            ActivityManager am = (ActivityManager)cxt.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> processInfoList = am.getRunningAppProcesses();
            if(processInfoList.isEmpty()) {
                Log.e(TAG, "isRunningApp() - processInfo NULL, sure???");
                bRet = false;
            }
            for(ActivityManager.RunningAppProcessInfo info: processInfoList) {
                Log.d(TAG, "isRunningApp() - process <" + info.pid + "> " + info.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "isRunningApp() - ERROR - " + e.toString());
            bRet = false;
        }
        return bRet;
    }

    public boolean checkApp() {
        if(!isRunningApp(null)) {
            Log.e(TAG, "checkApp() - isRunningApp - restart");
            return restart(null);
        }
        else if(!isRunningActivity(null)) {
            Log.e(TAG, "checkApp() - isRunningActivity - restart");
            return restart(null);
        }
        else if(!isRunningForeground(null)) {
            Log.e(TAG, "checkApp() - isRunningForeground - restart");
            return restart(null);
        }
        return true;
    }

    public boolean restart(Context context) {

        Log.i(TAG, "restart() Context - maybe need Release anything resource, eg activity service thread etc , later thinking...???");

        try {
            Log.i(TAG, "restart() Context - set -) " + context + " -) " + mainActivity + " -) " + this);
            Context cxt = (context != null) ? context : (mainActivity != null ? mainActivity : this);

            PackageManager pm = cxt.getPackageManager();
            if(pm == null) {
                Log.e(TAG, "restart() Context - PackageManager is null");
                return false;
            }

            Intent startIntent = pm.getLaunchIntentForPackage(cxt.getPackageName());
            if(startIntent == null) {
                Log.e(TAG, "restart() Context - Intent is null");
                return false;
            }
            startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            //create a pending intent so the application is restarted after System.exit(0) was called.
            // We use an AlarmManager to call this intent in 100ms
            int pendingIntentId = 223344;
            PendingIntent pendingIntent = PendingIntent.getActivity(cxt, pendingIntentId,
                    startIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) cxt.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);

            Log.e(TAG, "restart() Context - 100MS after - ");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "restart() Context - ERROR - " + e.toString());
            return false;
        }
        return true;
    }


    public void startBroadcastReceiver() {
        if(mKeepReceiver != null) {
            Log.i(TAG, "startBroadcastReceiver() - already ...");
            //return;
        }
        try {
            mKeepReceiver = new KeepliveReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            // not use filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.setPriority(2147483647);
            this.registerReceiver(mKeepReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startBroadcastReceiver() - ERROR " + e.toString());
        }
    }

    public void stopBroadcastReceiver() {
        if(mKeepReceiver != null)
            this.unregisterReceiver(mKeepReceiver);
    }

    // record media permission begin
    public void permissionStorageAudio() {
        //requestPermissionStorage();
        requestPermissionAudio();
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

    public void notifyKeeplive(Service keepService, String action) {
        Log.i(TAG, "notifyKeeplive() - action - " + action);
        try {
            Context cxt = (keepService != null) ? keepService : this;

            Intent hideIntent = new Intent(cxt, KeepliveService.class);
            hideIntent.setAction(action);
            cxt.startService(hideIntent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "notifyKeeplive() - ERROR - " + e.toString());
        }
    }


    public void startForeground(final int notificationID, String action, final Service innerService)
    {
        Log.i(TAG, "startForeground() Notification - " + notificationID + " VERSION " + Build.VERSION.SDK_INT + ", service list " + serviceList.size());
        Service foreService = null;
        for(Service service: serviceList) {
            String name = service.getClass().getCanonicalName();
            //Log.i(TAG, "startForeground() Notification - name (" + name + "),\n action (" + action + ")");
            if(name.equals(action)) {
                foreService = service;
                break;
            }
        }

        if(foreService != null) {
            Log.i(TAG, "startForeground() Notification ---- " + Build.VERSION.SDK_INT + ", inner " + innerService);
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                foreService.startForeground(notificationID, new Notification());
            } else {
                foreService.startForeground(notificationID, newNotification(foreService).build());
                if(innerService != null) {
                    innerService.startForeground(notificationID, newNotification(foreService).build());
                    innerService.stopSelf();
                }
            }
            Log.i(TAG, "startForeground() Notification - OK " + notificationID);
        }
        Log.e(TAG, "startForeground() Notification Service not here, why " + notificationID);
    }

    private NotificationCompat.Builder newNotification(Context s) {
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
            Log.e(TAG, "startJobScheduler() - System version not implement JobScheduler-" + Build.VERSION.SDK_INT);
            return;
        }

        int intervalMillis = 60*1000;
        try {
            JobInfo.Builder builder = new JobInfo.Builder(Const.getJobschedulerServiceId(),
                    //new ComponentName(mainActivity.getApplicationContext(), JobSchedulerService.class.getName()));
                    new ComponentName(this, JobSchedulerService.class.getName()));
            builder.setPeriodic(intervalMillis);
            builder.setPersisted(true);

            JobScheduler mJobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if(mJobScheduler.schedule(builder.build()) <= 0) {
                Log.e(TAG, "startJobScheduler() - start JobScheduler failed!");
            } else
                Log.i(TAG, "startJobScheduler() - JobScheduler - interval :" + intervalMillis + "ms");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startJobScheduler() - ERROR - " + e.toString());
        }
    }

    // check MainActivity going on
    public void startMain() {
        Log.i(TAG, "startMain() Check MainActivity!");
        try {
            Context ctx = this; //getApplicationContext();
            Intent mainIntent = new Intent(ctx, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(mainIntent);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "startMain() - ERROR - " + e.toString());
        }
    }

    // check wifi state
    private String checkWifiIp() {
        String ipAddressString;

        try {
            WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            Log.e(TAG, "checkWifi() Wifi Info: " + wifiInfo.toString());
            int ipAddress = wifiInfo.getIpAddress();
            if(ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                ipAddress = Integer.reverseBytes(ipAddress);
            }
            byte [] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e(TAG,  "checkWifiIp() Unable to get host address.");
            ipAddressString = null;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "checkWifiIp() - ERROR - " + e.toString());
            ipAddressString = null;
        }
        Log.e(TAG, "checkWifiIp() IP - " + ipAddressString);
        return ipAddressString;
    }

    public boolean checkWifi() {
        Log.i(TAG, "checkWifi() - Begin");
        boolean bRet = true;
        try {

            ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            if(connectivityManager == null) {
                Log.e(TAG, "checkWifi() - CONNECTIVITY_SERVICE NOT");
                bRet = false;
            }
            if(bRet) {
                bRet = false;
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if(networkInfo == null) {
                    Log.e(TAG, "checkWifi() - CONNECTIVITY_SERVICE WIFI NOT");
                } else if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI  && networkInfo.isConnected()){
                    Log.e(TAG, "checkWifi() - CONNECTIVITY_SERVICE WIFI Connected...");
                    bRet = true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "checkWifi() - ERROR -" + e.toString());
            bRet = false;
        }

        // notify monitor
        if(!isServiceWorking(null, PACKAGE_KEEPLIVE_MONITOR)) {
            Log.e(TAG, "checkWifi() - PACKAGE_KEEPLIVE_MONITOR not work");
            bRet = false;
        } else if(bRet) {
            startMonitorService(Const.getConnectivityActionStart(), checkWifiIp());
        } else {
            startMonitorService(Const.getConnectivityActionEnd(), null);
        }

        Log.i(TAG, "checkWifi() - End");
        return bRet;
    }

}
