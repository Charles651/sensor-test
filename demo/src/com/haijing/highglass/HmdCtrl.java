package com.haijing.highglass;

import android.util.Log;
import com.haijing.highglass.HmdContrast;

/**
 * Created by Administrator on 2016/6/17.
 */
public class HmdCtrl {
    private static final String TAG = "HmdCtrl";

    private static final byte COMM_SYNC_BEGIN = (byte)0x55;
    private static final byte COMM_SYNC_END = (byte)0xAA;

    private static final int COMM_LENGTH_MAX = 63;

    private static final int HMD_CTRL_CMD_GET_CONTRAST = 0x00;
    private static final int HMD_CTRL_CMD_SET_CONTRAST = 0x01;
    private static final int HMD_CTRL_CMD_GET_BRIGHTNESS = 0x02;
    private static final int HMD_CTRL_CMD_SET_BRIGHTNESS = 0x03;
    private static final int HMD_CTRL_CMD_GET_SATURATION = 0x04;
    private static final int HMD_CTRL_CMD_SET_SATURATION = 0x05;
    private static final int HMD_CTRL_CMD_GET_3D = 0x06;
    private static final int HMD_CTRL_CMD_SET_3D = 0x07;
    private static final int HMD_CTRL_CMD_GET_VIDEO_STATE = 0x08;
    private static final int HMD_CTRL_CMD_GET_VERSION = 0x09;
    private static final int HMD_CTRL_CMD_GET_LIGHT = 0x0A;

    public static final byte HMD_CTRL_PARAM_DISP_3D_ERROR = (byte)0xFF;
    public static final byte HMD_CTRL_PARAM_DISP_3D_NONE = (byte)0x00;
    public static final byte HMD_CTRL_PARAM_DISP_3D_HALF_LR = (byte)0x01;
    public static final byte HMD_CTRL_PARAM_DISP_3D_HALF_TB = (byte)0x02;
    public static final byte HMD_CTRL_PARAM_DISP_3D_FULL = (byte)0x03;

    public static final byte HMD_CTRL_PARAM_DISP_VIDEO_STATE_ERROR = (byte)0xFF;
    public static final byte HMD_CTRL_PARAM_DISP_VIDEO_STATE_OFF = (byte)0x00;
    public static final byte HMD_CTRL_PARAM_DISP_VIDEO_STATE_ON = (byte)0x01;

    private static HmdContrast contrastGlobal = new HmdContrast();
    private static HmdBrightness brightnessGlobal = new HmdBrightness();
    private static HmdSaturation saturationGlobal = new HmdSaturation();

    static {
        System.loadLibrary("hmd-ctrl");
    }

    private native static int xfer(byte[] sendBuf, int sendCount, byte[] recvBuf, int recvCount);

    private static short calcChkSum(byte[] buffer, int byteOffset, int byteCount) {
        short chkSum = 0;
        int byteEnd;

        byteEnd = byteOffset + byteCount;
        for (int i = byteOffset; i < byteEnd; i++) {
            chkSum += (short)(buffer[i] & 0x00FF);
        }
        return chkSum;
    }

    private static int pack(byte[] packedBuf, int packedCount, byte[] payloadBuf, int payloadOffset, int payloadCount) {
        int index;
        short chkSum;

        if (packedCount < (payloadCount + 6)) {
            Log.d(TAG, "pack : packedBuf too short");
            return -1;
        }

        index = 0;
        packedBuf[index] = COMM_SYNC_BEGIN;
        index++;
        packedBuf[index] = (byte)((payloadCount & 0x0000FF00) >> 8);
        index++;
        packedBuf[index] = (byte)(payloadCount & 0x000000FF);
        index++;
        for (int i = 0; i < payloadCount; i++, index++) {
            packedBuf[index] = payloadBuf[payloadOffset + i];
        }
        chkSum = calcChkSum(packedBuf, 1, payloadCount + 2);
        packedBuf[index] = (byte)((chkSum & 0xFF00) >> 8);
        index++;
        packedBuf[index] = (byte)(chkSum & 0x00FF);
        index++;
        packedBuf[index] = COMM_SYNC_END;
        index++;

        return index;
    }

    private static int unpack(byte[] packedBuf, int packedCount, byte[] payloadBuf, int payloadOffset, int payloadCount) {
        int index;
        short length;
        short chkSumRecv, chkSumCalc;

        index = 0;
        if (packedBuf[index] != COMM_SYNC_BEGIN) {
            Log.d(TAG, "unpack : COMM_SYNC_BEGIN error");
            return -1;
        }
        index++;
        length = (short)((packedBuf[index] & 0x00FF) << 8);
        index++;
        length |= (short)(packedBuf[index] & 0x00FF);
        index++;
        if (packedCount != (length + 6)) {
            Log.d(TAG, "unpack : length error");
            return -1;
        }
        if (payloadCount < length) {
            Log.d(TAG, "unpack : payloadBuf too short");
            return -1;
        }
        for (int i = 0; i < length; i++, index++) {
            payloadBuf[payloadOffset + i] = packedBuf[index];
        }
        chkSumCalc = calcChkSum(packedBuf, 1, length + 2);

        chkSumRecv = (short)((packedBuf[index] & 0x00FF) << 8);
        index++;
        chkSumRecv |= (short)(packedBuf[index] & 0x00FF);
        index++;

        if (chkSumRecv != chkSumCalc) {
            Log.d(TAG, "unpack : chkSum error");
            return -1;
        }
        if (packedBuf[index] != COMM_SYNC_END) {
            Log.d(TAG, "unpack : COMM_SYNC_END error");
            return -1;
        }

        return length;
    }

    private synchronized static int xferSync(byte[] sendBuf, int sendCount, byte[] recvBuf, int recvCount) {
        int ret;
        ret = xfer(sendBuf, sendCount, recvBuf, recvCount);
        return ret;
    }

    /**
     * set contrast.
     * tx : [cmd] [contrast.w] [contrast.r] [contrast.g] [contrast.b]
     * rx : [result] [contrast.w] [contrast.r] [contrast.g] [contrast.b]
     * @param contrast HmdContrast to set
     * @return HmdContrast set
     */
    public static HmdContrast setContrast(HmdContrast contrast) {
        byte[] sendPackedBuf = new byte[COMM_LENGTH_MAX];
        int sendPackedCount;
        byte[] recvPackedBuf = new byte[COMM_LENGTH_MAX];
        int recvPackedCount;
        byte[] sendCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int sendCmdCount;
        byte[] recvCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int recvCmdCount;

        sendCmdCount = 0;
        sendCmdBuf[sendCmdCount] = HMD_CTRL_CMD_SET_CONTRAST;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = (byte) contrast.w;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = (byte) contrast.r;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = (byte) contrast.g;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = (byte) contrast.b;
        sendCmdCount++;

        sendPackedCount = pack(sendPackedBuf, sendPackedBuf.length, sendCmdBuf, 0, sendCmdCount);
        if (sendPackedCount < 0) {
            Log.e(TAG, "pack error");
            return null;
        }
        recvPackedCount = xferSync(sendPackedBuf, sendPackedCount, recvPackedBuf, recvPackedBuf.length);
        if (recvPackedCount < 0) {
            Log.e(TAG, "xferSync error");
            return null;
        }
        recvCmdCount = unpack(recvPackedBuf, recvPackedCount, recvCmdBuf, 0, recvCmdBuf.length);
        if (recvCmdCount < 0) {
            Log.e(TAG, "unpack error");
            return null;
        }

        Log.d(TAG, "recv count : " + recvCmdCount);

        if (recvCmdBuf[0] != 0) {
            Log.e(TAG, "result error : " + recvCmdBuf[0]);
            return null;
        }

        contrast.w = recvCmdBuf[1];
        contrast.r = recvCmdBuf[2];
        contrast.g = recvCmdBuf[3];
        contrast.b = recvCmdBuf[4];

        return contrast;
    }

    /**
     * get contrast.
     * tx : [cmd]
     * rx : [result] [contrast.w] [contrast.r] [contrast.g] [contrast.b]
     * @return HmdContrast
     */
    public static HmdContrast getContrast() {
        byte[] sendPackedBuf = new byte[COMM_LENGTH_MAX];
        int sendPackedCount;
        byte[] recvPackedBuf = new byte[COMM_LENGTH_MAX];
        int recvPackedCount;
        byte[] sendCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int sendCmdCount;
        byte[] recvCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int recvCmdCount;

        sendCmdCount = 0;
        sendCmdBuf[sendCmdCount] = HMD_CTRL_CMD_GET_CONTRAST;
        sendCmdCount++;

        sendPackedCount = pack(sendPackedBuf, sendPackedBuf.length, sendCmdBuf, 0, sendCmdCount);
        if (sendPackedCount < 0) {
            Log.e(TAG, "pack error");
            return null;
        }
        recvPackedCount = xferSync(sendPackedBuf, sendPackedCount, recvPackedBuf, recvPackedBuf.length);
        if (recvPackedCount < 0) {
            Log.e(TAG, "xferSync error");
            return null;
        }
        recvCmdCount = unpack(recvPackedBuf, recvPackedCount, recvCmdBuf, 0, recvCmdBuf.length);
        if (recvCmdCount < 0) {
            Log.e(TAG, "unpack error");
            return null;
        }

        Log.d(TAG, "recv count : " + recvCmdCount);

        if (recvCmdBuf[0] != 0) {
            Log.e(TAG, "result error : " + recvCmdBuf[0]);
            return null;
        }

        HmdContrast contrast = new HmdContrast();
        contrast.w = recvCmdBuf[1];
        contrast.r = recvCmdBuf[2];
        contrast.g = recvCmdBuf[3];
        contrast.b = recvCmdBuf[4];

        return contrast;
    }

    /**
     * set brightness.
     * tx : [cmd] [brightness.w] [brightness.r] [brightness.g] [brightness.b]
     * rx : [result] [brightness.w] [brightness.r] [brightness.g] [brightness.b]
     * @param brightness HmdBrightness to set
     * @return HmdBrightness set
     */
    public static HmdBrightness setBrightness(HmdBrightness brightness) {
        byte[] sendPackedBuf = new byte[COMM_LENGTH_MAX];
        int sendPackedCount;
        byte[] recvPackedBuf = new byte[COMM_LENGTH_MAX];
        int recvPackedCount;
        byte[] sendCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int sendCmdCount;
        byte[] recvCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int recvCmdCount;

        sendCmdCount = 0;
        sendCmdBuf[sendCmdCount] = HMD_CTRL_CMD_SET_BRIGHTNESS;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = (byte) brightness.w;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = (byte) brightness.r;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = (byte) brightness.g;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = (byte) brightness.b;
        sendCmdCount++;

        sendPackedCount = pack(sendPackedBuf, sendPackedBuf.length, sendCmdBuf, 0, sendCmdCount);
        if (sendPackedCount < 0) {
            Log.e(TAG, "pack error");
            return null;
        }
        recvPackedCount = xferSync(sendPackedBuf, sendPackedCount, recvPackedBuf, recvPackedBuf.length);
        if (recvPackedCount < 0) {
            Log.e(TAG, "xferSync error");
            return null;
        }
        recvCmdCount = unpack(recvPackedBuf, recvPackedCount, recvCmdBuf, 0, recvCmdBuf.length);
        if (recvCmdCount < 0) {
            Log.e(TAG, "unpack error");
            return null;
        }

        Log.d(TAG, "recv count : " + recvCmdCount);

        if (recvCmdBuf[0] != 0) {
            Log.e(TAG, "result error : " + recvCmdBuf[0]);
            return null;
        }

        brightness.w = recvCmdBuf[1];
        brightness.r = recvCmdBuf[2];
        brightness.g = recvCmdBuf[3];
        brightness.b = recvCmdBuf[4];

        return brightness;
    }

    /**
     * get brightness.
     * tx : [cmd]
     * rx : [result] [brightness.w] [brightness.r] [brightness.g] [brightness.b]
     * @return HmdBrightness
     */
    public static HmdBrightness getBrightness() {
        byte[] sendPackedBuf = new byte[COMM_LENGTH_MAX];
        int sendPackedCount;
        byte[] recvPackedBuf = new byte[COMM_LENGTH_MAX];
        int recvPackedCount;
        byte[] sendCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int sendCmdCount;
        byte[] recvCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int recvCmdCount;

        sendCmdCount = 0;
        sendCmdBuf[sendCmdCount] = HMD_CTRL_CMD_GET_BRIGHTNESS;
        sendCmdCount++;

        sendPackedCount = pack(sendPackedBuf, sendPackedBuf.length, sendCmdBuf, 0, sendCmdCount);
        if (sendPackedCount < 0) {
            Log.e(TAG, "pack error");
            return null;
        }
        recvPackedCount = xferSync(sendPackedBuf, sendPackedCount, recvPackedBuf, recvPackedBuf.length);
        if (recvPackedCount < 0) {
            Log.e(TAG, "xferSync error");
            return null;
        }
        recvCmdCount = unpack(recvPackedBuf, recvPackedCount, recvCmdBuf, 0, recvCmdBuf.length);
        if (recvCmdCount < 0) {
            Log.e(TAG, "unpack error");
            return null;
        }

        Log.d(TAG, "recv count : " + recvCmdCount);

        if (recvCmdBuf[0] != 0) {
            Log.e(TAG, "result error : " + recvCmdBuf[0]);
            return null;
        }

        HmdBrightness brightness = new HmdBrightness();
        brightness.w = recvCmdBuf[1];
        brightness.r = recvCmdBuf[2];
        brightness.g = recvCmdBuf[3];
        brightness.b = recvCmdBuf[4];

        return brightness;
    }

    /**
     * set saturation.
     * tx : [cmd] [saturation.w] [saturation.r] [saturation.g] [saturation.b]
     * rx : [result] [saturation.w] [saturation.r] [saturation.g] [saturation.b]
     * @param saturation HmdSaturation to set
     * @return HmdSaturation set
     */
    public static HmdSaturation setSaturation(HmdSaturation saturation) {
        byte[] sendPackedBuf = new byte[COMM_LENGTH_MAX];
        int sendPackedCount;
        byte[] recvPackedBuf = new byte[COMM_LENGTH_MAX];
        int recvPackedCount;
        byte[] sendCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int sendCmdCount;
        byte[] recvCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int recvCmdCount;

        sendCmdCount = 0;
        sendCmdBuf[sendCmdCount] = HMD_CTRL_CMD_SET_SATURATION;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = (byte) saturation.w;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = (byte) saturation.r;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = (byte) saturation.g;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = (byte) saturation.b;
        sendCmdCount++;

        sendPackedCount = pack(sendPackedBuf, sendPackedBuf.length, sendCmdBuf, 0, sendCmdCount);
        if (sendPackedCount < 0) {
            Log.e(TAG, "pack error");
            return null;
        }
        recvPackedCount = xferSync(sendPackedBuf, sendPackedCount, recvPackedBuf, recvPackedBuf.length);
        if (recvPackedCount < 0) {
            Log.e(TAG, "xferSync error");
            return null;
        }
        recvCmdCount = unpack(recvPackedBuf, recvPackedCount, recvCmdBuf, 0, recvCmdBuf.length);
        if (recvCmdCount < 0) {
            Log.e(TAG, "unpack error");
            return null;
        }

        Log.d(TAG, "recv count : " + recvCmdCount);

        if (recvCmdBuf[0] != 0) {
            Log.e(TAG, "result error : " + recvCmdBuf[0]);
            return null;
        }

        saturation.w = recvCmdBuf[1];
        saturation.r = recvCmdBuf[2];
        saturation.g = recvCmdBuf[3];
        saturation.b = recvCmdBuf[4];

        return saturation;
    }

    /**
     * get saturation.
     * tx : [cmd]
     * rx : [result] [saturation.w] [saturation.r] [saturation.g] [saturation.b]
     * @return HmdSaturation
     */
    public static HmdSaturation getSaturation() {
        byte[] sendPackedBuf = new byte[COMM_LENGTH_MAX];
        int sendPackedCount;
        byte[] recvPackedBuf = new byte[COMM_LENGTH_MAX];
        int recvPackedCount;
        byte[] sendCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int sendCmdCount;
        byte[] recvCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int recvCmdCount;

        sendCmdCount = 0;
        sendCmdBuf[sendCmdCount] = HMD_CTRL_CMD_GET_SATURATION;
        sendCmdCount++;

        sendPackedCount = pack(sendPackedBuf, sendPackedBuf.length, sendCmdBuf, 0, sendCmdCount);
        if (sendPackedCount < 0) {
            Log.e(TAG, "pack error");
            return null;
        }
        recvPackedCount = xferSync(sendPackedBuf, sendPackedCount, recvPackedBuf, recvPackedBuf.length);
        if (recvPackedCount < 0) {
            Log.e(TAG, "xferSync error");
            return null;
        }
        recvCmdCount = unpack(recvPackedBuf, recvPackedCount, recvCmdBuf, 0, recvCmdBuf.length);
        if (recvCmdCount < 0) {
            Log.e(TAG, "unpack error");
            return null;
        }

        Log.d(TAG, "recv count : " + recvCmdCount);

        if (recvCmdBuf[0] != 0) {
            Log.e(TAG, "result error : " + recvCmdBuf[0]);
            return null;
        }

        HmdSaturation saturation = new HmdSaturation();
        saturation.w = recvCmdBuf[1];
        saturation.r = recvCmdBuf[2];
        saturation.g = recvCmdBuf[3];
        saturation.b = recvCmdBuf[4];

        return saturation;
    }

    /**
     * set 3D mode.
     * tx : [cmd] [mode]
     * rx : [result]
     * @param 3D mode to set
     * @return mode set
     */
    public static byte set3D(byte mode) {
        byte[] sendPackedBuf = new byte[COMM_LENGTH_MAX];
        int sendPackedCount;
        byte[] recvPackedBuf = new byte[COMM_LENGTH_MAX];
        int recvPackedCount;
        byte[] sendCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int sendCmdCount;
        byte[] recvCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int recvCmdCount;

        sendCmdCount = 0;
        sendCmdBuf[sendCmdCount] = HMD_CTRL_CMD_SET_3D;
        sendCmdCount++;
        sendCmdBuf[sendCmdCount] = mode;
        sendCmdCount++;

        sendPackedCount = pack(sendPackedBuf, sendPackedBuf.length, sendCmdBuf, 0, sendCmdCount);
        if (sendPackedCount < 0) {
            Log.e(TAG, "pack error");
            return HMD_CTRL_PARAM_DISP_3D_ERROR;
        }
        recvPackedCount = xferSync(sendPackedBuf, sendPackedCount, recvPackedBuf, recvPackedBuf.length);
        if (recvPackedCount < 0) {
            Log.e(TAG, "xferSync error");
            return HMD_CTRL_PARAM_DISP_3D_ERROR;
        }
        recvCmdCount = unpack(recvPackedBuf, recvPackedCount, recvCmdBuf, 0, recvCmdBuf.length);
        if (recvCmdCount < 0) {
            Log.e(TAG, "unpack error");
            return HMD_CTRL_PARAM_DISP_3D_ERROR;
        }

        Log.d(TAG, "recv count : " + recvCmdCount);

        if (recvCmdBuf[0] != 0) {
            Log.e(TAG, "result error : " + recvCmdBuf[0]);
            return HMD_CTRL_PARAM_DISP_3D_ERROR;
        }

        return mode;
    }

    /**
     * get 3D mode.
     * tx : [cmd]
     * rx : [result] [mode]
     * @return 3D mode
     */
    public static byte get3D() {
        byte[] sendPackedBuf = new byte[COMM_LENGTH_MAX];
        int sendPackedCount;
        byte[] recvPackedBuf = new byte[COMM_LENGTH_MAX];
        int recvPackedCount;
        byte[] sendCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int sendCmdCount;
        byte[] recvCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int recvCmdCount;

        sendCmdCount = 0;
        sendCmdBuf[sendCmdCount] = HMD_CTRL_CMD_GET_3D;
        sendCmdCount++;

        sendPackedCount = pack(sendPackedBuf, sendPackedBuf.length, sendCmdBuf, 0, sendCmdCount);
        if (sendPackedCount < 0) {
            Log.e(TAG, "pack error");
            return HMD_CTRL_PARAM_DISP_3D_ERROR;
        }
        recvPackedCount = xferSync(sendPackedBuf, sendPackedCount, recvPackedBuf, recvPackedBuf.length);
        if (recvPackedCount < 0) {
            Log.e(TAG, "xferSync error");
            return HMD_CTRL_PARAM_DISP_3D_ERROR;
        }
        recvCmdCount = unpack(recvPackedBuf, recvPackedCount, recvCmdBuf, 0, recvCmdBuf.length);
        if (recvCmdCount < 0) {
            Log.e(TAG, "unpack error");
            return HMD_CTRL_PARAM_DISP_3D_ERROR;
        }

        Log.d(TAG, "recv count : " + recvCmdCount);

        if (recvCmdBuf[0] != 0) {
            Log.e(TAG, "result error : " + recvCmdBuf[0]);
            return HMD_CTRL_PARAM_DISP_3D_ERROR;
        }

        return recvCmdBuf[1];
    }

    /**
     * get video state.
     * tx : [cmd]
     * rx : [result] [state]
     * @return video state
     */
    public static byte getVideoState() {
        byte[] sendPackedBuf = new byte[COMM_LENGTH_MAX];
        int sendPackedCount;
        byte[] recvPackedBuf = new byte[COMM_LENGTH_MAX];
        int recvPackedCount;
        byte[] sendCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int sendCmdCount;
        byte[] recvCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int recvCmdCount;

        sendCmdCount = 0;
        sendCmdBuf[sendCmdCount] = HMD_CTRL_CMD_GET_VIDEO_STATE;
        sendCmdCount++;

        sendPackedCount = pack(sendPackedBuf, sendPackedBuf.length, sendCmdBuf, 0, sendCmdCount);
        if (sendPackedCount < 0) {
            Log.e(TAG, "pack error");
            return HMD_CTRL_PARAM_DISP_VIDEO_STATE_ERROR;
        }
        recvPackedCount = xferSync(sendPackedBuf, sendPackedCount, recvPackedBuf, recvPackedBuf.length);
        if (recvPackedCount < 0) {
            Log.e(TAG, "xferSync error");
            return HMD_CTRL_PARAM_DISP_VIDEO_STATE_ERROR;
        }
        recvCmdCount = unpack(recvPackedBuf, recvPackedCount, recvCmdBuf, 0, recvCmdBuf.length);
        if (recvCmdCount < 0) {
            Log.e(TAG, "unpack error");
            return HMD_CTRL_PARAM_DISP_VIDEO_STATE_ERROR;
        }

        Log.d(TAG, "recv count : " + recvCmdCount);

        if (recvCmdBuf[0] != 0) {
            Log.e(TAG, "result error : " + recvCmdBuf[0]);
            return HMD_CTRL_PARAM_DISP_VIDEO_STATE_ERROR;
        }

        return recvCmdBuf[1];
    }
    
    
    public static int getLightState() {
        byte[] sendPackedBuf = new byte[COMM_LENGTH_MAX];
        int sendPackedCount;
        byte[] recvPackedBuf = new byte[COMM_LENGTH_MAX];
        int recvPackedCount;
        byte[] sendCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int sendCmdCount;
        byte[] recvCmdBuf = new byte[COMM_LENGTH_MAX - 6];
        int recvCmdCount;

        sendCmdCount = 0;
        sendCmdBuf[sendCmdCount] = HMD_CTRL_CMD_GET_LIGHT;
        sendCmdCount++;

        sendPackedCount = pack(sendPackedBuf, sendPackedBuf.length, sendCmdBuf, 0, sendCmdCount);
        if (sendPackedCount < 0) {
            Log.e(TAG, "pack error");
            return HMD_CTRL_PARAM_DISP_VIDEO_STATE_ERROR;
        }
        recvPackedCount = xferSync(sendPackedBuf, sendPackedCount, recvPackedBuf, recvPackedBuf.length);
        if (recvPackedCount < 0) {
            Log.e(TAG, "xferSync error");
            return HMD_CTRL_PARAM_DISP_VIDEO_STATE_ERROR;
        }
        recvCmdCount = unpack(recvPackedBuf, recvPackedCount, recvCmdBuf, 0, recvCmdBuf.length);
        if (recvCmdCount < 0) {
            Log.e(TAG, "unpack error");
            return HMD_CTRL_PARAM_DISP_VIDEO_STATE_ERROR;
        }

        

        if (recvCmdBuf[0] != 0) {
            Log.e(TAG, "result error : " + recvCmdBuf[0]);
            return HMD_CTRL_PARAM_DISP_VIDEO_STATE_ERROR;
        }
        
        Log.d(TAG, "recv light: " + (Integer.valueOf((recvCmdBuf[2]&0xff))<<8 |(recvCmdBuf[1]&0xff)));

        return Integer.valueOf((recvCmdBuf[2]&0xff)<<8 |(recvCmdBuf[1]&0xff));
    }
}
