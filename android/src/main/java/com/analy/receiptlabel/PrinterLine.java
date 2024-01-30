package com.analy.receiptlabel;

import android.graphics.Color;
import android.graphics.Paint;

public class PrinterLine {
    String text;
    Paint.Align align;
    boolean isNewLine = false;
    Float textSize;
    Integer textColor; // Color.BLACK
    boolean isBold = false;

    boolean isSameLine = false;

    boolean isParagraph = false;
}
