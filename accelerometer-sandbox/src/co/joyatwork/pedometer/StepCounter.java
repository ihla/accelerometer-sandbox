package co.joyatwork.pedometer;

public class StepCounter {
	
	public static final int X_AXIS = 0;
	public static final int Y_AXIS = 1;
	public static final int Z_AXIS = 2;

	private StepDetector[] stepDetectors = { new StepDetector(),
			new StepDetector(),
			new StepDetector()
	};

	private int[] crossingThresholdCnt = new int[3];
	private int[] stepCnt = new int[3];
	
	public StepCounter() {
		for (int i = 0; i < 3; i++) {
			crossingThresholdCnt[i] = 0;
			stepCnt[i] = 0;
		}
	}
	
	public int[] countSteps(float[] accelerationSamples, long sampleTimeInMilis) {

		return updateStepCounters(accelerationSamples, sampleTimeInMilis);

	}

	private int[] updateStepCounters(float[] accelerationSamples, long sampleTimeInMilis) {
		for(int i = 0; i < 3; i++) {
			stepDetectors[i].update(accelerationSamples[i], sampleTimeInMilis);
			crossingThresholdCnt[i] = stepDetectors[i].getCrossingThresholdCount();
		}
		return crossingThresholdCnt;
	}

	public int getStepCount(int axis) {
		return stepDetectors[axis].getStepCount();
	}

	//for testing
	public float[] getLinearAccelerationValues() {
		float[] linearAccelerationVector = new float[3];
		for (int axis = 0; axis < 3; axis++) {
			linearAccelerationVector[axis] = stepDetectors[axis].getLinearAcceleration();
		}
		return linearAccelerationVector;
	}
	
	public float[] getSmoothedAccelerationValues() {
		float[] smoothedAccelerationVector = new float[3];
		for (int axis = 0; axis < 3; axis++) {
			smoothedAccelerationVector[axis] = stepDetectors[axis].getSmoothedAcceleration();
		}
		return smoothedAccelerationVector;
	}
	
	public float[] getThresholdValues() {
		float[] thresholdValues = new float[3];
		for(int i = 0; i < 3; i++) {
			thresholdValues[i] = stepDetectors[i].getThresholdValue();
		}
		return thresholdValues;
	}
    
	public float getLinearAcceleration(int axis) {
		return stepDetectors[axis].getLinearAcceleration();
	}
	
	public float getSmoothedAcceleration(int axis) {
		return stepDetectors[axis].getSmoothedAcceleration();
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

	public boolean hasValidSteps(int axis) {
		return stepDetectors[axis].hasValidSteps();
	}
	
	public long getStepInterval(int axis) {
		return stepDetectors[axis].getStepInterval();
	}

	public long getAvgStepInterval(int axis) {
		return stepDetectors[axis].getAvgStepInterval();
	}

	public float getStepIntervalVariance(int axis) {
		return stepDetectors[axis].getStepIntervalVariance();
	}


}
