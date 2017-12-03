package com.example.lukas.bluetoothtest;

/**
 * Created by Lukas on 26.11.2017.
 */

public class TripRecord {
    private static TripRecord instance;


    // Attribute
    private String driver;
    private int startMileage;
    private int endMileage;
    private String driveMode;
    private long startTimestamp;
    private long endTimestamp;
    private String startAddress;
    private String endAddress;
    // TODO Höchstgeschwindigkeit, GPS, irgendwas mit Spritverbrauch?

    private TripRecord() {
        super();
    }

    public static TripRecord getTripRecord() {
        if (instance == null) {
            instance = new TripRecord();
        }
        return instance;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public int getStartMileage() {
        return startMileage;
    }

    public void setStartMileage(int startMileage) {
        this.startMileage = startMileage;
    }

    public int getEndMileage() {
        return endMileage;
    }

    public void setEndMileage(int endMileage) {
        this.endMileage = endMileage;
    }

    public String getDriveMode() {
        return driveMode;
    }

    public void setDriveMode(String driveMode) {
        this.driveMode = driveMode;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public String getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(String endAddress) {
        this.endAddress = endAddress;
    }

}
