package co.joyatwork.accelerometer.sandbox;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYPlot;

import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MainActivity extends Activity {

    private SensorManager sensorManager;
    
    private XYPlot xyPlot;


	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //set xy plot
        xyPlot = (XYPlot)findViewById(R.id.XYPlot);
        xyPlot.setDomainLabel("Elapsed Time (ms)");
        xyPlot.setRangeLabel("Acceleration (m/sec^2)");
        xyPlot.setBorderPaint(null);
        xyPlot.disableAllMarkup();
        xyPlot.setRangeBoundaries(-10, 10, BoundaryMode.FIXED);

	}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
