package broker;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.*;


public class SSLSocketServer extends Thread
{
	private int port;
	private boolean isServerDone = false;

	SSLSocketServer(int port){
		this.port = port;
		this.setName("SSL Socket Server");
	}

	// Create the and initialize the SSLContext
	private SSLContext createSSLContext(){
		try{
			KeyStore keyStore = KeyStore.getInstance("JKS");
			keyStore.load(new FileInputStream("lig.keystore"),"simulator".toCharArray());

			// Create key manager
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			keyManagerFactory.init(keyStore, "simulator".toCharArray());
			KeyManager[] km = keyManagerFactory.getKeyManagers();

			// Create trust manager
/*			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
			trustManagerFactory.init(keyStore);
			TrustManager[] tm = trustManagerFactory.getTrustManagers();
*/

			//Per ora creo un trustmanager che accetta tutto, altrimenti mi servono certificati
			TrustManager[] tm = new TrustManager[] {
					new X509TrustManager() {
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return new X509Certificate[0];
						}
						public void checkClientTrusted(
								java.security.cert.X509Certificate[] certs, String authType) {
						}
						public void checkServerTrusted(
								java.security.cert.X509Certificate[] certs, String authType) {
						}
					}
			};
			// Install the all-trusting trust manager
			try {
				SSLContext sc = SSLContext.getInstance("TLSv1.2");
				sc.init(null, tm, new java.security.SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
				return sc;
			} catch (GeneralSecurityException e) {
			}

/*			// Initialize SSLContext
			SSLContext sslContext = SSLContext.getInstance("TLSv1");
			sslContext.init(km,  tm, null);
			return sslContext;

 */
		} catch (Exception ex){
			ex.printStackTrace();
		}

		return null;
	}

	// Start to run the tcpDaemon
	public void run(){
		SSLContext sslContext = this.createSSLContext();

		try{
			// Create tcpDaemon socket factory
			SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

			// Create tcpDaemon socket
			SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(this.port);
			System.out.println(Arrays.toString(sslServerSocket.getEnabledProtocols()));
			System.out.println("SSL tcpDaemon started");
			while(!isServerDone){
				SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();

				// Start the tcpDaemon thread
				new ServerThread(sslSocket).start();
			}
		} catch (Exception ex){
			ex.printStackTrace();
		}
	}
}