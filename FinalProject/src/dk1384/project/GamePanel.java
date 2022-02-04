package dk1384.project;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

public class GamePanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int PANEL_X = 750;
	public static final int PANEL_Y = 750;
	public static final int FPS = 60;
	private SpaceCraft player1;
	private SpaceCraft player2;
	private Timer refreshTimer;
	private ActionListener refreshListener;
	private KeyAdapter keyAdapter;
	private List<Asteroid> asteroidList;
	private List<SpaceCraft> craftList;
	private static final int NUM_ASTEROIDS = 10;
	private static final int MAX_ASTEROIDS = 20;
	private static int asteroidCounter = 0;
	private static int asteroidThreshold = 15*FPS;


	private ArrayList<SimpleImmutableEntry<Path2D,Color>> renderList;

	private int port = 8000;
	private int beaconPort = 8001;
	private volatile ObjectInputStream in;
	private volatile ObjectOutputStream out;
	private volatile ServerSocket server = null;
	private volatile Socket socket = null;
	private enum ConType {SERVER, CLIENT, DISCONNECTED, BUSY}
	private volatile ConType connection = ConType.DISCONNECTED;
	String host = null;
	private volatile Thread dataThread = null; 
	private int beaconPeriod = 2000;


	
	public GamePanel() {
		super();
		this.setPreferredSize(new Dimension(PANEL_X,PANEL_Y));
		this.setBackground(Color.BLACK);
		player1 = new SpaceCraft();
		player2 = new SpaceCraft();
		player2.setColor(Color.GREEN);
		asteroidList = new ArrayList<>();
		craftList = new ArrayList<>();
		renderList = new ArrayList<>();
		spawnPlayer(player1);
		player1.setColor(Color.CYAN);
			
		keyAdapter = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(connection == ConType.CLIENT) {
					try {
						out.reset();
						out.writeInt(e.getKeyCode());
					} catch (IOException exception) {
						disconnect();
						exception.printStackTrace();
					}
				}
				else
					player1.keyPressed(e.getKeyCode());
			}
			@Override
			public void keyReleased(KeyEvent e) {
				if(connection == ConType.CLIENT) {
					try {
						out.reset();
						out.writeInt(-e.getKeyCode());
					} catch (IOException exception) {
						disconnect();
						exception.printStackTrace();
					}
				}
				else
					player1.keyReleased(e.getKeyCode());
			}
		};
		this.addKeyListener(keyAdapter);

		refreshListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaint();
				
				switch (connection) {
					case CLIENT:
						sendRemoteCommands();
						receiveRenderList();
						break;
					case SERVER:
						sendRenderList();
						receiveRemoteCommands();
					default:
						detectCollisions();
						craftList.removeIf(pe -> !pe.isRenderable());
						asteroidList.removeIf(pe -> !pe.isRenderable());
						craftList.forEach(sc -> {
							sc.getLaserList().removeIf(pe -> !pe.isRenderable());
							sc.updateKinematics();
							sc.getLaserList().forEach(PhysicsElement::updateKinematics);
						});
						asteroidList.forEach(PhysicsElement::updateKinematics);
						packRenderList();
						addAsteroids();
				}
			}
		};
		
		
		refreshTimer = new Timer(1000/FPS,refreshListener);
		setFocusable(true);
		refreshTimer.start();
		
	}

	public void spawnPlayer() {
		if(connection == ConType.CLIENT) {
			try {
				out.reset();
				out.writeInt(KeyEvent.VK_F20);

			} catch (IOException e) {
				disconnect();
				e.printStackTrace();
			}
		}
		else {
			this.spawnPlayer(player1);
			player1.setColor(Color.CYAN);

		}
	}
	
	public void spawnPlayer(SpaceCraft player) {
			player.resetPlayer();
			craftList.remove(player);
			craftList.add(player);
	}
	
	private void detectCollisions() {
		Rectangle2D hitBox;
		for( SpaceCraft sc: craftList){
			for ( Asteroid as: asteroidList) {
				hitBox = as.getWireFrame().getBounds2D();
				if(sc.getWireFrame().intersects(hitBox))
					sc.destroyed();
				for( LaserBeam lb : sc.getLaserList()) {
					if(lb.getWireFrame().intersects(hitBox)) {
						as.damage();
						lb.setRenderable(false);
					}
				}
			}
			
		}
	}
	
	private void renderGraphics(Graphics2D g2d) {
		for ( SimpleImmutableEntry<Path2D,Color> pair: renderList) {
			g2d.setColor(pair.getValue());
			g2d.fill(pair.getKey());
		}
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Graphics2D g2d = (Graphics2D) g;
		g2d.translate(GamePanel.PANEL_X/2, GamePanel.PANEL_Y/2);
		g2d.rotate(3*Math.PI/2);
		
		renderGraphics(g2d);
		
	}
	
	public void packRenderList() {
		renderList.clear();
		craftList.forEach(sc -> {
			renderList.add(new SimpleImmutableEntry<Path2D,Color>(sc.getWireFrame(),sc.getColor()));
			sc.getLaserList().forEach(lb -> {
				renderList.add(new SimpleImmutableEntry<Path2D,Color>(lb.getWireFrame(),lb.getColor()));
			});
		});
		asteroidList.forEach(as -> {
			renderList.add(new SimpleImmutableEntry<Path2D,Color>(as.getWireFrame(),as.getColor()));
		});
		renderList.trimToSize();
	}

	public void startServer() {
		Runnable dt, bt;
		bt = () -> {
			DatagramSocket bSocket = null;
			InetAddress address;
			byte[] buf = new byte[0];
			DatagramPacket beaconPacket;

			
			try	{
				address = InetAddress.getByName("255.255.255.255");
				bSocket = new DatagramSocket();
				bSocket.setBroadcast(true);
				beaconPacket = new DatagramPacket(buf,buf.length,address,beaconPort);	
				while (connection == ConType.BUSY) {
					bSocket.send(beaconPacket);
					Thread.sleep(beaconPeriod);
				}
				
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			} finally {
				if(bSocket != null) bSocket.close();
				bSocket = null;
			}
		};	

		dt = () -> {
			Thread beacon;
			try {
				connection = ConType.BUSY;
				server = new ServerSocket(port);
				beacon = new Thread(bt);
				beacon.start();
				socket = server.accept();
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(socket.getInputStream());
				connection = ConType.SERVER;
			} catch (IOException e) {
				disconnect();
				e.printStackTrace();
			}
		};
		


		if(connection == ConType.DISCONNECTED && server == null) {
			dataThread = new Thread(dt);
			dataThread.start();
		}
	}
	
	
	public void disconnect() {
		if (in != null ) {
			try {
				in.close();
				in = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (out != null) {
			try {
				out.close();
				out = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (socket != null){
			try {
				socket.close();
				socket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (server != null) {
			try {
				server.close();
				server = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		connection = ConType.DISCONNECTED;
	}
	
	public void connectClient(String host) {
		this.host = host;
		Runnable r = ()-> {
			try {
				connection = ConType.BUSY;
				socket = new Socket(host,port);
				in = new ObjectInputStream(socket.getInputStream());
				out = new ObjectOutputStream(socket.getOutputStream());
				connection = ConType.CLIENT;
			} catch (IOException e) {
				try {
					if(out != null) out.close();
					if(in != null) in.close();
					if(socket != null) socket.close();
					connection = ConType.DISCONNECTED;
				} catch (IOException e1) {
					e.printStackTrace();
				}
			}
		};
		if(connection == ConType.DISCONNECTED && socket == null) {
			dataThread = new Thread(r);
			dataThread.start();
		}
	}
	
	private void sendRenderList() {
		try {
		out.reset();
		out.writeObject(renderList);
		out.flush();
		} catch (IOException e) {
			disconnect();
			e.printStackTrace();
		}

	}
	
	@SuppressWarnings("unchecked")
	private void receiveRenderList() {
		try {
			Object received = in.readObject();
			if(renderList.getClass().isInstance(received))
				renderList = renderList.getClass().cast(received);

		} catch (ClassNotFoundException | IOException e) {
			disconnect();
			e.printStackTrace();
		}
	}
	
	private void sendRemoteCommands() {
		try{
			out.flush();
		} catch (IOException e) {
			disconnect();
			e.printStackTrace();
		}
	}
	
	private void receiveRemoteCommands() {
		
		try {
			while(in != null && in.available() >= 4) {
				int k = in.readInt();
				if(k == KeyEvent.VK_F20) {
					spawnPlayer(player2);
					player2.setColor(Color.GREEN);
				}
				else if(k >= 0)
					player2.keyPressed(k);
				else 
					player2.keyReleased(-k);
			}
		} catch (IOException e) {
			disconnect();
			e.printStackTrace();
		}
	}
	
	public void addAsteroids() {
		if(asteroidList.size() == 0) {
			for(int i = 0; i < NUM_ASTEROIDS; i++)
				asteroidList.add(new Asteroid());
		}
		if(asteroidList.size() <= MAX_ASTEROIDS) {
			asteroidCounter = (asteroidCounter+1)%asteroidThreshold;
			if(asteroidCounter == 0) {
				asteroidList.add(new Asteroid());
				if(asteroidThreshold > FPS*5)
					asteroidThreshold -= FPS;
			}
		}
	}

}