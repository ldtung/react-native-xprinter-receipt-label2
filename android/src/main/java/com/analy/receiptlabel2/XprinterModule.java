package com.analy.receiptlabel2;

import android.graphics.Paint;

import com.analy.receiptlabel2.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.hardware.usb.UsbDevice;

public class XprinterModule {

    public static String getUsbPrinterName(UsbDevice device) {
        if (device == null) {
            return "";
        }
        String usbNameTmp = "USB:%s-%s-%s";

        String deviceId = String.valueOf(device.getDeviceId());
        return String.format(usbNameTmp, device.getVendorId(), device.getProductId(), deviceId.substring(0, 1));
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
