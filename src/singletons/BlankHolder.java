package singletons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import plain.Blank;
import plain.Optimizer;

public class BlankHolder {
	
	private static BlankHolder instance;
	
	private HashMap<String, Blank> blanks;
	
	public static synchronized BlankHolder getInstance(String path){
		if (instance == null){
			instance = new BlankHolder(path);
		}
		return instance;
	}
	
	private BlankHolder(String path){
		File folder = new File(path);
		if(!folder.exists()){
			return;
		}
		HashMap<String, Blank> result = new HashMap<String, Blank>();
		
		String fileName;
		String buf;
		
		for (File fileEntry : folder.listFiles()){
			if(fileEntry.isDirectory()){
			}else{
				if (fileEntry.isFile()) {
					fileName = fileEntry.getPath();
					if(fileName.endsWith(".txt")){
						FileInputStream fis = null;
						try {
							fis = new FileInputStream(new File(fileName));
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						InputStreamReader isr = null;
						try {
							isr = new InputStreamReader(fis, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						BufferedReader bReader = new BufferedReader(isr);
						
						Blank blank = new Blank(fileEntry.getName().replace(".txt", ""));
						try {
							blank.setFullName(buf = bReader.readLine());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						List<Optimizer> optimizers = new ArrayList<>();
						
						try {
							while( (buf = bReader.readLine()) != null){
								Optimizer opt = new Optimizer(buf);							
								optimizers.add(opt);
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						try {
							bReader.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						blank.setOptimizers(optimizers);
						
						result.put(blank.getName(), blank);
					}
				}
			}
		}
		this.blanks = result;
	}
	
	public HashMap<String, Blank> getBlanks(){
		return this.blanks;
	}
	
	
}
