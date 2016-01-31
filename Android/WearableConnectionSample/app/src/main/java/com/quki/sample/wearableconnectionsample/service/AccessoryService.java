package com.quki.sample.wearableconnectionsample.service;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.quki.sample.wearableconnectionsample.action.ConnectAction;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by quki on 2016-01-31.
 */
public class AccessoryService extends SAAgent {

    public static final String TAG = "==AccessoryService==";
    private AccessoryServiceConnection mConnectionHandler;
    private HashMap<Integer, AccessoryServiceConnection> mConnectionsMap;
    private static final int CHANNEL_ALARM_DATA = 101;
    private ConnectAction connectAction;
    private IBinder mBinder = new MyBinder();
    public AccessoryService() {
        super(TAG, AccessoryServiceConnection.class);
    }

    public void registerAction(ConnectAction connectAction) {
        this.connectAction = connectAction;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SA mAccessory = new SA();
        try {
            mAccessory.initialize(this);
            Toast.makeText(getApplicationContext(), "SERVICE CREATED", Toast.LENGTH_SHORT).show();
        } catch (SsdkUnsupportedException e) {
            Log.e(TAG, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "SERVICE DESTROYED", Toast.LENGTH_SHORT).show();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    protected void onFindPeerAgentResponse(SAPeerAgent saPeerAgent, int result) {
        if (result == PEER_AGENT_FOUND) {
            Log.d(TAG, "onFindPeerAgentResponse : peerAgent = " + saPeerAgent);
            onPeerAgentFound(saPeerAgent);
        } else {
            Log.e(TAG, "onFindPeerAgentResponse : result = " + result);
        }
    }


    @Override
    protected void onServiceConnectionResponse(SAPeerAgent saPeerAgent, SASocket saSocket, int result) {

        if (result == SAAgent.CONNECTION_SUCCESS) {

            if (saSocket != null) {
                mConnectionHandler = (AccessoryServiceConnection) saSocket;
                if (mConnectionsMap == null) {
                    mConnectionsMap = new HashMap<>();
                }
                // Connection ID 생성
                mConnectionHandler.mConnectionId = (int) (System.currentTimeMillis() & 255);

                mConnectionsMap.put(mConnectionHandler.mConnectionId, mConnectionHandler);
                connectAction.onSuccessConnection();
                Log.d(TAG, "Connection  Success");

            } else {
                Log.e(TAG, "SASocket object is null");
            }
        } else if (result == SAAgent.CONNECTION_ALREADY_EXIST) {

            Toast.makeText(getApplicationContext(), "이미 연결되있습니다.", Toast.LENGTH_SHORT).show();
            connectAction.onSuccessConnection();
            Log.w(TAG, "CONNECTION_ALREADY_EXIST");


        } else if (result == SAAgent.CONNECTION_FAILURE_PEERAGENT_NO_RESPONSE) {
            Toast.makeText(getApplicationContext(), "기어측 어플로부터 응답이 없습니다.", Toast.LENGTH_SHORT).show();
            connectAction.onFailConnection();
            Log.w(TAG, "CONNECTION_FAILURE_PEERAGENT_NO_RESPONSE");
        }
    }

    /**
     * Find Peer Agents
     */
    public void findPeers() {
        findPeerAgents();
    }

    /**
     * Peer Agents Found
     *
     * @param peerAgent
     */
    public void onPeerAgentFound(SAPeerAgent peerAgent) {
        if (peerAgent != null)
            establishConnection(peerAgent);
    }

    /**
     * Request Service Connection to Peer Agent found
     *
     * @param peerAgent
     * @return
     */
    public boolean establishConnection(SAPeerAgent peerAgent) {
        if (peerAgent != null) {
            requestServiceConnection(peerAgent);
            return true;
        }
        return false;
    }

    /**
     * Close Socket
     *
     * @return
     */
    public boolean closeConnection() {
        if (mConnectionHandler != null) {
            Log.d(TAG, "Connection Close");
            mConnectionHandler.close();
            mConnectionHandler = null;
        }
        return true;
    }

    /**
     * Send data to Gear
     * @param data
     */
    public void sendData(final String data){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mConnectionHandler.send(CHANNEL_ALARM_DATA,data.getBytes());
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Binder
     */
    public class MyBinder extends Binder {

        public AccessoryService getService() {
            return AccessoryService.this;
        }
    }

    private class AccessoryServiceConnection extends SASocket {

        private int mConnectionId;

        public AccessoryServiceConnection() {
            super(AccessoryServiceConnection.class.getName());
        }

        @Override
        public void onError(int channelId, String error, int errorCOde) {

        }

        @Override
        public void onReceive(int channelId, byte[] bytes) {
            if (channelId == CHANNEL_ALARM_DATA) {
                final String data = new String(bytes);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.v(TAG, data);
                        connectAction.onSuccessTransfer(data);
                    }
                }).start();
            }
        }

        @Override
        protected void onServiceConnectionLost(int reason) {
            closeConnection();
        }
    }
}
