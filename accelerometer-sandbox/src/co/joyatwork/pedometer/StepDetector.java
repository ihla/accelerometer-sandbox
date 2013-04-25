package co.joyatwork.pedometer;

class StepDetector {
	
	interface StepDetectingStrategy {
		public void update(float sampleValue, long sampleTimeInMilis);
	}
	
	class SearchingDetector implements StepDetectingStrategy {

		private static final int VALID_STEPS_COUNT = 3;
		private StepDetector detector;
		private int validStepsCount = 0;

		@Override
		public void update(float sampleValue, long sampleTimeInMilis) {
			
			if (detector.isValidStepInterval(sampleTimeInMilis)) {
				validStepsCount++;
				if (validStepsCount >= VALID_STEPS_COUNT) {
					detector.setHasValidSteps(true);
				}
			}
			else {
				validStepsCount = 0;
				detector.setHasValidSteps(false);
			}
		
		}
		
		public SearchingDetector(StepDetector detector) {
			this.detector = detector;
		}
	
	}
	
	class CountingDetector implements StepDetectingStrategy {

		private StepDetector detector;

		@Override
		public void update(float sampleValue, long sampleTimeInMilis) {
			
		}
		
		public CountingDetector(StepDetector detector) {
			this.detector = detector;
		}
		
	}
	
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
	
	public StepDetector(Threshold t) {
		threshold = t;
		stepCount = 0; 
		lastSample = 0;
		thresholdValue = 0;
		stepInterval = 0;
		firstStep = true;
		
		searchingDetector = new SearchingDetector(this);
		countingDetector = new CountingDetector(this);
		detectingStrategy = searchingDetector;
	}

	public StepDetector() {
		this(new Threshold(THRESHOLD_WINDOW_SIZE));
	}

	public void update(float newSample, long sampleTimeInMilis) {
		calculateThreshold(newSample);
		
		if (hasValidPeak() && isCrossingBelowThreshold(newSample)) {
			restartPeakMeasurement();
			//TODO ???
			stepCount++;
			//detectingStrategy sets back the hasValidSteps flag!
			detectingStrategy.update(newSample, sampleTimeInMilis);
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

	private boolean isValidStepInterval(long stepTimeInMilis) {
		if (firstStep) {
			previousStepTime = stepTimeInMilis;
			firstStep = false;
			return true; //consider the 1st step is valid
		}
		else {
			stepInterval = stepTimeInMilis - previousStepTime;
			previousStepTime = stepTimeInMilis;
			if (stepInterval >= 200 && stepInterval <= 2000) { //<200ms, 2000ms>
				return true;
			}
		}
		return false;
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
