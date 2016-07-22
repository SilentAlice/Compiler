package compile;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

public class FSM {

	private PushbackReader reader;					// PushBack Reader
	private RowCol xy;
	private StringBuilder word;						// Temp Word
	private Hashtable<String, Integer> reserveTable;// Code 1 reserve word
	private Hashtable<String, Integer> tokenTable;	// Code 2 token
	private HashSet<String> numberSet;				// Code 3 numbers
	private HashSet<String> idSet;					// Code 4 id
	
	private ArrayList<Tuple> tupleArray;			// Store Tuple
	private HashMap<RowCol, String> errorTable;		// RowCol, ErrorInfo
	private String folderPath;
	
	
	@SuppressWarnings("deprecation")
	public FSM(File reserve, File token, File input) {
		
		// Initialize
		xy = new RowCol(0, 0);
		word = new StringBuilder();
		reserveTable = new Hashtable<String, Integer>();
		tokenTable = new Hashtable<String, Integer>();
		tupleArray = new ArrayList<Tuple>();
		errorTable = new HashMap<RowCol, String>();
		numberSet = new HashSet<String>();
		idSet = new HashSet<String>();
		
		try{
			// Initialize reserve table
			DataInputStream in = new DataInputStream(new FileInputStream(reserve));
			int tem = 0;
			String strTem = "";
			while((strTem = in.readLine()) != null) {
				if(strTem == "")
					continue;
				reserveTable.put(strTem.trim().toLowerCase(), tem++);
			}
			in.close();
			
			// Initialize token table
			in = new DataInputStream(new FileInputStream(token));
			tem = 0;
			strTem = "";
			while((strTem = in.readLine()) != null) {
				if(strTem == "")
					continue;
				tokenTable.put(strTem.trim().toLowerCase(), tem++);
			}
			in.close();
			
			// Get current folder path
			folderPath = input.getParent();
			
			reader = new PushbackReader(new FileReader(input));
		} catch (Exception e) {
			System.out.println("File Error Exist, Please Check!");
			e.printStackTrace();
			return ;
		}
	}
	
	private boolean isSpace(int ch) {
		if(ch == ' ') {				// White Space
			xy.col ++;
			return true;
		} else if(ch == '\t') {		// Tab
			xy.col += 4;
			return true;
		} else if(ch == '\r') {		// Return Head
			xy.col = 0;
			return true;
		} else if (ch == '\n') {	// Next Line
			xy.row++;
			return true;
		}
		return false;				// Not a space
		
	}
	
	private boolean isLetter(int ch) {
		// IT's a letter
		if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z') {
			xy.col++;
			return true;
		}
//		if(Character.isLetter((char)ch))
//			return true;
		return false;
	}
	
	private boolean isNumber(int ch) {
		if (ch >= '0' && ch <= '9') {
			xy.col++;
			return true;
		}
		return false;
	}
	
	private boolean isToken(int ch) {
		if(		ch > 0	&&
				(!Character.isWhitespace((char)ch)) &&
				(ch < '0' || ch > '9')	&&
				(ch < 'a' || ch > 'z')  &&
				(ch < 'A' || ch > 'Z')
		) {
			xy.col ++;
			return true;
		}
		return false;
	}
	
	public void outPut() {
		try {
			// Output tuples
			File tuples = new File(folderPath+"\\tuples.txt");
			FileOutputStream out = new FileOutputStream(tuples);
			out.write("Row\tCol\t(Code,Value)\r\n".getBytes());
			for(Tuple t : tupleArray) {
				out.write((t.xy.row + "\t" + t.xy.col + "\t" + "(" + t.code + "," + t.strContent + ")\r\n").getBytes());
			}
			out.close();
			
			int temNum = 0;
			// Output numbers
			File numbers = new File(folderPath+"\\numbers.txt");
			out = new FileOutputStream(numbers);
			out.write("Num\tUserNum\r\n".getBytes());
			for(String n : numberSet) {
				out.write((++temNum +"\t"+ n + "\r\n").getBytes());
			}
			out.close();
			
			// Output IDs
			temNum = 0;
			File ids = new File(folderPath+"\\ids.txt");
			out = (new FileOutputStream(ids));
			out.write("Num\tIdentifers\r\n".getBytes());
			for(String i : idSet) {
				out.write((++temNum + "\t" + i + "\r\n").getBytes());
			}
			out.close();
			
			// Output Errors
			
			if(errorTable.size() > 0) {
				temNum = 0;
				File errors = new File(folderPath+"\\errors.txt");
				final FileOutputStream errorOut = new FileOutputStream(errors);
				errorTable.forEach((xy,s)->{ 
					try { 
						errorOut.write((xy.row + ",\t" + (xy.col-1) + " :\t" + s +"\r\n").getBytes()); 
						} catch(Exception e){} } 
				);
				errorOut.close();
			}
		
		} catch (Exception e) {
			
		}
		
	}

	public void Start() throws IOException {
		int ch = 0;	// Temp char
	outer:while((ch = reader.read()) != -1 && ch != 65535) {
			
			// It's a space
			if(isSpace(ch)) { continue; }
			
			// Judge ID start with letter, contains numbers and letters----------------------------
			else if(isLetter(ch)) {
				xy.col--;	// Judge ch again;
				while (isLetter(ch) || isNumber(ch)) {
					word.append((char)ch);
					ch = reader.read();
				}
				
				// T2
				reader.unread(ch);
				
				// Is it a reserve word?
				if(reserveTable.containsKey(word.toString().toLowerCase())) {
					tupleArray.add(new Tuple(1, word.toString().toLowerCase(), new RowCol(xy.row, xy.col-word.length())));
					
				} else { // It's a ID
					idSet.add(word.toString().toLowerCase());
					tupleArray.add(new Tuple(4, word.toString().toLowerCase(), new RowCol(xy.row, xy.col-word.length())));
				}
				word.delete(0, word.length());
				continue outer;
				
			} 
			// Judge Numbers --------------------------------------------------
			else if(isNumber(ch)) {	
				xy.col--;
				while(isNumber(ch)) {
					word.append((char)ch);
					ch = reader.read();
				}
				// T4
				
				if(Character.isLetter(ch)) {
					errorTable.put(new RowCol(xy.row, xy.col-1), "Not an identifier: " + word.toString() + String.valueOf((char)ch) );
					reader.unread(ch);
					word.delete(0, word.length());
					continue outer;
				}
				reader.unread(ch);
				
				
				numberSet.add(word.toString());
				tupleArray.add(new Tuple(3,word.toString(),new RowCol(xy.row, xy.col-word.length())));
				word.delete(0, word.length());
				
				
				continue outer;
			} 
			
			// Judge annotation
			else if(ch == '/') {
				ch = reader.read();
				if(ch == '/') {			// It's //
					xy.col += 2;
				inner:while(true) {
						ch = reader.read();
						if(ch == '\r' || ch == -1)
							break inner;
						
						xy.col++;
						
						if(ch == '\t')
							xy.col += 3;
					}
					reader.unread(ch);
					continue outer; 	// Begin new while
				} else if (ch == '*') { // It's /*
					xy.col += 2;
					inner: while(true) {
						ch = reader.read();
						if(isSpace(ch))
							continue inner;
						else if(ch == '*') {
							ch = reader.read();
							xy.col ++;
							if(ch == '/') {
								xy.col ++;
								continue outer;
							} else {
								reader.unread(ch);
								continue inner;
							}
						} else if(ch == -1) {	// Abnormal end
							
							errorTable.put(new RowCol(xy.row, xy.col), "Abnormal End");
							
							reader.unread(ch);
							continue outer;
						} else {	// Ordinary text
							xy.col++;
						}	
					}
				} else {	// Division, trans it to tokenJudger
					xy.col ++;
					reader.unread(ch); // Pushback this
					
					if(tokenTable.containsKey("/")) {
						tupleArray.add(new Tuple(2,"/", new RowCol(xy.row, xy.col-1)));
						word.delete(0, word.length());
						continue outer;
					} else {
						errorTable.put(new RowCol(xy.row, xy.col-1), "Illegal Character: /");
						word.delete(0, word.length());
					}
					
				}
			}
			// Judge token -------------------------------------------------------
			else if(isToken(ch)) {
				xy.col--;
				while(isToken(ch)) {
					word.append((char)ch);
					ch = reader.read();
				}
				
				// T6
				reader.unread(ch);
				// It's a token
				if(tokenTable.containsKey(word.toString())) {
					tupleArray.add(new Tuple(2,word.toString(), new RowCol(xy.row, xy.col-word.length())));
					word.delete(0, word.length());
				}
				else {
					errorTable.put(new RowCol(xy.row, xy.col-1), "Illegal Character: " + word.toString());
					word.delete(0, word.length());
				} 
			}
	
		}
	}

	// Analyzer will use these
	ArrayList<Tuple> getTuples() {
		return tupleArray;
	}
	
	Hashtable<String, Integer> getReserveTable() {
		return reserveTable;
	}
	
	Hashtable<String, Integer> getTokenTable() {
		return tokenTable;
	}
	
	String getFolderPath() {
		return folderPath;
	}

}
