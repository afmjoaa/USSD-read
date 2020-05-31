package io.demo.ussbprototype;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Telephony;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private Button floatingBtn;
    private boolean isRegistered;
    private RadioGroup radioGroup;
    private RadioGroup radioGroupSim;
    private TextInputEditText editTextUssdCode;

    private SubscriptionManager mSubscriptionManager;
    public static boolean isMultiSimEnabled = false;
    public static List<SubscriptionInfo> subInfoList;
    private TextView tvSelectSim;
    private boolean simOne = true;
    private boolean above = true;
    private MaterialTextView retrievedResult;
    private MaterialCardView card;
    private String LOGTAG = "joaa";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSubscriptionManager = SubscriptionManager.from(this);

        //retrieving result for access ability
        //IntentFilter mFilter = new IntentFilter("REFRESH");
        //this.registerReceiver(mMessageReceiver, mFilter);
        //isRegistered = true;

        initFindView();
        initListener();

        initialize();
    }

    private void initialize() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.BIND_ACCESSIBILITY_SERVICE, Manifest.permission.READ_PHONE_STATE}, 2);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
            if (subInfoList.size() > 1) {
                isMultiSimEnabled = true;
                tvSelectSim.setText("Select Sim");
                radioGroupSim.setVisibility(View.VISIBLE);
            } else {
                tvSelectSim.setText("Default Sim Selected(Single sim)");
                radioGroupSim.setVisibility(View.GONE);
            }
        }
    }

    private void initListener() {
        floatingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAndRun();
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d("chk", "id" + checkedId);
                if (checkedId == R.id.above) {
                    above = true;
                } else if (checkedId == R.id.below) {
                    above = false;
                }
            }
        });

        radioGroupSim.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d("chk", "id" + checkedId);
                if (checkedId == R.id.simOne) {
                    simOne = true;
                } else if (checkedId == R.id.simTwo) {
                    simOne = false;
                }
            }
        });
    }

    private void initFindView() {
        floatingBtn = findViewById(R.id.ussdDemo);
        radioGroup = findViewById(R.id.radioGrp);
        radioGroupSim = findViewById(R.id.radioGrpSim);
        editTextUssdCode = findViewById(R.id.ussdCode);
        tvSelectSim = findViewById(R.id.select_sim);
        retrievedResult = findViewById(R.id.retrievedResult);
        card = findViewById(R.id.card);
    }

    private void checkPermissionAndRun() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.BIND_ACCESSIBILITY_SERVICE, Manifest.permission.READ_PHONE_STATE}, 1);
            return;
        } else {
            if (above) {
                runUssdCode(simOne);
            } else {
                runUssdCodeBefore26(simOne);
            }
        }
    }


    private void runUssdCodeBefore26(boolean simOne) {
        boolean enabled = isAccessibilityServiceEnabled(this, USSDService.class);

        if (enabled) {
            if (editTextUssdCode.getText() != null && editTextUssdCode.getText().length() != 0) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    //To find SIM ID
                    String primarySimId = null, secondarySimId = null;
                    SubscriptionManager subscriptionManager = (SubscriptionManager) this.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                    List<SubscriptionInfo> subList = subscriptionManager.getActiveSubscriptionInfoList();
                    int index = -1;
                    for (SubscriptionInfo subscriptionInfo : subList) {
                        index++;
                        if (index == 0) {
                            primarySimId = subscriptionInfo.getIccId();
                        } else {
                            secondarySimId = subscriptionInfo.getIccId();
                        }
                    }

                    // TO CREATE PhoneAccountHandle FROM SIM ID
                    TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
                    List<PhoneAccountHandle> list = telecomManager.getCallCapablePhoneAccounts();
                    PhoneAccountHandle primaryPhoneAccountHandle = null, secondaryPhoneAccountHandle = null;
                    for (PhoneAccountHandle phoneAccountHandle : list) {
                        if (phoneAccountHandle.getId().contains(primarySimId)) {
                            primaryPhoneAccountHandle = phoneAccountHandle;
                        }
                        if (phoneAccountHandle.getId().contains(secondarySimId)) {
                            secondaryPhoneAccountHandle = phoneAccountHandle;
                        }
                    }

                    Uri uri = Uri.fromParts("tel", editTextUssdCode.getText().toString(), "");
                    Bundle extras = new Bundle();

                    if (simOne) {
                        //To call from SIM 1
                        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, primaryPhoneAccountHandle);
                    } else {
                        //To call from SIM 2
                        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, secondaryPhoneAccountHandle);
                    }
                    telecomManager.placeCall(uri, extras);
                }
                // Uri.parse("tel:" + ussd) this also works
            /*String encodedHash = Uri.encode("#");
            String retrivedText = editTextUssdCode.getText().toString().substring(0,editTextUssdCode.getText().length()-1);
            String ussd = retrivedText + encodedHash;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(new Intent("android.intent.action.CALL",
                        Uri.parse("tel:" + ussd)), 1);
            }*/
            } else {
                Toast.makeText(this, "Please give the USSD Code in the field...", Toast.LENGTH_SHORT).show();
            }
        } else {
            //TODO ask for permission open settings
            Toast.makeText(this, "We will open the settings for you, you need to give Accessibility permission to the app...\nThen execute again...", Toast.LENGTH_LONG).show();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivityForResult(intent, 3);
                }
            }, 3000);
        }
        /*String encodedHash = Uri.encode("#");
        String ussd = "*152" + encodedHash;
        startActivityForResult(new Intent("android.intent.action.CALL",
                Uri.parse("tel:" + ussd)), 1);*/
    }


    @SuppressLint("MissingPermission")
    private void runUssdCode(boolean simOne) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(this);
        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        SubscriptionInfo subscriptionInfo;
        if (simOne) {
            subscriptionInfo = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0);
        } else {
            subscriptionInfo = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(1);
        }

        TelephonyManager manager2 = manager.createForSubscriptionId(subscriptionInfo.getSubscriptionId());
        if (editTextUssdCode.getText() != null && editTextUssdCode.getText().length() != 0) {
            manager2.sendUssdRequest(editTextUssdCode.getText().toString(), new TelephonyManager.UssdResponseCallback() {
                @Override
                public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                    super.onReceiveUssdResponse(telephonyManager, request, response);
                    Log.e("joaa success", request.toString() + " : " + response.toString());
                    card.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.badge_green));
                    retrievedResult.setText("Success : " + request.toString() + " : " + response.toString());
                }

                @Override
                public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                    super.onReceiveUssdResponseFailed(telephonyManager, request, failureCode);
                    Log.e("joaa fail", request.toString() + " : " + failureCode);
                    card.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.badge_red));
                    retrievedResult.setText("Fail : " + request.toString() + " : " + failureCode);
                }
            }, new Handler());
        } else {
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
                if (above) {
                    runUssdCode(simOne);
                } else {
                    runUssdCodeBefore26(simOne);
                }
            }
        }

        if (requestCode == 3) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runUssdCodeBefore26(simOne);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UssdRunEvent event) {
        card.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.secondaryLight));
        retrievedResult.setText(String.format(
                "class: %s \npackage: %s \ntime: %s \ntext: %s",
                event.className, event.packageName, event.time,
                event.message));
    }


    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityService) {
        ComponentName expectedComponentName = new ComponentName(context, accessibilityService);

        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServicesSetting == null)
            return false;

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);

            if (enabledService != null && enabledService.equals(expectedComponentName))
                return true;
        }
        return false;
    }

}


/*@Override
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

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            //Log.i("receiver", "Got message: " + message);
            Log.e("joaa success", message);
            card.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.secondaryLight));
            retrievedResult.setText(message);
        }
    };
    */
