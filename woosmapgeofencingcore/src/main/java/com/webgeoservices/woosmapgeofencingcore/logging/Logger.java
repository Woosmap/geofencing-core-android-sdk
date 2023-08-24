package com.webgeoservices.woosmapgeofencingcore.logging;

import android.util.Log;

import com.webgeoservices.woosmapgeofencingcore.WoosmapCore;

public class Logger {
    private static Logger _instance = null;
    private static final String TAG = "WoosmapGeofenceCoreSDK";
    private LogListener logListener;
    private Logger(){

    }
    public static Logger getInstance(){
        if (_instance == null){
            _instance = new Logger();
        }
        return _instance;
    }

    public void setLogListener(LogListener logListener){
        this.logListener = logListener;
    }

    public void d(String log){
        Log.d(TAG, log);
        if (logListener!=null){
            logListener.d(TAG, log);
        }
    }
    public void d(String log, Throwable throwable){
        Log.d(TAG, log, throwable);
        if (logListener!=null){
            logListener.d(TAG, log, throwable);
        }
    }
    public void e(String log){
        Log.e(TAG, log);
        if (logListener!=null){
            logListener.e(TAG, log);
        }
    }
    public void e(String log, Throwable throwable){
        Log.e(TAG, log, throwable);
        if (logListener!=null){
            logListener.e(TAG, log, throwable);
        }
    }
    public void v(String log){
        Log.v(TAG, log);
        if (logListener!=null){
            logListener.v(TAG, log);
        }
    }
    public void v(String log, Throwable throwable){
        Log.v(TAG, log, throwable);
        if (logListener!=null){
            logListener.v(TAG, log, throwable);
        }
    }
    public void i(String log){
        Log.i(TAG, log);
        if (logListener!=null){
            logListener.i(TAG, log);
        }
    }
    public void i(String log, Throwable throwable){
        Log.i(TAG, log, throwable);
        if (logListener!=null){
            logListener.i(TAG, log, throwable);
        }
    }
    public void w(String log){
        Log.w(TAG, log);
        if (logListener!=null){
            logListener.w(TAG, log);
        }
    }
    public void w(String log, Throwable throwable){
        Log.w(TAG, log, throwable);
        if (logListener!=null){
            logListener.w(TAG, log, throwable);
        }
    }
    public void wtf(String log){
        Log.wtf(TAG, log);
        if (logListener!=null){
            logListener.wtf(TAG, log);
        }
    }
    public void wtf(String log, Throwable throwable){
        Log.wtf(TAG, log, throwable);
        if (logListener!=null){
            logListener.wtf(TAG, log, throwable);
        }
    }

    public void wtf(Throwable throwable){
        Log.wtf(TAG, throwable);
        if (logListener!=null){
            logListener.wtf(TAG, throwable);
        }
    }
}
