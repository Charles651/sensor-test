package com.bgxt.x2demo;

import java.io.File;
import java.util.TimerTask;


import java.util.Timer;

import com.haijing.highglass.HmdCtrl;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import android.app.Activity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class test extends Activity {
	
	private SensorManager sensorManager;    
    private MySensorEventListener sensorEventListener;    
    private byte video_state;
    private byte stereo_mode;
    
    private final class MySensorEventListener implements SensorEventListener    
    {    
        //可以得到传感器实时测量出来的变化值    
        @Override    
        public void onSensorChanged(SensorEvent event)     
        {       
            //得到方向的值    
            if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD)    
            {    
                float x = event.values[SensorManager.DATA_X];          
                float y = event.values[SensorManager.DATA_Y];          
                float z = event.values[SensorManager.DATA_Z];      
                //Log.d("demo", "Orientation: " + x + ", " + y + ", " + z);    
                ((TextView)findViewById(R.id.TextView01)).setText("x:"+x+"y:"+y+"z:"+z+"    ");
                
            }    
            //得到加速度的值    
            else if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER)    
            {    
                float x = event.values[SensorManager.DATA_X];          
                float y = event.values[SensorManager.DATA_Y];          
                float z = event.values[SensorManager.DATA_Z];     
                //Log.d("demo", "Accelerometer: " + x + ", " + y + ", " + z);   
                ((TextView)findViewById(R.id.textSensor2)).setText("x:"+x+"y:"+y+"z:"+z+"    ");
            }    
            else if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE)    
            {    
                float x = event.values[SensorManager.DATA_X];          
                float y = event.values[SensorManager.DATA_Y];          
                float z = event.values[SensorManager.DATA_Z];     
                //Log.d("demo", "magnetic: " + x + ", " + y + ", " + z);   
                ((TextView)findViewById(R.id.textSensor3)).setText("x:"+x+"y:"+y+"z:"+z);
            }    
            
        }    
        //重写变化    
        @Override    
        public void onAccuracyChanged(Sensor sensor, int accuracy)     
        {    
        }    
    }   
    
    public void On3DClick(View view){
    	stereo_mode = HmdCtrl.get3D();
        Log.d("3ddemo", "Stereo mode : " + stereo_mode);
        HmdCtrl.set3D(HmdCtrl.HMD_CTRL_PARAM_DISP_3D_HALF_LR);

        // screen on/off test, please refer to HmdCtrl.
        video_state = HmdCtrl.getVideoState();
        Log.d("demo", "Video state : " + video_state);
    }
    
    public void On2DClick(View view){
    	stereo_mode = HmdCtrl.get3D();
        Log.d("2ddemo", "Stereo mode : " + stereo_mode);
        HmdCtrl.set3D(HmdCtrl.HMD_CTRL_PARAM_DISP_3D_NONE);

        // screen on/off test, please refer to HmdCtrl.
        video_state = HmdCtrl.getVideoState();
        Log.d("demo", "Video state : " + video_state);
    }
    
    private Handler handler = new Handler() {  
        @Override  
        public void handleMessage(Message msg) {  
            
        	((TextView)findViewById(R.id.textLight)).setText(msg.obj.toString());  
          
        }  
    };  
	
	@Override
	protected void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test); 
		//获取感应器管理器    
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorEventListener = new MySensorEventListener();
		
		TimerTask  task= new TimerTask() {  
	         @Override  
	         public void run() {  
	        	 video_state = HmdCtrl.getVideoState();
	        	 Message msg = new Message();  
	             msg.obj = String.format("距离："+video_state+"    亮度："+HmdCtrl.getLightState());  
	             handler.sendMessage(msg);  
	        	 //((TextView)findViewById(R.id.textLight)).setText("距离："+video_state+"    亮度："+HmdCtrl.getLightState());
	        	 Log.d("demo", "Video state : " + video_state);
	        	 Log.d("demo", "Light state : " + HmdCtrl.getLightState());
	             
	         }  
		};
	         
		 Timer timer = new Timer(true);
         
         //firstTime为Date类型,period为long，表示从firstTime时刻开始，每隔period毫秒执行一次。   
         timer.schedule(task, 1000,1000);            
	        

		
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		//获取陀螺仪    
        Sensor orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);    
        sensorManager.registerListener(sensorEventListener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL);    
            
        //获取加速度传感器    
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);    
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);   
        
      //获取磁场
        Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);    
        sensorManager.registerListener(sensorEventListener, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);  
        
      
	}
	
	@Override    
    protected void onPause()     
    {    
        sensorManager.unregisterListener(sensorEventListener);    
        super.onPause();    
    }      
	
	@Override  
    public boolean onKeyDown(int keyCode, KeyEvent event)  
    {  
         
        ((TextView)findViewById(R.id.textKey)).setText("键值："+keyCode);
          
        return false;  
          
    }  

}