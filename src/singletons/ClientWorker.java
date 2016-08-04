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
import java.util.HashMap;
import java.util.Map.Entry;


public class ClientWorker {

	private static ClientWorker instance;
	private HashMap<String, String> clients;
	private File clientFile;
	
	public static synchronized ClientWorker getInstance(String path){
		if (instance == null){
			instance = new ClientWorker(path);
		}
		return instance;
	}
	
	private ClientWorker(String path){
		File clientFile = new File(path);
		if(!clientFile.exists()){
			return;
		}
		this.clientFile = clientFile;
		HashMap<String, String> result = new HashMap<String, String>();
		
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
				result.put(clSite, clName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.clients = result;
	}

	public HashMap<String, String> getClients(){
		return this.clients;
	}
	
	public synchronized void addClient(String clName, String clSite){
		this.clients.put(clName, clSite);
		rewriteClientFile();
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
		
		for(Entry<String, String> cl : this.clients.entrySet()){
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
