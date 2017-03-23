package edu.rutgers.vmimo.message;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.Timer;

import edu.rutgers.vmimo.VmimoAnalytics;

public class MessagePanel extends JPanel{
	
	public MessagePanel(){
		setPreferredSize(new Dimension(VmimoAnalytics.WIDTH, VmimoAnalytics.HEIGHT));
		Timer timer = new Timer(1000 / VmimoAnalytics.FPS, new ActionListener() {
			long prevTime = System.currentTimeMillis();
			int frames = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
            	long thisTime = System.currentTimeMillis();
            	if(thisTime - prevTime >= 1000){
            		//System.out.println("FPS: " + frames);
            		prevTime = thisTime;
            		frames = 0;
            	}
            	frames ++;
                repaint();
                Toolkit.getDefaultToolkit().sync();
            }
        });
        timer.start();
	}
	
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(VmimoAnalytics.currentMessage != null) VmimoAnalytics.currentMessage.draw(g);
    }
	
}
