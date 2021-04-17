package com.dgudi.disk;

import static com.dgudi.disk.GeneralUtils.convertToGigabytes;
import static com.dgudi.disk.GeneralUtils.formatNumber;

public class MemoryUtils {

    static long maxMemUsage = 0;
    static double memDisplayFactor = 1;
    private static long availableMem;
    private static long cachedMem;
    private static long usedMem;

    static void setDisplayFactor(float width){
        availableMem = Runtime.getRuntime().maxMemory();
        memDisplayFactor = width / (double) availableMem;
    }

    static float getCachedMemInPixels(){
        update();
        return (float) (cachedMem * memDisplayFactor);
    }

    static float getUsedMemInPixels(){
        update();
        return (float) (usedMem * memDisplayFactor);
    }

    static float getMaxMemoryUsageInPixels(){
        update();
        return (float) (400 + maxMemUsage * memDisplayFactor);
    }

    static String getUsedMemInGb(){
        update();
        return String.valueOf(formatNumber(convertToGigabytes(usedMem)));
    }

    static String getMaxMemoryUsageInGb(){
        update();
        return String.valueOf(formatNumber(convertToGigabytes(maxMemUsage)));
    }

    static String getAvailableMemoryInGb(){
        update();
        return String.valueOf(formatNumber(convertToGigabytes(availableMem)));
    }

    private static void update(){
        cachedMem = Runtime.getRuntime().totalMemory();
        usedMem = cachedMem - Runtime.getRuntime().freeMemory();
        if (usedMem > maxMemUsage) {
            maxMemUsage = usedMem;
        }
    }

}
