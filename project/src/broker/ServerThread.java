package broker;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.Socket;

public class ServerThread extends Thread
{
	private Socket socket;
	private SSLSocket sslSocket; //è socket castata a SSL se possibile, altrimenti null

	ServerThread(Socket socket){
		this.socket = socket;
		if (socket instanceof SSLSocket)
			sslSocket = (SSLSocket) socket;
		else
			sslSocket = null;
	}

	public String getIp()
	{
		return socket.getInetAddress().getHostAddress()+":"+ socket.getPort();
	}

	public String getProtocol()
	{
		if (sslSocket!=null)
			return sslSocket.getSession().getProtocol();
		return null;
	}

	public String getChipherSuite()
	{
		if (sslSocket!=null)
			return sslSocket.getSession().getCipherSuite();
		return null;
	}

	public void run()
	{
	//	Main.addServerThread(this);
		System.out.print("Richiesta da "+socket.getInetAddress().getHostAddress()+"... ");
		try{
			if (sslSocket!=null)
			{
				System.out.print(" (SSL Socket!) ");
				sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
				// Start handshake
				sslSocket.startHandshake();
			}
			// Get session after the connection is established
/*			SSLSession sslSession = socket.getSession();

			System.out.println("SSLSession :");
			System.out.println("\tProtocol : "+sslSession.getProtocol());
			System.out.println("\tCipher suite : "+sslSession.getCipherSuite());
*/
			// Start handling application content
			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = socket.getOutputStream();

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

			String cmd = bufferedReader.readLine();
			//Mi aspetto formato: 	subscribe token_bot localport

			String[] splitted = cmd.split(" ");

			try {
				if (!splitted[0].toLowerCase().equals("subscribe"))
					throw new Exception("Method " + splitted[0] + " not recognized");
				if (!splitted[1].equals(Main.bot_token))
					throw new Exception("Token not recognized");
				int port = Integer.parseInt(splitted[2]);
				if (port < 1 || port > 256 * 256)
					throw new Exception("Invalid port");

				Main.addSubscribers(socket.getInetAddress().getHostAddress() + ":" + port);
				System.out.println("accettata");
				bufferedWriter.write("OK\n");
			}
			catch(Exception e)
			{
				System.out.println("rifiutata");
				bufferedWriter.write("ERROR\n");
			}
			bufferedWriter.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

//		Main.removeServerThread(this);
	}

	/*
	public String extractRequest(BufferedReader br) throws IOException
	{

		String line;
		StringBuffer sb = new StringBuffer();

		while((line = br.readLine()) != null){
			if(line.trim().isEmpty()){
				break;
			}
			sb.append(line).append("\n");
		}
		System.out.println(sb);
		return sb.toString();
	}

	private String[] extractInfo(String request)
	{
		BufferedReader br = new BufferedReader(new StringReader(request));
		String[] s;
		String token = "";
		String line;
		String offset = "";
		try
		{
			line = br.readLine();
			s = line.split(" ");
			if (s[0].toLowerCase().equals("get"))
			{
				s = s[1].substring(1).split("/");
				if (s[1].startsWith("getUpdates"))
				{
					token = s[0];
					if (s[1].contains("\\?"))
					{
						s = s[1].split("\\?");
						if (s[1].contains("="))
						{
							s = s[1].split("=");
							if (s[0].equals("offset"))
							{
								offset = s[1];
							}
						}
					}
				}
			}
			if (token.equals(""))
				return null;
			if (offset.equals(""))
				return new String[]{token};
			return new String[]{token,offset};
		}
		catch(IOException e){return null;}
	}

	public String buildResponse(String body)
	{
		StringBuffer sb = new StringBuffer();
		sb.append("HTTP/1.1 200 OK\n");
		sb.append("Content-Type: text/plain\n");
		sb.append("Server: telegram-message-dispatcher\n");
		sb.append("Content-Length: ").append(body.length()).append("\n");
		sb.append("Connection: closed\n\n");
		sb.append(body);
		return sb.toString();
	}
*/
}
