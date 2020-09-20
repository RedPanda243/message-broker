package broker;

import json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Main
{
	public static String bot_token;
	private static UpdateReader reader;

	private static TCPDaemon tcpDaemon;
	//private static ArrayList<ServerThread> threadlist = new ArrayList<>();
	private static ArrayList<String> subscribers = new ArrayList<>();
	private static final Object threadlistmonitor = new Object();
	private static final Object subscribersmonitor = new Object();
	private static final Object updatereadermonitor = new Object();

	public static void main(String args[]) throws Exception
	{
		//Leggo token bot da file. Leggo solo la prima riga del file
		BufferedReader br = new BufferedReader(new FileReader("token_bot"));
		bot_token = br.readLine();
		br.close();

		//Avvio il thread che scaricherà i messaggi da telegram
		reader = new UpdateReader(updatereadermonitor);
		reader.start();

		//Leggo serversocket/tcp_port da file settings.json e avvio tcpdaemon
		JSONObject settings = new JSONObject(new FileReader("settings.json"));
		tcpDaemon = new TCPDaemon(settings.get(Integer.class, "broker","tcp_port"));
		tcpDaemon.start();

		//Avvio una CLI per interagire con l'app
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String cmd;
		while(true)
		{
			cmd = in.readLine();
			if (cmd.toLowerCase().equals("status"))
			{
				printStatus();
			}
		}
	}

	public static void addSubscribers(String s)
	{
		synchronized (subscribersmonitor)
		{
			if (subscribers.contains(s))
				return;

			subscribers.add(s);
			if (subscribers.size() == 1) {
				synchronized (updatereadermonitor) {
					updatereadermonitor.notify();
				}
			}
		}
	}

	public static int countSubscribers()
	{
		synchronized (subscribersmonitor)
		{
			return subscribers.size();
		}
	}

	public static ArrayList<String> getSubscribers()
	{
		synchronized (subscribersmonitor)
		{
			return (ArrayList<String>) subscribers.clone();
		}
	}

	public static void removeSubscribers(String s)
	{
		synchronized (subscribersmonitor) {
			subscribers.remove(s);
		}
	}

/*	public static void addServerThread(ServerThread thread)
	{
		synchronized (threadlistmonitor) {
			threadlist.add(thread);
		}
	}

	public static void removeServerThread(ServerThread thread)
	{
		synchronized (threadlistmonitor) {
			threadlist.remove(thread);
		}
	}

 */
/*
	private synchronized static String getListThread()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(threadlist.size()).append(" active threads:");
		for (ServerThread s:threadlist)
			sb.append("\t").append(s.getIp()).append(",").append(s.getProtocol()).append(",").append(s.getChipherSuite()).append("\n");
		return sb.toString();
	}
*/
	private static void printStatus()
	{
		System.out.println("Telegram Update Reader: "+reader.getState()+" ("+reader.getPhase()+")");
		System.out.println("TCP broker (port="+tcpDaemon.getPort()+"): "+tcpDaemon.getState());
		System.out.println("Subscribers: "+subscribers);
		System.out.println();
	}

/*
	private static boolean startserver()
	{

		try
		{
			// setup the socket address
			InetSocketAddress address = new InetSocketAddress(9400);

			// initialise the HTTPS tcpDaemon
			HttpsServer httpsServer = HttpsServer.create(address, 0);
			SSLContext sslContext = SSLContext.getInstance("TLS");

			// initialise the keystore
			char[] password = "simulator".toCharArray();
			KeyStore ks = KeyStore.getInstance("JKS");
			FileInputStream fis = new FileInputStream("lig.keystore");
			ks.load(fis, password);

			// setup the key manager factory
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, password);

			// setup the trust manager factory
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);

			// setup the HTTPS context and parameters
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
				public void configure(HttpsParameters params) {
					try {
						// initialise the SSL context
						SSLContext context = getSSLContext();
						SSLEngine engine = context.createSSLEngine();
						params.setNeedClientAuth(false);
						params.setCipherSuites(engine.getEnabledCipherSuites());
						params.setProtocols(engine.getEnabledProtocols());

						// Set the SSL parameters
						SSLParameters sslParameters = context.getSupportedSSLParameters();
						engine.setSSLParameters(sslParameters);

					} catch (Exception ex) {
						System.out.println("Failed to create HTTPS port");
					}
				}
			});
			httpsServer.createContext("/update", new Dispatcher());
			httpsServer.setExecutor(null); // creates a default executor
			httpsServer.start();
		}
		catch (Exception e){e.printStackTrace();return false;}
		return true;
	}
*/

}
