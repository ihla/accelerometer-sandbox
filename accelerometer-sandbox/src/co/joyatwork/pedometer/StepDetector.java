package co.joyatwork.pedometer;

class StepDetector {
	
	private Threshold threshold;
	private int stepCount;
	private float lastSample;
	private float thresholdValue;
	private boolean firstStep;
	private long previousStepTime;
	
	public StepDetector(Threshold t) {
		threshold = t;
		stepCount = 0; 
		lastSample = 0;
		thresholdValue = 0;
		firstStep = true;
	}

	public int update(float newSample, long sampleTimeInMilis) {
		calculateThreshold(newSample);
		
		if (hasValidPeak() && isCrossingBelowThreshold(newSample)) {
			restartPeakMeasurement();
			//TODO ???
			if (isValidStepInterval(sampleTimeInMilis)) {
				stepCount++;
			}
		}
		lastSample = newSample;
		return stepCount;
	}

	private void restartPeakMeasurement() {
		threshold.setCurrentMinMax(thresholdValue);
	}

	private boolean isCrossingBelowThreshold(float newSample) {
		return (lastSample > thresholdValue) && (newSample < thresholdValue);
	}

	private boolean hasValidPeak() {
		//TODO threshold.getCurrentMaxValue() > 0.3F - how to ignore low values?
		return threshold.getCurrentMaxValue() > 0.3F;
	}
	
	public void update_(float sample, long sampleTimeInMilis) {
		calculateThreshold(sample);
		if (hasValidPeak() && isCrossingBelowThreshold(sample)) {
			restartPeakMeasurement();
			//TODO
		}
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
