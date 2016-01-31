package com.quki.sample.wearableconnectionsample.action;

/**
 * Created by quki on 2016-01-31.
 */
public interface ConnectAction {

    void onSuccessConnection();
    void onFailConnection();
    void onSuccessTransfer(String data);
}
