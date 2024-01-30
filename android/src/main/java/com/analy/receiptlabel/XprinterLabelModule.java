package com.analy.receiptlabel;

import static android.content.Context.BIND_AUTO_CREATE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.util.Log;

import com.analy.receiptlabel.utils.LabelPosConnect;
import com.analy.receiptlabel.utils.StringUtils;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.module.annotations.ReactModule;
import com.github.danielfelgar.drawreceiptlib.ReceiptBuilder;

import net.posprinter.IDeviceConnection;
import net.posprinter.IPOSListener;
import net.posprinter.POSConst;
import net.posprinter.POSPrinter;
import net.posprinter.TSCConst;
import net.posprinter.TSCPrinter;
import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.service.PosprinterService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ReactModule(name = XprinterLabelModule.NAME)
public class XprinterLabelModule extends ReactContextBaseJavaModule {
    public static final String NAME = "RNXprinterLabel";
    private ReactApplicationContext context;

    public static IMyBinder binder;
    private static final Object lockEthernetLabelPrinting = new Object();
    private static final Object lockBluetoothLabelPrinting = new Object();
    private static final Object lockUsbLabelPrinting = new Object();
    private static final Object lockPrintingLabelAsync = new Object();
    private static Long differentSecondsToReconnect = 15l;
    private static IDeviceConnection curEthernetConnectLabelPrinting = null;
    private static IDeviceConnection curBluetoothConnectLabelPrinting = null;
    private static IDeviceConnection curUsbConnectLabelPrinting = null;
    private static Date ethernetLabelLastConnectTime = null;
    private static Date bluetoothLabelLastConnectTime = null;
    private static Date usbLabelLastConnectTime = null;

    // bindService connection
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // Bind successfully
            binder = (IMyBinder) iBinder;
            Log.e("binder", "connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("disbinder", "disconnected");
        }
    };

    public XprinterLabelModule(ReactApplicationContext reactContext) {
        super(reactContext);

        this.context = reactContext;

        Intent intent = new Intent(this.context, PosprinterService.class);
        intent.putExtra("isconnect", true); // add
        this.context.bindService(intent, conn, BIND_AUTO_CREATE);
        Log.v(NAME, "RNXNetprinter alloc");

        // Try to print with new libs
        LabelPosConnect.init(this.context);

    }

    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void closeTcpLabelConnection(final Promise promise) {
        synchronized (lockEthernetLabelPrinting) {
            if (curEthernetConnectLabelPrinting != null) {
                try {
                    curEthernetConnectLabelPrinting.close();
                    curEthernetConnectLabelPrinting = null;
                    promise.resolve(true);
                    return;
                } catch (Exception ex) {
                    promise.reject("-1", "Can not close the connection");
                    return;
                } finally {
                    curEthernetConnectLabelPrinting = null;
                }
            }
            promise.resolve(true);
            return;
        }
    }

    @ReactMethod
    public void closeBluetoohLabelConnection(final Promise promise) {
        synchronized (lockBluetoothLabelPrinting) {
            if (curBluetoothConnectLabelPrinting != null) {
                try {
                    curBluetoothConnectLabelPrinting.close();
                    curBluetoothConnectLabelPrinting = null;
                    promise.resolve(true);
                    return;
                } catch (Exception ex) {
                    promise.reject("-1", "Can not close the connection");
                    return;
                } finally {
                    curBluetoothConnectLabelPrinting = null;
                }
            }
            promise.resolve(true);
            return;
        }
    }

    @ReactMethod
    public void closeUsbLabelConnection(final Promise promise) {
        synchronized (lockUsbLabelPrinting) {
            if (curUsbConnectLabelPrinting != null) {
                try {
                    curUsbConnectLabelPrinting.close();
                    curUsbConnectLabelPrinting = null;
                    promise.resolve(true);
                    return;
                } catch (Exception ex) {
                    promise.reject("-1", "Can not close the connection");
                    return;
                } finally {
                    curUsbConnectLabelPrinting = null;
                }
            }
            promise.resolve(true);
            return;
        }
    }

    @ReactMethod
    public void printLabelTcp(String ipAddress, int port, String payload, int labelWidth, int labelHeight, int labelGap, int labelSpaceLeft, int labelSpaceTop, final Promise promise) {
        synchronized (lockEthernetLabelPrinting) {
            printLabelTcp(ipAddress, port, payload, promise, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, this.context);
        }
    }

    @ReactMethod
    public void printLabelBluetooth(String macAddress, String payload, int labelWidth, int labelHeight, int labelGap, int labelSpaceLeft, int labelSpaceTop, final Promise promise) {
        synchronized (lockBluetoothLabelPrinting) {
            printLabelBluetooth(macAddress, payload, promise, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, this.context);
        }
    }

    @ReactMethod
    public void printLabelUsb(String payload, String usbDeviceName, int labelWidth, int labelHeight, int labelGap, int labelSpaceLeft, int labelSpaceTop, final Promise promise) {
        synchronized (lockUsbLabelPrinting) {
                printLabelUsb(payload, promise, usbDeviceName, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, this.context);
        }
    }

    private static void printLabelUsb(String payload, Promise promise, String usbDeviceName, int labelWidth, int labelHeight, int labelGap, int labelSpaceLeft, int labelSpaceTop, ReactApplicationContext context) {
        if (StringUtils.isBlank(payload)) {
            promise.reject("-1", "Should provide valid pageLoad to print");
            return;
        }
        List<PrinterLine> lines = XprinterModule.parsePayload(payload);
        ReactApplicationContext me = context;
        boolean needToReconnect = false;

        Date ethernetPrintingTimeNow = new Date();
        if (XprinterLabelModule.curUsbConnectLabelPrinting == null || usbLabelLastConnectTime == null || (ethernetPrintingTimeNow.getTime() - usbLabelLastConnectTime.getTime()) / 1000 > differentSecondsToReconnect) {

            try {
                if (XprinterLabelModule.curUsbConnectLabelPrinting != null) {
                    XprinterLabelModule.curUsbConnectLabelPrinting.close();
                }
            } catch (Exception ex) {

            }
            XprinterLabelModule.curUsbConnectLabelPrinting = LabelPosConnect.createDevice(LabelPosConnect.DEVICE_TYPE_USB);

            needToReconnect = true;
            usbLabelLastConnectTime = ethernetPrintingTimeNow;
        }

        String usbPathAddress = "";
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            promise.reject("-1", "Can not connect to usb printer");
            return;
        }
        Collection<UsbDevice> devicesList = usbManager.getDeviceList().values();
        if (devicesList == null || devicesList.size() == 0) {
            promise.reject("-1", "Can not connect to usb printer");
            return;
        }
        for (UsbDevice device : devicesList) {
            int usbClass = device.getDeviceClass();
            if ((usbClass == UsbConstants.USB_CLASS_PER_INTERFACE || usbClass == UsbConstants.USB_CLASS_MISC) && UsbDeviceHelper.findPrinterInterface(device) != null) {
                usbClass = UsbConstants.USB_CLASS_PRINTER;
            }
            String builtInPrinterName = XprinterModule.getUsbPrinterName(device);
            boolean isMatchingExpectedDevice = StringUtils.isBlank(usbDeviceName)
                    ||
                    (builtInPrinterName.equalsIgnoreCase(usbDeviceName))
                    ||
                    (usbDeviceName.contains(builtInPrinterName));
            if (usbClass == UsbConstants.USB_CLASS_PRINTER && isMatchingExpectedDevice) {
                usbPathAddress = device.getDeviceName();
                break;
            }
        }
        if (StringUtils.isBlank(usbPathAddress)) {
            promise.reject("-1", "Can not connect to usb printer");
            return;
        }
        if (needToReconnect) {
            doUsbLabelPrintingAndRetry(XprinterLabelModule.curUsbConnectLabelPrinting, promise, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, lines, me, usbPathAddress, true);
        } else {
            // Trigger print now.
            synchronized (lockPrintingLabelAsync) {
                doPrintingLabelService(XprinterLabelModule.curUsbConnectLabelPrinting, me, lines, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, promise, true);
            }
        }
    }

    private static void doUsbLabelPrintingAndRetry(IDeviceConnection curUsbConnectLabelPrinting, Promise promise, int labelWidth, int labelHeight, int labelGap,
                                                   int labelSpaceLeft, int labelSpaceTop, List<PrinterLine> lines,
                                                   ReactApplicationContext me, String usbPathAddress, boolean retryIfFailed) {
        curUsbConnectLabelPrinting.connect(usbPathAddress, new IPOSListener() {
            @Override
            public void onStatus(int i, String s) {
                switch (i) {
                    case LabelPosConnect.CONNECT_SUCCESS: {
                        synchronized (lockPrintingLabelAsync) {
                            doPrintingLabelService(curUsbConnectLabelPrinting, me, lines, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, promise, true);
                        }
                        break;
                    }
                    default: {
                        if (retryIfFailed) {
                            try {
                                if (curUsbConnectLabelPrinting != null) {
                                    curUsbConnectLabelPrinting.close();
                                }
                            } catch (Exception ex) {

                            }
                            doUsbLabelPrintingAndRetry(curUsbConnectLabelPrinting, promise, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, lines, me, usbPathAddress, false);
                        }
                        break;
                    }
                }

            }
        });
    }

    private static void printLabelBluetooth(String macAddress, String payload, Promise promise, int labelWidth, int labelHeight,
                                            int labelGap, int labelSpaceLeft, int labelSpaceTop, ReactApplicationContext context) {
        if (StringUtils.isBlank(macAddress)) {
            promise.reject("-1", "Should provide valid mac address");
            return;
        }
        if (StringUtils.isBlank(payload)) {
            promise.reject("-1", "Should provide valid pageLoad to print");
            return;
        }
        List<PrinterLine> lines = XprinterModule.parsePayload(payload);
        ReactApplicationContext me = context;
        boolean needToReconnect = false;

        Date ethernetPrintingTimeNow = new Date();
        if (XprinterLabelModule.curBluetoothConnectLabelPrinting == null || bluetoothLabelLastConnectTime == null || (ethernetPrintingTimeNow.getTime() - bluetoothLabelLastConnectTime.getTime()) / 1000 > differentSecondsToReconnect) {

            try {
                if (XprinterLabelModule.curBluetoothConnectLabelPrinting != null) {
                    XprinterLabelModule.curBluetoothConnectLabelPrinting.close();
                }
            } catch (Exception ex) {

            }
            XprinterLabelModule.curBluetoothConnectLabelPrinting = LabelPosConnect.createDevice(LabelPosConnect.DEVICE_TYPE_BLUETOOTH);

            needToReconnect = true;

            bluetoothLabelLastConnectTime = ethernetPrintingTimeNow;
        }

        if (needToReconnect) {
            doLabelBluetoothPrintingAndRetry(curBluetoothConnectLabelPrinting, macAddress, promise, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, lines, me, true);
        } else {
            // Trigger print now.
            synchronized (lockPrintingLabelAsync) {
                doPrintingLabelService(curBluetoothConnectLabelPrinting, me, lines, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, promise, true);
            }
        }
    }

    private static void doLabelBluetoothPrintingAndRetry(IDeviceConnection curBluetoothConnectLabelPrinting, String macAddress, Promise promise, int labelWidth, int labelHeight, int labelGap, int labelSpaceLeft, int labelSpaceTop, List<PrinterLine> lines, ReactApplicationContext me, boolean retryIfFailed) {
        curBluetoothConnectLabelPrinting.connect(macAddress, new IPOSListener() {
            @Override
            public void onStatus(int i, String s) {
                switch (i) {
                    case LabelPosConnect.CONNECT_SUCCESS: {
                        synchronized (lockPrintingLabelAsync) {
                            doPrintingLabelService(curBluetoothConnectLabelPrinting, me, lines, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, promise, true);
                        }
                        break;
                    }
                    default: {
                        if (retryIfFailed) {
                            try {
                                if (curBluetoothConnectLabelPrinting != null) {
                                    curBluetoothConnectLabelPrinting.close();
                                }
                            } catch (Exception ex) {

                            }
                            doLabelBluetoothPrintingAndRetry(curBluetoothConnectLabelPrinting, macAddress, promise, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, lines, me, false);
                        }
                        break;
                    }
                }

            }
        });
    }

    private static void printLabelTcp(String ipAddress, int port, String payload, Promise promise, int labelWidth,int labelHeight,
                                      int labelGap, int labelSpaceLeft, int labelSpaceTop, ReactApplicationContext context) {
        if (StringUtils.isBlank(ipAddress) || port <= 0) {
            promise.reject("-1", "Should provide valid ip address");
            return;
        }
        if (StringUtils.isBlank(payload)) {
            promise.reject("-1", "Should provide valid pageLoad to print");
            return;
        }
        List<PrinterLine> lines = XprinterModule.parsePayload(payload);
        ReactApplicationContext me = context;
        boolean needToReconnect = false;

        Date ethernetPrintingTimeNow = new Date();
        if (XprinterLabelModule.curEthernetConnectLabelPrinting == null || ethernetLabelLastConnectTime == null || (ethernetPrintingTimeNow.getTime() - ethernetLabelLastConnectTime.getTime()) / 1000 > differentSecondsToReconnect) {

            try {
                if (XprinterLabelModule.curEthernetConnectLabelPrinting != null) {
                    XprinterLabelModule.curEthernetConnectLabelPrinting.close();
                }
            } catch (Exception ex) {

            }
            XprinterLabelModule.curEthernetConnectLabelPrinting = LabelPosConnect.createDevice(LabelPosConnect.DEVICE_TYPE_ETHERNET);

            needToReconnect = true;

            ethernetLabelLastConnectTime = ethernetPrintingTimeNow;
        }

        if (needToReconnect) {
            doLabelTcpPrintingAndRetry(XprinterLabelModule.curEthernetConnectLabelPrinting, ipAddress, promise, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, lines, me, true);
        } else {
            // Trigger print now.
            synchronized (lockPrintingLabelAsync) {
                doPrintingLabelService(XprinterLabelModule.curEthernetConnectLabelPrinting, me, lines, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, promise, true);
            }
        }
    }

    private static void doLabelTcpPrintingAndRetry(IDeviceConnection curEthernetConnectLabelPrinting,
                                                   String ipAddress, Promise promise, int labelWidth,int labelHeight, int labelGap,
                                                   int labelSpaceLeft, int labelSpaceTop,
                                                   List<PrinterLine> lines, ReactApplicationContext me, boolean retryIfFailed) {
        curEthernetConnectLabelPrinting.connect(ipAddress, new IPOSListener() {
            @Override
            public void onStatus(int i, String s) {
                switch (i) {
                    case LabelPosConnect.CONNECT_SUCCESS: {
                        synchronized (lockPrintingLabelAsync) {
                            doPrintingLabelService(curEthernetConnectLabelPrinting, me, lines, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, promise, true);
                        }
                        break;
                    }
                    default: {
                        if (retryIfFailed) {
                            try {
                                if (curEthernetConnectLabelPrinting != null) {
                                    curEthernetConnectLabelPrinting.close();
                                }
                            } catch (Exception ex) {

                            }
                            doLabelTcpPrintingAndRetry(curEthernetConnectLabelPrinting, ipAddress, promise, labelWidth, labelHeight, labelGap, labelSpaceLeft, labelSpaceTop, lines, me, false);
                        }
                        break;
                    }
                }

            }
        });
    }

    private static void doPrintingLabelService(IDeviceConnection deviceConnection, ReactApplicationContext me,
                                          List<PrinterLine> lines, int labelWidth, int labelHeight, int labelGap,
                                               int labelSpaceLeft, int labelSpaceTop,
                                               Promise promise, boolean isLabelPrinting) {
        try {
            TSCPrinter printer = new TSCPrinter(deviceConnection);
            try {
                int marginDefault = 0;
                int receiptBuilderWidth = 1200;
                String fontRegular = "fonts/arial_regular.ttf";
                String fontBold = "fonts/arial_bold.ttf";
                float defaultTextSize = 60F;
                ReceiptBuilder receipt = new ReceiptBuilder(receiptBuilderWidth);
                receipt.setMargin(marginDefault, marginDefault);
                receipt.setAlign(Paint.Align.LEFT);
                receipt.setColor(Color.BLACK);
                receipt.setTextSize(defaultTextSize);
                receipt.setTypeface(me, fontRegular);
                for (PrinterLine line : lines) {
                    if (line.isNewLine) {
                        receipt.addLine();
                        continue;
                    } else if (line.isParagraph) {
                        receipt.addParagraph();
                        continue;
                    }

                    receipt.setTypeface(me, line.isBold ? fontBold : fontRegular);
                    receipt.setMargin(marginDefault, marginDefault);
                    receipt.setTextSize(line.textSize != null ? line.textSize : defaultTextSize);
                    receipt.setAlign(line.align != null ? line.align : Paint.Align.LEFT);
                    receipt.setColor(line.textColor != null ? line.textColor : Color.BLACK);
                    receipt.addText(line.text, !line.isSameLine);
                    if (!line.isSameLine && line.textSize != null && line.textSize > defaultTextSize) {
                        receipt.addBlankSpace(15);
                    }
                }
                Bitmap imageToPrint = receipt.build();
                printer.sizeMm(labelWidth, labelHeight)
                        .gapMm(labelGap, 0.0)
                        .cls()
                        .bitmap(labelSpaceLeft, labelSpaceTop, TSCConst.BMP_MODE_OVERWRITE, 400, imageToPrint).print(1);
            } catch (Exception ex) {
                promise.reject("-1", "Have error in preparing printing " + ex.getMessage());
                return;
            }
            promise.resolve(true);
        } catch (Exception ex) {
            // Error while printing
            promise.reject("-1", "There is an error while printing " + ex.getMessage());
        }
    }
}
