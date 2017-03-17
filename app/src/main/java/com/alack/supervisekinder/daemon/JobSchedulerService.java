package com.alack.supervisekinder.daemon;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Handler;
import android.os.Message;

import com.alack.supervisekinder.KinderApplication;
import com.alack.supervisekinder.utils.Const;
import com.alack.supervisekinder.utils.LogManager;

import java.util.List;

/**
 * Created by taurus on 2017/3/12.
 */

public class JobSchedulerService extends JobService {
    private static final String     TAG = "JobSchedulerService";
    private LogManager              Log = LogManager.getInstance();

    private static final String PACKAGE_KEEPLIVE_MONITOR      = "com.alack.supervisekinder.daemon.MonitorService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "JobSchedulerService task start...");
        mJobHandler.sendMessage(Message.obtain(mJobHandler, Const.getJobschedulerServiceId(), params));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(TAG, "JobSchedulerService task stop...");
        mJobHandler.removeMessages(Const.getJobschedulerServiceId());
        return false;
    }

    private Handler mJobHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Log.e(TAG, "JobSchedulerService task running...");
            if(!KinderApplication.getInstance().isServiceWorking(null, PACKAGE_KEEPLIVE_MONITOR)) {
                //startService(new Intent(getApplicationContext(), MonitorService.class));
                KinderApplication.getInstance().startMonitorService(null, null);
            }
            jobFinished((JobParameters)msg.obj, false);
            return true;
        }
    });
}
