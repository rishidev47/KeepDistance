package com.example.keepdistance;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView rssi_view;
    String rssi_str="";
    BluetoothAdapter BTAdapter;
    Activity ac;
    BluetoothLeAdvertiser advertiser;
    EditText txt_data;
    ToggleButton btn_advertise;
    String str_data="Data";
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ac=this;
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        advertiser = BTAdapter.getBluetoothLeAdvertiser();
        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

//        IntentFilter filter = new IntentFilter();
//        filter.addAction(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(receiver, filter);

        BTAdapter.startDiscovery();

        rssi_view=findViewById(R.id.rssi);
        txt_data=findViewById(R.id.txt_advertise);
        btn_advertise=findViewById(R.id.btn_advertise);
        Button btn_clear=findViewById(R.id.btn_clear);
        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rssi_str="";
                rssi_view.setText(rssi_str);
            }
        });

        rssi_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                BTAdapter.cancelDiscovery();
//                BTAdapter.startDiscovery();

            }
        });

        if( !BTAdapter.isMultipleAdvertisementSupported() ) {
            Toast.makeText( this, "Multiple advertisement not supported", Toast.LENGTH_SHORT ).show();
//            mAdvertiseButton.setEnabled( false );
//            mDiscoverButton.setEnabled( false );
        }

        // Advertising
        final AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode( AdvertiseSettings.ADVERTISE_MODE_LOW_POWER )
                .setTxPowerLevel( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable( false )
                .build();


        // Receiving Advertisement
        ArrayList<ScanFilter> filters= new ArrayList<>();
        ScanFilter filter1 = new ScanFilter.Builder()
                .setServiceUuid( new ParcelUuid(UUID.fromString( getString(R.string.ble_uuid ) ) ) )
                .build();
        filters.add( filter1);
        ScanSettings settings1 = new ScanSettings.Builder()
                .setScanMode( ScanSettings.SCAN_MODE_LOW_POWER )
                .build();

        mBluetoothLeScanner.startScan(filters,settings1 ,mScanCallback);
        btn_advertise.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled

                    str_data=txt_data.getText().toString();
                    ParcelUuid pUuid = new ParcelUuid( UUID.fromString( getString(R.string.ble_uuid)) );
                    AdvertiseData data = new AdvertiseData.Builder()
                            .setIncludeDeviceName(true)
                            .setIncludeTxPowerLevel(true)
                            .addServiceUuid( pUuid )
//                            .addServiceData( pUuid, str_data.getBytes( Charset.forName( "UTF-8" ) ) )
                            .build();
//                    mBluetoothLeScanner.startScan(mScanCallback);
                    advertiser.startAdvertising( settings, data, advertisingCallback );



                } else {
                    // The toggle is disabled
//                    mBluetoothLeScanner.stopScan(mScanCallback);
                    advertiser.stopAdvertising(advertisingCallback );
                }
            }
        });



    }
    AdvertiseCallback advertisingCallback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d( "BLE", "Advertising onStartSuccess: " + settingsInEffect.toString() );
            Toast.makeText(getApplicationContext(),"Advertisment Started",Toast.LENGTH_LONG).show();
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e( "BLE", "Advertising onStartFailure: " + errorCode );
            super.onStartFailure(errorCode);
        }

    };
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d("SCAN", "onScanResult: "+result.toString());
            super.onScanResult(callbackType, result);
            if( result == null
                    || result.getDevice() == null
                    || TextUtils.isEmpty(result.getDevice().getName()) )
                return;
            String str="";
            str=str+result.getDevice().getName()+" | "+result.getRssi()+" | "+result.getScanRecord().getTxPowerLevel()+" | ";
            if(android.os.Build.VERSION.SDK_INT>=26){
                str=str+result.getTxPower()+": ";
            }
            double distance=Math.round(calculateDistance(result.getRssi()) * 10000.0) / 10000.0;
            str =str+distance;
//            StringBuilder builder = new StringBuilder( result.getDevice().getName() );
//            builder.append("\n").append(new String(result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0)), Charset.forName("UTF-8")));
            rssi_str=rssi_str+"\n"+str;
            rssi_view.setText(rssi_str);
//            Toast.makeText( getApplicationContext(), str, Toast.LENGTH_SHORT ).show();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e( "BLE", "Discovery onScanFailed: " + errorCode );
            super.onScanFailed(errorCode);
        }
    };


    private final BroadcastReceiver receiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Log.d("TEST", "onReceive: "+action);

            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device =intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("TEST", "onReceive: "+device.getName());
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                if(device.getName().contains("Redmi"))rssi_str=rssi_str+"\n"+device.getName()+":"+rssi+ "dBm";
                ((MainActivity)ac).updateUI(rssi_str);
//                rssi_view.setText(rssi);

                Toast.makeText(getApplicationContext(),"  RSSI: " + rssi + "dBm", Toast.LENGTH_SHORT).show();
//                BTAdapter.cancelDiscovery();

            }

        }
    };
    public double  calculateDistance(int rssi) {

        int txPower = 127; //hard coded power value. Usually ranges between -59 to -65

        if (rssi == 0) {
            return -1.0;
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double distance =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return distance;
        }
    }
    public void updateUI(final String str) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                rssi_view.setText(str);
                //use findFragmentById for fragments defined in XML ((SimpleFragment)getSupportFragmentManager().findFragmentByTag(fragmentTag)).updateUI(str);
            }
        });
    }
    @Override
    protected void onDestroy() {
//        unregisterReceiver(receiver);
        mBluetoothLeScanner.stopScan(mScanCallback);
        super.onDestroy();
    }
}
