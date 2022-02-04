package dk1384.project;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.Serializable;

public class PhysicsElement implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3573748720365597649L;
	private Path2D wireFrame;
	private AffineTransform angularVelocity;
	private float acceleration = 0f;
	private Color color;
	private Point2D.Float location; // as point
	private Point2D.Float orientation; // as point
	private AffineTransform velocity;	// as AffineTransform
	private AffineTransform relocate;
	private boolean renderable = true;

	public PhysicsElement(float velocityX, float velocityY,float omega,float locationX,float locationY, float orientationX, float orientationY) {
		this.velocity = AffineTransform.getTranslateInstance(velocityX,velocityY);
		this.angularVelocity = AffineTransform.getRotateInstance(omega);
		this.location = new Point2D.Float(locationX,locationY);
		this.orientation = new Point2D.Float(orientationX,orientationY);
		relocate = new AffineTransform();
	}

	public void setAngularVelocity(float omega) {
		if(omega != 0f)
			angularVelocity.setToRotation(omega);
		else
			angularVelocity.setToIdentity();
	}
	public void setAcceleration(float acceleration) {
		this.acceleration = acceleration;
	}

	// new
	public Point2D.Float getLocation() {
		return location;
	}

	// new
	public void setLocation(float x, float y) {
		relocate.setToTranslation(x-location.getX(),y-location.getY());
		wireFrame.transform(relocate);
		location.setLocation(x,y);
	}
		
	public Point2D.Float getOrientation() {
		return orientation;
	}
	
	public void setOrientation(float x, float y) {
		relocate.setToTranslation(-location.getX(), -location.getY());
		wireFrame.transform(relocate);
		relocate.setToRotation(orientation.getX(),orientation.getY());
		try {
			relocate.invert();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		relocate.rotate(x,y);
		wireFrame.transform(relocate);
		relocate.setToTranslation(location.getX(), location.getY());
		wireFrame.transform(relocate);
		orientation.setLocation(x,y);
	}
	
	public void scaleWireFrame(float s) {
		relocate.setToTranslation(-location.getX(), -location.getY());
		wireFrame.transform(relocate);
		relocate.setToScale(s, s);
		wireFrame.transform(relocate);
		relocate.setToTranslation(location.getX(), location.getY());
		wireFrame.transform(relocate);
	}
	
	public void setColor(Color c) {
		color = c;
	}
	
	public Color getColor() {
		return color;
	}
	
	public Path2D getWireFrame() {
		return wireFrame;
	}
	
	public boolean isRenderable() {
		return renderable;
	}
	
	public void setRenderable(boolean b) {
		renderable = b;
	}
	
	public void setWireFrame(Path2D wireFrame) {
		this.wireFrame = wireFrame;
		relocate.setToRotation(orientation.getX(),orientation.getY());
		this.wireFrame.transform(relocate);
		relocate.setToTranslation(location.getX(),location.getY());
		this.wireFrame.transform(relocate);
	}
	
	public void resetPE() {
		velocity.setToIdentity();
		angularVelocity.setToIdentity();
		acceleration = 0f;
	}

	public void updateKinematics() {
		if(!angularVelocity.isIdentity()) {
			angularVelocity.transform(orientation,orientation);
			relocate.setToTranslation(-location.getX(),-location.getY());
			wireFrame.transform(relocate);
			wireFrame.transform(angularVelocity);
			relocate.setToTranslation(location.getX(),location.getY());
			wireFrame.transform(relocate);
		}
		
		if(acceleration > 0f)
			velocity.translate(acceleration*orientation.getX(),acceleration*orientation.getY());
		
		velocity.transform(location,location);
		wireFrame.transform(velocity);
	}
}