package edu.rutgers.vmimo;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JFrame;

import org.apache.commons.io.FileUtils;

import edu.rutgers.vmimo.message.MessageOutput;
import edu.rutgers.vmimo.message.MessagePack;
import edu.rutgers.vmimo.message.MessagePanel;
import edu.rutgers.vmimo.socket.SocketConnection;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;

public class VmimoAnalytics {

	
	public static final int WIDTH = 400, HEIGHT = 360;
	public static int FPS = 6;
	public static final Scanner in = new Scanner(System.in);
	public static MessagePack messagePack;
	public static SocketConnection socket;
	private static JFrame window;
	private static EmbeddedMediaPlayerComponent mediaPlayerComponent;
	private static boolean _TRAINING = true;
	public static MessageOutput currentMessage;
	private static ArrayList<int[]> colorPairs = new ArrayList<int[]>();
	
	/*
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
		
		createDisplayWindow();
		Thread socketThread = null;
		
		try{
			File messagesFile = new File(MessagePack._MESSAGES_SAVE_PATH);
			messagesFile.createNewFile();
			messagePack = new MessagePack(messagesFile);
			parseColorFile();
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
			else if(command.startsWith("train")) trainColours(Integer.parseInt(command.split(" ")[1]), Integer.parseInt(command.split(" ")[2]));
			else if(command.startsWith("test")) initializeTest(command.substring(5).split(" "));
			else System.out.println("Command not recognized: " + command);
		}
		try{
			System.out.println("Shutting down server");
			if(mediaPlayerComponent != null){
				mediaPlayerComponent.getMediaPlayer().stop();
				mediaPlayerComponent.release();
			}
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
	
	private static void parseColorFile(){
		try{
			BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("colors.txt")));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				String[] parts = line.split(":");
				int[] pair = {Integer.parseInt(parts[0], 16), Integer.parseInt(parts[1], 16)};
				colorPairs.add(pair);
			}
			bufferedReader.close();
		}catch(IOException e){e.printStackTrace();}
	}
	
	private static void createDisplayWindow(){
		window = new JFrame("RU VMIMO Bench Display");
		window.setSize(WIDTH, HEIGHT);
		window.setAlwaysOnTop(true);
		window.setUndecorated(true);
		window.add(new MessagePanel());
		window.pack();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		window.setLocation(dim.width/2-window.getSize().width/2, dim.height/2-window.getSize().height/2);
		/*mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
		mediaPlayerComponent.getMediaPlayer().setRepeat(true);
		mediaPlayerComponent.setSize(WIDTH, HEIGHT);
		mediaPlayerComponent.setBounds(0, 0, WIDTH, HEIGHT);*/
        //window.setContentPane(mediaPlayerComponent);
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
	
	private static void trainColours(int start, int end){
		if(socket.currentClient == null){
			System.out.println("No testing device connected.");
			//return;
		}
		
		if(end > colorPairs.size()){
			System.out.println("Requested to test color pair " + end + " but we only have " + colorPairs.size());
			return;
		}
		
		String[] receivedMessages = new String[end - start];
		double[] accuracies = new double[(end - start)];
		MessageOutput[] messageOutputs = new MessageOutput[end - start];
		
		for(int i = 0; i < end - start; i ++){
			int[] colorPair = colorPairs.get(i + start);
			messageOutputs[i] = new MessageOutput("10101010100101010101101010101001010101011010101010010101010110101010100101010101", colorPair[0], colorPair[1]);
		}
		
		for(int i = start; i < end; i ++){
			currentMessage = messageOutputs[i - start];
			String receivedMessage = "";
			/*do{
				socket.sendMessage("test=true;imgcount=1");
				receivedMessage = socket.getNextMessageOrWait();
			}while(receivedMessage.equalsIgnoreCase("SOCKET_TIMEOUT"));
			do{
				socket.sendMessage("test=true;imgcount=1");
				receivedMessage = socket.getNextMessageOrWait();
			}while(receivedMessage.equalsIgnoreCase("SOCKET_TIMEOUT"));*/
			receivedMessage = "10101010100101010101101010101001010101011010101010010101010110101010100101010101";
			receivedMessages[i - start] = receivedMessage;
			
			double accuracy = messagePack.getAccuracy("10101010100101010101101010101001010101011010101010010101010110101010100101010101", receivedMessage);
			System.out.println(accuracy + "%: " + receivedMessage);
			accuracies[i - start] = accuracy;
			
			try {Thread.sleep(2000);
			} catch (InterruptedException e1) {e1.printStackTrace();}
		}
		
		String output = "[";
		for(int i = start; i < end; i ++){
			output += accuracies[i - start] + ", ";
		}
		try {
			BufferedWriter outputWriter = new BufferedWriter(new FileWriter(new File(System.getProperty("user.dir"), "training_accuracies_off_" + start + ".txt")));
			outputWriter.write(output + "]");
			outputWriter.newLine();
			for(int i = start; i < end; i ++){
				outputWriter.write(accuracies[i - start] + "");
				outputWriter.newLine();
			}
			for(int i = start; i < end; i ++){
				outputWriter.write(i + " " + receivedMessages[i - start]);
				outputWriter.newLine();
			}
			outputWriter.flush();
			outputWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//Example of params: 1 12 0 50 200 1 5 90 12
	/**
	 * Params array expected in the following order:
	 * startingFps, endingFps, startingDelta, endingDelta, imagesPerDelta, deltaStep, imageId, angle (deg), distance (inches)
	 * @param params
	 */
	private static void initializeTest(String[] params){
		if(!_TRAINING){
			System.out.println("TRAINING INSTANCE! No testing allowed!");
			return;
		}
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
								File tempFile = new File(System.getProperty("user.dir"), "vmimo_video_" + imageID + "_" +fps + "_" + delta + "_" + currentMessage + ".webm");
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
					File videoFile = new File(System.getProperty("user.dir"), "vmimo_video_" + imageID + "_" +fps + "_" + delta + "_" + currentMessage + ".webm");
					while(!videoFile.exists()){/*waiting*/}
					String[] options = {":file-caching=3000", ":network-caching=300",
			                ":sout = #transcode{vcodec=VP8,vb=5000,scale=1,acodec=,fps=" + fps + "}"};
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
						if(message.equalsIgnoreCase("SOCKET_TIMEOUT")) return;
						if(image == 0){
							picturesTaken --;
							System.out.println("Not saving the first one!");
							continue;
						}
						double accuracy = messagePack.getAccuracy(messagePack.binaryMessages[currentMessage], message);
						System.out.println("Accuracy: " + accuracy);
						messageAccuracySum += accuracy;
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					currentIndex ++;
					accuracySum += messageAccuracySum;
					commandReportLines.add("Accuracy of '" + messagePack.messages[currentMessage] + "' " + delta + " delta @ " + fps + " FPS: " + ((double) ((int) messageAccuracySum / (picturesPerMessage) * 100) / 100) + "%");
					System.out.println("PERCENTAGE COMPLETE: " + ( ( picturesTaken * 1.0 / totalPictures ) * 100.0 ) + "%");
					videoFile.deleteOnExit();
				}
				commandReportLines.add("Accuracy of " + delta + " delta @ " + fps + "FPS: " + ((double) ((int) accuracySum / (imagesPerDelta) * 100) / 100) + "%");
				System.out.println("Accuracy of " + delta + " delta @ " + fps + "FPS: " + ((double) ((int) accuracySum / (imagesPerDelta) * 100) / 100) + "%");
			}
		}
		
		mediaPlayerComponent.getMediaPlayer().stop();
		commandReportLines.add(0, "RUN TIME: " + ((System.currentTimeMillis() / 1000) - startTime) + " seconds");
		
		File reportsFolder = ReportUtils.generateNewReportFolder();
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
