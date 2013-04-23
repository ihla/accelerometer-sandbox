package co.joyatwork.accelerometer.sandbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import co.joyatwork.pedometer.StepDetector;
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
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

	private SensorManager sensorManager;
	private Sensor accelerometer;

	private static final String TAG = "AccelerationEventListener";
    private static final char CSV_DELIM = ',';
    private static final String CSV_HEADER_ACCEL_FILE =
            "Time,X Axis,Y Axis,Z Axis,X Avg,Y Avg,Z Avg";
    private static final String CSV_HEADER_STEPS_FILE =
            "Time,X-Avg,Y-Avg,Z-Avg,X-Thld,Y-Thld,Z-Thld,X-Min,X-Max,Y-Min,Y-Max,Z-Min,Z-Max,X-Step,Y-Step,Z-Step";
    private static final String CSV_HEADER_P2P_FILE =
            "Time,X-Avg,Y-Avg,Z-Avg,X-CurrP,X-P,Y-CurrP,Y-P,Z-CurrP,Z-P";

    private PrintWriter accelerometerDataWriter;
    private PrintWriter stepsDataWriter;
    private PrintWriter peak2peakDataWriter;

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

    //TDOD remove
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
	private float[] thresholdValues;
	private SimpleXYSeries dataPlotSeries3;
	private float xStep;
	private float yStep;
	private float zStep;
	private SimpleXYSeries dataPlotSeries4;
	private StepDetector stepDetector;
	private int[] oldStepCounts;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		stepDetector = new StepDetector();
		oldStepCounts = new int[3];
		oldStepCounts[0] = 0;
		oldStepCounts[1] = 0;
		oldStepCounts[2] = 0;
		
		xStep = -0.5F;
		yStep = -0.5F;
		zStep = -0.5F;
		
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
		dataPlotSeries1 = new SimpleXYSeries("Z sample");
		dataPlotSeries2 = new SimpleXYSeries("Z avg");
		dataPlotSeries3 = new SimpleXYSeries("Z thold");
		dataPlotSeries4 = new SimpleXYSeries("step");
		dataPlot.addSeries(dataPlotSeries1, LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.BLUE, Color.BLUE, null));
		dataPlot.addSeries(dataPlotSeries2, LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.RED, Color.RED, null));
		dataPlot.addSeries(dataPlotSeries3, LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.YELLOW, Color.YELLOW, null));
		dataPlot.addSeries(dataPlotSeries4, LineAndPointRenderer.class,
				new LineAndPointFormatter(Color.MAGENTA, Color.MAGENTA, null));
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
        File accelerometerDataFile = new File(getExternalCacheDir(), "accelerometer.csv");
        File stepsDataFile = new File(getExternalCacheDir(), "steps.csv");
        File peak2peakDataFile = new File(getExternalCacheDir(), "peaks.csv");
        //Log.d(TAG, dataFile.getAbsolutePath());
		try {
			//FileWriter calls directly OS for every write request
			//For better performance the FileWriter is wrapped into BufferedWriter, which calls out for a batch of bytes
			//PrintWriter allows a human-readable writing into a file
			accelerometerDataWriter = new PrintWriter(new BufferedWriter(new FileWriter(accelerometerDataFile)));
			accelerometerDataWriter.println(CSV_HEADER_ACCEL_FILE);

			stepsDataWriter = new PrintWriter(new BufferedWriter(new FileWriter(stepsDataFile)));
			stepsDataWriter.println(CSV_HEADER_STEPS_FILE);
			
			peak2peakDataWriter = new PrintWriter(new BufferedWriter(new FileWriter(peak2peakDataFile)));
			peak2peakDataWriter.println(CSV_HEADER_P2P_FILE);

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
		long sampleTimeInMilis = sampleTime / 1000000;
        
		int[] stepCounts = stepDetector.countSteps(values, sampleTimeInMilis);
		
		//TODO temp visualization of step counting
		if (oldStepCounts[0] != stepCounts[0]) {
			xStep *= -1;
		}
		if (oldStepCounts[1] != stepCounts[1]) {
			yStep *= -1;
		}
		if (oldStepCounts[2] != stepCounts[2]) {
			zStep *= -1;
		}
		oldStepCounts[0] = stepCounts[0];
		oldStepCounts[1] = stepCounts[1];
		oldStepCounts[2] = stepCounts[2];
		
		values = stepDetector.getLinearAccelerationValues();
		smoothedValues = stepDetector.getSmoothedAccelerationValues();
		thresholdValues = stepDetector.getThresholdValues();
 
		updateTextViewCounters(stepCounts);
		writeData();
 	    plotData();
	}

	private void updateTextViewCounters(int[] stepCounts) {
		TextView xStepsCountView = (TextView) findViewById(R.id.xStepsCountTextView);
		xStepsCountView.setText("" + stepCounts[0]);
		TextView yStepsCountView = (TextView) findViewById(R.id.yStepsCountTextView);
		yStepsCountView.setText("" + stepCounts[1]);
		TextView zStepsCountView = (TextView) findViewById(R.id.zStepsCountTextView);
		zStepsCountView.setText("" + stepCounts[2]);
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
	        addDataPoint(dataPlotSeries3, timestamp, thresholdValues[2]);
	        addDataPoint(dataPlotSeries4, timestamp, zStep);
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
		long timeStampInMilis = (sampleTime / 1000000) - startTime;
		if (accelerometerDataWriter != null) {
			StringBuffer sb = new StringBuffer()
				.append(timeStampInMilis).append(CSV_DELIM)
				.append(values[0]).append(CSV_DELIM) // x
				.append(values[1]).append(CSV_DELIM) // y
				.append(values[2]).append(CSV_DELIM) // z
				.append(smoothedValues[0]).append(CSV_DELIM)
				.append(smoothedValues[1]).append(CSV_DELIM)
				.append(smoothedValues[2])
				;

			accelerometerDataWriter.println(sb.toString());
			if (accelerometerDataWriter.checkError()) {
				Log.w(TAG, "Error writing sensor event data");
			}
		}
		if (stepsDataWriter != null) {
			StringBuffer sb = new StringBuffer()
				.append(timeStampInMilis).append(CSV_DELIM)
				.append(smoothedValues[0]).append(CSV_DELIM)
				.append(smoothedValues[1]).append(CSV_DELIM)
				.append(smoothedValues[2]).append(CSV_DELIM)
				.append(thresholdValues[0]).append(CSV_DELIM)
				.append(thresholdValues[1]).append(CSV_DELIM)
				.append(thresholdValues[2]).append(CSV_DELIM)
				.append(stepDetector.getThresholds()[0].getFixedMinValue()).append(CSV_DELIM)
				.append(stepDetector.getThresholds()[0].getFixedMaxValue()).append(CSV_DELIM)
				.append(stepDetector.getThresholds()[1].getFixedMinValue()).append(CSV_DELIM)
				.append(stepDetector.getThresholds()[1].getFixedMaxValue()).append(CSV_DELIM)
				.append(stepDetector.getThresholds()[2].getFixedMinValue()).append(CSV_DELIM)
				.append(stepDetector.getThresholds()[2].getFixedMaxValue()).append(CSV_DELIM)
				.append(xStep).append(CSV_DELIM)
				.append(yStep).append(CSV_DELIM)
				.append(zStep)
				;

			stepsDataWriter.println(sb.toString());
			if (stepsDataWriter.checkError()) {
				Log.w(TAG, "Error writing steps data");
			}
		}
		if (peak2peakDataWriter != null) {
			StringBuffer sb = new StringBuffer()
				.append(timeStampInMilis).append(CSV_DELIM)
				.append(smoothedValues[0]).append(CSV_DELIM)
				.append(smoothedValues[1]).append(CSV_DELIM)
				.append(smoothedValues[2]).append(CSV_DELIM)
				.append(stepDetector.getThresholds()[0].getCurrentPeak2PeakValue()).append(CSV_DELIM)
				.append(stepDetector.getThresholds()[0].getFixedPeak2PeakValue()).append(CSV_DELIM)
				.append(stepDetector.getThresholds()[1].getCurrentPeak2PeakValue()).append(CSV_DELIM)
				.append(stepDetector.getThresholds()[1].getFixedPeak2PeakValue()).append(CSV_DELIM)
				.append(stepDetector.getThresholds()[2].getCurrentPeak2PeakValue()).append(CSV_DELIM)
				.append(stepDetector.getThresholds()[2].getFixedPeak2PeakValue())
				;

			peak2peakDataWriter.println(sb.toString());
			if (stepsDataWriter.checkError()) {
				Log.w(TAG, "Error writing peak data");
			}
		}
	}

}
