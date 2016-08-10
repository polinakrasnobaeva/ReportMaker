package summCounterModule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedHashMap;

public class Reacher {
	private String path;
	private String siteName;
	
	public Reacher(String path, String siteName){
		this.path = path;
		this.siteName = siteName;
	}
	
	public boolean exists(){
		File f = new File(this.path + File.separator + siteName + ".txt");
		return f.exists();
	}
	
	public LinkedHashMap<Long, Float> loadStats(){
		if(!exists()){
			return null;
		}
		LinkedHashMap<Long, Float> result = new LinkedHashMap<>();
		
		BufferedReader bReader = null;
		try {
			FileInputStream fis = new FileInputStream(this.path + File.separator + siteName + ".txt");
			InputStreamReader isr = new InputStreamReader(fis, "utf-8");
			bReader = new BufferedReader(isr);
			
			String buf = null;
			while( (buf = bReader.readLine()) != null){
				String s[] = buf.split("\t", 2);
				result.put(Long.valueOf(s[0]), Float.valueOf(s[1]));
			}
			
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static boolean createFile(String path){
		File f = new File(path);
		try {
			return f.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public static boolean fileExists(String path){
		File f = new File(path);
		return f.exists();
	}

	public static void pushStats(String path, long timeMillis, float percent){
		if(!fileExists(path)){
			createFile(path);
		}
		
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(path, true)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    out.println(timeMillis + "\t" + percent);
	    out.close();
	}
}
