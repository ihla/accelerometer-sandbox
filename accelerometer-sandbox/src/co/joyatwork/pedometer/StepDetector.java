package co.joyatwork.pedometer;

class StepDetector {
	
	interface StepDetectingStrategy {
		public void update(float sampleValue, long sampleTimeInMilis);
	}
	
	class SearchingDetector implements StepDetectingStrategy {

		private StepDetector detector;

		@Override
		public void update(float sampleValue, long sampleTimeInMilis) {
		
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
	
	public StepDetector(Threshold t) {
		threshold = t;
		stepCount = 0; 
		lastSample = 0;
		thresholdValue = 0;
		firstStep = true;
		
		searchingDetector = new SearchingDetector(this);
		countingDetector = new CountingDetector(this);
		detectingStrategy = searchingDetector;
	}

	public StepDetector() {
		this(new Threshold(THRESHOLD_WINDOW_SIZE));
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
		//TODO how to ignore low values?
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
