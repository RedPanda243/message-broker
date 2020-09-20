package broker;

import json.*;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

public class UpdateReader extends Thread
{
	private File storedfile = new File("stored");
	private boolean sent = false;
	private String phase;
	private final Object monitor;

	public UpdateReader(Object monitor)
	{
		this.monitor = monitor;
		phase = "created";
	}

	public void run()
	{

		try
		{
			JSONArray updates;
			while(true)
			{
				if (Main.countSubscribers()==0)
				{
					phase = "Waiting for subscribers";
					synchronized (monitor)
					{
						monitor.wait();
					}
				}
				if (storedfile.exists())
				{
					phase = "Loading from store";
					updates=loadFromStored();
				}
				else
				{
					phase = "Downloading updates";
					updates = getUpdates();
				}
				if (updates.size()>0) {
					phase = "Sending updates";
					sendToSubscribers(updates);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			phase = e.getClass().getName() + " thrown";
		}
	}

	public String getPhase()
	{
		return phase;
	}

	private static JSONArray getUpdates() throws JSONException, IOException {
		JSONObject r = send("getUpdates"); //prendo tutti gli updates non confermati
		JSONArray updates = r.get(JSONArray.class, "result");
		long lastindex;
		if (updates.size() > 0) {
			lastindex = updates.get(Integer.class, "" + (updates.size() - 1), "update_id");

			send("getUpdates?offset=" + (lastindex + 1));
			/*
			 * Con questa istruzione confermo l'ultimo messaggio (e i precedenti).
			 * L'array restituito non viene salvato ma contiene tutti i messaggi di updates
			 * piÃ¹ eventuali nuovi messaggi.
			 * Se esistono, tali messaggi non sono confermati a questo giro e quindi
			 * verranno catturati nuovamente alla prossima invocazione di send(getUpdates)
			 */
//			System.out.println(r.toString(3));
		}
		return updates;
	}

	private static JSONObject send(String req) throws JSONException, IOException
	{
		URL url=new URL("https://api.telegram.org/bot"+Main.bot_token+"/"+req);
		return new JSONObject(new InputStreamReader(url.openConnection().getInputStream()));
	}

	private void sendToSubscribers(JSONArray json) throws IOException
	{
		sent = false;
		ArrayList<Thread> a = new ArrayList<>();
		for (String sub:Main.getSubscribers())
		{
			a.add(new Dispatcher(sub,json));
			a.get(a.size()-1).start();
		}

		for (Thread t:a) //Attendo la terminazione dei dispatcher
		{
			try
			{
				t.join();
			}
			catch (InterruptedException e){}
		}

		if (!sent)
			store(json);
	}

	private JSONArray loadFromStored()
	{
		try
		{
			FileReader fr = new FileReader(storedfile);
			JSONArray a = new JSONArray(fr);
			fr.close();
			storedfile.delete();
			return a;
		}
		catch (IOException e)
		{
			return new JSONArray();
		}
	}

	private void store(JSONArray json) throws IOException
	{
		System.out.println("Storing "+json.toString(0));
		JSONArray stored = loadFromStored();
		stored.addAll(json);
		PrintWriter pw = new PrintWriter(new FileWriter(storedfile));
		pw.write(stored.toString(0));
		pw.close();
	}

	private class Dispatcher extends Thread
	{
		private String s;
		private JSONArray updates;
		public Dispatcher(String subscriber, JSONArray json)
		{
			s = subscriber;
			updates = json;
		}

		public void run()
		{
			try
			{
				String[] sp = s.split(":");
				Socket socket =  new Socket(sp[0], Integer.parseInt(sp[1]));
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				bw.write(updates.toString(0));
				bw.flush();
				sent = true;
			}
			catch (IOException e)
			{
				Main.removeSubscribers(s);
			}
		}
	}
}
