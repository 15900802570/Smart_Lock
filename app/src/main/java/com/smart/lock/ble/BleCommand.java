package com.smart.lock.ble;

import android.util.Log;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AT命令解析
 */
public class BleCommand {

    public static final String TAG = BleCommand.class.getSimpleName();

    /**
     * 对象池队列
     */
    private static Queue<BleCommand> queue = new ConcurrentLinkedQueue<BleCommand>();
    /**
     * 对象池计数器
     */
    private static int count = 0;

    /**
     * 完整指令
     */
    public String command;
    /**
     * 指令头
     */
    public String header;
    /**
     * 指令体，按逗号拆分过的数据
     */
    public String[] body;

    public static List<BleCommand> parse(byte[] command, List<BleCommand> list) {
        if (list == null) {
            list = new LinkedList<BleCommand>();
        }

        if (command.length == 0) {
            Log.w(TAG, "command is empty");
            return list;
        }

        parseCommand(command, list);
        return list;
    }

    /**
     * 解析命令
     */
    private static void parseCommand(byte[] command, List<BleCommand> atList) {
        Log.d(TAG,Arrays.toString(command));


    }

    public static BleCommand obtain() {
        Log.d(TAG, "obtain count " + count);

        BleCommand bleCommand = queue.poll();

        if (bleCommand == null) {
            bleCommand = new BleCommand();
        } else {
            count--;
        }

        return bleCommand;
    }

    public static void recycle(List<BleCommand> atList) {
        for (BleCommand bleCommand : atList) {
            recycle(bleCommand);
        }
    }

    public static void recycle(BleCommand command) {
        command.clear();

        if (queue.offer(command)) {
            count++;
        } else {
            Log.w(TAG, "ATCommand queue is full");
        }

        Log.d(TAG, "recycle count " + count);
    }

    public void clear() {
        command = null;
        header = null;
        body = null;
    }

    @Override
    public String toString() {
        return "bleCommand [command=" + command + ", header=" + header
                + ", body=" + Arrays.toString(body) + "]";
    }

}
