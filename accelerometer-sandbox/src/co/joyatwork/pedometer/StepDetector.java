package co.joyatwork.pedometer;

import co.joyatwork.filters.MovingAverage;

public class StepDetector {

	public class Threshold {
    	
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

	private class StepCounter {
		
		private Threshold threshold;
		private int stepCount;
		private float lastSample;
		private float thresholdValue;
		
		public StepCounter(Threshold t) {
			threshold = t;
			stepCount = 0; 
			lastSample = 0;
			thresholdValue = 0;
		}
	
	    /**
	     * Calculates thresholds for step detections
	     */
		private void calculateThresholdValue(float newSample) {
			threshold.pushSample(newSample);
			thresholdValue = threshold.calculate();
		}

		public int update(float newSample) {
			calculateThresholdValue(newSample);
			//TODO threshold.getCurrentMaxValue() > 0.3F - how to ignore low values?
			if (threshold.getCurrentMaxValue() > 0.3F && lastSample > thresholdValue && newSample < thresholdValue) {
				stepCount++;
			}
			lastSample = newSample;
			return stepCount;
		}
		
		public float getThresholdValue() {
			return thresholdValue;
		}
		
		public int getValue() {
			return stepCount;
		}

	}
	
	public static final int X_AXIS = 0;
	public static final int Y_AXIS = 1;
	public static final int Z_AXIS = 2;

    private static final int MOVING_AVG_WINDOW_SIZE = 10;
    private MovingAverage[] movingAvgCalculators = { new MovingAverage(MOVING_AVG_WINDOW_SIZE),
    		new MovingAverage(MOVING_AVG_WINDOW_SIZE),
    		new MovingAverage(MOVING_AVG_WINDOW_SIZE) };
	private float[] smoothedAccelerationVector = new float[3];

    private static final int THRESHOLD_WINDOW_SIZE = 50;
    private Threshold[] thresholdCalculator = { new Threshold(THRESHOLD_WINDOW_SIZE), 
    		new Threshold(THRESHOLD_WINDOW_SIZE),
    		new Threshold(THRESHOLD_WINDOW_SIZE)
    };
	
	private StepCounter[] stepCounters = { new StepCounter(thresholdCalculator[X_AXIS]),
			new StepCounter(thresholdCalculator[Y_AXIS]),
			new StepCounter(thresholdCalculator[Z_AXIS])
	};

    private static final float ALPHA = 0.8f;
    private float[] gravity = new float[3];
	private float[] linearAccelerationVector = new float[3];
	private float[] lastStepCounters = new float[3];
	private int[] stepCnt = new int[3];
	
	public StepDetector() {
		lastStepCounters[X_AXIS] = 0;
		lastStepCounters[Y_AXIS] = 0;
		lastStepCounters[Z_AXIS] = 0;
		
		stepCnt[X_AXIS] = 0;
		stepCnt[Y_AXIS] = 0;
		stepCnt[Z_AXIS] = 0;
	}
	
	/**
	 * Calculates gravity-less values from acceleration samples by simple inverted Low Pass.
	 * @param accelearationVector - acc. sensor samples 
	 */
	private void calculateLinearAcceleration(float[] accelearationVector) {
		//Low Pass
		gravity[X_AXIS] = ALPHA * gravity[X_AXIS] + (1 - ALPHA) * accelearationVector[X_AXIS];
		gravity[Y_AXIS] = ALPHA * gravity[Y_AXIS] + (1 - ALPHA) * accelearationVector[Y_AXIS];
		gravity[Z_AXIS] = ALPHA * gravity[Z_AXIS] + (1 - ALPHA) * accelearationVector[Z_AXIS];
		//High Pass = Inverted Low Pass
		linearAccelerationVector[X_AXIS] = accelearationVector[X_AXIS] - gravity[X_AXIS];
		linearAccelerationVector[Y_AXIS] = accelearationVector[Y_AXIS] - gravity[Y_AXIS];
		linearAccelerationVector[Z_AXIS] = accelearationVector[Z_AXIS] - gravity[Z_AXIS];
	}

	/**
	 * Smoothes values by Moving Average Filter
	 */
	private void smoothLinearAcceleration() {
		movingAvgCalculators[X_AXIS].pushValue(linearAccelerationVector[X_AXIS]);
		movingAvgCalculators[Y_AXIS].pushValue(linearAccelerationVector[Y_AXIS]);
		movingAvgCalculators[Z_AXIS].pushValue(linearAccelerationVector[Z_AXIS]);
		
		smoothedAccelerationVector[X_AXIS] = movingAvgCalculators[X_AXIS].getValue();
		smoothedAccelerationVector[Y_AXIS] = movingAvgCalculators[Y_AXIS].getValue();
		smoothedAccelerationVector[Z_AXIS] = movingAvgCalculators[Z_AXIS].getValue();
	}

	
    //TODO remove
    public Threshold[] getThresholds() {
    	return thresholdCalculator;
    }
    

	public int[] countSteps(float[] accelerationSamples) {

		calculateLinearAcceleration(accelerationSamples);
        smoothLinearAcceleration();
		return updateStepCounters();

	}

	private int[] updateStepCounters() {
		for(int i = 0; i < 3; i++) {
			stepCnt[i] = stepCounters[i].update(smoothedAccelerationVector[i]);
		}
		return stepCnt;
	}

	//for testing
	public float[] getLinearAccelerationValues() {
		return linearAccelerationVector;
	}
	
	public float[] getSmoothedAccelerationValues() {
		return smoothedAccelerationVector;
	}
	
	public float[] getThresholdValues() {
		float[] thresholdValues = new float[3];
		for(int i = 0; i < 3; i++) {
			thresholdValues[i] = stepCounters[i].getThresholdValue();
		}
		return thresholdValues;
	}
}
