package com.example.lukas.bluetoothtest.io;/*
 *  Copyright 2006 Corey Goldberg (cgoldberg _at_ gmail.com)
 *
 *  This file is part of NetPlot.
 *
 *  NetPlot is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  NetPlot is distributed in the hope that it will be useful,
 *  but without any warranty; without even the implied warranty of
 *  merchantability or fitness for a particular purpose.  See the
 *  GNU General Public License for more details.
 */


import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Stopwatch {
    
    private long startTime = 0;
    private long stopTime = 0;
    private boolean running = false;

    
    public void start() {
        this.startTime = System.currentTimeMillis();
        this.running = true;
    }

    
    public void stop() {
        this.stopTime = System.currentTimeMillis();
        this.running = false;
    }

    public void setTimer(long startTime) {
        this.startTime = startTime;
        this.running = true;
    }

    public long getStartTime() {
        return this.startTime;
    }

    //elaspsed time in milliseconds
    public long getElapsedTimeMili() {
        long elapsed = 0;
        if (running) {
            elapsed =((System.currentTimeMillis() - startTime)/100) % 1000 ;
        }
        return elapsed;
    }

    //elaspsed time in seconds
    public long getElapsedTimeSecs() {
        long elapsed = 00;
        if (running) {
            elapsed = ((System.currentTimeMillis() - startTime) / 1000) % 60;
        }
        return elapsed;
    }

    //elaspsed time in minutes
    public long getElapsedTimeMin() {
        long elapsed = 00;
        if (running) {
            elapsed = (((System.currentTimeMillis() - startTime) / 1000) / 60 ) % 60;
        }
        return elapsed;
    }

    //elaspsed time in hours
    public long getElapsedTimeHour() {
        long elapsed = 00;
        if (running) {
            elapsed = ((((System.currentTimeMillis() - startTime) / 1000) / 60 ) / 60);
        }
        return elapsed;
    }

    public String toString() {
        String time = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - startTime) % 24,
                TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startTime) % 60,
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime) % 60);
        return time;
    }
}


