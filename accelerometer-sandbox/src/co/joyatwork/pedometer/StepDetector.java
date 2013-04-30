package co.joyatwork.pedometer;

class StepDetector {
	
	interface StepDetectingStrategy {
		public void update(float sampleValue, long sampleTimeInMilis);
	}
	
	class SearchingDetector implements StepDetectingStrategy {
		/**
		 * Step count is valid if 4 intervals between 5 consecutive steps
		 * are all in the range, and all interval variances are in range.
		 */
		private static final int VALID_STEPS_COUNT = 5;
		private int validStepsCount = 1; //anticipate the 1st step is ok, next steps will be validated
		private long avgStepIntervalSum = 0; 

		@Override
		public void update(float sampleValue /* not used */, long sampleTimeInMilis) {
			
			//TODO refactor this mess!
			calculateStepInterval(sampleTimeInMilis);
			if (isStepIntervalInRange()) {
				
				avgStepIntervalSum += stepInterval;
				
				if (validStepsCount < 2) { // 2 intervals for variance calculation not measured yet
					previousStepInterval = stepInterval;
					validStepsCount++; // add step, next steps will be validated at the next run
					setHasValidSteps(false);
					return;
				}
				
				calculateStepIntervalVarianceFor(previousStepInterval);
				if (isStepIntervalVarianceInRange() == false) {
					/*  if any one out of the step no. 3,4,5 has variance out of range,
					 *  the step counting is restarted!
					 */
					avgStepIntervalSum = 0;
					stepIntervalVariance = 0;
					validStepsCount = 1; // reset and quit
					setHasValidSteps(false);
					return;
				}

				validStepsCount++;
				if (validStepsCount >= VALID_STEPS_COUNT) {
					avgStepInterval = avgStepIntervalSum / (validStepsCount - 1);
					setHasValidSteps(true);
					detectingStrategy = countingDetector; // steps validated, switch to counting
				}
			}
			else { // step interval out of range
				avgStepIntervalSum = 0;
				validStepsCount = 1; //reset to 1 - the 1st step is added
				previousStepInterval = stepInterval;
				setHasValidSteps(false);
			}
		
		}

		public void reset(long initPreviousStepTime, long initPreviousStepInterval) {
			previousStepTime = initPreviousStepTime;
			previousStepInterval = initPreviousStepInterval;
			validStepsCount = 1;
			avgStepIntervalSum = 0;
			avgStepInterval = 0;
			stepIntervalVariance = 0;
			setHasValidSteps(false);
		}
	
	}
	
	class CountingDetector implements StepDetectingStrategy {

		@Override
		public void update(float sampleValue, long sampleTimeInMilis) {
			
			long tmpPreviousStepTime = previousStepTime;
			long tmpPreviousStepInterval = previousStepInterval;
			
			calculateStepInterval(sampleTimeInMilis);
			if (isStepIntervalInRange()) {
				
				calculateStepIntervalVarianceFor(avgStepInterval);
				if (isStepIntervalVarianceInRange()) {
					//TODO update counter
					
				}
				else { // step interval variance out of range - switch to searching
					detectingStrategy = searchingDetector;
					searchingDetector.reset(tmpPreviousStepTime, tmpPreviousStepInterval);
					searchingDetector.update(sampleValue, sampleTimeInMilis);
				}
			}
			else { // step interval out of range - switch to searching
				detectingStrategy = searchingDetector;
				searchingDetector.reset(tmpPreviousStepTime, tmpPreviousStepInterval);
				searchingDetector.update(sampleValue, sampleTimeInMilis);
			}
				
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
	private boolean hasValidSteps;
	private long stepInterval;
	private long avgStepInterval;
	private long previousStepInterval;
	private float stepIntervalVariance;
	
	public StepDetector(Threshold t) {
		threshold = t;
		lastSample = 0;
		thresholdValue = 0;
		stepCount = 0; 
		stepInterval = 0;
		avgStepInterval = 0;
		previousStepTime = 0;
		previousStepInterval = 0;
		stepIntervalVariance = 0;
		hasValidSteps = false;
		
		
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
		calculateStepIntervalVarianceFor(previousStepInterval);
		previousStepInterval = stepInterval;
		
		return false;
	}

	private void calculateStepIntervalVarianceFor(long referenceValue) {
		if (stepInterval != 0) {
			//TODO performance optimization: use multiplication instead of division???
			stepIntervalVariance = referenceValue / ((float)stepInterval);
		}
		else {
			stepIntervalVariance = 0;
		}
		previousStepInterval = stepInterval;
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
