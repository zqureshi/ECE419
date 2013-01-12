import java.net.*;
import java.io.*;

public class EchoServerHandlerThread extends Thread {
	private Socket socket = null;

	public EchoServerHandlerThread(Socket socket) {
		super("EchoServerHandlerThread");
		this.socket = socket;
		System.out.println("Created new Thread to handle client");
	}

	public void run() {

		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			EchoPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			

			while (( packetFromClient = (EchoPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */
				EchoPacket packetToClient = new EchoPacket();
				packetToClient.type = EchoPacket.ECHO_REPLY;
				
				/* process message */
				/* just echo in this example */
				if(packetFromClient.type == EchoPacket.ECHO_REQUEST) {
					packetToClient.message = packetFromClient.message;
					System.out.println("From Client: " + packetFromClient.message);
				
					/* send reply back to client */
					toClient.writeObject(packetToClient);
					
					/* wait for next packet */
					continue;
				}
				
				/* Sending an ECHO_NULL || ECHO_BYE means quit */
				if (packetFromClient.type == EchoPacket.ECHO_NULL || packetFromClient.type == EchoPacket.ECHO_BYE) {
					gotByePacket = true;
					packetToClient = new EchoPacket();
					packetToClient.type = EchoPacket.ECHO_BYE;
					packetToClient.message = "Bye!";
					toClient.writeObject(packetToClient);
					break;
				}
				
				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown ECHO_* packet!!");
				System.exit(-1);
			}
			
			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

		} catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
	}
}
