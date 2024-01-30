package com.analy.receiptlabel.utils;

import net.posprinter.IPOSListener;
import net.posprinter.POSConnect;

import b.c;

public abstract class LabelConnectionD extends b.d {
    public void connect(String var1, IPOSListener var2) {
        this.b = var2;
        LabelPosConnect.backgroundThreadExecutor.execute(() -> {
            if (this.a) {
                this.close();
            }

            try {
                Thread.sleep(200L);
            } catch (InterruptedException ex) {
                // Ignore
            }

            super.connect(var1, var2);
        });
    }

    public void close() {
        this.b = null;
        POSConnect.backgroundThreadExecutor.execute(this::a);
        Thread var1;
        if ((var1 = this.c) != null) {
            var1.interrupt();
            this.c = null;
        }

        this.d.clear();
    }
}
