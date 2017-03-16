package com.alack.supervisekinder.daemon;

import android.app.ActivityManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import com.alack.supervisekinder.utils.Const;
import com.alack.supervisekinder.utils.LogManager;

import java.util.List;

/**
 * Created by taurus on 2017/3/12.
 */

public class JobSchedulerService extends JobService {
    private static final String     TAG = "JobSchedulerService";
    private LogManager              Log = LogManager.getInstance();

    private static final String JobName = "com.alack.supervisekinder.daemon.MonitorService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "JobService task start...");
        mJobHandler.sendMessage(Message.obtain(mJobHandler, Const.getJobschedulerServiceId(), params));
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "JobService task stop...");
        mJobHandler.removeMessages(Const.getJobschedulerServiceId());
        return false;
    }

    private Handler mJobHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.e(TAG, "JobService task running...");
            if(!isServiceWorking(getApplicationContext(), JobName)) {
                startService(new Intent(getApplicationContext(), MonitorService.class));
            }
            jobFinished((JobParameters)msg.obj, false);
            return true;
        }
    });

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isServiceWorking(Context cxt, String serviceName) {
        ActivityManager activityMgr = (ActivityManager)cxt.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityMgr.getRunningServices(100);

        if(serviceList.isEmpty()) {
            Log.e(TAG, "isServiceWorking() - ACTIVITY_SERVICE is NO!!, why???");
            return false;
        }
        for(int i=0; i<serviceList.size(); i++) {
            String name = serviceList.get(i).service.getClassName();
            if (name.equals(serviceName)) {
                Log.e(TAG, "isServiceWorking() - service <" + name + "> yet at.");
                return true;
            }
        }
        Log.e(TAG, "isServiceWorking() - service <" + serviceName + "> not yet at.");
        return false;
    }
}
