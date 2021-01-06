package raisa.domain.robot;

import raisa.config.VisualizerConfig;
import raisa.domain.samples.Sample;
import raisa.util.RandomUtil;
import raisa.util.Vector2D;

public class SimpleRobotMovementEstimator implements RobotMovementEstimator {

	private boolean usingParticleFilter;
	
	public SimpleRobotMovementEstimator(boolean usingParticleFilter) {
		this.usingParticleFilter = usingParticleFilter;
	}
	
	@Override
	public RobotState moveRobot(RobotState state, Sample sample) {
		RobotState robot = new RobotState();
		float leftTrackTrip;
		float rightTrackTrip;
		if (state == null) {
			return robot;
		} else if (sample.getLeftTrackTicks() != 0 || sample.getRightTrackTicks() != 0) {
			leftTrackTrip = (Robot.WHEEL_DIAMETER * sample.getLeftTrackTicks() * Robot.TICK_RADIANS) / 2.0f;
			rightTrackTrip = (Robot.WHEEL_DIAMETER * sample.getRightTrackTicks() * Robot.TICK_RADIANS) / 2.0f;
		} else if (sample.getLeftWheelOdometer() != 0 || sample.getRightWheelOdometer() != 0) {
			leftTrackTrip = (float)(sample.getLeftWheelOdometer() / 10.0);
			rightTrackTrip = (float)(sample.getRightWheelOdometer() / 10.0);
		} else {
			return state;
		}

		float h = state.getHeading();
		Vector2D positionLeftTrack = new Vector2D(state.getPositionLeftTrack().x + leftTrackTrip * (float) Math.sin(h),
				state.getPositionLeftTrack().y - leftTrackTrip * (float) Math.cos(h));

		Vector2D positionRightTrack = new Vector2D(state.getPositionRightTrack().x + rightTrackTrip * (float) Math.sin(h),
				state.getPositionRightTrack().y - rightTrackTrip * (float) Math.cos(h));
		
		robot.setDirectionLeftTrackForward(sample.getLeftTrackTicks() >= 0 ? true : false);
		robot.setDirectionRightTrackForward(sample.getRightTrackTicks() >= 0 ? true : false);
		
		robot.setPositionLeftTrack(positionLeftTrack);
		robot.setPositionRightTrack(positionRightTrack);
		
		if (VisualizerConfig.getInstance().getUseCompass() && !usingParticleFilter) {
			h = sample.getCompassDirection();
		} else {
			h += (leftTrackTrip - rightTrackTrip) / Robot.ROBOT_WIDTH;
		}
		// add noise	
		if (usingParticleFilter) {
			float noiseMagnitude = 5.0f;
			float a = (float)(RandomUtil.random() * Math.PI * 2.0f);
			float r = (float)RandomUtil.random() * noiseMagnitude;
			positionLeftTrack.x += (float)Math.cos(a) * r;
			positionLeftTrack.y += (float)Math.sin(a) * r;
			positionRightTrack.x += (float)Math.cos(a) * r;
			positionRightTrack.y += (float)Math.sin(a) * r;
			h += (float)((RandomUtil.random() * 8.0f - 4.0f) / 180.0f * Math.PI);
		}
		robot.setHeading(h);

		return robot;
	}
}
