package edu.rutgers.vmimo.message;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class MessageOutput{

	
	public static final int WIDTH = 400, HEIGHT = 360;
	private String message;
	int baseColor, secondaryColor;
	BufferedImage img1, img2;
	boolean drawImage1 = true;
	
	public MessageOutput(String message, int baseColor, int secondaryColor){
		this.message = message;
		this.baseColor = baseColor;
		this.secondaryColor = secondaryColor;
		img1 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		img2 = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

		int blockWidth = WIDTH / 10;
		int blockHeight = HEIGHT / 8;
		boolean[] blockVals = new boolean[80];
		
		for(int i = 0; i < blockVals.length; i ++) blockVals[i] = (message.charAt(i) == '1');
		
		for(int x = 0; x < WIDTH; x ++){
			for(int y = 0; y < HEIGHT; y ++){
				img1.setRGB(x, y, baseColor);
				int block = (y / blockHeight)* 10 + (x / blockWidth);
				int img2Color = baseColor;
				if(blockVals[block] == true) img2Color = secondaryColor;
				img2.setRGB(x, y, img2Color);
			}
		}
	
	}
	
	public void draw(Graphics g){
		BufferedImage img = img1;
		if(!drawImage1) img = img2;
		g.drawImage(img, (WIDTH - img.getWidth()) / 2, (HEIGHT - img.getHeight()) / 2, null);
		drawImage1 = !drawImage1;
	}
	
}
