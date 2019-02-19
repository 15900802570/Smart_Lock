
package com.smart.lock.ble;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Arrays;

/**
 * Ble Message Session, the commond and response will be one by one.
 */
public class MsgSession
{
    private final static String TAG = "MsgSession";

    public static final int INT_SESSION_TYPE_CMD = 0x01;
    public static final int INT_SESSION_TYPE_RSP = 0x02;
    public static final int INT_SESSION_TYPE_CMD_03 = 0x03;
    private static final int INT_SESSION_PACKAGE_LEN = 20;
    private static final int INT_SESSION_BUF_MAX_LEN = 512;

    private static int mCmdRead;
    private static int mCmdWrite;
    private static int mCmdLength;
    private static byte[] mCmdBuf;

    private static int mRspRead;
    private static int mRspWrite;
    private static int mRspLength;
    private static boolean mRspNext;
    private static byte[] mRspBuf;

    private static Handler mChannelHandler;

    /**
     * MsgSession().
     */
    public MsgSession(Handler h)
    {
        mChannelHandler = h;
        mCmdRead = mCmdWrite = mCmdLength = 0;
        mRspRead = mRspWrite = mRspLength = 0;
        mCmdBuf = new byte[INT_SESSION_BUF_MAX_LEN];
        mRspBuf = new byte[INT_SESSION_BUF_MAX_LEN];
    }

    public static boolean hasNextPacket(int type)
    {
        if (INT_SESSION_TYPE_CMD == type)
        {
            if (mCmdLength - mCmdRead > INT_SESSION_PACKAGE_LEN)
                return true;
            else
                return false;
        }
        else if (INT_SESSION_TYPE_RSP == type)
        {
            if (mRspLength - mRspRead > INT_SESSION_PACKAGE_LEN)
                return true;
            else
                return false;
        }
        else
        {
            Log.e(TAG, "hasNextPacket() param fail.");
            return false;
        }
    }

    public static boolean isNewSession(int type)
    {
        if (INT_SESSION_TYPE_CMD == type)
        {
            if (0 == mCmdLength)
                return true;
            else
                return false;
        }
        else if (INT_SESSION_TYPE_RSP == type)
        {
            if (0 == mRspLength)
                return true;
            else
                return false;
        }
        else
        {
            Log.e(TAG, "isNewSession() param fail.");
            return true;
        }
    }

    private static void clearSession(int type)
    {
        if (INT_SESSION_TYPE_CMD == type)
        {
            mCmdRead = 0;
            mCmdLength = 0;
        }
        else if (INT_SESSION_TYPE_RSP == type)
        {
            mRspRead = 0;
            mRspLength = 0;
        }
        else
        {
            Log.e(TAG, "clearSession() param fail.");
        }
    }

    /**
     * byte2Int().
     */
    private static int byte2Int(byte[] bytes)
    {
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    public static int getCmdID()
    {
        if (0 == mCmdLength)
            return 0;

        if (0x70 == mCmdBuf[0])
        {
            byte[] c1 = Arrays.copyOfRange(mCmdBuf, 2, 6);

            return byte2Int(c1);
        }
        else
        {
            return 0;
        }
    }

    public synchronized static boolean initSession(int type, int length)
    {
        Log.w(TAG, "initSession() type = " + type + " length = " + length);

        if (INT_SESSION_TYPE_CMD == type)
        {
            if (length > INT_SESSION_BUF_MAX_LEN)
                return false;

            mCmdLength = length;
            mCmdRead = mCmdWrite = 0;

            for (int i = 0; i < INT_SESSION_BUF_MAX_LEN; i++)
            {
                mCmdBuf[i] = 0;
            }

            return true;
        }
        else if (INT_SESSION_TYPE_RSP == type)
        {
            if (length > INT_SESSION_BUF_MAX_LEN)
                return false;

            mRspLength = length;
            mRspRead = mRspWrite = 0;

            for (int i = 0; i < INT_SESSION_BUF_MAX_LEN; i++)
            {
                mRspBuf[i] = 0;
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    public static boolean writeData(int type, byte[] data, int length)
    {
        if (length >= INT_SESSION_BUF_MAX_LEN)
        {
            Log.e(TAG, "writeData() fail length >= INT_SESSION_BUF_MAX_LEN");
            return false;
        }


        if (INT_SESSION_TYPE_CMD == type)
        {
            if (0 == mCmdLength)
            {
                Log.e(TAG, "writeData() fail mCmdLength = 0");
                return false;
            }

            if (mCmdWrite + length > mCmdLength)
            {
                for (int i = 0; i < mCmdLength - mCmdWrite; i++)
                {
                    mCmdBuf[mCmdWrite + i] = data[i];
                }

                Log.w(TAG, "writeData() mCmdWrite + length > mCmdLength");

                sendMessage(BleMsg.ID_EVENT_SEND_DATA);

                return true;
            }
            else
            {
                for (int i = 0; i < length; i++)
                {
                    mCmdBuf[mCmdWrite + i] = data[i];
                }

                Log.w(TAG, "writeData() mCmdWrite + length > mCmdLength");

                mCmdWrite = mCmdWrite + length;
            }

            if (mCmdWrite >= mCmdLength)
            {
                sendMessage(BleMsg.ID_EVENT_SEND_DATA);
            }

            return true;
        }
        else if (INT_SESSION_TYPE_RSP == type)
        {
            if (0 == mRspLength)
            {
                Log.e(TAG, "writeData() fail mRspLength = 0");
                return false;
            }

            Log.i(TAG, "writeData() mRspWrite = " + mRspWrite + " mRspLength = " + mRspLength);

            if (mRspWrite + length > mRspLength)
            {
                for (int i = 0; i < mRspLength - mRspWrite; i++)
                {
                    mRspBuf[mRspWrite + i] = data[i];
                }

                Log.i(TAG, "writeData() mRspWrite + length > mRspLength");

                sendMessage(BleMsg.ID_EVENT_RECV_DATA);

                return true;
            }
            else
            {
                for (int i = 0; i < length; i++)
                {
                    mRspBuf[mRspWrite + i] = data[i];
                }

                mRspWrite = mRspWrite + length;
            }

            if (mRspWrite >= mRspLength)
            {
                Log.i(TAG, "writeData() mRspWrite >= mRspLength, sendMessage!!!!");

                sendMessage(BleMsg.ID_EVENT_RECV_DATA);
            }

            return true;
        } 
        else
        {
            Log.e(TAG, "writeData() fail type = ." + type);
            return false;
        }
    }

    public  static int readData(int type, byte[] data, int len)
    {
        int read_length;

        Log.i(TAG, "readData() type = " + type + " len = " + len);

        if (INT_SESSION_TYPE_CMD == type)
        {
            if (0 == mCmdLength)
            {
                Log.e(TAG, "readData() mCmdLength = 0.");
                return 0;
            }

            if (mCmdLength == mCmdRead)
            {
                return 0;
            }

            if (len > mCmdLength - mCmdRead)
            {
                read_length = mCmdLength - mCmdRead;

                for (int i = 0; i < read_length; i++)
                {
                    data[i] = mCmdBuf[mCmdRead + i];
                }

                mCmdRead = mCmdLength;

                clearSession(type);

                return read_length;
            }
            else
            {
                for (int i = 0; i < len; i++)
                {
                    data[i] = mCmdBuf[mCmdRead + i];
                }

                mCmdRead = mCmdRead + len;

                return len;
            }
        }
        else
        {
            if (0 == mRspLength)
            {
                Log.e(TAG, "readData() mRspLength = 0.");
                return 0;
            }

            if (mRspLength == mRspRead)
            {
                return 0;
            }

            if (len > mRspLength - mRspRead)
            {
                read_length = mRspLength - mRspRead;

                for (int i = 0; i < read_length; i++)
                {
                    data[i] = mRspBuf[mRspRead + i];
                }

                mRspRead = mRspLength;

                clearSession(type);

                return read_length;
            }
            else
            {
                for (int i = 0; i < len; i++)
                {
                    data[i] = mRspBuf[mRspRead + i];
                }

                mRspRead = mRspRead + len;

                return len;
            }
        }
    }

    public synchronized static int readPacket(int type, byte[] data)
    {
        int read_length = readData(type, data, 20);

        if (read_length > 0)
        {
            if (INT_SESSION_TYPE_CMD == type)
            {
                sendMessage(BleMsg.ID_EVENT_SEND_DATA);
            }
            else
            {
                sendMessage(BleMsg.ID_EVENT_RECV_DATA);
            }

            return read_length;
        }
        else
        {
            clearSession(type);
        }

        return 0;
    }

    private synchronized static boolean sendMessage(int msg_id)
    {
        Message msg = new Message();
        msg.what = msg_id;
        msg.obj = null;

        if (null != mChannelHandler)
        {
            mChannelHandler.sendMessage(msg);
        }

        return true;
    }
}
