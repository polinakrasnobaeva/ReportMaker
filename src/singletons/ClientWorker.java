package singletons;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map.Entry;


public class ClientWorker {

	private static ClientWorker instance;
	private ArrayList<Entry<String, String>> clients;
	private File clientFile;
	
	public static synchronized ClientWorker getInstance(String path){
		if (instance == null){
			instance = new ClientWorker(path);
		}
		return instance;
	}
	
	private class ClientComparator implements Comparator<Entry<String, String>>{
		@Override
		public int compare(Entry<String, String> o1, Entry<String, String> o2) {
			return o1.getKey().compareTo(o2.getKey());
		}
	}
	
	private ClientWorker(String path){
		File clientFile = new File(path);
		if(!clientFile.exists()){
			return;
		}
		this.clientFile = clientFile;
		ArrayList<Entry<String, String>> result = new ArrayList<>();
		String buf;
		
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(clientFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(fis, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		BufferedReader bReader = new BufferedReader(isr);
	

		String clSite;
		String clName;
		int i;
		try {
			while( (buf = bReader.readLine()) != null){
				i = buf.indexOf('!');
				clSite = buf.substring(0, i);
				clName = buf.substring(i+1, buf.length());
				result.add(new AbstractMap.SimpleEntry<String, String>(clSite, clName));
				//result.put(clSite, clName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		result.sort(new ClientComparator());
		
		this.clients = result;
	}

	public ArrayList<Entry<String, String>> getClients(){
		return this.clients;
	}
	
	public synchronized void addClient(String clName, String clSite){
		boolean b = true;
		for(Entry<String, String> e : this.clients){
			if(e.getKey().equals(clSite)){
				b = false;
				break;
			}
		}
		if(b){
			this.clients.add(new AbstractMap.SimpleEntry<String, String>(clName, clSite));
			rewriteClientFile();
		}
	}
	
	private synchronized void rewriteClientFile(){
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(this.clientFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		OutputStreamWriter osw = null;
		try {
			osw = new OutputStreamWriter(fos, "utf-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		BufferedWriter bw = new BufferedWriter(osw);
		
		for(Entry<String, String> cl : this.clients){
			try {
				bw.write(cl.getKey() + "!" + cl.getValue() + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
