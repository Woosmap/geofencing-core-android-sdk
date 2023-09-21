package com.webgeoservices.woosmapgeofencingcore.logging;

import android.util.Log;

public interface LogListener {
    void d(String tag, String log);

    void d(String tag, String log, Throwable throwable);

    void e(String tag, String log);

    void e(String tag, String log, Throwable throwable);

    void v(String tag, String log);

    void v(String tag, String log, Throwable throwable);

    void i(String tag, String log);

    void i(String tag, String log, Throwable throwable);

    void w(String tag, String log);

    void w(String tag, String log, Throwable throwable);

    void wtf(String tag, String log);

    void wtf(String tag, String log, Throwable throwable);

    void wtf(String tag, Throwable throwable);
}
