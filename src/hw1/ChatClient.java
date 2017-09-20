package hw1;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;


public class ChatClient {
	
	public static int portNum = 4444;
	public static String host = "localhost"; 
	
	//Sets up socket connection and runs init functions for UI
	public static void main(String args[]) throws UnknownHostException, IOException {
		
		Scanner in = new Scanner(System.in);
	
		System.out.print("Please Enter a User Name: "); 
		String name = in.nextLine();
		
		// connect to the site
		Socket socket = new Socket(host,portNum);
				
		OutputStream os = socket.getOutputStream();
		PrintWriter out = new PrintWriter(os);
		
		out.println(name);
		out.flush(); // makes sure data is sent over.
	
		//Create server listener thread
		Thread listener = new Thread(new ServerListener(socket));
		listener.start();
		
		//Creates class to send messages
		MailMan mm = new MailMan(socket);
		//initializes UI
		UserInterface ui = new UserInterface(mm, name);
		ui.init();
		
	}
	
} //end of class


//Listens for messages coming from the server
class ServerListener implements Runnable{
	Socket s;
	ServerListener(Socket s){this.s = s;}
	
	public void run(){
		Scanner in;
		String message;
		try{
			in = new Scanner(s.getInputStream());
		}catch(IOException e){
			System.out.println("UH OH. Something happened :(");
			e.printStackTrace();
			in = null;
		}
		while(true){
						
			message = in.nextLine();
			System.out.println(message);
			
		}
		
	}
}

//Use interface that can be used to select files and type message to send to server
class UserInterface extends JFrame implements ActionListener{
	
	JLabel name;
	JButton messageButton;
	JButton imageButton;
	JTextField input;
	MailMan mm;
	final JFileChooser fc;
	
	
	/**
	 * I don't know why I need this, but eclipse was yelling at me
	 */
	private static final long serialVersionUID = 1L;

	UserInterface(MailMan mm, String name){
		this.mm = mm; 
		fc = new JFileChooser(); 
		this.name = new JLabel(name, SwingConstants.CENTER);
	}
	
	//handles actions that happen in the UI
	public void actionPerformed(ActionEvent e) {
		
		String action = e.getActionCommand();
		
		//directs user to message sending screen
		if (action.equals("message")) {
			getContentPane().removeAll();
			revalidate();
			repaint();
			input = new JTextField(5);
			input.setActionCommand("messageField");
			input.addActionListener(this);
	        input.setBounds(50,20,200,20);
	        add(input);
		}
		//grabs message from message input box
		if(action.equals("messageField")){
			String msg = input.getText();
			System.out.println(msg);
			mm.sendMessage(msg);
			
			showHomePage();
		}
		//pulls up file picker
		if(action.equals("image")){
			int result = fc.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				File selectedFile = fc.getSelectedFile();
				System.out.println(selectedFile.getName());
				mm.sendImage(selectedFile);
				
			}
		}
	}
	
	//Renders the homepage
	public void showHomePage(){
		getContentPane().removeAll();
		revalidate();
		repaint();
		add(messageButton);//Add the button to the JFrame.
		add(imageButton);//Add the button to the JFrame.
		add(name);
	}
	
	//initializes the UI
	public void init(){
		
		fc.setCurrentDirectory(new File(System.getProperty("user.home")));
		
		getContentPane().setLayout(new GridLayout(3, 3));
		
		messageButton = new JButton("Send Message");//The JButton name.
		messageButton.setActionCommand("message");
		messageButton.addActionListener(this);//Reads the action.
		
		imageButton = new JButton("Send Image");//The JButton name.
		imageButton.setActionCommand("image");
		imageButton.addActionListener(this);//Reads the action.
		
		showHomePage();
		
	    setSize(300,300);
	    setLocationRelativeTo(null);
	    setVisible(true); 
	}
}//end of class



class MailMan{
	Socket socket; OutputStream os; PrintWriter pw;
	MailMan(Socket s){
		socket = s;
		try{
			os = socket.getOutputStream();
			pw = new PrintWriter(os);
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void sendMessage(String message){
		
		try {
			ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
			os.writeObject(message);
			os.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
	}
	public void sendImage(File f){
		
		try {
			ObjectOutputStream os =  new ObjectOutputStream(socket.getOutputStream());
			os.writeObject(f);
			os.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
		
