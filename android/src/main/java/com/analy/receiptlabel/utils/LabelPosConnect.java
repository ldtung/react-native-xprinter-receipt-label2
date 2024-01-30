package com.analy.receiptlabel.utils;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import net.posprinter.IDeviceConnection;
import net.posprinter.IPOSListener;
import net.posprinter.POSConnect;
import net.posprinter.POSPrinter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import b.c;
import d.a;

public class LabelPosConnect {
    public static Context appCtx;
    private static c a;
    public static ExecutorService backgroundThreadExecutor;

    public static Executor mainThreadExecutor = new LabelPosConnect.b();
    public static final int CONNECT_SUCCESS = 1;
    public static final int CONNECT_FAIL = 2;
    public static final int SEND_FAIL = 3;
    public static final int CONNECT_INTERRUPT = 4;
    public static final int USB_ATTACHED = 5;
    public static final int USB_DETACHED = 6;
    public static final int BLUETOOTH_INTERRUPT = 7;
    public static final int DEVICE_TYPE_USB = 1;
    public static final int DEVICE_TYPE_BLUETOOTH = 2;
    public static final int DEVICE_TYPE_ETHERNET = 3;
    public static final int DEVICE_TYPE_SERIAL = 4;
    private static Handler b;

    public LabelPosConnect() {
    }

    public static void init(Context appContext) {
        init(appContext, (IPOSListener)null);
    }

    /** @deprecated */
    @Deprecated
    public static void init(Context appContext, IPOSListener listener) {
        appCtx = appContext.getApplicationContext();
        if (backgroundThreadExecutor == null) {
            backgroundThreadExecutor = Executors.newSingleThreadExecutor();
        }

        if (a == null) {
            a = new c(listener);
        }

        String appContext1 = GetCopyRight();
        System.out.print(appContext1.length() + "");
    }

    /** @deprecated */
    @Deprecated
    public static void connectUSB() {
        connectUSB((String)null);
    }

    /** @deprecated */
    @Deprecated
    public static void connectUSB(String usbPathName) {
        if (appCtx != null) {
            a.c(usbPathName);
        } else {
            throw new NullPointerException("Please call init method first");
        }
    }

    /** @deprecated */
    @Deprecated
    public static void connectBT(String address) {
        if (appCtx != null) {
            a.a(address);
        } else {
            throw new NullPointerException("Please call init method first");
        }
    }

    /** @deprecated */
    @Deprecated
    public static void connectNet(String ip) {
        if (appCtx != null) {
            a.b(ip);
        } else {
            throw new NullPointerException("Please call init method first");
        }
    }

    /** @deprecated */
    @Deprecated
    public static void connectSerial(String serialName, String serialBaudRate) {
        if (appCtx != null) {
            a.a(serialName, serialBaudRate);
        } else {
            throw new NullPointerException("Please call init method first");
        }
    }

    public static IDeviceConnection connectMac(String mac, final IPOSListener listener) {
        if (appCtx != null) {
            (b = new Handler(Looper.getMainLooper()) {
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == 1000) {
                        listener.onStatus(2, "device not found");
                    }

                }
            }).sendEmptyMessageDelayed(1000, 2000L);
            IDeviceConnection var10000 = createDevice(3);
            POSPrinter.searchNetDevice((device) -> {
                if (device.getMacStr().equals(mac)) {
                    b.removeMessages(1000);
                    var10000.connect(device.getIpStr(), listener);
                }

            });
            return var10000;
        } else {
            throw new NullPointerException("Please call init method first");
        }
    }

    /** @deprecated */
    @Deprecated
    public static IDeviceConnection getConnect() {
        if (appCtx != null) {
            return a.c();
        } else {
            throw new NullPointerException("Please call init method first");
        }
    }

    public static IDeviceConnection createDevice(int deviceType) {
        if (appCtx != null) {
            return a.a(deviceType);
        } else {
            throw new NullPointerException("Please call init method first");
        }
    }

    /** @deprecated */
    @Deprecated
    public static void disconnect() {
        c var0;
        if ((var0 = a) != null) {
            var0.a();
        }

    }

    public static void exit() {
        c var0;
        if ((var0 = a) != null) {
            var0.b();
        }

        ExecutorService var1;
        if ((var1 = backgroundThreadExecutor) != null) {
            var1.shutdown();
        }

        backgroundThreadExecutor = null;
        a = null;
        appCtx = null;
    }

    public static Context getAppCtx() {
        return appCtx;
    }

    public static String GetCopyRight() {
        return "MFT:28 43 29 32 30 32 32 20 43 6F 70 79 72 69 67 68 74 2C 20 58 70 72 69 6E 74 65 72 20 54 65 63 68 2E 2C 4C 74 64 2E 0D 0A 58 70 72 69 6E 74 65 72 28 52 29 D0 BE EC C7";
    }

    private static class b implements Executor {
        final Handler a = new Handler(Looper.getMainLooper());

        b() {
        }

        public void execute(Runnable command) {
            this.a.post(command);
        }
    }
}
