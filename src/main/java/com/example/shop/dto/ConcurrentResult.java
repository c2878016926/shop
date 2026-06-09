package com.example.shop.dto;

public class ConcurrentResult {
    private int threadCount;
    private int quantityPerThread;
    private int successCount;
    private int failCount;
    private int remainingStock;
    private long elapsedMillis;

    public ConcurrentResult() {}

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public int getQuantityPerThread() { return quantityPerThread; }
    public void setQuantityPerThread(int quantityPerThread) { this.quantityPerThread = quantityPerThread; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }

    public int getRemainingStock() { return remainingStock; }
    public void setRemainingStock(int remainingStock) { this.remainingStock = remainingStock; }

    public long getElapsedMillis() { return elapsedMillis; }
    public void setElapsedMillis(long elapsedMillis) { this.elapsedMillis = elapsedMillis; }
}
