package dk1384.project;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class SpaceCraft extends PhysicsElement{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3997250472421458148L;
	private static final int[] yPoints = {10,0,-10,0};
	private static final int[] xPoints = {-10,10,-10,-5};
	private static final float ACCL = 0.4f*30/GamePanel.FPS;
	private static final float ANG_V = 0.15f*30/GamePanel.FPS;
	private static final int LASER_DELAY = GamePanel.FPS/4;
	private static GeneralPath craftWireFrame;

	private boolean twistLeftState = false;
	private boolean twistRightState = false;
	private boolean alive = true;
	private int persistCounter = GamePanel.FPS*3;
	private int invincibleCounter = GamePanel.FPS;
	private boolean fireLaser = false;
	private int laserCounter = 0;
	private List<LaserBeam> laserList = null;

	static {
		craftWireFrame = new GeneralPath();
		craftWireFrame.moveTo(xPoints[0],yPoints[0]);
		for(int i = 1; i < xPoints.length; i++)
			craftWireFrame.lineTo(xPoints[i], yPoints[i]);
		craftWireFrame.closePath();
	}

	public SpaceCraft() {
		super(0f,0f,0f,0f,0f,1f,0f);
		setWireFrame(craftWireFrame.getClass().cast(craftWireFrame.clone()));
		setColor(Color.GRAY);
		laserList = new ArrayList<LaserBeam>();
	}
	
	public List<LaserBeam> getLaserList() {
		return laserList;
	}
	
	public void updateRotation() {
		if(alive) {
			if(twistLeftState && !twistRightState) {
				this.setAngularVelocity(-ANG_V);
			} else if ( !twistLeftState && twistRightState) {
				this.setAngularVelocity(ANG_V);
			}
			else this.setAngularVelocity(0f);
		}
	}
	
	@Override
	public boolean isRenderable() {
		if(alive) return true;
		else if(persistCounter-- > 0) return true;
		else return false;
	}
	
	public void resetPlayer() {
		persistCounter = GamePanel.FPS*3;
		invincibleCounter = GamePanel.FPS*3;
		alive = true;
		setRenderable(true);
		setColor(Color.GRAY);
		setLocation(0f,0f);
		setOrientation(1f,0f);
		resetPE();
	}
	
	public boolean isAlive() {
		return alive;
	}
	public void destroyed() { 

		if(alive && invincibleCounter  <= 0) { 
			setColor(Color.YELLOW); 
			alive = false;
		}
	}
	
	private void fire() {
		if(laserCounter == 0) {
				laserList.add(new LaserBeam(
					(float)this.getLocation().getX(),
					(float)this.getLocation().getY(),
					(float)this.getOrientation().getX(),
					(float)this.getOrientation().getY()
				));
			}
		laserCounter = (laserCounter+1)%LASER_DELAY;
	}

	@Override
	public void updateKinematics() {
		super.updateKinematics();

		Point2D.Float location = this.getLocation();
		if(Math.abs(location.getX()) > GamePanel.PANEL_X/2) {
			setLocation((float)-Math.signum(location.getX())*GamePanel.PANEL_X/2,(float)location.getY());
		}

		if(Math.abs(location.getY()) > GamePanel.PANEL_Y/2) {
			setLocation((float)location.getX(),(float)-Math.signum(location.getY())*GamePanel.PANEL_Y/2);
		}

		if(alive && fireLaser) fire();
		
		if(invincibleCounter > 0) invincibleCounter--;
	}
	
	public void keyPressed(int e) {
		switch (e) {
		case KeyEvent.VK_UP:
			if(alive) setAcceleration(ACCL);
			break;
		case KeyEvent.VK_LEFT:
			twistLeftState = true;
			updateRotation();
			break;
		case KeyEvent.VK_RIGHT:
			twistRightState = true;
			updateRotation();
			break;
		case KeyEvent.VK_SPACE:
			fireLaser = true;
			break;
		default:
			break;
		}
	}
	public void keyReleased(int e) {
		switch (e) {
		case KeyEvent.VK_UP:
			setAcceleration(0f);
			break;
		case KeyEvent.VK_LEFT:
			twistLeftState = false;
			updateRotation();
			break;
		case KeyEvent.VK_RIGHT:
			twistRightState = false;
			updateRotation();
			break;
		case KeyEvent.VK_SPACE:
			fireLaser = false;
			laserCounter = 0;
			break;
		default:
			break;
		}
	}
}