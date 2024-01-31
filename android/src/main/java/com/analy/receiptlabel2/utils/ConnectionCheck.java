package com.analy.receiptlabel2.utils;

public class ConnectionCheck {
    private boolean statusReceived = false;

    private boolean timeOut = false;

    private boolean connected = false;

    public boolean isStatusReceived() {
        return statusReceived;
    }

    public void setStatusReceived(boolean statusReceived) {
        this.statusReceived = statusReceived;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isTimeOut() {
        return timeOut;
    }

    public void setTimeOut(boolean timeOut) {
        this.timeOut = timeOut;
    }
}
