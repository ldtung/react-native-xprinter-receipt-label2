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
import android.os.IBinder;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.analy.receiptlabel.utils.StringUtils;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.module.annotations.ReactModule;
import com.github.danielfelgar.drawreceiptlib.ReceiptBuilder;

import net.posprinter.IDeviceConnection;
import net.posprinter.IPOSListener;
import net.posprinter.POSConnect;
import net.posprinter.POSConst;
import net.posprinter.POSPrinter;
import net.posprinter.TSCConst;
import net.posprinter.TSCPrinter;
import net.posprinter.ZPLPrinter;
import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.service.PosprinterService;

import android.hardware.usb.UsbConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

@ReactModule(name = XprinterModule.NAME)
public class XprinterModule extends ReactContextBaseJavaModule {
    public static String DISCONNECT = "com.posconsend.net.disconnetct";
    public static final String NAME = "RNXprinter";
    private ReactApplicationContext context;

    private byte[] mBuffer = new byte[0];

    public static IMyBinder binder;
    public static boolean ISCONNECT;

    // Bluetooth
    BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> mPairedDevices;

    private static final Object lockEthernet = new Object();
    private static final Object lockPrintingAsync = new Object();
    private static final Object lockBluetooth = new Object();
    private static final Object lockUsb = new Object();

    private static IDeviceConnection curEthernetConnect = null;
    private static Date ethernetLastConnectTime = null;
    private static Date bluetoothLastConnectTime = null;
    private static Date usbLastConnectTime = null;
    private static Long differentSecondsToReconnect = 15l;
    private static IDeviceConnection curBluetoothConnect = null;
    private static IDeviceConnection curUsbConnect = null;

    public static int PRINTER_80mm_WIDTH = 574;
    public static int PRINTER_58mm_WIDTH = 368;

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

    public XprinterModule(ReactApplicationContext reactContext) {
        super(reactContext);

        this.context = reactContext;

        Intent intent = new Intent(this.context, PosprinterService.class);
        intent.putExtra("isconnect", true); // add
        this.context.bindService(intent, conn, BIND_AUTO_CREATE);
        Log.v(NAME, "RNXNetprinter alloc");

        // Try to print with new libs
        POSConnect.init(this.context);

    }

    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void printTcp80mm(String ipAddress, int port, String payload, final Promise promise) {
        synchronized (lockEthernet) {
            int receiptWidth = PRINTER_80mm_WIDTH;
            printTcp(ipAddress, port, payload, promise, receiptWidth, this.context);
        }
    }

    @ReactMethod
    public void closeTcpConnection(final Promise promise) {
        synchronized (lockEthernet) {
            if (curEthernetConnect != null) {
                try {
                    curEthernetConnect.close();
                    curEthernetConnect = null;
                    promise.resolve(true);
                    return;
                } catch (Exception ex) {
                    promise.reject("-1", "Can not close the connection");
                    return;
                } finally {
                    curEthernetConnect = null;
                }
            }
            promise.resolve(true);
            return;
        }
    }

    @ReactMethod
    public void closeBluetoohConnection(final Promise promise) {
        synchronized (lockBluetooth) {
            if (curBluetoothConnect != null) {
                try {
                    curBluetoothConnect.close();
                    curBluetoothConnect = null;
                    promise.resolve(true);
                    return;
                } catch (Exception ex) {
                    promise.reject("-1", "Can not close the connection");
                    return;
                } finally {
                    curBluetoothConnect = null;
                }
            }
            promise.resolve(true);
            return;
        }
    }

    @ReactMethod
    public void closeUsbConnection(final Promise promise) {
        synchronized (lockUsb) {
            if (curUsbConnect != null) {
                try {
                    curUsbConnect.close();
                    curUsbConnect = null;
                    promise.resolve(true);
                    return;
                } catch (Exception ex) {
                    promise.reject("-1", "Can not close the connection");
                    return;
                } finally {
                    curUsbConnect = null;
                }
            }
            promise.resolve(true);
            return;
        }
    }

    @ReactMethod
    public void printTcp58mm(String ipAddress, int port, String payload, final Promise promise) {
        synchronized (lockEthernet) {
            int receiptWidth = PRINTER_58mm_WIDTH;
            printTcp(ipAddress, port, payload, promise, receiptWidth, this.context);
        }
    }

    @ReactMethod
    public void getUsbDeviceList(Promise promise) {
        try {
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
            WritableArray rnArray = new WritableNativeArray();
            for (UsbDevice device : devicesList) {
                int usbClass = device.getDeviceClass();
                if ((usbClass == UsbConstants.USB_CLASS_PER_INTERFACE || usbClass == UsbConstants.USB_CLASS_MISC) && UsbDeviceHelper.findPrinterInterface(device) != null) {
                    usbClass = UsbConstants.USB_CLASS_PRINTER;
                }
                if (usbClass == UsbConstants.USB_CLASS_PRINTER) {
                    rnArray.pushString(getUsbPrinterName(device));
                }
            }
            promise.resolve(rnArray);
        } catch (Exception e) {
            promise.reject("USB Error", e.getMessage());
        }
    }


    @ReactMethod
    public void printBluetooth80mm(String macAddress, String payload, final Promise promise) {
        synchronized (lockBluetooth) {
            int receiptWidth = PRINTER_80mm_WIDTH;
            printBluetooth(macAddress, payload, promise, receiptWidth, this.context);
        }
    }

    @ReactMethod
    public void printBluetooth58mm(String macAddress, String payload, final Promise promise) {
        synchronized (lockBluetooth) {
            int receiptWidth = PRINTER_58mm_WIDTH;
            printBluetooth(macAddress, payload, promise, receiptWidth, this.context);
        }
    }

    @ReactMethod
    public void printUsb80mm(String payload, String usbDeviceName, final Promise promise) {
        synchronized (lockUsb) {
            int receiptWidth = PRINTER_80mm_WIDTH;
            printUsb(payload, promise, usbDeviceName, receiptWidth, this.context);
        }
    }

    @ReactMethod
    public void printUsb58mm(String payload, String usbDeviceName, final Promise promise) {
        synchronized (lockUsb) {
            int receiptWidth = PRINTER_58mm_WIDTH;
            printUsb(payload, promise, usbDeviceName, receiptWidth, this.context);
        }
    }

    private static void printUsb(String payload, Promise promise, String usbDeviceName, int receiptWidth, ReactApplicationContext context) {
        if (StringUtils.isBlank(payload)) {
            promise.reject("-1", "Should provide valid pageLoad to print");
            return;
        }
        List<PrinterLine> lines = parsePayload(payload);
        ReactApplicationContext me = context;

        boolean needToReconnect = false;
        Date ethernetPrintingTimeNow = new Date();
        if (XprinterModule.curUsbConnect == null || usbLastConnectTime == null || (ethernetPrintingTimeNow.getTime() - usbLastConnectTime.getTime()) / 1000 > differentSecondsToReconnect) {

            try {
                if (XprinterModule.curUsbConnect != null) {
                    XprinterModule.curUsbConnect.close();
                }
            } catch (Exception ex) {

            }
            XprinterModule.curUsbConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_USB);
            needToReconnect = true;

            usbLastConnectTime = ethernetPrintingTimeNow;
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
            String builtInPrinterName = getUsbPrinterName(device);
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
            doUsbPrintingAndRetry(XprinterModule.curUsbConnect, promise, receiptWidth, lines, me, usbPathAddress, true);
        } else {
            // Trigger print now.
            synchronized (lockPrintingAsync) {
                doPrintingService(XprinterModule.curUsbConnect, me, lines, receiptWidth, promise, false);
            }
        }
    }

    public static String getUsbPrinterName(UsbDevice device) {
        if (device == null) {
            return "";
        }
        String usbNameTmp = "USB:%s-%s-%s";

        String deviceId = String.valueOf(device.getDeviceId());
        return String.format(usbNameTmp, device.getVendorId(), device.getProductId(), deviceId.substring(0, 1));
    }

    private static void doUsbPrintingAndRetry(IDeviceConnection curUsbConnect, Promise promise, int receiptWidth, List<PrinterLine> lines,
                                              ReactApplicationContext me, String usbPathAddress, boolean retryIfFailed) {
        curUsbConnect.connect(usbPathAddress, new IPOSListener() {
            @Override
            public void onStatus(int i, String s) {
                switch (i) {
                    case POSConnect.CONNECT_SUCCESS: {
                        synchronized (lockPrintingAsync) {
                            doPrintingService(curUsbConnect, me, lines, receiptWidth, promise, false);
                        }
                        break;
                    }
                    default: {
                        if (retryIfFailed) {
                            try {
                                if (curUsbConnect != null) {
                                    curUsbConnect.close();
                                }
                            } catch (Exception ex) {

                            }
                            doUsbPrintingAndRetry(curUsbConnect, promise, receiptWidth, lines, me, usbPathAddress, false);
                        }
                        break;
                    }
                }

            }
        });
    }

    private static void printBluetooth(String macAddress, String payload, Promise promise, int receiptWidth, ReactApplicationContext context) {
        if (StringUtils.isBlank(macAddress)) {
            promise.reject("-1", "Should provide valid mac address");
            return;
        }
        if (StringUtils.isBlank(payload)) {
            promise.reject("-1", "Should provide valid pageLoad to print");
            return;
        }
        List<PrinterLine> lines = parsePayload(payload);
        ReactApplicationContext me = context;
        boolean needToReconnect = false;

        Date ethernetPrintingTimeNow = new Date();
        if (XprinterModule.curBluetoothConnect == null || bluetoothLastConnectTime == null || (ethernetPrintingTimeNow.getTime() - bluetoothLastConnectTime.getTime()) / 1000 > differentSecondsToReconnect) {
            try {
                if (XprinterModule.curBluetoothConnect != null) {
                    XprinterModule.curBluetoothConnect.close();
                }
            } catch (Exception ex) {

            }
            XprinterModule.curBluetoothConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH);

            needToReconnect = true;

            bluetoothLastConnectTime = ethernetPrintingTimeNow;
        }

        if (needToReconnect) {
            doBluetoothPrintingAndRetry(XprinterModule.curBluetoothConnect, macAddress, promise, receiptWidth, lines, me, true);
        } else {
            // Trigger print now.
            synchronized (lockPrintingAsync) {
                doPrintingService(XprinterModule.curBluetoothConnect, me, lines, receiptWidth, promise, false);
            }
        }
    }

    private static void doBluetoothPrintingAndRetry(IDeviceConnection curBluetoothConnect, String macAddress, Promise promise, int receiptWidth,
                                                    List<PrinterLine> lines, ReactApplicationContext me, boolean retryIfFailed) {
        curBluetoothConnect.connect(macAddress, new IPOSListener() {
            @Override
            public void onStatus(int i, String s) {
                switch (i) {
                    case POSConnect.CONNECT_SUCCESS: {
                        synchronized (lockPrintingAsync) {
                            doPrintingService(curBluetoothConnect, me, lines, receiptWidth, promise, false);
                        }
                        break;
                    }
                    default: {
                        if (retryIfFailed) {
                            try {
                                if (curBluetoothConnect != null) {
                                    curBluetoothConnect.close();
                                }
                            } catch (Exception ex) {

                            }
                            doBluetoothPrintingAndRetry(curBluetoothConnect, macAddress, promise, receiptWidth, lines, me, false);
                        }
                        break;
                    }
                }

            }
        });
    }

    private static void printTcp(String ipAddress, int port, String payload, Promise promise, int receiptWidth, ReactApplicationContext context) {
        if (StringUtils.isBlank(ipAddress) || port <= 0) {
            promise.reject("-1", "Should provide valid ip address");
            return;
        }
        if (StringUtils.isBlank(payload)) {
            promise.reject("-1", "Should provide valid pageLoad to print");
            return;
        }
        List<PrinterLine> lines = parsePayload(payload);
        ReactApplicationContext me = context;
        boolean needToReconnect = false;
        Date ethernetPrintingTimeNow = new Date();
        if (XprinterModule.curEthernetConnect == null || ethernetLastConnectTime == null || (ethernetPrintingTimeNow.getTime() - ethernetLastConnectTime.getTime()) / 1000 > differentSecondsToReconnect) {
            try {
                if (XprinterModule.curEthernetConnect != null) {
                    XprinterModule.curEthernetConnect.close();
                }
            } catch (Exception ex) {

            }
            XprinterModule.curEthernetConnect = POSConnect.createDevice(POSConnect.DEVICE_TYPE_ETHERNET);
            needToReconnect = true;
            ethernetLastConnectTime = ethernetPrintingTimeNow;
        }

        if (needToReconnect) {
            doTcpPrintingAndRetry(curEthernetConnect, ipAddress, promise, receiptWidth, lines, me, true);
        } else {
            // Trigger print now.
            synchronized (lockPrintingAsync) {
                doPrintingService(curEthernetConnect, me, lines, receiptWidth, promise, false);
            }
        }
    }

    private static void doTcpPrintingAndRetry(IDeviceConnection curEthernetConnect, String ipAddress, Promise promise, int receiptWidth, List<PrinterLine> lines, ReactApplicationContext me, boolean retryIfFailed) {
        curEthernetConnect.connect(ipAddress, new IPOSListener() {
            @Override
            public void onStatus(int i, String s) {
                switch (i) {
                    case POSConnect.CONNECT_SUCCESS: {
                        synchronized (lockPrintingAsync) {
                            doPrintingService(curEthernetConnect, me, lines, receiptWidth, promise, false);
                        }
                        break;
                    }
                    default: {
                        if (retryIfFailed) {
                            try {
                                if (curEthernetConnect != null) {
                                    curEthernetConnect.close();
                                }
                            } catch (Exception ex) {

                            }
                            doTcpPrintingAndRetry(curEthernetConnect, ipAddress, promise, receiptWidth, lines, me, false);
                        }
                        break;
                    }
                }

            }
        });
    }

    private static void doPrintingService(IDeviceConnection deviceConnection, ReactApplicationContext me,
                                          List<PrinterLine> lines, int receiptWidth, Promise promise, boolean isLabelPrinting) {
        try {
            POSPrinter printer = new POSPrinter(deviceConnection);
            printer.initializePrinter();
            try {
                int marginDefault = 0;
                int receiptBuilderWidth = 1200;
                String fontRegular = "fonts/RobotoMono-Regular.ttf";
                String fontBold = "fonts/RobotoMono-Bold.ttf";
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
                if (!isLabelPrinting) {
                    printer.feedLine(marginDefault);
                }
                printer.printBitmap(imageToPrint, POSConst.ALIGNMENT_CENTER, receiptWidth);
            } catch (Exception ex) {
                promise.reject("-1", "Have error in preparing printing " + ex.getMessage());
                return;
            }
            if (!isLabelPrinting) {
                printer.feedLine();
                printer.cutHalfAndFeed(1);
            }
            promise.resolve(true);
        } catch (Exception ex) {
            // Error while printing
            promise.reject("-1", "There is an error while printing " + ex.getMessage());
        }
    }

    public static List<PrinterLine> parsePayload(String payload) {
        List<PrinterLine> lines = new ArrayList<>();
        String[] payloadItems = payload.split("@@NL@@");
        for (String payloadLine : payloadItems) {
            List<String> inlineStrings = splitString(payloadLine);
            for (String inlineText : inlineStrings) {
                if (StringUtils.isNotBlank(inlineText)) {
                    lines.add(buildPrinterLine(splitPrefixText(inlineText)));
                }
            }
        }
        return lines;
    }

    public static PrinterLine buildPrinterLine(String[] lineToPrintAndFormat) {
        PrinterLine line = new PrinterLine();
        line.text = lineToPrintAndFormat[1];
        if (StringUtils.isNotBlank(lineToPrintAndFormat[0])) {
            String lineFormat = lineToPrintAndFormat[0];
            lineFormat = lineFormat.replace("[", "");
            lineFormat = lineFormat.replace("]", "");
            String[] formats = lineFormat.split(",");
            for (String format : formats) {
                if ("B".equalsIgnoreCase(format)) {
                    line.isBold = true;
                } else if ("R".equalsIgnoreCase(format)) {
                    line.align = Paint.Align.RIGHT;
                } else if ("C".equalsIgnoreCase(format)) {
                    line.align = Paint.Align.CENTER;
                } else if ("XL".equalsIgnoreCase(format)) {
                    line.textSize = 90F;
                } else if ("BL".equalsIgnoreCase(format)) {
                    line.textSize = 80F;
                } else if ("L".equalsIgnoreCase(format)) {
                    line.textSize = 70F;
                } else if ("T".equalsIgnoreCase(format)) {
                    line.textSize = 60F;
                } else if ("VT".equalsIgnoreCase(format)) {
                    line.textSize = 50F;
                } else if ("S".equalsIgnoreCase(format)) {
                    line.isSameLine = true;
                } else if ("P".equalsIgnoreCase(format)) {
                    line.isParagraph = true;
                }
            }
        }
        return line;
    }

    public static List<String> splitString(String input) {
        // P: Paragraph
        // S: Same line
        // C: center
        // B: Bold
        // R: Right
        // XL: Size big text
        // BL: big large
        // L: Size medium text
        // T: Tiny text (for lable printer)
        // VT: Very tiny text
        String[] letters = {"P", "S", "C", "B", "R", "XL", "L", "T", "VT", "BL"};
        String regexPattern = generateRegexPattern(letters);
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(input);

        List<String> splitStrings = new ArrayList<>();
        int lastMatchEnd = 0;
        String currentPrefix = "";

        while (matcher.find()) {
            int start = matcher.start();
            if (start > lastMatchEnd) {
                splitStrings.add(currentPrefix + input.substring(lastMatchEnd, start).trim());
            }
            currentPrefix = matcher.group();
            lastMatchEnd = matcher.end();
        }

        if (lastMatchEnd < input.length()) {
            splitStrings.add(currentPrefix + input.substring(lastMatchEnd).trim());
        }

        return splitStrings;
    }

    public static String[] splitPrefixText(String line) {
        // Initialize the prefix and text strings
        String prefix = "";
        String text = "";

        // Find the index of the first closing square bracket ']' to determine the prefix
        int closingBracketIndex = line.indexOf(']');
        if (closingBracketIndex != -1) {
            prefix = line.substring(0, closingBracketIndex + 1); // Include the closing bracket
            text = line.substring(closingBracketIndex + 1).trim();
        } else {
            // If the closing bracket is not found, consider the whole line as text
            text = line.trim();
        }

        return new String[]{prefix, text};
    }

    public static String generateRegexPattern(String[] letters) {
        List<String> patterns = new ArrayList<>();

        for (int i = 1; i <= letters.length; i++) {
            generateCombinations(letters, 0, i, "", patterns);
        }

        return String.join("|", patterns);
    }

    public static void generateCombinations(String[] letters, int index, int length, String current, List<String> patterns) {
        if (length == 0) {
            patterns.add("\\[" + current.replaceAll(",", ",") + "\\]");
            return;
        }
        if (index == letters.length) {
            return;
        }

        if (!current.isEmpty()) {
            generateCombinations(letters, index + 1, length - 1, current + "," + letters[index], patterns);
        } else {
            generateCombinations(letters, index + 1, length - 1, letters[index], patterns);
        }
        generateCombinations(letters, index + 1, length, current, patterns);
    }
}
