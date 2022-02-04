package dk1384.project;

import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Random;

public class Asteroid extends PhysicsElement {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5750356901638568098L;
	private static final float BASE_V = 250f/GamePanel.FPS;
	private static final float BASE_OMEGA = 6f/GamePanel.FPS;
	private static final float GPX = GamePanel.PANEL_X/2;
	private static final float GPY = GamePanel.PANEL_Y/2;
	private float halfWidth;
	private float side;
	private GeneralPath asteroidWireFrame;
	private float[] xPoints;
	private float[] yPoints;
	private float rnd;

	private static final float damageFactor = 0.75f;
	private static final float minAsteroidSide = 5;


	
	private static Random rng;
	static {
		rng = new Random();
	}

	public Asteroid(float s) {
		super(
				(rng.nextFloat()-0.5f)*BASE_V,
				(rng.nextFloat()-0.5f)*BASE_V,
				(rng.nextFloat()-0.5f)*BASE_OMEGA,
				0f,
				0f,
				1f,
				0f
			);
		side = s;
		halfWidth = side*(1+((float) Math.sqrt(2)))/2;
		xPoints = new float[] {-halfWidth,-halfWidth,-side/2,side/2,halfWidth,halfWidth,side/2,-side/2};
		yPoints = new float[] {side/2,-side/2,-halfWidth,-halfWidth,-side/2,side/2,halfWidth,halfWidth};
		asteroidWireFrame = new GeneralPath();
		asteroidWireFrame.moveTo(xPoints[0], yPoints[0]);
		for( int i = 1; i < xPoints.length; i++)
			asteroidWireFrame.lineTo(xPoints[i], yPoints[i]);
		asteroidWireFrame.closePath();
		setWireFrame(asteroidWireFrame);
		setColor(Color.RED);
		rnd = rng.nextFloat()*2*(GPX+GPY);
		/*
		setLocation((rng.nextFloat()/2+0.25f) * GamePanel.PANEL_X/2 * (rng.nextBoolean() ? -1:1),
				(rng.nextFloat()/2+0.25f) * GamePanel.PANEL_Y/2 * (rng.nextBoolean() ? -1:1));
		*/
		setLocation(Math.min(rnd-GPX,GPX)-Math.max(rnd-(4*GPY+2*GPX),0),
				Math.max(Math.min(Math.max(rnd-(GPX+GPY), 0)-GPX, GPX)-Math.max(rnd-2*(GPX+GPY),0), -GPY));
	}
	
	public Asteroid() {
		this((new Random()).nextFloat()*10+10);
	}
	
	public void damage() {
		side *= damageFactor;
		if( side < minAsteroidSide) {
			this.setRenderable(false);
		}
		else {
			this.scaleWireFrame(damageFactor);
		}
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
	}
}
