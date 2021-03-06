package com.example.lenovo.azimuthtest.MainClass;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lenovo.azimuthtest.CollectTimeSet.CollectTime;
import com.example.lenovo.azimuthtest.CollectTimeSet.FileName;
import com.example.lenovo.azimuthtest.QuaternionClass.FusingClass;
import com.example.lenovo.azimuthtest.R;
import com.example.lenovo.azimuthtest.StepDectClass.StepDectFsm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private static String TAG="AzimuthTest";
    private SensorManager sensorManager;
    private Sensor accsensor,magsensor,gsensor,grsensor,presensor,lightsensor,linaccsensor,unmagsensor;
    private TextView tv1,tv2,tv3,tv4,tv5,tv6,tv7,tv8,tv9,tv10;
    private EditText editText;
    private Button bt1,bt2;
    float[] Rorate=new float[9];
    float[] OriVal=new float[3];
    float[] accVal=new float[3];
    float[] magVal=new float[3];
    float[] gVal=new float[3];
    float[] grVal=new float[3];
    float[] linaccVal=new float[3];
    float lightRSS=0f;
    float pressure=0f;
    private float Epsilon=0.0009765625f;
    private float Threshold=0.5f-Epsilon;
    FusingClass fusingClass;
    private float[] Quaternion=new float[4];
    private float[] Euler_degrees=new float[3];
    private StepDectFsm stepDectFsm;
    private int stepcount=0;
    private float zerosTimes=0f;
    private float azimuthInDegree = 0f;//?????????
    private float lastAzimuthInDegree = 0f;//????????????????????????
    private int azimuthUpdateTimes = 0;
    private int zeroTimes = 0;//???????????????????????????????????????????????????????????????
    private float allAzimuthInDegree = 0f;
    private float meanAzimuthInDegree = 0f;
    private String[] needed_permission;
    private boolean doWrite=false;
    String fileName="imusensordata";
    String sdPath;
    private float stepLength=0f;
    float[] Euler=new float[3];
    private int update=0;
    boolean isGRa=false,isGYR=false,isMAg=false;
    private int count=1;
    private int sampletime=0;
    private TimeCount timecount;
    Calendar calendar=Calendar.getInstance();
    int year=calendar.get(Calendar.YEAR);
    int month=calendar.get(Calendar.MONTH);
    int day=calendar.get(Calendar.DAY_OF_MONTH);
    int hour=calendar.get(Calendar.HOUR_OF_DAY);
    int minute=calendar.get(Calendar.MINUTE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        requestApplicationPermission();
        fusingClass=new FusingClass();
        fileName=fileName+"_"+year+"_"+month+"_"+day+"_"+hour+"_"+minute;
        //????????????
        stepDectFsm=new StepDectFsm();
        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sampletime=Integer.valueOf(String.valueOf(editText.getText().toString()));
                timecount=new TimeCount(sampletime*1000,1000);
                timecount.start();
                doWrite=true;
//                count=count+1;
                fileName=fileName+"_"+count;
                Log.d(TAG,"Start...");
            }
        });
        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                doWrite=false;
//                fileName="Azimuthdata";
                fileName="imusensordata"+"_"+year+"_"+month+"_"+day+"_"+hour+"_"+minute;
                count=count+1;
                editText.setText(sampletime+"");

            }
        });

    }

    private void initView() {
        tv1=(TextView)findViewById(R.id.text1);
        tv2=(TextView)findViewById(R.id.text2);
        tv3=(TextView)findViewById(R.id.text3);
        tv4=(TextView)findViewById(R.id.text4);
        tv5=(TextView)findViewById(R.id.text5);
        tv6=(TextView)findViewById(R.id.text6);
        tv7= (TextView) findViewById(R.id.text7);
        tv8= (TextView) findViewById(R.id.text8);
        tv9= (TextView) findViewById(R.id.text9);
        tv10= (TextView) findViewById(R.id.text10);
        editText= (EditText) findViewById(R.id.edit_text);
        bt1=(Button)findViewById(R.id.button);
        bt2=(Button)findViewById(R.id.button1);
    }

    private void requestApplicationPermission() {
        needed_permission = new String[]{
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.READ_LOGS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        boolean permission_ok = true;
        for (String permission : needed_permission) {
            if (ContextCompat.checkSelfPermission(this,
                    permission) != PackageManager.PERMISSION_GRANTED) {
                permission_ok = false;
//                mTextView.append(String.valueOf(permission_ok)+"\n");
            }
        }
        if (!permission_ok) {
            ActivityCompat.requestPermissions(this, needed_permission, 1);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
    //?????????????????????
    @Override

    protected void onResume() {
        super.onResume();
        sensorManager=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accsensor=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magsensor=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gsensor=sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        grsensor=sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        presensor=sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        lightsensor=sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        linaccsensor=sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        sensorManager.registerListener(this,accsensor,CollectTime.COLLECT_NOR);
        sensorManager.registerListener(this,magsensor,CollectTime.COLLECT_NOR);
        sensorManager.registerListener(this,gsensor,CollectTime.COLLECT_NOR);
        sensorManager.registerListener(this,grsensor,CollectTime.COLLECT_NOR);
        sensorManager.registerListener(this,presensor,CollectTime.COLLECT_NOR);
        sensorManager.registerListener(this,lightsensor,CollectTime.COLLECT_NOR);
        sensorManager.registerListener(this,linaccsensor,CollectTime.COLLECT_NOR);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onSensorChanged(SensorEvent event) {
        String string="";
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                accVal=event.values.clone();
                if(stepDectFsm.StepDect(accVal)){
                    stepLength=stepDectFsm.getStepLength();
                    stepcount++;
                    tv1.setText("Step number is : "+stepcount);
                    tv2.setText("Step length is : "+stepLength);
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magVal=event.values.clone();
                isMAg=true;
                tv3.setText("magx is???"+magVal[0]);
                tv4.setText("magy is???"+magVal[1]);
                tv5.setText("mayz is???"+magVal[2]);
                update++;
                break;
            case Sensor.TYPE_GRAVITY:
                gVal=event.values.clone();
                isGRa=true;
//                string=string+gVal[0]+" "+gVal[1]+" "+gVal[2]+" ";
//                tv4.setText("???????????????x?????????"+gVal[0]);
//                tv5.setText("???????????????y?????????"+gVal[1]);
//                tv6.setText("???????????????z?????????"+gVal[2]);sb.append(magVal[0]).append(',').append(magVal[1]).append(',').append(magVal[2]).append(',');
                update++;
                break;
            case Sensor.TYPE_GYROSCOPE:
                grVal=event.values.clone();
                update++;
                isGYR=true;
                break;
            case Sensor.TYPE_PRESSURE:
                pressure=event.values[0];
                Log.i("TAG-pressure: ",""+pressure);
                break;
            case Sensor.TYPE_LIGHT:
                lightRSS=event.values[0];
                Log.i("TAG-lightRSS: ",""+lightRSS);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                linaccVal=event.values.clone();
                break;
            default:
                break;
        }

        float Azimuth=0f,Pitch=0f,Roll=0f;
        //??????MCF??????
//        if(isGRa&&isGYR&&isMAg) {
//            Quaternion = fusingClass.AHRSupdate(grVal[0], grVal[1], grVal[2], gVal[0], gVal[1], gVal[2], magVal[0], magVal[1], magVal[2]);
//            QuaternionToEuler(Quaternion);
//            Euler_degrees=Euler.clone();
//            isGRa=false;
//            isGYR=false;
//            isMAg=false;
//            Log.i(TAG,"data"+","+gVal[0]+","+gVal[1]+","+gVal[2]+","+
//                    grVal[0]+","+grVal[1]+","+grVal[2]+","+magVal[0]+","+magVal[1]+","+magVal[2]);
//            Azimuth=(float)Math.toDegrees(Euler_degrees[2])+90.0f;
//            if(Azimuth<0){
//                Azimuth=360+Azimuth;
//                Azimuth=Azimuth-4.5f;//??????????????????
//                tv3.setText("Azimuth is : "+Azimuth);
//            }else{
//                Azimuth=Azimuth-4.5f;
//                if(Azimuth<0){
//                    Azimuth=0f;
//                }
//                tv3.setText("Azimuth is : "+Azimuth);
//            }
//            //???????????????????????????????????????????????????????????????????????????????????????....
////            if(Azimuth>=0&&Azimuth<=270){
////                Azimuth=100+Azimuth;
////                tv3.setText("Azimuth is : "+Azimuth);
////            }else {
////                Azimuth=Azimuth-270;
////                if(Azimuth>0&&Azimuth<=180){
////                    Azimuth=Azimuth-10;
////                }
////                tv3.setText("Azimuth is : "+Azimuth);
////            }
//            Log.i(TAG," "+Azimuth);
//            String message=null;
//            //float Azimuth=getAzimuthInDegree(gVal,magVal);
//            message=""+Azimuth+" "+stepLength+"\n";
//            //??????????????????????????????
//            if(doWrite){
//                WriteFileSdcard(message);
//            }
//        }

        if(isGRa&&isGYR&&isMAg){
            SensorManager.getRotationMatrix(Rorate,null,gVal,magVal);
            SensorManager.getOrientation(Rorate,OriVal);
            isGRa=false;
            isGYR=false;
            isMAg=false;
            String strings;
            //????????????
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
            strings=sdf.format(new Date());
            string=strings+" "+accVal[0]+" "+accVal[1]+" "+accVal[2]+" "+magVal[0]+" "+magVal[1]+" "+magVal[2]+" "
            +string+grVal[0]+" "+grVal[1]+" "+grVal[2]+" "+linaccVal[0]+" "+linaccVal[1]+" "+linaccVal[2]+" ";
            Azimuth= (float) Math.toDegrees(OriVal[0]);
            Pitch=(float)Math.toDegrees(OriVal[1]);
            Roll= (float) Math.toDegrees(OriVal[2]);
            if(Math.toDegrees(OriVal[0])<0){
                Azimuth=360+Azimuth;
                tv6.setText("Yaw is : "+Azimuth);
            }else{
                tv6.setText("Yaw is : "+Azimuth);
            }

//            if(Math.toDegrees(Euler_degrees[0])<0){
//                Azimuth=360+Azimuth;
//                tv3.setText("Azimuth is : "+Azimuth);
//            }else {
//                tv3.setText("Azimuth is : "+Azimuth);
//            }
//            tv3.setText("Azimuth is : "+Euler_degrees[0]);
            //????????????
//            String message=null;
//            //float Azimuth=getAzimuthInDegree(gVal,magVal);
//            message=""+Azimuth+" "+stepLength+"\n";
            string=string+Azimuth+" "+Pitch+" "+Roll+" "+stepLength+"\n";
            //??????????????????????????????
            if(doWrite){
                WriteFileSdcard(string);
            }
            Log.i(TAG," "+string);
            tv7.setText("Roll is : "+Roll);
            tv8.setText("Pitch is : "+Pitch);
            tv9.setText("LightRSS is: "+lightRSS);
            tv10.setText("Pressure is: "+pressure);
            string=null;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private float getAzimuthInDegree(float[] accVal,float[] magVal){
        float azimuth=0.0f;
        if (accVal!= null && magVal!= null) {
            azimuthUpdateTimes++; //??????????????????????????????
            if (accVal.equals(magVal)) {
                azimuthInDegree = lastAzimuthInDegree;
            } else {
                SensorManager.getRotationMatrix(Rorate, null, accVal,magVal);
                SensorManager.getOrientation(Rorate,OriVal);
                float azimuthInRad = OriVal[0];
                if (azimuthInRad < 0) {
                    azimuthInRad += 2 * Math.PI;
                }
                azimuthInDegree = (float) Math.toDegrees(azimuthInRad) - 5.9f;//???????????????????????????,??????????????????????????????5.9
            }
            lastAzimuthInDegree = azimuthInDegree;
            if (Math.abs(azimuthInDegree - 0) < 0.0001) {
                zeroTimes++; //?????????0?????????
            }
            allAzimuthInDegree += azimuthInDegree;
//            mAccValues = null;
//            mMagValues = null;
            if (azimuthUpdateTimes >= 5) {
                if (zeroTimes == 5) {
                    meanAzimuthInDegree = 0f;
                } else {
                    meanAzimuthInDegree = allAzimuthInDegree / (azimuthUpdateTimes - zeroTimes);
                }
                allAzimuthInDegree = 0f;
                azimuthUpdateTimes = 0;
                zeroTimes = 0;
//                double tmp=0.5+0.1*Math.random()-0.15*Math.random();
                azimuth=meanAzimuthInDegree;

            }
        }

        return azimuth;
    }
    //?????????????????????
    public float[] QuaternionToEuler(float[] Q){
        //??????????????????????????????????????????????????????????????????????????????????????????
        //float[] Euler=new float[3];
        float TEST=Q[0]*Q[1]+Q[2]*Q[3];
        //??????????????????????????????90
        if(TEST>Threshold){
            Euler[2]= (float) (2*Math.atan2(Q[0],Q[3]));
            Euler[1]= (float) (Math.PI/2);
            Euler[0]=0.0f;
            return Euler;
        }
        if(TEST<-Threshold){
            Euler[2]= -(float) (2*Math.atan2(Q[0],Q[3]));
            Euler[1]= -(float) (Math.PI/2);
            Euler[0]=0.0f;
            return Euler;
        }
        float sqx=Q[0]*Q[0];
        float sqy=Q[1]*Q[1];
        float sqz=Q[2]*Q[2];
        Euler[0]= (float) Math.atan2(2*Q[1]*Q[3]-2*Q[0]*Q[2], 1 - 2*sqy - 2*sqz);
        Euler[1]= (float) Math.asin(2*TEST);
        Euler[2]=(float) Math.atan2(2*Q[0]*Q[3]-2*Q[1]*Q[2],1-2*sqx-2*sqz);
        return Euler;
    }

    private void WriteFileSdcard(String message) {
        try{
            //???????????????
            sdPath = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator;
            File file = new File(sdPath+ FileName.str+File.separator);
            if(!file.exists()){
                file.mkdir();
            }
            //?????????????????????
            File file1=new File(sdPath+FileName.str+File.separator+fileName+".txt");
            if (!file1.exists()) {
                file1.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file1,true);
            fos.write(message.getBytes());
            fos.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }


    //?????????
    private class TimeCount extends CountDownTimer {

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            editText.setText(millisUntilFinished/1000+"");
        }

        @Override
        public void onFinish() {
            doWrite=false;
        }
    }
}
