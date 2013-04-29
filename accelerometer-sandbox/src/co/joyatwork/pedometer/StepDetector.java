package co.joyatwork.pedometer;

class StepDetector {
	
	interface StepDetectingStrategy {
		public void update(float sampleValue, long sampleTimeInMilis);
	}
	
	class SearchingDetector implements StepDetectingStrategy {
		/**
		 * Step count is valid if intervals between 4 consecutive steps
		 * are all in the range.
		 */
		private static final int VALID_STEPS_COUNT = 4;
		private int validStepsCount = 0;
		private long avgStepIntervalSum = 0; 

		@Override
		public void update(float sampleValue, long sampleTimeInMilis) {
			
			boolean isFirstStep = check1stStepCalculateStepInterval(sampleTimeInMilis);
			// refactor this mess!
			if (isFirstStep || isValidStepInterval()) {
				validStepsCount++;
				calculateAvgStepInterval(isFirstStep);
				if (validStepsCount >= VALID_STEPS_COUNT) {
					setHasValidSteps(true);
					validStepsCount = 0; // will be updated after switched back from countingDetector
					detectingStrategy = countingDetector;
				}
			}
			else {
				validStepsCount = 0;
				setHasValidSteps(false);
			}
		
		}
	
		private void calculateAvgStepInterval(boolean isFirstStep) {
			if (isFirstStep) {
				avgStepInterval = 0;
				avgStepIntervalSum = 0;
				return;
			}
			avgStepIntervalSum += stepInterval;
			avgStepInterval = avgStepIntervalSum / validStepsCount;
		}

	}
	
	class CountingDetector implements StepDetectingStrategy {

		@Override
		public void update(float sampleValue, long sampleTimeInMilis) {
			calculateStepInterval(sampleTimeInMilis);
			calculateStepIntervalVariance(avgStepInterval);
			if (isValidStepInterval()) {
				//TODO update counter
			}
			else {
				// Rhythmic pattern lost - switch back to searching
				setHasValidSteps(false);
				setFirstStep(true);
				detectingStrategy = searchingDetector;
				detectingStrategy.update(sampleValue, sampleTimeInMilis); //update to not lose any step
			}
				
		}

		private void setFirstStep(boolean val) {
			firstStep = val;
		}
		
	}

	private static final int MIN_STEP_INTERVAL = 200;  //ms, people can walk/run as fast as 5 steps/sec
	private static final int MAX_STEP_INTERVAL = 2000; //ms, people can walk/run as slow as 1 step/2sec
	private static final float MIN_STEP_INTERVAL_VARIANCE = 0.7F; //-30%
	private static final float MAX_STEP_INERVAL_VARIANCE = 1.3F;//+30%
    private static final int THRESHOLD_WINDOW_SIZE = 50;
	private Threshold threshold;
	private StepDetectingStrategy detectingStrategy;
	private SearchingDetector searchingDetector;
	private CountingDetector countingDetector;
	
	private int stepCount;
	private float lastSample;
	private float thresholdValue;
	private boolean firstStep;
	private long previousStepTime;
	private boolean hasValidSteps = false;
	private long stepInterval;
	private long avgStepInterval;
	private long previousStepInterval;
	private float stepIntervalVariance;
	
	public StepDetector(Threshold t) {
		threshold = t;
		stepCount = 0; 
		lastSample = 0;
		thresholdValue = 0;
		stepInterval = 0;
		firstStep = true;
		
		searchingDetector = new SearchingDetector();
		countingDetector = new CountingDetector();
		detectingStrategy = searchingDetector;
	}

	public StepDetector() {
		this(new Threshold(THRESHOLD_WINDOW_SIZE));
	}

	public void update(float newSample, long sampleTimeInMilis) {
		calculateThreshold(newSample);
		
		if (hasValidPeak() && isCrossingBelowThreshold(newSample)) {
			//TODO ???
			stepCount++;
			//detectingStrategy sets back the hasValidSteps flag!
			detectingStrategy.update(newSample, sampleTimeInMilis);
			restartPeakMeasurement();
		}
		lastSample = newSample;
	}

	private void restartPeakMeasurement() {
		threshold.setCurrentMinMax(thresholdValue);
	}

	private boolean isCrossingBelowThreshold(float newSample) {
		return (lastSample > thresholdValue) && (newSample < thresholdValue);
	}

	private boolean hasValidPeak() {
		//TODO how to ignore low values?
		return threshold.getCurrentMaxValue() > 0.3F;
	}

    /**
     * Calculates thresholds for step detections
     */
	private void calculateThreshold(float newSample) {
		threshold.pushSample(newSample);
		thresholdValue = threshold.getThresholdValue();
	}

	/**
	 * Checks the step period as one of the characteristics of step rhythmic pattern
	 * @param sampleTimeInMilis
	 * @return - true if the 1st step is detected
	 */
	private boolean check1stStepCalculateStepInterval(long sampleTimeInMilis) {
		if (firstStep) {
			previousStepTime = sampleTimeInMilis;
			stepInterval = 0;
			previousStepInterval = 0;
			stepIntervalVariance = 0;
			firstStep = false;
			return true; //consider the 1st step is valid
		}
		
		calculateStepInterval(sampleTimeInMilis);
		calculateStepIntervalVariance(previousStepInterval);
		previousStepInterval = stepInterval;
		
		return false;
	}

	private void calculateStepIntervalVariance(long referenceValue) {
		if (stepInterval != 0) {
			//TODO performance optimization: use multiplication instead of division???
			stepIntervalVariance = referenceValue / ((float)stepInterval);
		}
		else {
			stepIntervalVariance = 0;
		}
	}

	private void calculateStepInterval(long sampleTimeInMilis) {
		stepInterval = sampleTimeInMilis - previousStepTime;
		previousStepTime = sampleTimeInMilis;
	}


	private boolean isValidStepInterval() {
		if (isStepIntervalInRange()	&& isStepIntervalVarianceInRange()) {
			return true;
		}
		return false;
	}

	private boolean isStepIntervalVarianceInRange() {
		return stepIntervalVariance >= MIN_STEP_INTERVAL_VARIANCE && stepIntervalVariance <= MAX_STEP_INERVAL_VARIANCE;
	}

	private boolean isStepIntervalInRange() {
		return stepInterval >= MIN_STEP_INTERVAL && stepInterval <= MAX_STEP_INTERVAL;
	}

	private void setHasValidSteps(boolean value) {
		hasValidSteps = value;
	}

	public boolean hasValidSteps() {
		return hasValidSteps;
	}

	public int getStepCount() {
		return stepCount;
	}

	public long getStepInterval() {
		return stepInterval;
	}

	public long getAvgStepInterval() {
		return avgStepInterval;
	}

	public float getStepIntervalVariance() {
		return stepIntervalVariance;
	}
	
	public float getThresholdValue() {
		return thresholdValue;
	}
	
	public float getFixedPeak2PeakValue() {
		return threshold.getFixedPeak2PeakValue();
	}
	
	public float getCurrentPeak2PeakValue() {
		return threshold.getCurrentPeak2PeakValue();
	}

	public float getFixedMinValue() {
		return threshold.getFixedMinValue();
	}

	public float getFixedMaxValue() {
		return threshold.getFixedMaxValue();
	}

}
