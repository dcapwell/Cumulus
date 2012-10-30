package com.ekaqu.cunulus;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ThreadPools {
  public static final ThreadFactory DAEMON_FACTORY = new ThreadFactoryBuilder().setDaemon(true).build();
  private static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 2 + 1;

  public static ExecutorService getMaxSizePool(final Object caller) {
    return getFixedSizePool(MAX_THREAD_COUNT, caller.getClass());
  }

  public static ExecutorService getMaxSizePool(final Class<?> caller) {
    return getFixedSizePool(MAX_THREAD_COUNT, caller);
  }

  public static ExecutorService getFixedSizePool(final int size, final Class<?> caller) {
    final ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat(caller.getSimpleName() + "-%s")
        .setDaemon(true)
        .build();
    return Executors.newFixedThreadPool(size, threadFactory);
  }
}
