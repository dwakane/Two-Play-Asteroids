package dk1384.project;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Objects;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class GameWindow extends JFrame{
	private static final long serialVersionUID = 1L;
	GamePanel gamePanel;
	private Font textFont;

	private static int beaconPort = 8001;
	byte[] buf = new byte[0];
	volatile boolean threadLoop = true;
	HashMap<String,Long> serverMap =new HashMap<>();
	volatile JOptionPane serverPane = null;
	int socketTimeOut = 1000;

	private JLabel instructions;

	public GameWindow(String s) {
		super(s);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		instructions = new JLabel("Arrow Keys:    \u2191 Thruster    \u2190 Rotate CCW    \u2192 Rotate CW      SPACE Fire" );
		instructions.setFont(new Font("SanSerif",0,20));
		gamePanel = new GamePanel();

		this.add(instructions,BorderLayout.NORTH);
		this.add(gamePanel,BorderLayout.CENTER);
		createMenus();
		this.setResizable(false);
		pack();
		
	}
	
	public GameWindow() {
		this("");
	}
	
	private void createMenus() {
		textFont = new Font("SansSerif", Font.PLAIN, 20);
		
		//Menu Bar
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		// File Menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		fileMenu.setFont(textFont);
		menuBar.add(fileMenu);

		//Respawn Menu Item
		JMenuItem spawnPlayer = new JMenuItem("(Re)Spawn");
		spawnPlayer.setMnemonic('R');
		spawnPlayer.setFont(textFont);
		spawnPlayer.addActionListener(e -> gamePanel.spawnPlayer());
		fileMenu.add(spawnPlayer);

		fileMenu.addSeparator(); // Separator Line
	
		//StartServer Menu Item
		JMenuItem startServer = new JMenuItem("StartServer");
		startServer.setMnemonic('S');
		startServer.setFont(textFont);
		startServer.addActionListener(e -> {
			gamePanel.startServer();
			this.setTitle("Final Project dk1384 - Server");
		});
		fileMenu.add(startServer);
		
		//StartClient Menu Item
		JMenuItem startClient = new JMenuItem("StartClient");
		startClient.setMnemonic('C');
		startClient.setFont(textFont);
		startClient.addActionListener(e -> {
			Thread t = null;
			Runnable r = () -> {
				DatagramSocket socket = null;
				DatagramPacket beaconPacket = null;
				boolean added = false;
				boolean removed = false;
				threadLoop = true;
				
				try {
					socket = new DatagramSocket(beaconPort);
					socket.setBroadcast(true);
					socket.setSoTimeout(socketTimeOut);
					beaconPacket = new DatagramPacket(buf,buf.length);
					serverMap.clear();
					while(threadLoop) {

						try {
							socket.receive(beaconPacket);
							added = Objects.isNull(
									serverMap.put(
											beaconPacket.getAddress().getHostAddress()
											,System.currentTimeMillis()
										)
									);
							
						} catch (SocketTimeoutException e1) {}
						removed = serverMap.values().removeIf(val -> 
							(System.currentTimeMillis()- val > 6*socketTimeOut));
						if(serverPane != null && (added || removed )) {
							serverPane.setSelectionValues(serverMap.keySet().toArray());
							added = removed = false;
						}
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					if(socket != null) { 
						socket.close() ;
						socket = null;
					}
				}
			};

			serverPane = new JOptionPane(
				"Available Servers:",
				JOptionPane.PLAIN_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION
			);
			serverPane.setSelectionValues(new String[] {""});
			
			t = new Thread(r);
			t.start();

			serverPane.createDialog(this,"Servers").setVisible(true);;
			String server = (String) serverPane.getInputValue();
			if(server != null && server.length() > 0)
				gamePanel.connectClient(server);
			threadLoop = false;
			this.setTitle("Final Project dk1384 - Client");
		});
		fileMenu.add(startClient);
		

		//StopServer Menu Item
		JMenuItem disconnect= new JMenuItem("Disconnect");
		disconnect.setMnemonic('D');
		disconnect.setFont(textFont);
		disconnect.addActionListener(e -> {
			gamePanel.disconnect();
			this.setTitle("Final Project dk1384") ;
		});
		fileMenu.add(disconnect);

		fileMenu.addSeparator(); // Separator Line
		
		//Exit Menu Item
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.setMnemonic('x');
		exitItem.setFont(textFont);
		exitItem.addActionListener(e -> System.exit(0));
		fileMenu.add(exitItem);
	}

}
