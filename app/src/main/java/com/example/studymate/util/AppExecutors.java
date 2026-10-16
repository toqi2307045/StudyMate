package com.example.studymate.util;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shared thread pool for disk/CPU work plus a main-thread poster. */
public final class AppExecutors {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {}

    public static void io(Runnable r) { POOL.execute(r); }

    public static void main(Runnable r) { MAIN.post(r); }

    public static ExecutorService pool() { return POOL; }
}
