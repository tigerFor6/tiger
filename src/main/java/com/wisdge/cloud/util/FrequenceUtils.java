package com.wisdge.cloud.util;

import org.apache.commons.lang.time.StopWatch;
/**
 * @Author tiger
 * @Date 2022/06/14
 */
public class FrequenceUtils {

    /**
     * <p>
     * Limit call count in split time
     * </p>
     *
     * @param limitSplitTime
     * @param limitCount
     * @throws InterruptedException
     */
    public static void limit(final long limitSplitTime, final int limitCount) throws InterruptedException {
        FrequenceUnit funit = threadLocal.get();
        funit.limitSplitTime = limitSplitTime;
        funit.limitCount = limitCount;
        funit.watch.split();
        long diffTime = funit.limitSplitTime - funit.watch.getSplitTime();
        if (diffTime >= 0) {
            if (funit.realCount >= funit.limitCount) {
                funit.watch.suspend();
                Thread.sleep(diffTime);
                funit.watch.resume();
                funit.realCount = 0;
            }
        }
        funit.realCount++;
    }

    /**
     * FrequenceUnit
     */
    private static class FrequenceUnit {
        FrequenceUnit() {
            this.watch = new StopWatch();
        }
        long limitSplitTime;
        int limitCount;
        StopWatch watch;
        int realCount = 0;
    }

    private static ThreadLocal<FrequenceUnit> threadLocal = new ThreadLocal<FrequenceUnit>(){
        protected synchronized FrequenceUnit initialValue() {
            FrequenceUnit funit = new FrequenceUnit();
            funit.watch.start();
            return funit;
        }
    };

}
