package edu.rutgers.vmimo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class CSVManager {

	public class CSVEntry{
		
		public String[] data;
		
		public CSVEntry(int size, String line){
			data = new String[size];
			String part = "";
			int count = 0;
			for(int i = 0; i < line.length(); i ++){
				if(line.charAt(i) != ',') part += line.charAt(i);
				else{
					data[count ++] = part;
					part = "";
				}
			}
			if(part.length() != 0) data[count ++] = part;
		}
		
		public String toString(){
			String result = "";
			for(String s : data) result += s + ",";
			if(result.length() > 0) result = result.substring(0, result.length() - 1); // Get rid of the last comma.
			return result;
		}
		
	}
	
	public ArrayList<CSVEntry> entries;
	private String fileName;
	private File file;
	
	public CSVManager(String fileName){
		this.fileName = fileName;
		file = new File(fileName);
		try{
			file.createNewFile();
		}catch(Exception e){
			e.printStackTrace();
			file = null;
		}
		entries = new ArrayList<CSVEntry>();
	}
	
	public void addEntry(String s){
		int numEntries = (s.length() - s.replaceAll(",", "").length()) + 1; // Subtract length without commas from length with commas, and add one = # commas + 1.
		entries.add(new CSVEntry(numEntries, s));
	}
	
	public void save(){
		if(file == null){
			System.out.println("Attempting to save to a file that doesn't exist: " + fileName);
			return;
		}
		
		PrintWriter writer;
		try {
			writer = new PrintWriter(file, "UTF-8");
			for(CSVEntry entry : entries) writer.println(entry.toString());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public void load(){
		if(file == null){
			System.out.println("Attempting to load a file that doesn't exist: " + fileName);
			return;
		}
		
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = "";
			while( (line = reader.readLine()) != null){
				addEntry(line);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
