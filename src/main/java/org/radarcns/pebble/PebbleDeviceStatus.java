/*
 * Copyright 2017 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarcns.pebble;

import android.os.Parcel;

import org.radarcns.android.device.DeviceStateCreator;
import org.radarcns.android.device.BaseDeviceState;
import org.radarcns.util.Serialization;

import java.util.Arrays;

/**
 * The status on a single point in time of an Empatica E4 device.
 */
public class PebbleDeviceStatus extends BaseDeviceState {
    private float[] acceleration = {Float.NaN, Float.NaN, Float.NaN};
    private float batteryLevel = Float.NaN;
    private Boolean batteryIsCharging = null;
    private Boolean batteryIsPlugged = null;
    private float heartRate = Float.NaN;
    private float heartRateFiltered = Float.NaN;

    public static final Creator<PebbleDeviceStatus> CREATOR = new DeviceStateCreator<>(PebbleDeviceStatus.class);

    @Override
    public synchronized void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeFloat(this.acceleration[0]);
        dest.writeFloat(this.acceleration[1]);
        dest.writeFloat(this.acceleration[2]);
        dest.writeFloat(this.batteryLevel);
        dest.writeByte(Serialization.booleanToByte(batteryIsCharging));
        dest.writeByte(Serialization.booleanToByte(batteryIsPlugged));
        dest.writeFloat(this.heartRate);
        dest.writeFloat(this.heartRateFiltered);
    }

    public void updateFromParcel(Parcel in) {
        super.updateFromParcel(in);
        acceleration[0] = in.readFloat();
        acceleration[1] = in.readFloat();
        acceleration[2] = in.readFloat();
        batteryLevel = in.readFloat();
        batteryIsCharging = Serialization.byteToBoolean(in.readByte());
        batteryIsPlugged = Serialization.byteToBoolean(in.readByte());
        heartRate = in.readFloat();
        heartRateFiltered = in.readFloat();
    }

    @Override
    public boolean hasAcceleration() {
        return true;
    }

    @Override
    public float[] getAcceleration() {
        return acceleration;
    }

    public synchronized void setAcceleration(float x, float y, float z) {
        this.acceleration[0] = x;
        this.acceleration[1] = y;
        this.acceleration[2] = z;
    }

    @Override
    public float getBatteryLevel() {
        return batteryLevel;
    }

    public synchronized void setBatteryLevel(float batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public Boolean getBatteryIsCharging() {
        return batteryIsCharging;
    }

    public void setBatteryIsCharging(Boolean batteryIsCharging) {
        this.batteryIsCharging = batteryIsCharging;
    }

    public Boolean getBatteryIsPlugged() {
        return batteryIsPlugged;
    }

    public void setBatteryIsPlugged(Boolean batteryIsPlugged) {
        this.batteryIsPlugged = batteryIsPlugged;
    }

    @Override
    public boolean hasHeartRate() {
        return true;
    }

    @Override
    public float getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(float heartRate) {
        this.heartRate = heartRate;
    }

    public float getHeartRateFiltered() {
        return heartRateFiltered;
    }

    public void setHeartRateFiltered(float heartRateFiltered) {
        this.heartRateFiltered = heartRateFiltered;
    }

    public String toString() {
        return "{status: " + getStatus() + ", acceleration: " + Arrays.toString(acceleration) +
                ", batteryLevel: " + batteryLevel + ", heartRate: " + heartRate +
                ", heartRateFiltered: " + heartRateFiltered + "}";
    }
}
