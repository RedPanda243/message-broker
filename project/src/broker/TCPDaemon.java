package broker;

import java.io.IOException;
import java.net.ServerSocket;

public class TCPDaemon extends Thread
{
	private int port;
	private ServerSocket serverSocket;

	TCPDaemon(int port) throws IOException
	{
		serverSocket = new ServerSocket(port);
		this.setName("TCP Socket Server");
		this.port = serverSocket.getLocalPort();
	}

	public int getPort()
	{
		return port;
	}

	// Start to run the tcpDaemon
	public void run()
	{
		try
		{
			while(true)
			{
				new ServerThread(serverSocket.accept()).start();
			}
		}
		catch(IOException e){e.printStackTrace();}
	}
}
