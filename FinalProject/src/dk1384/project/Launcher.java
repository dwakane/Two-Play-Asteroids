package dk1384.project;

import java.awt.EventQueue;

public class Launcher {

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			GameWindow gameWindow = new GameWindow("Final Project dk1384");
			gameWindow.setVisible(true);
		});
	}
}
