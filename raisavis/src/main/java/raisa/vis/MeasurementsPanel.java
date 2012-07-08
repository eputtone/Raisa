package raisa.vis;

import java.awt.Dimension;
import java.awt.Graphics;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class MeasurementsPanel extends JPanel implements Observer {
	private static final long serialVersionUID = 1L;

	private AccelerationPanel accelerationPanel; 
	private GyroscopePanel gyroscopePanel; 
	
	public MeasurementsPanel(WorldModel worldModel) {
		worldModel.addObserver(this);
		TitledBorder border = new TitledBorder("Measurements");
		setBorder(border);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setMinimumSize(new Dimension(200, 300));
		this.setPreferredSize(new Dimension(200, 300));
		this.setMaximumSize(new Dimension(200, 300));
		accelerationPanel = new AccelerationPanel(worldModel);
		this.add(accelerationPanel);
		gyroscopePanel = new GyroscopePanel();
		this.add(gyroscopePanel);		
	}
	
	public void update(Observable worldModel, Object sample) {
		accelerationPanel.update((WorldModel)worldModel, (Sample)sample);
		gyroscopePanel.update((WorldModel)worldModel, (Sample)sample);
	}
	
	class AccelerationPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		private JLabel accXField;
		private JLabel accYField;
		private JLabel accZField;
		private AccelerationGraphPanel accXPanel;
		private AccelerationGraphPanel accYPanel;
		private AccelerationGraphPanel accZPanel;

		public AccelerationPanel(WorldModel worldModel) {
			this.setMinimumSize(new Dimension(190, 240));
			this.setPreferredSize(new Dimension(190, 240));
			this.setMaximumSize(new Dimension(190, 240));
			TitledBorder border = new TitledBorder("Acceleration (m/s^2)");
			setBorder(border);
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			accXField = new JLabel("X: -");
			accXField.setAlignmentX(LEFT_ALIGNMENT);
			accYField = new JLabel("Y: -");
			accYField.setAlignmentX(LEFT_ALIGNMENT);
			accZField = new JLabel("Z: -");
			accZField.setAlignmentX(LEFT_ALIGNMENT);
			accXPanel = new AccelerationGraphPanel();
			accXPanel.setAlignmentX(LEFT_ALIGNMENT);
			accYPanel = new AccelerationGraphPanel();
			accYPanel.setAlignmentX(LEFT_ALIGNMENT);
			accZPanel = new AccelerationGraphPanel();	
			accZPanel.setAlignmentX(LEFT_ALIGNMENT);
			this.add(accXField);		
			this.add(accXPanel);
			this.add(accYField);	
			this.add(accYPanel);	
			this.add(accZField);
			this.add(accZPanel);	
		}
			
		public void update(WorldModel worldModel, Sample sample) {
			DecimalFormat format = new DecimalFormat("0.000");			
			Vector3D acceleration = sample.getAcceleration();
			accXField.setText("X: " + format.format(acceleration.getX()));
			accYField.setText("Y: " + format.format(acceleration.getY()));
			accZField.setText("Z: " + format.format(acceleration.getZ()));
			List<Sample> samples = worldModel.getLastSamples(120);
			List<Float> samplesX = new LinkedList<Float>();
			List<Float> samplesY = new LinkedList<Float>();
			List<Float> samplesZ = new LinkedList<Float>();
			for (Sample s : samples) {
				samplesX.add(s.getAcceleration().getX());
				samplesY.add(s.getAcceleration().getY());
				samplesZ.add(s.getAcceleration().getZ());
			}
			accXPanel.setMeasurements(samplesX);
			accYPanel.setMeasurements(samplesY);
			accZPanel.setMeasurements(samplesZ);
			repaint();
		}
		
	}
	
	class GyroscopePanel extends JPanel {
		private static final long serialVersionUID = 1L;

		private JLabel gyroXField;
		private JLabel gyroYField;
		private JLabel gyroZField;
		
		public GyroscopePanel() {
			this.setMinimumSize(new Dimension(190,80));
			this.setPreferredSize(new Dimension(190,80));
			this.setMaximumSize(new Dimension(190, 80));
			TitledBorder border = new TitledBorder("Gyroscope (dps)");
			setBorder(border);
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			gyroXField = new JLabel("X: -");
			gyroYField = new JLabel("Y: -");
			gyroZField = new JLabel("Z: -");
			this.add(gyroXField);		
			this.add(gyroYField);			
			this.add(gyroZField);						
		}
			
		public void update(WorldModel worldModel, Sample sample) {
			DecimalFormat format = new DecimalFormat("0.00");			
			Vector3D angularAcceleration = sample.getAngularAcceleration();
			gyroXField.setText("X: " + format.format(angularAcceleration.getX()));
			gyroYField.setText("Y: " + format.format(angularAcceleration.getY()));
			gyroZField.setText("Z: " + format.format(angularAcceleration.getZ()));	
			repaint();
		}
		
	}
	
	class AccelerationGraphPanel extends JPanel {
		
		private List<Float> measurements = new LinkedList<Float>();
		
		public AccelerationGraphPanel() {
			this.setPreferredSize(new Dimension(190, 50));
			this.setMaximumSize(new Dimension(190, 50));
		}
		
	    public Dimension getPreferredSize() {
	        return new Dimension(190,50);
	    }
	    
	    public void setMeasurements(List<Float> measurements) {
	    	this.measurements = measurements;
	    }

	    protected void paintComponent(Graphics g) {
	        super.paintComponent(g);   
	        for (int i=0; i<measurements.size(); i++) {
	        	g.drawLine(20 + i, 25, 20 + i, 25 + (int)(20 * measurements.get(i).floatValue()));
	        }
	    }  
	    
	}
	
}