package dk1384.project;

import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

public class LaserBeam extends PhysicsElement {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3288136623399902728L;
	private static final float BASE_V = 300f/GamePanel.FPS;
	private Path2D laserWireFrame;
	private static final int[] xPoints = {5,5,-5,-5};
	private static final int[] yPoints = {1,-1,-1,-1};


	public LaserBeam(
			float locationX,
			float locationY,
			float orientationX,
			float orientationY
	) {
		super(
			BASE_V*orientationX,
			BASE_V*orientationY,
			0f,
			locationX,
			locationY,
			orientationX,
			orientationY
		);

		laserWireFrame = new GeneralPath();
		laserWireFrame.moveTo(xPoints[0],yPoints[0]);
		for(int i = 1; i < xPoints.length; i++)
			laserWireFrame.lineTo(xPoints[i], yPoints[i]);
		laserWireFrame.closePath();

		setWireFrame(laserWireFrame);
		setColor(Color.MAGENTA);

	}
	
	@Override
	public void updateKinematics() {
		super.updateKinematics();

		Point2D.Float localLocation = this.getLocation();
		if(Math.abs(localLocation.getX()) > GamePanel.PANEL_X/2 
			|| Math.abs(localLocation.getY()) > GamePanel.PANEL_Y/2) {
			setRenderable(false);
		}
	}

}
