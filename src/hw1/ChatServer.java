package hw1;

import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class ChatServer {
	public static void main(String[] args) throws IOException {
		//Where the file locations of the files are specified
		String fileSaveLoc = "c://temp//";
		String chatRecord = "c://temp//chat.txt";
		String chatNum = "c://temp//chatNum.txt";
		File theDir = new File(fileSaveLoc);
		
		//makes the directory to store images if it doesn't exist
		if (!theDir.exists()) {
		    try{
		        theDir.mkdir();
		    } 
		    catch(SecurityException se){
		        se.printStackTrace();//handle it
		    }        
		}
		
		ServerSocket serverSocket = null;
		int clientNum = 0;
		MessageSender ms = new MessageSender();
		Recorder rec = new Recorder(chatRecord, chatNum);
		
		//Starts the webserves on port 4444
		try {
			serverSocket = new ServerSocket(4444); // provide MYWEBSERVICE at port 4444
			System.out.println(serverSocket);
		} catch (IOException e) {
			System.out.println("Could not listen on port: 4444");
			System.exit(-1);
		}

		// LOOP FOREVER - SERVER IS ALWAYS WAITING TO PROVIDE SERVICE!
		while (true) {
			Socket clientSocket = null;
			try {
				clientSocket = serverSocket.accept();
				clientNum++;
				ms.addClient(clientNum, clientSocket);
				Thread t = new Thread(new ChatClientHandler(clientSocket, clientNum, ms, fileSaveLoc, rec));
				t.start();
			} catch (IOException e) {
				System.out.println("Accept failed: 4444");
				System.exit(-1);
			}
						

		}

	}
} //end of class


/**
 * Handles client messages and images. Makes new file names for incoming files and
 * sends messages on to other class that will handle message distribution. Also, saves incoming files.
 * @author franciss
 *
 */
class ChatClientHandler implements Runnable  {
	Socket s; int num; String name; MessageSender ms; String fileSavePath; Recorder rec;
	
	ChatClientHandler(Socket s, int n, MessageSender ms, String fsp, Recorder rec){
		this.s = s; 
		num = n;
		this.ms = ms; 
		fileSavePath = fsp;
		this.rec = rec;
	}
	
	public void run() {
		Scanner in; 
		String message;
		try {
			in = new Scanner(s.getInputStream());
			name = in.nextLine(); // skip header line
			System.out.println(name + " has joined to the server");
			ms.distributeMessage(num, "Has joined the server", name);
			
			//Always looping, waiting for messages from client
			while(true){
				
				ObjectInputStream is = new ObjectInputStream(s.getInputStream());
				Object o = is.readObject();
				
				//handles images
				if(o instanceof File){
					File clientImg = (File) o;
					String fileName = formatImageName(clientImg);
					String displayMsg = name + ": " + fileName;
					rec.write(displayMsg);
					System.out.println(displayMsg);
					saveFile(clientImg, fileName);
					ms.distributeMessage(num, fileName, name);

				}
				//handles messages
				if(o instanceof String){
					String msg = ((String)o);
					String displayMsg = name + ": " + msg;
					System.out.println(displayMsg);
					rec.write(displayMsg);
					ms.distributeMessage(num, msg, name);
				}
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	//saves image files
	public void saveFile(File f, String fileName){
		
		try {
			BufferedImage image = ImageIO.read(f);
			ImageIO.write(image, getExtension(fileName), new File(fileSavePath + fileName));
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	//formats the name of image files to include date and name
	public String formatImageName(File f){
		String oldFileName = f.getName();
		StringBuilder sb = new StringBuilder(oldFileName);
		int i  = oldFileName.lastIndexOf('.');
		String time = LocalDateTime.now().getHour()+ "_ " + LocalDateTime.now().getMinute();
		sb.insert(i,"_" + name + "_" + time);
		String newFileName = sb.toString();
		return newFileName;
	}
	//gets the file extension of file
	public static String getExtension(String fileName){
		int i = fileName.lastIndexOf('.');
		return fileName.substring(i+1);
	}

}//end of class

/**
 * Class which handles outgoing message
 * @author franciss
 *
 */
class MessageSender{
	Map<Integer, Socket> clientList = new HashMap<>();//stores client sockets and associated client number
	
	//add client to Map
	public void addClient(int num, Socket s){
		clientList.put(num, s);
	}
	
	//distributes messages to all clients other than one specified
	public void distributeMessage(int clientNum, String message, String name){
		
		for (Map.Entry<Integer, Socket> entry : clientList.entrySet()) { 
			if(clientNum != entry.getKey()){
				try {
					PrintWriter out = new PrintWriter(entry.getValue().getOutputStream());
					out.println(name + ": " + message);
					out.flush();
					
				} catch (IOException e) {					
					e.printStackTrace();
				}
			}
		}
	}	
}

/**
 * records all chat records in a text file
 * @author franciss
 *
 */
class Recorder{
	private int msgNum;
	private String chatFile;
	private String chatNum;
	
	Recorder(String chatFile, String chatNum){
		this.chatFile = chatFile;
		this.chatNum = chatNum;
		
		//Create file to record chat num, if it doesn't already exist
		File msgNumFile = new File(chatNum);
		try {
			msgNumFile.createNewFile();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		Scanner in;
		try {
			in = new Scanner(msgNumFile);
		} catch (FileNotFoundException e1) {
			in = null;
		}
		
		//get the chat num. If there isn't one, set it to zero and record it 
		//in the file
		if(in.hasNextInt()){
			msgNum = in.nextInt();
		}
		else{
			msgNum = 0;
		}
		in.close();
		
		//Create fileOutputStream to write to chat num file
		FileOutputStream tempStream;
		try {
			tempStream = new FileOutputStream(chatNum, false);
			tempStream.write((msgNum + "").getBytes());
			tempStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		//create chat record file, if it doesn't exist
		File chat = new File(chatFile);
		try {
			chat.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
		
	} 
	
	//writes to the chat file
	public synchronized void write(String s){
		msgNum++;
		FileOutputStream chatNumFos;
		FileOutputStream chatFos;
		try {
			chatNumFos = new FileOutputStream(chatNum);
			chatFos = new FileOutputStream(chatFile, true);
			chatFos.write((msgNum + "--" + s + "\n").getBytes());
			chatNumFos.write((msgNum + "").getBytes());
			chatNumFos.close();
			chatFos.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
	
	}
	
}
