package io.demo.ussbprototype;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button floatingBtn;
    private boolean isRegistered;
    private RadioGroup radioGroup;
    private TextInputEditText editTextUssdCode;

    private SubscriptionManager mSubscriptionManager;
    public static boolean isMultiSimEnabled = false;
    public static List<SubscriptionInfo> subInfoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSubscriptionManager = SubscriptionManager.from(this);

        //retrieving result for access ability
        IntentFilter mFilter = new IntentFilter("REFRESH");
        this.registerReceiver(mMessageReceiver, mFilter);
        isRegistered = true;

        initFindView();
        floatingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAndRun();
            }
        });
    }

    private void initFindView() {
        floatingBtn = findViewById(R.id.ussdDemo);
        radioGroup = findViewById(R.id.radioGrp);
        editTextUssdCode = findViewById(R.id.ussdCode);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (isRegistered) {
                this.unregisterReceiver(mMessageReceiver);
                isRegistered = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkPermissionAndRun() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.BIND_ACCESSIBILITY_SERVICE, Manifest.permission.READ_PHONE_STATE}, 1);
            return;
        } else {
            runUssdCodeBefore26();
        }
    }


    private void runUssdCodeBefore26() {
        String encodedHash = Uri.encode("#");
        if (editTextUssdCode.getText() != null && editTextUssdCode.getText().length() != 0) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
                if (subInfoList.size() > 1) {
                    isMultiSimEnabled = true;
                }
                //Toast.makeText(this, isMultiSimEnabled + " " + subInfoList.size() , Toast.LENGTH_SHORT).show();
            }




            //To find SIM ID
            String primarySimId = null,secondarySimId = null;
            SubscriptionManager subscriptionManager = (SubscriptionManager) this.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            List<SubscriptionInfo> subList = subscriptionManager.getActiveSubscriptionInfoList();
            int index=-1;
            for (SubscriptionInfo subscriptionInfo : subList) {
                index++;
                if(index == 0){
                    primarySimId=subscriptionInfo.getIccId();
                }else {
                    secondarySimId=subscriptionInfo.getIccId();
                }
            }

            // TO CREATE PhoneAccountHandle FROM SIM ID
            TelecomManager telecomManager =(TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            List<PhoneAccountHandle> list = telecomManager.getCallCapablePhoneAccounts();
            PhoneAccountHandle primaryPhoneAccountHandle =null,secondaryPhoneAccountHandle = null;
            for(PhoneAccountHandle phoneAccountHandle:list){
                if(phoneAccountHandle.getId().contains(primarySimId)){
                    primaryPhoneAccountHandle=phoneAccountHandle;
                }
                if(phoneAccountHandle.getId().contains(secondarySimId)){
                    secondaryPhoneAccountHandle=phoneAccountHandle;
                }
            }



            //To call from SIM 2
           /* Uri uri = Uri.fromParts("tel",number, "");
            Bundle extras = new Bundle();  extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,secondaryPhoneAccountHandle);
            telecomManager.placeCall(uri, extras);*/









            //To call from SIM 1
            Uri uri = Uri.fromParts("tel", editTextUssdCode.getText().toString(), "");
            Bundle extras = new Bundle();  extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,primaryPhoneAccountHandle);
            telecomManager.placeCall(uri, extras);

            // Uri.parse("tel:" + ussd) this also works
            /*
            String retrivedText = editTextUssdCode.getText().toString().substring(0,editTextUssdCode.getText().length()-1);
            String ussd = retrivedText + encodedHash;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(new Intent("android.intent.action.CALL",
                        Uri.parse("tel:" + ussd)), 1);
            }*/
        }else{
            Toast.makeText(this, "Please give the USSD Code in the field...", Toast.LENGTH_SHORT).show();
        }

        /*String encodedHash = Uri.encode("#");
        String ussd = "*152" + encodedHash;
        startActivityForResult(new Intent("android.intent.action.CALL",
                Uri.parse("tel:" + ussd)), 1);*/
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.i("receiver", "Got message: " + message);
            Log.e("joaa success", message);
            //showText(message);
        }
    };

    @SuppressLint("MissingPermission")
    private void runUssdCode() {
        SubscriptionManager subscriptionManager =  SubscriptionManager.from(this);
        SubscriptionInfo subscriptionInfo = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0);
        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);


        TelephonyManager manager2 = manager.createForSubscriptionId(subscriptionInfo.getSubscriptionId());
        if (editTextUssdCode.getText() != null && editTextUssdCode.getText().length() != 0) {
            manager2.sendUssdRequest(editTextUssdCode.getText().toString(), new TelephonyManager.UssdResponseCallback() {
                @Override
                public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                    super.onReceiveUssdResponse(telephonyManager, request, response);
                    Log.e("joaa success", request.toString() + " : " + response.toString());
                }

                @Override
                public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                    super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode);
                    Log.e("joaa fail", request.toString() + " : " + failureCode);
                }
            }, new Handler());
        }else{
            Toast.makeText(this, "Please give the USSD Code in the field...", Toast.LENGTH_SHORT).show();
        }
       /* List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
            int subscriptionId = subscriptionInfo.getSubscriptionId();
            Log.d("Sims", "subscriptionId:" + subscriptionId);
        }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runUssdCodeBefore26();
            }
        }
    }
}
