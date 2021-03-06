package raisa.domain;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import raisa.comms.SampleParser;
import raisa.comms.SensorListener;
import raisa.domain.landmarks.Landmark;
import raisa.domain.landmarks.LandmarkManager;
import raisa.domain.plan.MotionPlan;
import raisa.domain.robot.Robot;
import raisa.domain.robot.RobotStateListener;
import raisa.domain.samples.AveragingSampleFixer;
import raisa.domain.samples.Sample;
import raisa.domain.samples.SampleFixer;
import raisa.domain.samples.SampleListener;
import raisa.util.CollectionUtil;
import raisa.util.Vector2D;


public class WorldModel implements SensorListener {
	private static final Logger log = LoggerFactory.getLogger(WorldModel.class);
	private List<Sample> samples = new ArrayList<Sample>();
	private final List<SampleFixer> sampleFixers = new ArrayList<SampleFixer>();
	private final List<SampleListener> sampleListeners = new ArrayList<SampleListener>();

	private Grid grid = new Grid();
	private final LandmarkManager landmarkManager = new LandmarkManager();
	private String latestMapFilename;

	private List<Robot> states = new ArrayList<Robot>();
	private final List<RobotStateListener> stateListeners = new ArrayList<RobotStateListener>();

	private final MotionPlan motionPlan = new MotionPlan();

	public WorldModel() {
		addState(new Robot());
		sampleFixers.add(new AveragingSampleFixer(5, 40.0f));
	}

	public List<Sample> getSamples() {
		return samples;
	}

	public List<Robot> getStates() {
		List<Robot> copy = new ArrayList<Robot>();
		synchronized(states) {
			copy.addAll(states);
		}
		return copy;
	}

	public MotionPlan getMotionPlan() {
		return this.motionPlan;
	}

	public List<Landmark> getLandmarks() {
		return this.landmarkManager.getLandmarks();
	}

	public LandmarkManager getLandmarkManager() {
		return this.landmarkManager;
	}

	@Override
	public synchronized void sampleReceived(String message) {
		Sample sample = new SampleParser().parse(message);
		for (SampleFixer fixer : sampleFixers) {
		 sample = fixer.fix(sample);
		}
		addSample(sample);
	}

	public void addSample(Sample sample) {
		samples.add(sample);
		notifySampleListeners(sample);
	}

	public void addRobotStateListener(RobotStateListener listener) {
		stateListeners.add(listener);
	}

	public void addState(Robot state) {
		synchronized(states) {
			states.add(state);
			for (RobotStateListener listener : stateListeners) {
				listener.robotStateChanged(state);
			}
		}
	}

	public Robot getLatestState() {
		synchronized (states) {
			if (states.size() == 0) {
				// when there are no states, the callers of this method usually fail to handle nulls properly
				// Executing Reset from menu resets state count to zero
				// so returning a null object
				return new Robot();
			}
			return states.get(states.size() - 1);
		}
	}

	public Sample getLatestSample() {
		if (samples.size() == 0) {
			return null;
		}
		return samples.get(samples.size() - 1);
	}

	public void reset() {
		samples = new ArrayList<Sample>();
		states = new ArrayList<Robot>();
		for (SampleFixer fixer : sampleFixers) {
			fixer.reset();
		}
		grid = new Grid();
		if(latestMapFilename != null) {
			loadMap(latestMapFilename);
		}
		addState(new Robot());
		landmarkManager.reset();
	}

	public void removeOldSamples(int preserveLength) {
		samples = CollectionUtil.takeLast(samples, preserveLength);
	}

	public void clearSamples() {
		samples = new ArrayList<Sample>();
	}

	public List<Sample> getLastSamples(int numberOfSamples) {
		return CollectionUtil.takeLast(samples, numberOfSamples);
	}

	public void setGridPosition(Vector2D position, boolean isBlocked) {
		grid.setGridPosition(position, isBlocked);
	}

	public void setUserPosition(Vector2D position, boolean isBlocked) {
		grid.setUserPosition(position, isBlocked);
	}

	public void pushUserEditUndoLevel() {
		grid.pushUserUndoLevel();
	}

	public void popUserEditUndoLevel() {
		grid.popUserUndoLevel();
	}

	public void redoUserEditUndoLevel() {
		grid.redoUserUndoLevel();
	}

	public boolean isUserEditUndoable() {
		return grid.isUserEditUndoable();
	}

	public boolean isUserEditRedoable() {
		return grid.isUserEditRedoable();
	}

	public int getUserUndoLevels() {
		return grid.getUserUndoLevels();
	}

	public int getUserRedoLevels() {
		return grid.getUserRedoLevels();
	}

	public void saveMap(String fileName) {
		try {
			ImageIO.write(grid.getUserImage(), "PNG", new File(fileName));
		} catch (IOException e) {
			log.error("Failed to save map", e);
		}
	}

	public void loadMap(String fileName) {
		try {
			BufferedImage mapImage = ImageIO.read(new File(fileName));
			grid.pushUserUndoLevel();
			grid.setUserImage(mapImage);
			latestMapFilename = fileName;
		} catch (IOException e) {
			log.error("Failed to load map", e);
		}
	}

	public void resetMap() {
		grid.resetUserImage();
	}

	public BufferedImage getUserImage() {
		return grid.getUserImage();
	}

	public Image getBlockedImage() {
		return grid.getBlockedImage();
	}

	public float traceRay(Vector2D from, float angle) {
		return grid.traceRay(from, angle);
	}

	public float getWidth() {
		return grid.getWidth();
	}

	public float getHeight() {
		return grid.getHeight();
	}

	public void addSampleListener(SampleListener listener) {
		synchronized (sampleListeners) {
			if (this.sampleListeners.contains(listener)) {
				return;
			}
			this.sampleListeners.add(listener);
		}
	}

	public void removeSampleListener(SampleListener listener) {
		synchronized (sampleListeners) {
			this.sampleListeners.remove(listener);
		}
	}

	private void notifySampleListeners(Sample sample) {
		synchronized (sampleListeners) {
			for (SampleListener listener : sampleListeners) {
				listener.sampleAdded(sample);
			}
		}
	}

	public boolean isClear(Vector2D position) {
		return grid.isClear(position);
	}

	public float getCellSize() {
		return grid.getCellSize();
	}

	public boolean isClear(Vector2D position, float epsilon) {
		return grid.isClear(position, epsilon);
	}
}
