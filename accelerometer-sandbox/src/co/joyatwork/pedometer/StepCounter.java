package co.joyatwork.pedometer;

import co.joyatwork.filters.MovingAverage;

public class StepCounter {
	
	public static final int X_AXIS = 0;
	public static final int Y_AXIS = 1;
	public static final int Z_AXIS = 2;

	//TODO better to move to StepDetector
    private static final int MOVING_AVG_WINDOW_SIZE = 10;
    private MovingAverage[] movingAvgCalculators = { new MovingAverage(MOVING_AVG_WINDOW_SIZE),
    		new MovingAverage(MOVING_AVG_WINDOW_SIZE),
    		new MovingAverage(MOVING_AVG_WINDOW_SIZE) };
	private float[] smoothedAccelerationVector = new float[3];

	private StepDetector[] stepDetectors = { new StepDetector(),
			new StepDetector(),
			new StepDetector()
	};

    private static final float ALPHA = 0.8f;
    private float[] gravity = new float[3];
	private float[] linearAccelerationVector = new float[3];
	private float[] lastStepCounters = new float[3];
	private int[] stepCnt = new int[3];
	
	public StepCounter() {
		for (int i = 0; i < 3; i++) {
			lastStepCounters[i] = 0;
			stepCnt[i] = 0;
		}
	}
	
	public int[] countSteps(float[] accelerationSamples, long sampleTimeInMilis) {

		//TODO this should be moved to StepDetector >>
		calculateLinearAcceleration(accelerationSamples);
        smoothLinearAcceleration();
        //<<
		return updateStepCounters(sampleTimeInMilis);

	}

	/**
	 * Calculates gravity-less values from acceleration samples by simple inverted Low Pass.
	 * @param accelearationVector - acc. sensor samples 
	 */
	private void calculateLinearAcceleration(float[] accelearationVector) {
		for (int i = 0; i < 3; i++) {
			//Low Pass
			gravity[i] = ALPHA * gravity[i] + (1 - ALPHA) * accelearationVector[i];
			//High Pass = Inverted Low Pass
			linearAccelerationVector[i] = accelearationVector[i] - gravity[i];
		}
	}

	/**
	 * Smoothes values by Moving Average Filter
	 */
	private void smoothLinearAcceleration() {
		for (int i = 0; i < 3; i++) {
			movingAvgCalculators[i].pushValue(linearAccelerationVector[i]);
			smoothedAccelerationVector[i] = movingAvgCalculators[i].getValue();
		}
	}

	
	private int[] updateStepCounters(long sampleTimeInMilis) {
		for(int i = 0; i < 3; i++) {
			stepCnt[i] = stepDetectors[i].update(smoothedAccelerationVector[i], sampleTimeInMilis);
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
			thresholdValues[i] = stepDetectors[i].getThresholdValue();
		}
		return thresholdValues;
	}
    
	public float getFixedPeak2PeakValue(int axis) {
		return stepDetectors[axis].getFixedPeak2PeakValue();
	}
	
	public float getCurrentPeak2PeakValue(int axis) {
		return stepDetectors[axis].getCurrentPeak2PeakValue();
	}

	public float getFixedMinValue(int axis) {
		return stepDetectors[axis].getFixedMinValue();
	}

	public float getFixedMaxValue(int axis) {
		return stepDetectors[axis].getFixedMaxValue();
	}

}
