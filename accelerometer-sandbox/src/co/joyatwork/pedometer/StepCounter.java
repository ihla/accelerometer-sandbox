package co.joyatwork.pedometer;

import co.joyatwork.filters.MovingAverage;

public class StepCounter {
	
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
	
	private StepDetector[] stepCounters = { new StepDetector(thresholdCalculator[X_AXIS]),
			new StepDetector(thresholdCalculator[Y_AXIS]),
			new StepDetector(thresholdCalculator[Z_AXIS])
	};

    private static final float ALPHA = 0.8f;
    private float[] gravity = new float[3];
	private float[] linearAccelerationVector = new float[3];
	private float[] lastStepCounters = new float[3];
	private int[] stepCnt = new int[3];
	
	public StepCounter() {
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
