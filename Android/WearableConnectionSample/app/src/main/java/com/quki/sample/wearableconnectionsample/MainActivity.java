package com.quki.sample.wearableconnectionsample;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.quki.sample.wearableconnectionsample.action.ConnectAction;
import com.quki.sample.wearableconnectionsample.bluetooth.BluetoothConfig;
import com.quki.sample.wearableconnectionsample.bluetooth.BluetoothConnection;
import com.quki.sample.wearableconnectionsample.service.AccessoryService;

public class MainActivity extends AppCompatActivity implements BluetoothConnection {

    private AccessoryService mAccessoryService = null;
    private boolean isBound;
    private Button requestBtn, sendBtn;
    private TextView statusTxtView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initBluetoothConnection();
        bindAccessoryService();

        requestBtn = (Button) findViewById(R.id.requestBtn);
        sendBtn = (Button) findViewById(R.id.sendBtn);
        statusTxtView = (TextView) findViewById(R.id.statusTxtView);
        sendBtn.setEnabled(false);
        requestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConnection();
            }
        });
        sendBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String data =  "Hello GearS2";
                sendDataToService(data);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindAccessoryService();
    }

    @Override
    public void initBluetoothConnection() {
        BluetoothAdapter mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBTAdapter.isEnabled()) {
            Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(btIntent, BluetoothConfig.REQUEST_ENABLE_BT);
        }
    }

    /**
     *
     */
    private void bindAccessoryService() {
        Intent serviceIntent = new Intent(this, AccessoryService.class);
        bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    /**
     *
     */
    private void unbindAccessoryService() {
        unbindService(mServiceConnection);
    }

    /**
     *
     */
    private void startConnection(){
        if(isBound && mAccessoryService != null){
            mAccessoryService.findPeers();
        }
    }

    /**
     * AccessoryService으로 알람시간간격 전달
     * @param mData
     */
    private void sendDataToService(String mData) {

        if (isBound && mAccessoryService != null) {
            mAccessoryService.sendData(mData);
        } else {
            Toast.makeText(getApplicationContext(), "기어와 연결을 확인하세요", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothConfig.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "BLUETOOTH ON", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "PLEASE BLUETOOTH ON", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mAccessoryService = ((AccessoryService.MyBinder) service).getService();
            mAccessoryService.registerAction(getAction());
            isBound = true;
            Toast.makeText(getApplicationContext(),"onSERVICECONNECTED",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mAccessoryService = null;
            isBound = false;
        }
    };

    private ConnectAction getAction(){
        return new ConnectAction() {
            @Override
            public void onSuccessConnection() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sendBtn.setEnabled(true);
                    }
                });
            }

            @Override
            public void onFailConnection() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"FAIL TO SERVICE CONNECTION",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onSuccessTransfer(final String data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusTxtView.setText(data);
                    }
                });
            }
        };
    }

}
