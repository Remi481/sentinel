package com.remi481.sentinel.agent;

public class AgentMain {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Sentinel agent starting...");

        SystemMetricsCollector collector = new SystemMetricsCollector();
        Thread.sleep(1000);

        System.out.println(collector.collect());
    }
}