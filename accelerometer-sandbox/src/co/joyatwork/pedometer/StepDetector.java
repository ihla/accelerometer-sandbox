package co.joyatwork.pedometer;

import co.joyatwork.filters.MovingAverage;

public class StepDetector {

	//TODO Threshold is probably not good name since it evaluates many characteristics of periodic curve!
	public class Threshold {
    	
    	private final static String TAG = "Threshold";
    	private final int windowSize;

    	// these vars hold min/max values during measurement, 
		// at the end of measuring window they are reset to sample value
    	private float measuredMinValue;
		private float measuredMaxValue;

		// these are auxiliary variables to provide correct min/max at the end of measuring window
		// when the measured values are reset
		private float currentMinValue;
		private float currentMaxValue;
		
		private int sampleCount; // counts samples to control measuring window for threshold
		private float minValue;  // min value at the end of window
		private float maxValue;  // max value at the end of window
		
		private boolean isFirstSample; // controls initialization of the threshold measurement
		private float firstSample;     // used to ignore min/max values if it was the value of 1st sample
		private boolean isFirstWindow; // controls re-initialization after the 1st window 
		

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
		
		//TODO refactor this mess!
		public void pushSample(float newSample) {
			
			if (isFirstSample) {
				isFirstSample = false;
				initializeMeasurement(newSample);
				return;
			}
			if (newSample < measuredMinValue) {
				measuredMinValue = newSample;
			}
			else if (newSample > measuredMaxValue) {
				measuredMaxValue = newSample;
			}
			if (newSample < currentMinValue) {
				currentMinValue = newSample;
			}
			else if (newSample > currentMaxValue) {
				currentMaxValue = newSample;
			}
			sampleCount++;
			if (sampleCount == windowSize) {
				sampleCount = 0;
				//TODO how to decouple it logically from the DC filter algorithm?  
				// this is necessary due to use of high pass filter to eliminate DC offset from samples
				// the 1st window yields incorrect min/max values because the output of DC filter is not steady 
				if (isFirstWindow) {
					isFirstWindow = false;
					if (isFirstSampleEqualToMinOrMaxValue()) {
						// if the 1st sample value is still the highest/lowest value at the end of window,
						// keep original min/max values and start measurement with the current sample value
						setMinMaxValues(newSample);
						return;
					}
				}
				// store measured values, they are valid for the duration of next window
				minValue = measuredMinValue;
				maxValue = measuredMaxValue;
				// reset values for the comparison  in the next window
				measuredMinValue = measuredMaxValue = newSample;
			}
		}

		private void initializeMeasurement(float newSample) {
			firstSample = newSample;
			setMinMaxValues(firstSample);
		}
		
		private void setMinMaxValues(float value) {
			measuredMinValue = measuredMaxValue = value;
			currentMaxValue = measuredMaxValue;
			currentMinValue = measuredMinValue;
		}

		private boolean isFirstSampleEqualToMinOrMaxValue() {
			return (firstSample == measuredMinValue) || (firstSample == measuredMaxValue);
		}

		/**
		 * Returns actual min value measured in the running window
		 */
		public float getCurrentMinValue() {
			return currentMinValue;
		}
		
		/**
		 * Returns actual max value measured in the running window
		 */
		public float getCurrentMaxValue() {
			return currentMaxValue;
		}
		
		/**
		 * Returns fixed min value as measured in the previous window
		 */
		public float getFixedMinValue(){
			return minValue;
		}
		
		/**
		 * Returns fixed max value as measured in the previous window
		 */
		public float getFixedMaxValue() {
			return maxValue;
		}
		
		public float getThresholdValue() {
			return (minValue + maxValue)/2;
		}
		
		public float getCurrentPeak2PeakValue() {
			return (currentMaxValue - currentMinValue);
		}

		public float getFixedPeak2PeakValue() {
			return (maxValue - minValue);
		}

		public void setCurrentMinMax(float value) {
			currentMaxValue = currentMinValue = value;
		}

	}

	private class StepCounter {
		
		private Threshold threshold;
		private int stepCount;
		private float lastSample;
		private float thresholdValue;
		private boolean firstStep;
		private long previousStepTime;
		
		public StepCounter(Threshold t) {
			threshold = t;
			stepCount = 0; 
			lastSample = 0;
			thresholdValue = 0;
			firstStep = true;
		}
	
		public int update(float newSample, long sampleTimeInMilis) {
			calculateThreshold(newSample);
			//TODO threshold.getCurrentMaxValue() > 0.3F - how to ignore low values?
			if (threshold.getCurrentMaxValue() > 0.3F 
			    && lastSample > thresholdValue 
			    && newSample < thresholdValue) {
				//start to measure new current min/max values when threshold is crossed
				threshold.setCurrentMinMax(thresholdValue);
				//TODO ???
				if (isValidStepInterval(sampleTimeInMilis)) {
					stepCount++;
				}
			}
			lastSample = newSample;
			return stepCount;
		}

	    /**
	     * Calculates thresholds for step detections
	     */
		private void calculateThreshold(float newSample) {
			threshold.pushSample(newSample);
			thresholdValue = threshold.getThresholdValue();
		}

		private boolean isValidStepInterval(long stepTimeInMilis) {
			return true; //TODO temp
			/*
			if (firstStep) {
				previousStepTime = stepTimeInMilis;
				firstStep = false;
				return true;
			}
			else {
				long stepInterval = stepTimeInMilis - previousStepTime;
				previousStepTime = stepTimeInMilis;
				if (stepInterval >= 200 && stepInterval <= 2000) { //<200ms, 2000ms>
					return true;
				}
			}
			return false;
			*/
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
    

	public int[] countSteps(float[] accelerationSamples, long sampleTimeInMilis) {

		calculateLinearAcceleration(accelerationSamples);
        smoothLinearAcceleration();
		return updateStepCounters(sampleTimeInMilis);

	}

	private int[] updateStepCounters(long sampleTimeInMilis) {
		for(int i = 0; i < 3; i++) {
			stepCnt[i] = stepCounters[i].update(smoothedAccelerationVector[i], sampleTimeInMilis);
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
