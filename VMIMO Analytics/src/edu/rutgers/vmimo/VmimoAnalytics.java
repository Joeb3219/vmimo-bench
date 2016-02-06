package edu.rutgers.vmimo;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JFrame;

import org.apache.commons.io.FileUtils;

import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import edu.rutgers.vmimo.socket.SocketConnection;

public class VmimoAnalytics {

	public static final Scanner in = new Scanner(System.in);
	public static MessagePack messagePack;
	public static SocketConnection socket;
	private static JFrame window;
	private static EmbeddedMediaPlayerComponent mediaPlayerComponent;
	
	/*
	 * 
	 * Proposed Workflow:
	 * 
	 * - User begins bench by starting Java program.
	 * - Bench requests a port number, then instantiates Socket Server (Bench itself is the server) on given port.
	 * - User is then shown login info (IP + port) to input into phone -> phone will join session
	 * - Upon detecting a new socket client, bench will ask if device is the intended test device
	 * 		- If no, disconnect it & wait.
	 * - Bench now ignores all other incoming clients -> auto DC them 
	 * - Bench waits for user input of command
	 * - Commands:
	 * 		- flushCache
	 * 			- Flushes the cached messages used in embedding images (and then generates new ones)
	 * 		- newDevice
	 * 			- Disconnects current test device & looks for a new one (return to step 4)
	 * 		- test
	 * 			- params: -startingFps -endingFps -startingDelta -endingDelta -imagesPerDelta -deltaStep -imageId
	 * 			- Will begin the code to test if 
	 * 
	 */
	
	public static void main(String[] args){
		new NativeDiscovery().discover();
		
		//Testing
		boolean[] reference = {false, false, true, true, true, true, false, false, false, true, true, true, false, true, false, false, true, true, true, false, false, false, false, true, true, false, true, true, false, false, true, true, false, true, false, false, false, true, true, false, false, true, false, false, true, true, false, false, false, false, false, true, false, true, true, true, false, false, true, false, true, true, false, false, false, true, false, true, false, true, false, false, true, false, true, false, false, true, true, true};
		for(boolean b : reference){
			if(b) System.out.print("1");
			else System.out.print("0");
		}
		
		System.out.println();
		System.out.println(MessagePack.convertStringToBinary("abcdefghijk"));
		
		int[] ref = {1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 0};
		String ninetyFiveCorrectBinary = "";
		for(int i : ref){
			if(i == 1) ninetyFiveCorrectBinary += "1";
			else ninetyFiveCorrectBinary += "0";
		}
		System.out.println(ninetyFiveCorrectBinary);
		File mFile = new File(MessagePack._MESSAGES_SAVE_PATH);
		try{mFile.createNewFile();}catch(Exception e){}
		messagePack = new MessagePack(mFile);
		System.out.println(messagePack.getAccuracy(ninetyFiveCorrectBinary, MessagePack.convertStringToBinary("abcdefghijk")));
		
		
		createDisplayWindow();
		Thread socketThread = null;
		
		try{
			File messagesFile = new File(MessagePack._MESSAGES_SAVE_PATH);
			messagesFile.createNewFile();
			messagePack = new MessagePack(messagesFile);
		}catch(IOException e){e.printStackTrace();}
		
		try{
			try
		      {
		         socket = new SocketConnection(getIntInput("Enter a port number"));
		         socketThread = new Thread(socket, "Socket Thread");
		         socketThread.start();
		      }catch(Exception e){e.printStackTrace();}
			System.out.println("Enter this login key into your phone: " + SocketConnection.getIP() + ":" + socket.getLocalPort());
		}catch(Exception e){e.printStackTrace();}
		String command = "";
		while( !(command = getStringInput("Enter a command or [q]uit")).equals("q") && !command.equals("quit") ){
			if(command.equalsIgnoreCase("help")) loadHelpMessages();
			else if(command.equalsIgnoreCase("flushCache")) flushCache();
			else if(command.equalsIgnoreCase("newDevice")) newDevice();
			else if(command.startsWith("test")) initializeTest(command.substring(5).split(" "));
			else System.out.println("Command not recognized: " + command);
		}
		try{
			System.out.println("Shutting down server");
			mediaPlayerComponent.getMediaPlayer().stop();
			mediaPlayerComponent.release();
			window.dispose();
			socket.serverRunning = false;
			socket.invalidateCurrentClient();
			if(!socket.isClosed()) socket.close();
			if(socketThread != null){
				socketThread.interrupt();
				socketThread.join();
			}
		}catch(Exception e){e.printStackTrace();}
	}

	private static void createDisplayWindow(){
		window = new JFrame("RU VMIMO Bench Display");
		window.setBounds(100, 100, 600, 400);
		window.setAlwaysOnTop(true);
		mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
		mediaPlayerComponent.getMediaPlayer().setRepeat(true);
        window.setContentPane(mediaPlayerComponent);
		window.setVisible(true);
	}
	
	private static void flushCache(){
		System.out.println("Flushing messages cache.");
		messagePack.flush();
		System.out.println("Creating new message cache.");
		messagePack.build();
		messagePack.savePack();
	}
	
	private static void newDevice(){
		System.out.println("Ending socket connection with previous device.");
		socket.invalidateCurrentClient();
	}
	
	
	//Example of params: 1 12 0 50 200 1 5 90 12
	/**
	 * Params array expected in the following order:
	 * startingFps, endingFps, startingDelta, endingDelta, imagesPerDelta, deltaStep, imageId, angle (deg), distance (inches)
	 * @param params
	 */
	private static void initializeTest(String[] params){
		long startTime = System.currentTimeMillis() / 1000;
		if(params.length < 7){
			System.out.println("Not enough parameters: expected 9, got " + params.length + ".");
			return;
		}else if(socket.currentClient == null){
			System.out.println("No testing device connected.");
			return;
		}
		int startingFPS = Integer.parseInt(params[0]);
		int endingFPS = Integer.parseInt(params[1]);
		int startingDelta = Integer.parseInt(params[2]);
		int endingDelta = Integer.parseInt(params[3]);
		int imagesPerDelta = Math.max(Integer.parseInt(params[4]), messagePack._PACK_SIZE); //At least one per message.
		int deltaStep = Integer.parseInt(params[5]);
		int imageID = Integer.parseInt(params[6]);
		int angle = Integer.parseInt(params[7]);
		int distance = Integer.parseInt(params[8]);
		int picturesPerMessage = imagesPerDelta / messagePack._PACK_SIZE;
		final int totalPictures = (Math.abs(endingDelta - startingDelta + 1) * imagesPerDelta) * (Math.abs(endingFPS - startingFPS + 1));
		if(getStringInput("This will take approximately " + ( ((endingDelta - startingDelta + 1) * (endingFPS - startingFPS + 1) * imagesPerDelta) * (2 + imagesPerDelta * 3) ) + " seconds. Continue? [y/n]").equalsIgnoreCase("n")) return;
		
		
		new Thread(new Runnable() {
		    public void run() {
				for(int fps = startingFPS; fps < endingFPS + 1; fps ++){
					for(int delta = startingDelta; delta < endingDelta + deltaStep; delta += deltaStep){
						for(int currentMessage = 0; currentMessage < messagePack._PACK_SIZE; currentMessage ++){
							try {
								File tempFile = new File(System.getProperty("user.dir"), "vmimo_video_" + fps + "" + delta + "" + currentMessage + ".webm");
								FileUtils.copyURLToFile(new URL("http://vmimo.convex.vision/encode/" + imageID + "/" + messagePack.messages[currentMessage] + 
										"?alpha=" + delta + "&fps=" + fps), tempFile);
							} catch (MalformedURLException e) {e.printStackTrace();} 
							catch (IOException e) {e.printStackTrace();}
						}
					}
				}
				return;
		    }
		}).start();
		
		File reportsFolder = ReportUtils.generateNewReportFolder();
		ArrayList<String> commandReportLines = new ArrayList<String>();
		commandReportLines.add("COMMAND: test " + String.join(" ", params));
		commandReportLines.add("Angle: " + angle + " degrees");
		commandReportLines.add("Distance: " + distance + " inches");
		
		int picturesTaken = 0;
		int currentIndex = 0;
		for(int fps = startingFPS; fps < endingFPS + 1; fps ++){
			for(int delta = startingDelta; delta < endingDelta + deltaStep; delta += deltaStep){
				double accuracySum = 0.00;
				for(int currentMessage = 0; currentMessage < messagePack._PACK_SIZE; currentMessage ++){
					double messageAccuracySum = 0;
					System.out.println("Current: " + messagePack.binaryMessages[currentMessage]);
					File videoFile = new File(System.getProperty("user.dir"), "vmimo_video_" + fps + "" + delta + "" + currentMessage + ".webm");
					while(!videoFile.exists()){/*waiting*/}
					String[] options = {":file-caching=300", ":network-caching=300",
			                ":sout = #transcode{vcodec=x264,vb=800,scale=1,acodec=,fps=" + "" + "}:display :no-sout-rtp-sap :no-sout-standard-sap :ttl=1 :sout-keep"};
					mediaPlayerComponent.getMediaPlayer().stop();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					mediaPlayerComponent.getMediaPlayer().playMedia(videoFile.getAbsolutePath(), options);
					while(!mediaPlayerComponent.getMediaPlayer().isPlaying()){ //Wait until video actually playing
						try{Thread.sleep(50);}catch(Exception e){e.printStackTrace();}
					}
					for(int image = 0; image < picturesPerMessage + 1; image ++){
						picturesTaken ++;
						socket.sendMessage("test=true;imgcount=" + picturesPerMessage);
						String message = socket.getNextMessageOrWait();
						if(image == 0){
							System.out.println("Not saving the first one!");
							continue;
						}
						double accuracy = messagePack.getAccuracy(messagePack.binaryMessages[currentMessage], message);
						System.out.println("Accuracy: " + accuracy);
						messageAccuracySum += accuracy;
					}
					
					currentIndex ++;
					accuracySum += messageAccuracySum;
					commandReportLines.add("Accuracy of '" + messagePack.messages[currentMessage] + "' " + delta + " delta @ " + fps + " FPS: " + ((double) ((int) messageAccuracySum / (picturesPerMessage) * 100) / 100) + "%");
					System.out.println("PERCENTAGE COMPLETE: " + ( ( picturesTaken * 1.0 / totalPictures ) * 100.0 ) + "%");
					videoFile.delete();
				}
				commandReportLines.add("Accuracy of " + delta + " delta @ " + fps + "FPS: " + ((double) ((int) accuracySum / (imagesPerDelta) * 100) / 100) + "%");
				System.out.println("Accuracy of " + delta + " delta @ " + fps + "FPS: " + ((double) ((int) accuracySum / (imagesPerDelta) * 100) / 100) + "%");
			}
		}
		
		mediaPlayerComponent.getMediaPlayer().stop();
		commandReportLines.add(0, "RUN TIME: " + ((System.currentTimeMillis() / 1000) - startTime) + " seconds");
		ReportUtils.writeToFile(ReportUtils.createNewFile(reportsFolder, ReportUtils._REPORT_FILE_NAME), commandReportLines);
		messagePack.generateMissedBitsReport(reportsFolder);
		ReportUtils.copyMessagePackToReports(reportsFolder, messagePack);
	}
	
	private static void loadHelpMessages(){
		System.out.println("Valid commands: ");
		System.out.println("flushCache -> flushes old caches messages and regenerates new ones.");
		System.out.println("newDevice -> disconnects current test device and looks for a new one.");
		System.out.println("test [params: -startingFps -endingFps -startingDelta -endingDelta -imagesPerDelta -deltaStep -imageId] -> begins an accuracy test.");
	}
	
	public static String getStringInput(String message){
		System.out.print(message + ": ");
		return in.nextLine();
	}
	
	public static int getIntInput(String message){
		while(true){
			System.out.print(message + ": ");
			try {
				return Integer.parseInt(in.nextLine());
			} catch (NumberFormatException e) {
				System.out.println("Integer expected.");
			}
		}
	}	
	
}