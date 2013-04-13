package co.joyatwork.accelerometer.sandbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import co.joyatwork.filters.MovingAverage;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity implements SensorEventListener {

	private SensorManager sensorManager;
	private Sensor accelerometer;

	private static final String TAG = "AccelerationEventListener";
    private static final char CSV_DELIM = ',';
    private static final int THRESHHOLD = 2;
    private static final String CSV_HEADER =
            "Time,X Axis,Y Axis,Z Axis,X Avg, Y Avg, Z Avg";

    private PrintWriter printWriter;

	private XYPlot accelerationPlot;
	private SimpleXYSeries xAxisSeries;
	private SimpleXYSeries yAxisSeries;
	private SimpleXYSeries zAxisSeries;
	private SimpleXYSeries accelerationSeries;
	
	//private XYPlot cosAlphaPlot; 
	//private SimpleXYSeries cosAlphaSeries;
	
	private XYPlot dataPlot;
	private SimpleXYSeries dataPlotSeries1;
	private SimpleXYSeries dataPlotSeries2;
	
	private static final int CHART_REFRESH = 10;
    private static final int MAX_SERIES_SIZE = 30;
	private long lastChartRefresh;
    private long startTime;

    private static final float ALPHA = 0.8f;
    private float[] gravity = new float[3];
    
    private static final int MOVING_AVG_WINDOW_SIZE = 4;
    private MovingAverage[] movingAvgCalculators = { new MovingAverage(MOVING_AVG_WINDOW_SIZE),
    		new MovingAverage(MOVING_AVG_WINDOW_SIZE),
    		new MovingAverage(MOVING_AVG_WINDOW_SIZE) };
    
    private class CosAlpha {
    	private float lastVectorX = 0;
    	private float lastVectorY = 0;
    	private float lastVectorZ = 0;
    	
    	private boolean hasLastValues = false;
    	
    	public CosAlpha() {
			// TODO Auto-generated constructor stub
		}
    	
    	public double calculate(float vectorX, float vectorY, float vectorZ) {
    		if (hasLastValues == false) {
    			lastVectorX = vectorX;
    			lastVectorY = vectorY;
    			lastVectorZ = vectorZ;
    			hasLastValues = true;
    			return 1;
    		}
    		double dotProduct = lastVectorX * vectorX + lastVectorY * vectorY + lastVectorZ * vectorZ;
			double lastVectorMagnitude = Math.sqrt(lastVectorX * lastVectorX + lastVectorY * lastVectorY + lastVectorZ * lastVectorZ);
    		double vectorMagnitude = Math.sqrt(vectorX * vectorX + vectorY * vectorY + vectorZ * vectorZ);
    		double dotProductMagnitude = lastVectorMagnitude * vectorMagnitude;
    		double cosAlpha = dotProduct / dotProductMagnitude;
    		
    		lastVectorX = vectorX;
    		lastVectorY = vectorY;
    		lastVectorZ = vectorZ;
    		
    		return cosAlpha;
    	}
    }
    
    private CosAlpha cosAlpha = new CosAlpha();
	private float[] values;
	private long sampleTime;
	private float[] smoothedValues;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		// set xy plot
		accelerationPlot = (XYPlot) findViewById(R.id.accelerationPlot);
		accelerationPlot.setDomainLabel("Elapsed Time (ms)");
		accelerationPlot.setRangeLabel("Acceleration (m/sec^2)");
		accelerationPlot.setBorderPaint(null);
		accelerationPlot.disableAllMarkup();
		accelerationPlot.setRangeBoundaries(-3, 3, BoundaryMode.FIXED);

		xAxisSeries = new SimpleXYSeries("X Axis");
		yAxisSeries = new SimpleXYSeries("Y Axis");
		zAxisSeries = new SimpleXYSeries("Z Axis");
		accelerationSeries = new SimpleXYSeries("Acceleration");
		accelerationPlot.addSeries(xAxisSeries, LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.RED, Color.RED, null));
		accelerationPlot.addSeries(yAxisSeries, LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.GREEN, Color.GREEN, null));
		accelerationPlot.addSeries(zAxisSeries, LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.BLUE, Color.BLUE, null));
		accelerationPlot.addSeries(accelerationSeries, LineAndPointRenderer.class,
                  new LineAndPointFormatter(Color.CYAN, Color.CYAN, null));
		
		dataPlot = (XYPlot) findViewById(R.id.dataPlot);
		dataPlot.setDomainLabel("Elapsed Time (ms)");
		dataPlot.setRangeLabel("Acceleration (m/sec^2)");
		dataPlot.setBorderPaint(null);
		dataPlot.disableAllMarkup();
		dataPlot.setRangeBoundaries(-3, 3, BoundaryMode.FIXED);
		dataPlotSeries1 = new SimpleXYSeries("SZ");
		dataPlotSeries2 = new SimpleXYSeries("FZ");
		dataPlot.addSeries(dataPlotSeries1, LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.BLUE, Color.BLUE, null));
		dataPlot.addSeries(dataPlotSeries2, LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.RED, Color.RED, null));
		/*
		cosAlphaPlot = (XYPlot) findViewById(R.id.cosAlphaPlot);
		cosAlphaPlot.setDomainLabel("Elapsed Time (ms)");
		cosAlphaPlot.setRangeLabel("Cos Alpha");
		cosAlphaPlot.setBorderPaint(null);
		cosAlphaPlot.disableAllMarkup();
		cosAlphaPlot.setRangeBoundaries(0.8, 1, BoundaryMode.FIXED);
		cosAlphaSeries = new SimpleXYSeries("cos");
		cosAlphaPlot.addSeries(cosAlphaSeries, LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.CYAN, Color.CYAN, null));
		*/
		
		// Data files are stored on the external cache directory so they can
        // be pulled off of the device by the user
        File dataFile = new File(getExternalCacheDir(), "accelerometer.csv");
        //Log.d(TAG, dataFile.getAbsolutePath());
		try {
			//FileWriter calls directly OS for every write request
			//For better performance the FileWriter is wrapped into BufferedWriter, which calls out for a batch of bytes
			//PrintWriter allows a human-readable writing into a file
			printWriter = new PrintWriter(new BufferedWriter(new FileWriter(dataFile)));
			printWriter.println(CSV_HEADER);
		} catch (IOException e) {
			Log.e(TAG, "Could not open CSV file(s)", e);
		}
        
		startTime = SystemClock.uptimeMillis();
		lastChartRefresh = 0;
	}

	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, accelerometer,
				SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		//Log.d("acc-sensor", " -x " + event.values[0] + " -y " + event.values[1] + " -z " + event.values[2]);
		
		
		values = event.values.clone(); //TODO cloning might cause bad performance!!!
		sampleTime = event.timestamp;
        values = highPass(values[0], values[1], values[2]);
        
        smoothedValues = smoothValues(values);
		
        /*
		double sumOfSquares = (values[0] * values[0])
                 + (values[1] * values[1])
                 + (values[2] * values[2]);
        */
        //double acceleration = Math.sqrt(sumOfSquares);
        
		//double cosA = cosAlpha.calculate(event.values[0], event.values[1], event.values[2]);
		
        writeData();
 	    plotData();
	}

	private float[] smoothValues(float[] values) {
		movingAvgCalculators[0].pushValue(values[0]);
		movingAvgCalculators[1].pushValue(values[1]);
		movingAvgCalculators[2].pushValue(values[2]);
		
		float[] retVal = new float[3];
		retVal[0] = movingAvgCalculators[0].getValue();
		retVal[1] = movingAvgCalculators[1].getValue();
		retVal[2] = movingAvgCalculators[2].getValue();
		return retVal;
	}

	private float[] highPass(float x, float y, float z) {
		float[] filteredValues = new float[3];

		gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * x;
		gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * y;
		gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * z;

		filteredValues[0] = x - gravity[0];
		filteredValues[1] = y - gravity[1];
		filteredValues[2] = z - gravity[2];

		return filteredValues;
	}

	private void plotData() {
		long current = SystemClock.uptimeMillis();
		// Limit how much the chart gets updated
	    if ((current - lastChartRefresh) >= CHART_REFRESH) {
	        long timestamp = (sampleTime / 1000000) - startTime;
	        
	        // Plot data
	        addDataPoint(xAxisSeries, timestamp, values[0]);
	        addDataPoint(yAxisSeries, timestamp, values[1]);
	        addDataPoint(zAxisSeries, timestamp, values[2]);
	        accelerationPlot.redraw();
	        
	        addDataPoint(dataPlotSeries1, timestamp, values[2]);
	        addDataPoint(dataPlotSeries2, timestamp, smoothedValues[2]);
	        dataPlot.redraw();
	        
	        lastChartRefresh = current;
	    }
	}

	private void addDataPoint(SimpleXYSeries series, Number timestamp,
			Number value) {
		if (series.size() == MAX_SERIES_SIZE) {
			series.removeFirst();
		}

		series.addLast(timestamp, value);
	}

	private void writeData() {
		if (printWriter != null) {
			StringBuffer sb = new StringBuffer()
				.append((sampleTime / 1000000) - startTime).append(CSV_DELIM)
				.append(values[0]).append(CSV_DELIM) // x
				.append(values[1]).append(CSV_DELIM) // y
				.append(values[2]).append(CSV_DELIM) // z
				.append(smoothedValues[0]).append(CSV_DELIM)
				.append(smoothedValues[1]).append(CSV_DELIM)
				.append(smoothedValues[2])
				;

			printWriter.println(sb.toString());
			if (printWriter.checkError()) {
				Log.w(TAG, "Error writing sensor event data");
			}
		}
	}

}
