/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Myriad Mobile
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.myriadmobile.library.lantern;


import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class that represents an iBeacon.
 */
public class IBeacon extends Beacon implements Parcelable {

    public IBeacon() {

    }

    public IBeacon(String uuid, int major, int minor, int txPower, int rssi) {
        super(uuid, major, minor, txPower, rssi);
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public String getUuid() {
        return uuid;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public Integer getProximity() {
        return proximity;
    }

    public int getRssi() {
        return rssi;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    protected void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    @Override
    public int hashCode() {
        return minor;
    }

    /**
     * Compares two beacons for parity. Two iBeacons are considered equal when UUID, Major,
     * and Minor states are equal.
     *
     * @param that The other beacon being tested for equality.
     * @return Whether the beacons are equal.
     */
    @Override
    public boolean equals(Object that) {
        if (!(that instanceof IBeacon)) {
            return false;
        }
        IBeacon thatBeacon = (IBeacon) that;
        return (thatBeacon.getMajor() == this.getMajor() &&
                thatBeacon.getMinor() == this.getMinor() &&
                thatBeacon.getUuid().equals(this.getUuid()));
    }

    /**
     * Estimates the distance of a beacon.
     *
     * @param rssi    The RSSI of a beacon.
     * @param txPower The calibrated tx power of a beacon.
     * @return The distance calculated of the beacon.
     */
    public static double calculateDistance(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0;
        }

        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            double distance = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            return distance;
        }
    }

    /**
     * Finds the proximity value of a beacon.
     *
     * @param distance The distance of the beacon.
     * @return The proximity that was calculated.
     */
    public static int calculateProximity(double distance) {
        if (distance < 0) {
            return PROXIMITY_UNKNOWN;
        }
        if (distance < 0.5) {
            return PROXIMITY_IMMEDIATE;
        }
        if (distance <= 4.0) {
            return PROXIMITY_NEAR;
        }
        return PROXIMITY_FAR;

    }

    /**
     * Returns the proximity as a human readable string.
     *
     * @param proximity The proximity of the beacon.
     * @return The human readable proximity.
     */
    public static String proximityToString(int proximity) {
        if (proximity == 1) {
            return "Immediate";
        } else if (proximity == 2) {
            return "Near";
        } else if (proximity == 3) {
            return "Far";
        }
        return "Unknown";
    }


    /**
     * Returns a beacon object from the data obtained from a low energy scan.
     *
     * @param rssi     The RSSI of the beacon.
     * @param device   The beacon device.
     * @param scanData The data obtained from the scan.
     * @return The beacon object.
     */
    public static IBeacon fromScanData(byte[] scanData, int rssi, BluetoothDevice device) {
        int startByte = 2;
        boolean patternFound = false;
        while (startByte <= 5) {
            if (((int) scanData[startByte + 2] & 0xff) == 0x02 && ((int) scanData[startByte + 3] & 0xff) == 0x15) {
                patternFound = true;
                break;
            }
            startByte++;
        }


        if (!patternFound) {
            return null;
        }

        IBeacon iBeacon = new IBeacon();

        iBeacon.major = (scanData[startByte + 20] & 0xff) * 0x100 + (scanData[startByte + 21] & 0xff);
        iBeacon.minor = (scanData[startByte + 22] & 0xff) * 0x100 + (scanData[startByte + 23] & 0xff);
        iBeacon.txPower = (int) scanData[startByte + 24]; // this one is signed
        iBeacon.rssi = rssi;
        iBeacon.distance = calculateDistance(iBeacon.txPower, iBeacon.rssi);
        iBeacon.proximity = calculateProximity(calculateDistance(iBeacon.txPower, iBeacon.rssi));


        byte[] proximityUuidBytes = new byte[16];
        System.arraycopy(scanData, startByte + 4, proximityUuidBytes, 0, 16);
        String hexString = bytesToHex(proximityUuidBytes);
        StringBuilder sb = new StringBuilder();
        sb.append(hexString.substring(0, 8));
        sb.append("-");
        sb.append(hexString.substring(8, 12));
        sb.append("-");
        sb.append(hexString.substring(12, 16));
        sb.append("-");
        sb.append(hexString.substring(16, 20));
        sb.append("-");
        sb.append(hexString.substring(20, 32));
        iBeacon.uuid = sb.toString();

        if (device != null) {
            iBeacon.bluetoothAddress = device.getAddress();
        }

        return iBeacon;
    }

    /**
     * Converts bytes to hex.
     *
     * @param bytes The bytes to be converted.
     * @return The hex that was converted from the bytes.
     */
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    protected IBeacon(Parcel in) {
        uuid = in.readString();
        major = in.readInt();
        minor = in.readInt();
        proximity = in.readByte() == 0x00 ? null : in.readInt();
        distance = in.readByte() == 0x00 ? null : in.readDouble();
        rssi = in.readInt();
        txPower = in.readInt();
        bluetoothAddress = in.readString();
        expirationTime = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uuid);
        dest.writeInt(major);
        dest.writeInt(minor);
        if (proximity == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(proximity);
        }
        if (distance == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeDouble(distance);
        }
        dest.writeInt(rssi);
        dest.writeInt(txPower);
        dest.writeString(bluetoothAddress);
        dest.writeLong(expirationTime);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<IBeacon> CREATOR = new Parcelable.Creator<IBeacon>() {
        @Override
        public IBeacon createFromParcel(Parcel in) {
            return new IBeacon(in);
        }

        @Override
        public IBeacon[] newArray(int size) {
            return new IBeacon[size];
        }
    };

}