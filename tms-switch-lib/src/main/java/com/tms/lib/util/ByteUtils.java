package com.tms.lib.util;

public class ByteUtils {

    private ByteUtils(){

    }
    public static int decodeSignedLenBytes(byte firstByte, byte secondByte) {
        return ((firstByte & 255) << 8) + (secondByte & 255);
    }

    public static byte[] prependLenBytes(byte[] data) {
        short len = (short)data.length;
        byte[] newBytes = new byte[len + 2];
        newBytes[0] = (byte)(len / 256);
        newBytes[1] = (byte)(len & 255);
        System.arraycopy(data, 0, newBytes, 2, len);
        return newBytes;
    }

    public static byte[] exclusiveOr(byte[] data1, byte[] data2) {
        byte[] result = new byte[Math.min(data1.length, data2.length)];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (((int) data1[i]) ^ ((int) data2[i]));
        }
        return result;
    }
}
