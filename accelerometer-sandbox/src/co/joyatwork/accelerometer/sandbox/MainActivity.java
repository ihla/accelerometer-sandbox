package co.joyatwork.accelerometer.sandbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Currency;

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
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {

	private SensorManager sensorManager;
	private Sensor accelerometer;

	private static final String TAG = "AccelerationEventListener";
    private static final char CSV_DELIM = ',';
    private static final String CSV_HEADER =
            "Time,X Axis,Y Axis,Z Axis,X Avg,Y Avg,Z Avg,X Thld,Y Thld,Z Thld,Min Z,Max Z,step";

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
    
    private static final int MOVING_AVG_WINDOW_SIZE = 10;
    private MovingAverage[] movingAvgCalculators = { new MovingAverage(MOVING_AVG_WINDOW_SIZE),
    		new MovingAverage(MOVING_AVG_WINDOW_SIZE),
    		new MovingAverage(MOVING_AVG_WINDOW_SIZE) };
    
    private class Threshold {
    	
    	private final static String TAG = "Threshold";
    	private final int windowSize;
		private float measuredMinValue;
		private float measuredMaxValue;
		private float currentMinValue;
		private float currentMaxValue;
		private int sampleCount;
		private float minValue;
		private float maxValue;
		private boolean isFirstSample;
		private float firstSample;
		private boolean isFirstWindow;
		

		public Threshold(int numberOfSamples) {
    		this.windowSize = numberOfSamples;
    		this.measuredMinValue = 0;
    		this.measuredMaxValue = 0;
    		this.currentMaxValue = 0;
    		this.currentMinValue = 0;
    		this.sampleCount = 0; 
    		this.minValue = 0;
    		this.maxValue = 0;
    		this.isFirstSample = true;
    		this.isFirstWindow = true;
    	}
		
		public void pushSample(float newSample) {
			
			if (isFirstSample) {
				isFirstSample = false;
				firstSample = newSample;
				measuredMinValue = measuredMaxValue = firstSample;
				currentMaxValue = measuredMaxValue;
				currentMinValue = measuredMinValue;
				return;
			}
			if (newSample < measuredMinValue) {
				measuredMinValue = newSample;
				currentMinValue = measuredMinValue;
			}
			else if (newSample > measuredMaxValue) {
				measuredMaxValue = newSample;
				currentMaxValue = measuredMaxValue;
			}
			sampleCount++;
			//Log.d(TAG, "current cnt: " + sampleCount + "min " + currentMinValue + "max " + currentMaxValue);
			if (sampleCount == windowSize) {
				sampleCount = 0;
				if (isFirstWindow) {
					isFirstWindow = false;
					if ((firstSample == measuredMinValue) || (firstSample == measuredMaxValue)) {
						measuredMinValue = measuredMaxValue = newSample;
						currentMaxValue = measuredMaxValue;
						currentMinValue = measuredMinValue;
						return;
					}
				}
				minValue = measuredMinValue;
				maxValue = measuredMaxValue;
				measuredMinValue = measuredMaxValue = newSample;
				//Log.d(TAG, "min: " + minValue + "max: " + maxValue + "treshold: " + calculate());
			}
		}
		
		public float getCurrentMinValue() {
			return currentMinValue;
		}
		
		public float getCurrentMaxValue() {
			return currentMaxValue;
		}
		
		public float getMinValue(){
			return minValue;
		}
		
		public float getMaxValue() {
			return maxValue;
		}
		
		public float calculate() {
			return (minValue + maxValue)/2;
		}
    }
    
    private static final int THRESHOLD_WINDOW_SIZE = 50;
    private Threshold[] thresholds = { new Threshold(THRESHOLD_WINDOW_SIZE), 
    		new Threshold(THRESHOLD_WINDOW_SIZE),
    		new Threshold(THRESHOLD_WINDOW_SIZE)
    };
    
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
	private float[] oldValues;
	private float zStep;
	private SimpleXYSeries dataPlotSeries4;
	private int zStepsCount;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		oldValues = new float[3];
		oldValues[0] = 0;
		oldValues[1] = 0;
		oldValues[2] = 0;
		zStep = -0.5F;
		zStepsCount = 0;
		
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
        
		values = highPass(values);
        smoothedValues = smoothValues(values);
		thresholdValues = calculateThreshold(smoothedValues);
		int[] stepCounts = countSteps();
		
 
		TextView zStepsCountView = (TextView) findViewById(R.id.zStepsCountTextView);
		zStepsCountView.setText("" + stepCounts[2]);

		writeData();
 	    plotData();
	}

	private int[] countSteps() {
		int[] returnValues = new int[3];
		if (thresholds[2].getCurrentMaxValue() > 0.3F && oldValues[2] > thresholdValues[2] && smoothedValues[2] < thresholdValues[2]) {
			zStep *= -1;
			zStepsCount++;
		}
		oldValues[2] = smoothedValues[2];
		returnValues[2] = zStepsCount;
		return returnValues;
	}

	private float[] calculateThreshold(float[] values) {
		thresholds[0].pushSample(values[0]);
		thresholds[1].pushSample(values[1]);
		thresholds[2].pushSample(values[2]);
		
		float[] returnValues = new float[3];
		returnValues[0] = thresholds[0].calculate();
		returnValues[1] = thresholds[1].calculate();
		returnValues[2] = thresholds[2].calculate();
		return returnValues;
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

	private float[] highPass(float[] values) {
		float[] filteredValues = new float[3];

		gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * values[0];
		gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * values[1];
		gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * values[2];

		filteredValues[0] = values[0] - gravity[0];
		filteredValues[1] = values[1] - gravity[1];
		filteredValues[2] = values[2] - gravity[2];

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
		if (printWriter != null) {
			StringBuffer sb = new StringBuffer()
				.append((sampleTime / 1000000) - startTime).append(CSV_DELIM)
				.append(values[0]).append(CSV_DELIM) // x
				.append(values[1]).append(CSV_DELIM) // y
				.append(values[2]).append(CSV_DELIM) // z
				.append(smoothedValues[0]).append(CSV_DELIM)
				.append(smoothedValues[1]).append(CSV_DELIM)
				.append(smoothedValues[2]).append(CSV_DELIM)
				.append(thresholdValues[0]).append(CSV_DELIM)
				.append(thresholdValues[1]).append(CSV_DELIM)
				.append(thresholdValues[2]).append(CSV_DELIM)
				.append(thresholds[2].getMinValue()).append(CSV_DELIM)
				.append(thresholds[2].getMaxValue()).append(CSV_DELIM)
				.append(zStep)
				;

			printWriter.println(sb.toString());
			if (printWriter.checkError()) {
				Log.w(TAG, "Error writing sensor event data");
			}
		}
	}

}
