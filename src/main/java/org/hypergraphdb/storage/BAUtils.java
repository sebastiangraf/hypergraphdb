/* 
 * This file is part of the HyperGraphDB source distribution. This is copyrighted 
 * software. For permitted uses, licensing options and redistribution, please see  
 * the LicensingInformation file at the root level of the distribution.  
 * 
 * Copyright (c) 2005-2010 Kobrix Software, Inc.  All rights reserved. 
 */
package org.hypergraphdb.storage;

/**
 * <p>
 * This class contains some utilities methods to read/write primitively typed
 * values from/to a byte buffer.
 * </p>
 * 
 * @author Borislav Iordanov
 */
public class BAUtils {
    public static long readLong(byte[] bytes, int offset) {
        return new Long(
                (((long) bytes[offset] << 56)
                        + ((long) (bytes[offset + 1] & 255) << 48)
                        + ((long) (bytes[offset + 2] & 255) << 40)
                        + ((long) (bytes[offset + 3] & 255) << 32)
                        + ((long) (bytes[offset + 4] & 255) << 24)
                        + ((bytes[offset + 5] & 255) << 16)
                        + ((bytes[offset + 6] & 255) << 8) + ((bytes[offset + 7] & 255) << 0)));
    }

    public static void writeLong(long v, byte[] data, int offset) {
        data[offset] = (byte) ((v >>> 56));
        data[offset + 1] = (byte) ((v >>> 48));
        data[offset + 2] = (byte) ((v >>> 40));
        data[offset + 3] = (byte) ((v >>> 32));
        data[offset + 4] = (byte) ((v >>> 24));
        data[offset + 5] = (byte) ((v >>> 16));
        data[offset + 6] = (byte) ((v >>> 8));
        data[offset + 7] = (byte) ((v >>> 0));
    }

    public static int readInt(byte[] buffer, int offset) {
        int ch1 = buffer[offset];
        int ch2 = buffer[offset + 1];
        int ch3 = buffer[offset + 2];
        int ch4 = buffer[offset + 3];
        return ((ch1 & 0xFF) << 24) | ((ch2 & 0xFF) << 16)
                | ((ch3 & 0xFF) << 8) | (ch4 & 0xFF);
    }

    public static void writeInt(int c, byte[] buffer, int offset) {
        buffer[offset + 0] = (byte) ((c >>> 24) & 0xFF);
        buffer[offset + 1] = (byte) ((c >>> 16) & 0xFF);
        buffer[offset + 2] = (byte) ((c >>> 8) & 0xFF);
        buffer[offset + 3] = (byte) ((c >>> 0) & 0xFF);
    }

    public static boolean eq(byte[] left, int leftPos, byte[] right,
            int rightPos, int size) {
        int i = leftPos, j = rightPos;
        if (leftPos + size > left.length)
            return false;
        if (rightPos + size > right.length)
            return false;
        while (size > 0) {
            if (left[i++] != right[j++])
                return false;
            size--;
        }
        return true;
    }

    public static int compare(byte[] left, int leftPos, byte[] right,
            int rightPos, int max) {
        int maxLeft = leftPos + max;
        int maxRight = rightPos + max;
        int i = leftPos;
        int j = rightPos;
        int comp = 0;
        while (comp == 0 && i < maxLeft && j < maxRight)
            comp = left[i++] - right[j++];
        return comp;
    }
}
