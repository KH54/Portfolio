package com.ktds.cocomo.mobile_finalproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Process;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    TextView tv_emg_msg, tx1;
    EditText et_name, et_tel, et_emg_msg, et_del, et_search;
    Button btn_add, btn_set, btn_send, btn_del, btn_alldel, btn_return, btn_search, btn_exit;
    SQLiteDatabase helper;
    SQLiteDatabase msghelper;
    SQLiteDatabase db, msg_db;
    SensorManager sm;
    ListView lv;
    String[] names = new String[10];
    String[] tels = new String[10];
    int i = 0;
    Switch aswitch;
    String setmsg = "";
    private int count = 0;
    private final int SMS_KEY = 8995;
    private static final float SHAKE_THRESHOLD = 90.0f;
    private long lastTime;
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private boolean Running = true;
    private long mInitTime;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        helper = new EmgHelper(this);
        try {
            db = helper.getWritableDatabase();
        } catch (SQLiteException ex) {
            db = helper.getReadableDatabase();
        }
        msghelper = new MsgHelper(this);
        try {
            msg_db = msghelper.getWritableDatabase();
        } catch (SQLiteException ex) {
            msg_db = msghelper.getReadableDatabase();
        }
        lv = findViewById(R.id.listview);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String vo = (String) adapterView.getAdapter().getItem(i);
                et_del.setText(vo);
            }
        });
        final UserInfo userinfo = new UserInfo();
        lv.setAdapter(userinfo);
        tv_emg_msg = findViewById(R.id.tv_emg_msg);
        et_name = findViewById(R.id.et_name);
        et_tel = findViewById(R.id.et_tel);
        et_emg_msg = findViewById(R.id.et_emg);
        et_del = findViewById(R.id.et_del);
        et_search = findViewById(R.id.et_search);
        tx1 = findViewById(R.id.tx1);
        btn_add = findViewById(R.id.btn_add);
        btn_set = findViewById(R.id.btn_set);
        btn_send = findViewById(R.id.btn_send);
        btn_del = findViewById(R.id.btn_del);
        btn_exit = findViewById(R.id.btn_exit);
        btn_alldel = findViewById(R.id.btn_alldel);
        btn_return = findViewById(R.id.btn_return);
        btn_search = findViewById(R.id.btn_search);
        aswitch = findViewById(R.id.switch1);
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sm.registerListener(MainActivity.this, s, SensorManager.SENSOR_DELAY_UI); // ???????????? ??????
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String qry = "select name, tel from info";
        Cursor c = db.rawQuery(qry, null);
        while (c.moveToNext()) {
            String name = c.getString(0);
            String tel = c.getString(1);
            names[i] = name;
            tels[i] = tel;
            i++;
            lv.setAdapter(userinfo);
        }
        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        } else {
            checkRunTimePermission();
        }
        String msg_qry = "select emsg from msg";
        Cursor msg_c = msg_db.rawQuery(msg_qry, null);
        while (msg_c.moveToNext()) {
            String emsg = msg_c.getString(0);
            tv_emg_msg.setText("???????????????: " + emsg);
        }// ????????? ?????? ??????
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("sms_info");
                builder.setMessage("This??app??won't??work??properly??unless??you??grant??SMS??permission.");
                builder.setIcon(android.R.drawable.ic_dialog_info);
                builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS}, SMS_KEY);
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_KEY);
            }
        }
        btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MyService.class);
                stopService(intent);
                sm.unregisterListener(MainActivity.this);
                moveTaskToBack(true);
                finishAndRemoveTask();
                Process.killProcess(Process.myPid()); // ????????? ??????
            }
        });
        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    names[i] = et_name.getText().toString();
                    tels[i] = et_tel.getText().toString();
                } catch (ArrayIndexOutOfBoundsException e) {
                    i--;
                    Toast.makeText(getApplicationContext(), "????????? ?????? ??? ??? ????????????.", Toast.LENGTH_LONG).show();
                } finally {
                    String name = names[i];
                    String tel = tels[i];
                    String qry = "insert into info values(NULL, '" + name + "', '" + tel + "');";
                    db.execSQL(qry);
                    et_name.setText("");
                    et_tel.setText("");
                    lv.setAdapter(userinfo);
                    Toast.makeText(MainActivity.this, "?????? ???????????????.", Toast.LENGTH_SHORT).show(); // ????????? ?????? ??????
                    i++;
                }
            }
        });
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (i = 0; i < 10; i++) {
                    tels[i] = "";
                    names[i] = "";
                }
                i = 0; //??????

                String search = et_search.getText().toString();
                String qry = "select name, tel from info where name = '" + search + "';";
                Cursor c = db.rawQuery(qry, null);
                while (c.moveToNext()) {
                    String name = c.getString(0);
                    String tel = c.getString(1);
                    tels[i] = tel;
                    names[i] = name;
                    lv.setAdapter(userinfo);
                }
                et_del.setText(tels[i]);
            }
        }); // ????????? ????????? ??????
        btn_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String del = et_del.getText().toString();
                for (i = 0; i < 10; i++) {
                    if (tels[i].equals(del)) {
                        String qry = "delete from info where tel = '" + del + "';";
                        db.execSQL(qry);
                        names[i] = "";
                        tels[i] = "";
                        Cursor c = db.rawQuery(qry, null);
                        break;
                    }
                }
                lv.setAdapter(userinfo);
                Toast.makeText(MainActivity.this, "?????????????????????.", Toast.LENGTH_SHORT).show();
                et_search.setText("");
                et_del.setText("");
            }
        }); // ????????? ????????? ??????
        btn_return.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (i = 0; i < 10; i++) {
                    tels[i] = "";
                    names[i] = "";
                }
                i = 0; //?????????
                String qry = "select name, tel from info";
                Cursor c = db.rawQuery(qry, null);
                while (c.moveToNext()) {
                    String name = c.getString(0);
                    String tel = c.getString(1);
                    names[i] = name;
                    tels[i] = tel;
                    i++;
                    lv.setAdapter(userinfo);
                }
                et_search.setText("");
                et_del.setText("");
            }
        }); // ????????? ????????????
        btn_alldel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String qry = "delete from info;";
                db.execSQL(qry);
                for (i = 0; i < 10; i++) {
                    names[i] = "";
                    tels[i] = "";
                }
                i = 0;
                lv.setAdapter(userinfo);//??????????????? ????????? ?????????

            }
        }); // ????????? ?????? ??????
        btn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emsg = et_emg_msg.getText().toString();
                String msg_qry = "insert into msg values(NULL, '" + emsg + "');";
                msg_db.execSQL(msg_qry);
                setmsg = emsg;
                tv_emg_msg.setText(emsg);
            }
        }); // ??????????????? ??????
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (i = 0; i < 10; i++) {
                    String phoneNo = tels[i];
                    String nameing = names[i];
                    String msg = tv_emg_msg.getText().toString();
                    try {//??????

                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(phoneNo, null, msg, null, null);
                        Toast.makeText(getApplicationContext(), nameing + "?????? ?????? ??????!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "?????? ??????. ????????? ????????? ????????????.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                        break;
                    }
                }
                i = 0;
            }
        }); // ?????? ????????? ?????? ??? ?????????
        tx1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count++;
                if (count == 5) {
                    Toast.makeText(MainActivity.this, "?????????: ?????????-20160749", Toast.LENGTH_SHORT).show();
                }
            }
        });
    } // ???????????????

    @Override
    void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long diff = (currentTime - lastTime);
            if (diff > 1000) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                float abs = (float) Math.sqrt(x * x + y * y + z * z); //??????
                if (abs > SHAKE_THRESHOLD) {
                    lastTime = currentTime;
                    String msgg = tv_emg_msg.getText().toString();
                    tv_emg_msg.setText("???????????????: " + msgg + "\n??????????????? \n??????:" + latitude + "\n??????:" + longitude);
                    lastTime = currentTime;
                    Toast.makeText(MainActivity.this, "????????????! ?????????????????? ???????????????.", Toast.LENGTH_SHORT).show();
                    TimeCheck tc = new TimeCheck();
                    tc.start();
                }
                i = 0;
            }
        }
    } //
// ?????? ??????over ?????? ??????, ?????? ????????? ???TimeCheck ???????????? ?????????.

    class TimeCheck extends Thread {
        @Override
        void run() {
            super.run();
            mInitTime = System.currentTimeMillis();
            while (Running) {
                long t = System.currentTimeMillis();
                long check = t - mInitTime;
                if (aswitch.isChecked() == false) {
                    if (check == 6000) {
                        MyThread mt = new MyThread();
                        mt.start();
                    }
                } else if (aswitch.isChecked() == true) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "???????????? ?????? ?????? ???????????????.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    interrupt();
                }
            }
            aswitch.setChecked(false);
            return;
        }
    } // 6?????? ???????????? ????????????switch???true??? ?????? ?????????MyTread ???????????? ????????? ???, ???????????? ??????.

    class MyThread extends Thread {
        public void run() {
            super.run();
            for (i = 0; i < 10; i++) {
                System.out.println(4);
                String phoneNo = tels[i];
                String nameing = names[i];
                String msg = tv_emg_msg.getText().toString();
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNo, null, msg, null, null);
                    Toast.makeText(getApplicationContext(), nameing + "?????? ?????? ??????!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    if (lv == null) {
                        Toast.makeText(getApplicationContext(), "?????? ??????. ????????? ????????? ????????????.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
    } // TimeCheck ??????????????? ????????? ????????? ???????????? ?????????

    @Override
    void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    class UserInfo extends BaseAdapter//ListView ??????
    {
        @Override
        int getCount() {
            return tels.length; // ????????? ???????????? ??????
        }

        @Override
        Object getItem(int position) {
            return tels[position];
        }

        @Override
        long getItemId(int position) {
            return position;
        }

        @Override
        View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout layout = new LinearLayout(getApplicationContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextView nameView = new TextView(getApplicationContext());
            nameView.setText(names[position]);
            nameView.setTextColor(Color.WHITE);
            nameView.setTextSize(20.0f);
            layout.addView(nameView, params);
            TextView telView = new TextView(getApplicationContext());
            telView.setText(tels[position]);
            telView.setTextColor(Color.WHITE);
            telView.setTextSize(15.0f);
            layout.addView(telView, params);
            return layout;
        }
    }
}