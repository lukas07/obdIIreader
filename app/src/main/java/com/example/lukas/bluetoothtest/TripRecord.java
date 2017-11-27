package com.example.lukas.bluetoothtest;

/**
 * Created by Lukas on 26.11.2017.
 */

public class TripRecord {
    private static TripRecord instance;


    // Attribute
    private String driver;
    private int mileage;
    private String driveMode;
    private long startTimestamp;
    private long endTimestamp;
    private String startAddress;
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

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
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
}
