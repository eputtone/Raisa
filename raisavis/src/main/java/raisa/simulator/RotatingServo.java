package raisa.simulator;


public class RotatingServo {
	private float heading;
	private final float rotatingSpeed = 60.0f / 0.17f;
	private final float minHeading = -90;
	private final float maxHeading = 90;
	private static final int LEFT = -1;
	private static final int RIGHT = +1;
	private int direction = RIGHT;
	private final ServoScanListener servoScanListener;

	public RotatingServo(ServoScanListener servoScanListener) {
		this.heading = (minHeading + maxHeading) / 2.0f;
		this.servoScanListener = servoScanListener;
	}

	public RotatingServo rotate(float timestep) {
		heading += direction * timestep * rotatingSpeed;
		scan(heading);
		int nextDirection = direction;
		if (heading >= maxHeading) {
			nextDirection = LEFT;
		}
		if (heading <= minHeading) {
			scan(heading);
			nextDirection = RIGHT;
		}
		direction = nextDirection;
		return this;
	}

	private void scan(float servoHeading) {
		servoScanListener.scan(servoHeading);
	}

	public float getHeading() {
		return heading;
	}
}
