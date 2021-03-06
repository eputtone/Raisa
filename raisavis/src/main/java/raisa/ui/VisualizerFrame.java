package raisa.ui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import raisa.comms.Communicator;
import raisa.comms.ControlMessage;
import raisa.comms.FailoverCommunicator;
import raisa.comms.SampleParser;
import raisa.comms.controller.BasicController;
import raisa.comms.controller.PidController;
import raisa.comms.controller.ReplayController;
import raisa.comms.serial.JserialSerialCommunicator;
import raisa.config.VisualizerConfig;
import raisa.domain.WorldModel;
import raisa.domain.particlefilter.ParticleFilter;
import raisa.domain.robot.RobotStateAggregator;
import raisa.domain.samples.Sample;
import raisa.session.SessionWriter;
import raisa.simulator.RobotSimulator;
import raisa.ui.controls.ControlPanel;
import raisa.ui.controls.sixaxis.SixaxisInput;
import raisa.ui.measurements.MeasurementsPanel;
import raisa.ui.options.VisualizationOptionsDialog;
import raisa.ui.tool.DrawTool;
import raisa.ui.tool.MeasureTool;
import raisa.ui.tool.Tool;
import raisa.ui.tool.WaypointTool;
import raisa.util.Vector2D;

@SuppressWarnings(value="SE_BAD_FIELD", justification="VisualizerFrame needs not to be serializable")
public class VisualizerFrame extends JFrame {
	private static final Logger log = LoggerFactory.getLogger(VisualizerFrame.class);
	private static final long serialVersionUID = 1L;
	private final int nparticles = 1000;
	private final VisualizerPanel visualizerPanel;
	private final File currentDirectory = new File(".");
	private File defaultDirectory = new File(currentDirectory, "data");
	private final File sessionDirectory = new File(currentDirectory, "sessions");
	private final WorldModel worldModel;
	private Tool currentTool;
	private final DrawTool drawTool = new DrawTool(this);
	private final MeasureTool measureTool = new MeasureTool(this);
	private final WaypointTool waypointTool;
	private final List<UserEditUndoListener> userEditUndoListeners = new ArrayList<UserEditUndoListener>();
	private final Communicator communicator;
	private final BasicController basicController;
	private final PidController pidController;
	private final RobotStateAggregator robotStateAggregator;
	private final ParticleFilter particleFilter;
	private final SessionWriter sessionWriter;
	private final RobotSimulator robotSimulator;
	private final FileBasedSimulation fileBasedSimulation;
	private final VisualizationOptionsDialog visualizationOptionsDialog;

	private final FileNameExtensionFilter mapFileFilter = new FileNameExtensionFilter("Map file (png)", "png");
	private final FileNameExtensionFilter sensorFileFilter = new FileNameExtensionFilter("Sensor file", "sensor");
	private final FileNameExtensionFilter controlFileFilter = new FileNameExtensionFilter("Control file", "control");

	public VisualizerFrame(final WorldModel worldModel) {
		addIcon();
		this.worldModel = worldModel;
		this.particleFilter = new ParticleFilter(worldModel, nparticles);
		this.robotStateAggregator = new RobotStateAggregator(worldModel, particleFilter, worldModel.getLandmarkManager());
		worldModel.addSampleListener(robotStateAggregator);

		robotSimulator = RobotSimulator.createRaisaInstance(new Vector2D(0, 0), 0, worldModel);

		visualizerPanel = new VisualizerPanel(this, worldModel, robotSimulator);
		VisualizerConfig.getInstance().addVisualizerConfigListener(visualizerPanel);
		visualizationOptionsDialog = new VisualizationOptionsDialog(this);

		MeasurementsPanel measurementsPanel = new MeasurementsPanel(worldModel);
		JMenuBar menuBar = new JMenuBar();
		createMainMenu(worldModel, menuBar);

		createViewMenu(menuBar);

		sessionWriter = new SessionWriter(sessionDirectory, "data");

		communicator = new FailoverCommunicator(new JserialSerialCommunicator().addSensorListener(worldModel), sessionWriter);
		communicator.connect();

		fileBasedSimulation = new FileBasedSimulation(worldModel);

		robotSimulator.addSensorListener(sessionWriter, worldModel);
		basicController = new BasicController(communicator, sessionWriter, robotSimulator);
		pidController = new PidController(worldModel, basicController, communicator, sessionWriter, robotSimulator);
		waypointTool = new WaypointTool(this, worldModel);

		setCurrentTool(drawTool);
		communicator.addSensorListener(sessionWriter);
		ControlPanel controlPanel = new ControlPanel(this, visualizerPanel, worldModel, basicController, pidController, communicator, sessionWriter, robotSimulator);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				IOUtils.closeQuietly(sessionWriter);
			}
		});

		createKeyboardShortCuts();

		getContentPane().add(visualizerPanel, BorderLayout.CENTER);
		getContentPane().add(controlPanel, BorderLayout.WEST);
		getContentPane().add(measurementsPanel, BorderLayout.EAST);
		setJMenuBar(menuBar);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		SixaxisInput.getInstance().activate();
	}

	private void createKeyboardShortCuts() {
		int nextFreeActionKey = 0;
		final int ZOOM_IN_ACTION_KEY = ++nextFreeActionKey;
		final int ZOOM_OUT_ACTION_KEY = ++nextFreeActionKey;
		final int CLEAR_HISTORY_ACTION_KEY = ++nextFreeActionKey;
		final int LIMIT_HISTORY_ACTION_KEY = ++nextFreeActionKey;
		final int STOP_ACTION_KEY = ++nextFreeActionKey;
		final int LEFT_ACTION_KEY = ++nextFreeActionKey;
		final int RIGHT_ACTION_KEY = ++nextFreeActionKey;
		final int FORWARD_ACTION_KEY = ++nextFreeActionKey;
		final int BACK_ACTION_KEY = ++nextFreeActionKey;
		final int LIGHTS_ACTION_KEY = ++nextFreeActionKey;
		final int UNDO_ACTION_KEY = ++nextFreeActionKey;
		final int REDO_ACTION_KEY = ++nextFreeActionKey;
		final int STEP_SIMULATION_ACTION_KEY = ++nextFreeActionKey;
		final int RANDOMIZE_PARTICLES_ACTION_KEY = ++nextFreeActionKey;

		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('+'), ZOOM_IN_ACTION_KEY);
		visualizerPanel.getActionMap().put(ZOOM_IN_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				visualizerPanel.zoomIn();
				updateTitle();
			}
		});
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('-'), ZOOM_OUT_ACTION_KEY);
		visualizerPanel.getActionMap().put(ZOOM_OUT_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				visualizerPanel.zoomOut();
				updateTitle();
			}
		});
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('c'), CLEAR_HISTORY_ACTION_KEY);
		visualizerPanel.getActionMap().put(CLEAR_HISTORY_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				visualizerPanel.clear();
			}
		});
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('h'), LIMIT_HISTORY_ACTION_KEY);
		visualizerPanel.getActionMap().put(LIMIT_HISTORY_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				visualizerPanel.removeOldSamples();
			}
		});
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
				LEFT_ACTION_KEY);
		visualizerPanel.getActionMap().put(LEFT_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				basicController.sendLeft();
			}
		});
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
				RIGHT_ACTION_KEY);
		visualizerPanel.getActionMap().put(RIGHT_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				basicController.sendRight();
			}
		});
		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
				STOP_ACTION_KEY);
		visualizerPanel.getActionMap().put(STOP_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				basicController.sendStop();
			}
		});

		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
				FORWARD_ACTION_KEY);
		visualizerPanel.getActionMap().put(FORWARD_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				basicController.sendForward();
			}
		});

		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
				BACK_ACTION_KEY);
		visualizerPanel.getActionMap().put(BACK_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				basicController.sendBack();
			}
		});

		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('l'), LIGHTS_ACTION_KEY);
		visualizerPanel.getActionMap().put(LIGHTS_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				basicController.sendLights();
			}
		});

		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), UNDO_ACTION_KEY);
		visualizerPanel.getActionMap().put(UNDO_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (isUserEditUndoable()) {
					popUserEditUndoLevel();
					VisualizerFrame.this.repaint();
				}
			}
		});

		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK), REDO_ACTION_KEY);
		visualizerPanel.getActionMap().put(REDO_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				if (isUserEditRedoable()) {
					redoUserEditUndoLevel();
					VisualizerFrame.this.repaint();
				}
			}
		});

		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('p'),
				STEP_SIMULATION_ACTION_KEY);
		visualizerPanel.getActionMap().put(STEP_SIMULATION_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				fileBasedSimulation.setStepSimulation(false);
			}
		});

		visualizerPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('r'),
				RANDOMIZE_PARTICLES_ACTION_KEY);
		visualizerPanel.getActionMap().put(RANDOMIZE_PARTICLES_ACTION_KEY, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent event) {
				particleFilter.randomizeParticles(nparticles);
				repaint();
			}
		});
	}

	private void createViewMenu(JMenuBar menuBar) {
		JMenu viewMenu = new JMenu("View");
		viewMenu.setMnemonic('v');
		JMenuItem zoomIn = new JMenuItem("Zoom in");
		zoomIn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				visualizerPanel.zoomIn();
				updateTitle();
			}
		});
		zoomIn.setMnemonic('i');
		JMenuItem zoomOut = new JMenuItem("Zoom out");
		zoomOut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				visualizerPanel.zoomOut();
				updateTitle();
			}
		});
		zoomOut.setMnemonic('o');

		JMenuItem visualizationOptions = new JMenuItem("Options");
		visualizationOptions.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				visualizationOptionsDialog.setVisible(true);
			}
		});

		viewMenu.add(zoomIn);
		viewMenu.add(zoomOut);
		viewMenu.addSeparator();
		viewMenu.add(visualizationOptions);

		menuBar.add(viewMenu);
	}

	private JMenu createMainMenu(final WorldModel worldModel, JMenuBar menuBar) {
		JMenu mainMenu = new JMenu("Main");
		mainMenu.setMnemonic('m');
		JMenuItem reset = new JMenuItem("Reset");
		reset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				reset();
				repaint();
			}
		});
		reset.setMnemonic('r');
		JMenuItem loadData = new JMenuItem("Load sensor file...");
		loadData.setMnemonic('d');
		loadData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadSensorSamples(null);
			}
		});
		JMenuItem loadReplay = new JMenuItem("Load control file...");
		loadReplay.setMnemonic('p');
		loadReplay.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadReplay(null);
			}
		});
		JMenuItem saveAs = new JMenuItem("Save sensor samples as...");
		saveAs.setMnemonic('a');
		saveAs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveSensorSamples(null);
			}
		});
		JMenuItem loadMap = new JMenuItem("Load map...");
		loadMap.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadMap(null);
				notifyUserEditUndoAction();
				VisualizerFrame.this.repaint();
			}
		});
		JMenuItem resetMap = new JMenuItem("Reset map");
		resetMap.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pushUserEditUndoLevel();
				worldModel.resetMap();
				VisualizerFrame.this.repaint();
			}
		});
		JMenuItem saveMapAs = new JMenuItem("Save map as...");
		saveMapAs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveMap(null);
			}
		});
		JMenuItem exit = new JMenuItem("Exit");
		exit.setMnemonic('x');
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exit();
			}
		});
		mainMenu.add(reset);
		mainMenu.addSeparator();
		mainMenu.add(loadData);
		mainMenu.add(loadReplay);
		mainMenu.add(saveAs);
		mainMenu.addSeparator();
		mainMenu.add(resetMap);
		mainMenu.add(loadMap);
		mainMenu.add(saveMapAs);
		mainMenu.addSeparator();
		mainMenu.add(exit);
		menuBar.add(mainMenu);
		return mainMenu;
	}

	public void open() {
		updateTitle();
		setSize(600, 400);
		setVisible(true);
		setLocationRelativeTo(null);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
	}

	public void loadMap(String fileName) {
		if (fileName == null) {
			final JFileChooser chooser = new JFileChooser(defaultDirectory);
			chooser.setDialogTitle("Open map file");
			chooser.setFileFilter(mapFileFilter);
			chooser.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					String fileName = chooser.getSelectedFile().getAbsolutePath();
					try {
						saveDefaultDirectory(fileName);
						worldModel.loadMap(fileName);
						particleFilter.randomizeParticles(nparticles);
					} catch (Exception e) {
						log.error("Opening file failed", e);
					}
				}
			});
			chooser.showOpenDialog(this);
		} else {
			try {
				worldModel.loadMap(fileName);
			} catch (Exception e) {
				log.error("Loading map failed", e);
			}
		}
	}

	protected void saveMap(String fileName) {
		if (fileName == null) {
			final JFileChooser chooser = new JFileChooser(defaultDirectory);
			chooser.setDialogTitle("Save map file");
			chooser.setFileFilter(mapFileFilter);
			chooser.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					String fileName = chooser.getSelectedFile().getAbsolutePath();
					try {
						saveDefaultDirectory(fileName);
						worldModel.saveMap(fileName);
					} catch (Exception e) {
						log.error("Saving file failed", e);
					}
				}
			});
			chooser.showSaveDialog(this);
		} else {
			try {
				worldModel.saveMap(fileName);
			} catch (Exception e) {
				log.error("Saving map failed", e);
			}
		}
	}

	private void setCurrentTool(Tool tool) {
		this.currentTool = tool;
	}

	public void saveSensorSamples(String fileName) {
		if (fileName == null) {
			final JFileChooser chooser = new JFileChooser(defaultDirectory);
			chooser.setDialogTitle("Save sensor file");
			chooser.setFileFilter(sensorFileFilter);
			chooser.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					String fileName = chooser.getSelectedFile().getAbsolutePath();
					try {
						saveDefaultDirectory(fileName);
						internalSaveSensorSamples(fileName);
					} catch (Exception e) {
						log.error("Saving file failed", e);
					}
				}
			});
			chooser.showSaveDialog(this);
		} else {
			try {
				internalSaveSensorSamples(fileName);
			} catch (Exception e) {
				log.error("Saving sensor samples failed", e);
			}
		}
	}

	public void loadSensorSamples(String filename) {
		if (filename == null) {
			final JFileChooser chooser = new JFileChooser(defaultDirectory);
			chooser.setDialogTitle("Open sensor file");
			chooser.setFileFilter(sensorFileFilter);
			chooser.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					String fileName = chooser.getSelectedFile().getAbsolutePath();
					try {
						saveDefaultDirectory(fileName);
						internalLoadSensorSamples(fileName, false);
					} catch (Exception e) {
						log.error("Opening file failed", e);
					}
				}
			});
			chooser.showOpenDialog(this);
		} else {
			try {
				internalLoadSensorSamples(filename, false);
			} catch (Exception e) {
				log.error("Loading sensor samples failed", e);
			}
		}
	}

	public void loadReplay(String filename) {
		if (filename == null) {
			final JFileChooser chooser = new JFileChooser(defaultDirectory);
			chooser.setDialogTitle("Open control file");
			chooser.setFileFilter(controlFileFilter);
			chooser.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (chooser.getSelectedFile() == null) {
						log.debug("Canceled replay file selection");
						return;
					}
					String fileName = chooser.getSelectedFile().getAbsolutePath();
					try {
						saveDefaultDirectory(fileName);
						internalLoadReplay(fileName);
					} catch (Exception e) {
						log.error("Opening file failed", e);
					}
				}
			});
			chooser.showOpenDialog(this);
		} else {
			try {
				internalLoadReplay(filename);
			} catch (Exception e) {
				log.error("Loading control failed");
			}
		}
	}

	private void internalLoadReplay(String fileName) throws FileNotFoundException, IOException {
		log.debug("Loading replay file {}", fileName);
		List<ControlMessage> controlMessages = new ArrayList<ControlMessage>();
		try (BufferedReader fr = new BufferedReader(new FileReader(fileName))) {
			String line = fr.readLine();
			while (line != null) {
				// TODO error handling
				ControlMessage controlMessage = ControlMessage.fromJson(line);
				if (controlMessage != null) {
					controlMessages.add(controlMessage);
				}
				line = fr.readLine();
			}
		}
		log.info("Replaying {} control messages", controlMessages.size());
		ReplayController replayController = new ReplayController(controlMessages, communicator, robotSimulator);
		basicController.copyListenersTo(replayController);
		replayController.start();
	}

	private void internalSaveSensorSamples(String fileName) throws Exception {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			for (Sample sample : worldModel.getSamples()) {
				writer.write(sample.getSampleString());
				writer.newLine();
			}
		}
	}

	private void internalLoadSensorSamples(String fileName, boolean delayed) throws FileNotFoundException, IOException {
		List<String> sampleStrings = new ArrayList<String>();
		SampleParser parser = new SampleParser();

		try (BufferedReader fr = new BufferedReader(new FileReader(fileName))) {
			String line = fr.readLine();
			while (line != null) {
				if (!parser.isValid(line)) {
					if(line.length() > 0) {
						log.warn("Invalid sample! \"{}\"", line);
					}
				} else {
					sampleStrings.add(line);
				}
				line = fr.readLine();
			}
		}
		spawnSimulationThread(sampleStrings, delayed);
	}


	public void reset() {
		visualizerPanel.reset();
		robotSimulator.reset();
		particleFilter.reset();
		updateTitle();
	}

	public void exit() {
		System.exit(0);
	}

	public void spawnSampleSimulationThread(final List<Sample> samples, final boolean delayed) {
		if (!samples.isEmpty()) {
			new Thread(new Runnable() {
				private int nextSample = 0;

				@Override
				public void run() {
					while (nextSample < samples.size()) {
						worldModel.addSample(samples.get(nextSample));
						++nextSample;
						if (delayed) {
							try {
								Thread.sleep(50);
							} catch (InterruptedException e) {
							}
						}
					}
				}
			}).start();
		}
	}

	public void spawnSimulationThread(final List<String> samples, final boolean delayed) {
		fileBasedSimulation.setSamples(samples, delayed);
		fileBasedSimulation.start();
	}

	public VisualizerPanel getVisualizer() {
		return visualizerPanel;
	}

	private void saveDefaultDirectory(String filename) {
		defaultDirectory = new File(filename).getParentFile();
	}

	private void updateTitle() {
		setTitle("Raisa Visualizer - " + Math.round(visualizerPanel.getScale() * 100.0f) + "%");
	}

	public void selectedWaypointTool() {
		setCurrentTool(waypointTool);
	}

	public void selectedMeasureTool() {
		setCurrentTool(measureTool);
	}

	public void selectedDrawTool() {
		setCurrentTool(drawTool);
	}

	public Tool getCurrentTool() {
		return currentTool;
	}

	public float getScale() {
		return visualizerPanel.getScale();
	}

	public void panCameraBy(float dx, float dy) {
		visualizerPanel.panCameraBy(dx, dy);
	}

	public void setGridPosition(Vector2D position, boolean isBlocked) {
		worldModel.setGridPosition(position, isBlocked);
	}

	public void setUserPosition(Vector2D position, boolean isBlocked) {
		worldModel.setUserPosition(position, isBlocked);
	}

	public Vector2D toWorld(Vector2D screenPosition) {
		return visualizerPanel.toWorld(screenPosition);
	}

	public float toWorld(float screenDistance) {
		return visualizerPanel.toWorld(screenDistance);
	}

	public void pushUserEditUndoLevel() {
		worldModel.pushUserEditUndoLevel();
		notifyUserEditUndoAction();
	}

	private void notifyUserEditUndoAction() {
		for (UserEditUndoListener listener : userEditUndoListeners) {
			listener.usedEditUndoAction();
		}
	}

	public void addUserEditUndoListener(UserEditUndoListener listener) {
		userEditUndoListeners.add(listener);
	}

	public void popUserEditUndoLevel() {
		worldModel.popUserEditUndoLevel();
		notifyUserEditUndoAction();
	}

	public void redoUserEditUndoLevel() {
		worldModel.redoUserEditUndoLevel();
		notifyUserEditUndoAction();
	}

	public boolean isUserEditUndoable() {
		return worldModel.isUserEditUndoable();
	}

	public boolean isUserEditRedoable() {
		return worldModel.isUserEditRedoable();
	}

	public int getUserUndoLevels() {
		return worldModel.getUserUndoLevels();
	}

	public int getUserRedoLevels() {
		return worldModel.getUserRedoLevels();
	}

	public ParticleFilter getParticleFilter() {
		return particleFilter;
	}

	public RobotSimulator getRobotSimulator() {
		return robotSimulator;
	}

	private void addIcon() {
		setIconImage(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/raisa-icon.png")));
	}

}
