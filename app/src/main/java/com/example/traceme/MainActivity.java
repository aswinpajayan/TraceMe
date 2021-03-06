package com.example.traceme;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.text.DateFormat;
import java.util.Date;

import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    public static final String MESSENGER_INTENT_KEY = "msg-intent-key";

    private TextView locationMsg;

    // as google doc says
    // Handler for incoming messages from the service.
    private IncomingMessageHandler mHandler;
    public static final String AUTHORITY = "com.example.traceme";
    public static final String ACCOUNT_TYPE = "example.com";
    public static final String ACCOUNT = "Google";
    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SYNC_INTERVAL_IN_MINUTES = 1L;
    public static final long SYNC_INTERVAL =
            SYNC_INTERVAL_IN_MINUTES *
                    SECONDS_PER_MINUTE;
    ContentResolver mResolver;
    Account mAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        locationMsg = findViewById(R.id.location);

        mHandler = new IncomingMessageHandler();

        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .getBoolean("isFirstRun", true);

        if (isFirstRun) {
            //show start activity

            startActivity(new Intent(MainActivity.this, Register.class));
            Toast.makeText(MainActivity.this, "Enable TraceMe in Autostart Menu..", Toast.LENGTH_LONG)
                    .show();
            askIgnoreOptimization();
            mioppo();
        }


        getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
                .putBoolean("isFirstRun", false).apply();
        requestPermissions();

        mAccount = CreateSyncAccount(this);
        mResolver = getContentResolver();

        setAccountSyncable();
        ContentResolver.setSyncAutomatically(mAccount,AUTHORITY,true);
        ContentResolver.addPeriodicSync(
                mAccount,
                AUTHORITY,
                Bundle.EMPTY,
                SYNC_INTERVAL);

        startWork();
    }

    private void startWork() {
        OneTimeWorkRequest refreshWork = new OneTimeWorkRequest.Builder(MyWorker.class).build();
        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork("Location", ExistingWorkPolicy.KEEP, refreshWork);
    }

    public static Account CreateSyncAccount(Context context) {
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
        } else {
        }
        return newAccount;
    }

    private void setAccountSyncable() {
        if (ContentResolver.getIsSyncable(mAccount, AUTHORITY) == 0) {
            Log.i("Data", "sync option was set to Off");
            ContentResolver.setIsSyncable(mAccount, AUTHORITY, 1);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
                finish();
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // can be schedule in this way also
                //  Utils.scheduleJob(this, LocationUpdatesService.class);
                //doing this way to communicate via messenger
                // Start service and provide it a way to communicate with this class.
                Intent startServiceIntent = new Intent(this, LocationUpdatesService.class);
                Messenger messengerIncoming = new Messenger(mHandler);
                startServiceIntent.putExtra(MESSENGER_INTENT_KEY, messengerIncoming);
                startService(startServiceIntent);
            } else {
                // Permission denied.
                finish();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        initAlarm();
        mHandler = null;
    }


    private void initAlarm() {
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, StartServiceReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() +
                        30*1000, alarmIntent);

    }

    private void askIgnoreOptimization() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent intent = new Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
        }
    }

    private void mioppo() {
        if(Build.BRAND.equalsIgnoreCase("xiaomi") ){
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                startActivity(intent);
            }
            catch (Exception xiami){}

        }else if(Build.BRAND.equalsIgnoreCase("Letv")){
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"));
                startActivity(intent);
            }
            catch (Exception Letv){}
        }
        else if(Build.BRAND.equalsIgnoreCase("Honor")){

            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
                startActivity(intent);
            }
            catch(Exception honor){}
        }
        else if(Build.BRAND.equalsIgnoreCase("vivo")){

            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"));
                startActivity(intent);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.vivo.permissionmanager",
                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
                    startActivity(intent);
                } catch (Exception ex) {
                    try {
                        Intent intent = new Intent();
                        intent.setClassName("com.iqoo.secure",
                                "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager");
                        startActivity(intent);
                    } catch (Exception exx) {
                        ex.printStackTrace();
                    }
                }
            }

        }
        else if (Build.MANUFACTURER.equalsIgnoreCase("oppo")) {
            try {
                Intent intent = new Intent();
                intent.setClassName("com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity");
                startActivity(intent);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent();
                    intent.setClassName("com.oppo.safe",
                            "com.oppo.safe.permission.startup.StartupAppListActivity");
                    startActivity(intent);

                } catch (Exception ex) {
                    try {
                        Intent intent = new Intent();
                        intent.setClassName("com.coloros.safecenter",
                                "com.coloros.safecenter.startupapp.StartupAppListActivity");
                        startActivity(intent);
                    } catch (Exception exx) {

                    }
                }
            }
        }
        else{}
    }
//    @Override
//    public void onBackPressed() {
//        DrawerLayout drawer = findViewById(R.id.drawer_layout);
//        if (drawer.isDrawerOpen(GravityCompat.START)) {
//            drawer.closeDrawer(GravityCompat.START);
//        } else {
//            super.onBackPressed();
//        }
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.job_service_demo, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
//
//    @SuppressWarnings("StatementWithEmptyBody")
//    @Override
//    public boolean onNavigationItemSelected(MenuItem item) {
//        // Handle navigation view item clicks here.
//        int id = item.getItemId();
//
//        if (id == R.id.nav_camera) {
//            // Handle the camera action
//        } else if (id == R.id.nav_gallery) {
//
//        } else if (id == R.id.nav_slideshow) {
//
//        } else if (id == R.id.nav_manage) {
//
//        } else if (id == R.id.nav_share) {
//
//        } else if (id == R.id.nav_send) {
//
//        }
//
//        DrawerLayout drawer = findViewById(R.id.drawer_layout);
//        drawer.closeDrawer(GravityCompat.START);
//        return true;
//
//    }

    class IncomingMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "handleMessage..." + msg.toString());

            super.handleMessage(msg);

            switch (msg.what) {
                case LocationUpdatesService.LOCATION_MESSAGE:
                    Location obj = (Location) msg.obj;
                    String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                    String name,mail,mobile;
                    name =  getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                            .getString("name","NULL");
                    mail = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                            .getString("mail","NULL");
                    mobile = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                            .getString("mobile","NULL");
                    locationMsg.setText("Name : "+name+"\nEmail : "+mail+"\nMobile : "+mobile+"\n\n LAT :  " + obj.getLatitude() + "\nLNG : " + obj.getLongitude() + "\n\n\n\n\nLast updated- " + currentDateTimeString);
                    break;
            }
        }
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            // Request permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}
