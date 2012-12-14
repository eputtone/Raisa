package raisa.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import raisa.domain.AlgorithmTypeEnum;
import raisa.ui.controls.ControlTypeEnum;
import raisa.ui.measurements.MeasurementTypeEnum;

public class VisualizerConfig {
		
	private List<VisualizerConfigListener> visualizerConfigListeners = new ArrayList<VisualizerConfigListener>();
	private static VisualizerConfig instance;
	private List<VisualizerConfigItemEnum> changedConfigs = new ArrayList<VisualizerConfigItemEnum>();
	
	// IO-source and state for activated algorithms
	private InputOutputTargetEnum inputOutputTarget = InputOutputTargetEnum.FILE_SIMULATION;
	private LocalizationModeEnum localizationMode = LocalizationModeEnum.NONE;	
	
	private Set<AlgorithmTypeEnum> activatedAlgorithms = new HashSet<AlgorithmTypeEnum>();

	// map area visualization options
	private boolean displayMap = true;
	private boolean displayTrail = true;
	private boolean displayRobot = true;
	private boolean displayParticles = true;
	private boolean displaySonarScan = true;
	private boolean displayIrScan = true;
	private int displayMinAgeForParticles = 0;
	
	// measurements panel visualization options
	private Set<MeasurementTypeEnum> displayedMeasurements = new HashSet<MeasurementTypeEnum>();
	
	// displayed controls subpanels
	private Set<ControlTypeEnum> displayedControls = new HashSet<ControlTypeEnum>();
	
	private VisualizerConfig() {
		// TODO: default initialization can be moved to an init file some day
		this.displayedMeasurements.add(MeasurementTypeEnum.HEADING);
		this.displayedMeasurements.add(MeasurementTypeEnum.SPEED);
		this.displayedMeasurements.add(MeasurementTypeEnum.ODOMETER);
		this.displayedMeasurements.add(MeasurementTypeEnum.SAMPLE_COUNTER);
		this.displayedMeasurements.add(MeasurementTypeEnum.DISTANCE_SENSOR_STATUS);
		
		this.displayedControls.add(ControlTypeEnum.DRAWING_TOOL);
		this.displayedControls.add(ControlTypeEnum.MOVEMENT);
		this.displayedControls.add(ControlTypeEnum.OTHER);
		this.displayedControls.add(ControlTypeEnum.ALGORITHM_SELECTION);

		this.activatedAlgorithms.add(AlgorithmTypeEnum.RANSAC_LANDMARK_EXTRACTION);
		this.activatedAlgorithms.add(AlgorithmTypeEnum.SPIKES_LANDMARK_EXTRACTION);
	}
	
	public static VisualizerConfig getInstance() {
		if (instance == null) {
			instance = new VisualizerConfig();
		}
		return instance;
	}
	
	public InputOutputTargetEnum getInputOutputTarget() {
		return this.inputOutputTarget;
	}

	public void setInputOutputTarget(InputOutputTargetEnum inputOutputTarget) {
		if (this.inputOutputTarget != inputOutputTarget) {
			this.inputOutputTarget = inputOutputTarget;
			this.changedConfigs.add(VisualizerConfigItemEnum.INPUT_OUTPUT_TARGET);
		}
	}	
	
	public void setLocalizationMode(LocalizationModeEnum localizationMode) {
		if (this.localizationMode != localizationMode) {
			this.localizationMode = localizationMode;
			this.changedConfigs.add(VisualizerConfigItemEnum.LOCALIZATION_MODE);
		}
	}

	public LocalizationModeEnum getLocalizationMode() {
		return localizationMode;
	}	
	
	public boolean isChanged(VisualizerConfigItemEnum ... configItems) {
		for (VisualizerConfigItemEnum configItem : configItems) {
			if (changedConfigs.contains(configItem)) {
				return true;
			}
		}
		return changedConfigs.contains(VisualizerConfigItemEnum.ALL_CONFIG_ITEMS);
	}
	
	public void addVisualizerConfigListener(VisualizerConfigListener listener) {
		visualizerConfigListeners.add(listener);
	}
	
	public void setChanged(VisualizerConfigItemEnum configItem) {
		changedConfigs.add(configItem);
	}
		
	public void notifyVisualizerConfigListeners() {
		for (VisualizerConfigListener listener : visualizerConfigListeners) {
			listener.visualizerConfigChanged(this);
		}
		this.changedConfigs.clear();
	}

	public boolean isDisplayMap() {
		return displayMap;
	}

	public void setDisplayMap(boolean displayMap) {
		if(this.displayMap != displayMap) {
			this.displayMap = displayMap;
			this.changedConfigs.add(VisualizerConfigItemEnum.DISPLAY_MAP);
		}
	}

	public boolean isDisplayTrail() {
		return displayTrail;
	}

	public void setDisplayTrail(boolean displayTrail) {
		if(this.displayTrail != displayTrail) {
			this.displayTrail = displayTrail;
			this.changedConfigs.add(VisualizerConfigItemEnum.DISPLAY_TRAIL);
		}
	}

	public boolean isDisplayRobot() {
		return displayRobot;
	}

	public void setDisplayRobot(boolean displayRobot) {
		if(this.displayRobot != displayRobot) {
			this.displayRobot = displayRobot;
			this.changedConfigs.add(VisualizerConfigItemEnum.DISPLAY_ROBOT);
		}
	}

	public boolean isDisplayParticles() {
		return displayParticles;
	}

	public void setDisplayParticles(boolean displayParticles) {
		if(this.displayParticles != displayParticles) {
			this.displayParticles = displayParticles;
			this.changedConfigs.add(VisualizerConfigItemEnum.DISPLAY_PARTICLES);
		}
	}

	public boolean isDisplaySonarScan() {
		return displaySonarScan;
	}

	public void setDisplaySonarScan(boolean displaySonarScan) {
		if(this.displaySonarScan != displaySonarScan) {
			this.displaySonarScan = displaySonarScan;
			this.changedConfigs.add(VisualizerConfigItemEnum.DISPLAY_SONAR_SCAN);
		}
	}

	public boolean isDisplayIrScan() {
		return displayIrScan;
	}

	public void setDisplayIrScan(boolean displayIrScan) {
		if(this.displayIrScan != displayIrScan) {
			this.displayIrScan = displayIrScan;
			this.changedConfigs.add(VisualizerConfigItemEnum.DISPLAY_IR_SCAN);
		}
	}

	public int getDisplayMinAgeForParticles() {
		return displayMinAgeForParticles;
	}

	public void setDisplayMinAgeForParticles(int displayMinAgeForParticles) {
		this.displayMinAgeForParticles = displayMinAgeForParticles;
	}
	
	public void addDisplayedMeasurement(MeasurementTypeEnum measurementType) {
		this.displayedMeasurements.add(measurementType);
		this.setChanged(VisualizerConfigItemEnum.DISPLAYED_MEASUREMENTS);
	}
	
	public void removeDisplayedMeasurement(MeasurementTypeEnum measurementType) {
		this.displayedMeasurements.remove(measurementType);
		this.setChanged(VisualizerConfigItemEnum.DISPLAYED_MEASUREMENTS);		
	}

	public Set<MeasurementTypeEnum> getDisplayedMeasurements() {
		return this.displayedMeasurements;
	}
	
	public void addDisplayedControl(ControlTypeEnum controlType) {
		this.displayedControls.add(controlType);
		this.setChanged(VisualizerConfigItemEnum.DISPLAYED_CONTROLS);
	}
	
	public void removeDisplayedControl(ControlTypeEnum controlType) {
		this.displayedControls.remove(controlType);
		this.setChanged(VisualizerConfigItemEnum.DISPLAYED_CONTROLS);		
	}

	public Set<ControlTypeEnum> getDisplayedControls() {
		return this.displayedControls;
	}	
	
	public void addActivatedAlgorithm(AlgorithmTypeEnum algoritmType) {
		this.activatedAlgorithms.add(algoritmType);
		this.setChanged(VisualizerConfigItemEnum.ACTIVATED_ALGORITHMS);
	}
	
	public void removeActivatedAlgorithm(AlgorithmTypeEnum algoritmType) {
		this.activatedAlgorithms.remove(algoritmType);
		this.setChanged(VisualizerConfigItemEnum.ACTIVATED_ALGORITHMS);		
	}

	public Set<AlgorithmTypeEnum> getActivatedAlgorithms() {
		return this.activatedAlgorithms;
	}	
		
}
