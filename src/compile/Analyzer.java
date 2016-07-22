package compile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;

class Analyzer {

	
	// Info goten from FSM
	private ArrayList<Tuple> tuples;
	
	// Attributes of Analyzer
	private int IP; // Indicate the position of the tuples (from 0)
	private ArrayList<Quaternary> quats; // Quaternaries
	private ArrayList<String> errors;	// Errors
	private Tuple tempTuple; // Code1: Reserve Code2: Token Code3: Number Code4: ID 
	private HashSet<String> idSet;	// Record defined ID
	private HashSet<T> TSet;	// Resource pool T
	private int Tnum; // Record numbers of Ts
	
	public Analyzer(ArrayList<Tuple> tupleArray) {
		// FSM trans info
		this.tuples = tupleArray;
		
		// Initialize
		IP = 0;
		quats = new ArrayList<Quaternary>();
		errors = new ArrayList<String>();	
		idSet = new HashSet<String>();
		TSet = new HashSet<T>();
		Tnum = 0;
	}
	
	// Get next tuple
	private void advance() {
		if(IP < tuples.size()) {
			tempTuple = tuples.get(IP++);
		} else {
			// Error abnormal End
			errorHandle(3);
			this.outErrors("T:/Compile");
			System.exit(0);
		}
	}
	
	public void Run() {
		advance();
		Start();
		
	}
	
	// Start -> <Program> ID; SDefine; <begin> Sequences <end>
	public void Start() {
		if(tempTuple.strContent.equals("program")) {
			advance();
			if(tempTuple.code == 4) { // Get a ID for program
				advance();
			} else {
				errorHandle(1); // Lack an ID
				if(!tempTuple.strContent.equals("begin")) // Not a begin
					advance();
			}
			
			if(tempTuple.strContent.equals(";")) {
				advance();
			} else {
				// Error : lack of ;
				errorHandle(2);
			}
			
			SDefine();
			
			if(tempTuple.strContent.equals("begin")) {
				// Begin Sequences
				advance();
				Sequences();
				if(tempTuple.strContent.equals("end")) {
					// Finish
					outSyt("³ÌÐò¶¨Òå");
				} else {
					// Error No end
					errorHandle(4);
				}
				
			} else { // Lack of ;
				errorHandle(2);
				while(tempTuple.code != 1) {
					advance(); // find next Sequence
				}
			}	
		}
	}
	
	// Sequences -> Sequence <;> | Sequence <;> Sequences
	private Msg Sequences() {
		Msg msg = new Msg('S');
		while((!tempTuple.strContent.equals("end")) && (tempTuple.code == 1 || tempTuple.code == 4)) {
			Sequence();
			if(tempTuple.strContent.equals(";")) {
				advance();
				if(tempTuple == null) {
					// End
					return msg;
				}
			}
			else {
				// Error Lack of ;
				//errorHandle(2);	
			}
		}
		return msg;
	}
	
	// Sequence -> SAssignment | SDefine | SIf | SWhile | SDo
	private Msg Sequence() {
		Msg msg = new Msg('S');
		msg.nextlist = null;
		if(tempTuple.strContent.equals("var")) {
			// SDefine
			SDefine();
		} else if(tempTuple.code == 4) {
			// SAssignment
			SAssignment();
		} else if(tempTuple.strContent.equals("if")) {
			// SIf
			SIf();
		} else if(tempTuple.strContent.equals("while")) {
			// SWhile
			SWhile();
		} else  {
			// Error Not a sequence
			errorHandle(5);
			while(!tempTuple.strContent.equals(";"))
				advance();	// Find next sentence
			advance(); // Read ;
			msg = Sequence();
		}
		return msg;
	}
	
	// SDefine -> <var> <int> ID { ,ID }
	private Msg SDefine() {
		Msg msg = new Msg('S');
		msg.nextlist = null;
		advance();
		if(tempTuple.strContent.equals("int")) {
			advance();
		} else {
			// Error: Invalid type 
			errorHandle(6);
			while(tempTuple.code != 4) { // Get next ID
				advance();
			}
		}
		
		while(tempTuple.code == 4) {
			if(idSet.add(tempTuple.strContent)) // New ID
				advance();
			else {
				// Error Replicate ID
				errorHandle(7);
				advance();
			}
			if(tempTuple.strContent.equals(";")) { // Sentence end
				advance();
				break;
			}
			if(tempTuple.strContent.equals(",")) // Next ID
				advance();
		}
		outSyt("ÉùÃ÷Óï¾ä");
		return msg;
	}
	
	// SAssignment -> ID <:=> EXP
	private Msg SAssignment() {
		Msg msg = new Msg('S');
		msg.nextlist = null;
		if(idSet.contains(tempTuple.strContent)) {
			// Has defined
			Quaternary newQuat = new Quaternary(":=","","-",tempTuple.strContent);
			advance();	// Read ID
			if(tempTuple.strContent.equals(":=")) {
				advance(); // Read :=
			} else {
				// Error : Single ID;
				errorHandle(8);
				advance(); // read this ID
				outSyt("¸³ÖµÓï¾ä");
				return msg;
			}
			
			T tem = EXP().value;
			if(tem.isTemp) { // Is a temp T
				newQuat.s2 = "T"+tem.number;
			} else { // Is a value
				newQuat.s2 = tem.strValue;
			}
			quats.add(newQuat);
			putT(tem);	// This T is used
		} else { 
			// Error: Undefined ID
			errorHandle(9);
			while(!tempTuple.strContent.equals(";"))
				advance(); 	// Get next sentence
			return null;
			
		}
		outSyt("¸³ÖµÓï¾ä");
		return msg;
	}
	
	// SIf -> IF BEXP Then <begin Sequences end | Sequence > [ ELSE < begin Sequences end | Sequence > ]
	private Msg SIf() {
		
		Msg msg = new Msg('S');
		advance();	// Read IF
		Msg msgB = BEXP();
		if(tempTuple.strContent.equals("then")) {
			advance();
		} else { // Lack of then
			// Error Lack of then
			errorHandle(10);
			
		}
		int M1 = quats.size();
		Msg msgS1;
		if(tempTuple.strContent.equals("begin")) { // A sequence block
			advance();
			msgS1 = Sequences();
			advance();					// This is end
		} else {						// Just one Sequence
			msgS1 = Sequence();
			if(tempTuple.strContent.equals(";"))
				advance(); // Read a ;
			else {
				// Error Lack of ;
				errorHandle(2);
			}
			
		}
		
		if(tempTuple.strContent.equals("else")) {	// Has else
			Msg msgN = new Msg('N');
			msgN.nextlist = new List();
			msgN.nextlist.pQuat = quats.size();
			Quaternary newQuat = new Quaternary("J","-","-","0");
			quats.add(newQuat);
			
			advance();
			
			int M2 = quats.size();
			Msg msgS2;
			
			if(tempTuple.strContent.equals("begin")) {	// Sequence block
				advance();
				msgS2 = Sequences();
				advance();				// End
			} else {
				msgS2 = Sequence();
			}
			
			BackPatch(msgB.truelist, M1);
			BackPatch(msgB.falselist, M2);
			msg.nextlist = Merge(msgS1.nextlist, msgN.nextlist, msgS2.nextlist);
			
			outSyt("Ìõ¼þÓï¾ä");
			return msg;
			
		} else {						// Has not else
			BackPatch(msgB.truelist, M1);
			msg.nextlist = Merge(msgB.falselist, msgS1.nextlist);
			
			outSyt("Ìõ¼þÓï¾ä");
			return msg;
		}
	}
	
	// SWhile -> while BEXP do < begin Sequences end | Sequence > 
	private Msg SWhile() {
		
		Msg msg = new Msg('S');
		advance();	// Read while
		int M1 = quats.size();
		Msg msgB = BEXP();
		Msg msgS;
		int M2 = quats.size();
		if(tempTuple.strContent.equals("do")) {
			advance();	// Read do
			if(tempTuple.strContent.equals("begin")) {	// Sequence block
				advance();	// Read begin
				msgS = Sequences();
				advance();	// Read end
			} else {	// Just one sequence
				msgS = Sequence();
			}
			
			BackPatch(msgB.truelist, M2);
			msg.nextlist = msgB.falselist;
			BackPatch(msgS.nextlist, M1);
			Quaternary newQuat = new Quaternary("J","-","-",String.valueOf(M1));
			quats.add(newQuat);
			
			outSyt("Ñ­»·Óï¾ä");
			return msg;
			
		} else {
			// Error: lack of do
			errorHandle(11);
			return msg;
		}
		
	}
	
	private void errorHandle(int errorCode) {
		switch (errorCode) {
		case 0: // Not a start
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tNot a start");
			// It will find the start
			while(tempTuple!= null && !tempTuple.strContent.equals("program")) {
				advance();
			}
			if(tempTuple != null) {
				Start();	// Find start of the program
			}
			// Not find start. End
			break;
		case 1: // Lack an ID
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tMiss An ID");
			break;
		case 2: // Lack of ;
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tMiss ;");
			break;
		case 3: // Abnormal End
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tAbnormal End");
			break;
		case 4: // Miss End
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tMiss End");
			break;
		case 5: // Not A Sequence
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tNot A Sequence");
			break;
		case 6: // Invalid Type (not int)
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tInvalid Type");
			break;
		case 7: // Replicate Definition
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tReplicate Definition");
			break;
		case 8: // Single ID
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tSingle ID");
			break;
		case 9: // Undefined ID
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tUndefined ID");
			break;
		case 10: // Miss Then
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tMiss Then");
			break;
		case 11: // Miss Do
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tMiss Do");
			break;
		case 12: // Miss )
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tMiss )");
			break;
		case 13: // Not An Expression
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tNot An Expression");
			break;
		case 14: // Operation Error
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tOperation Error");
			break;
		case 15: // Miss Operation
			errors.add(tempTuple.xy.row + "\t" + tempTuple.xy.col + "\tMiss Operation");
			break;
		}
	}

	// EXP -> EXP1 { +EXP1 | -EXP1 } 
	private Msg EXP() {
		Msg msg1 = EXP1();
		while(tempTuple.strContent.equals("+") || tempTuple.strContent.equals("-")) {
			T t3 = getT();
			Quaternary newQuat = new Quaternary(tempTuple.strContent,"","","T"+t3.number); 
			
			advance();
			Msg msg2 = EXP1();
			T t1 = msg1.value;
			T t2 = msg2.value;
			
			if(t1.isTemp) { 
				newQuat.s2 = "T"+t1.number;
			} else {
				newQuat.s2 = t1.strValue;
			}
			putT(t1);
			
			if(t2.isTemp)
				newQuat.s3 = "T"+t2.number;
			else
				newQuat.s3 = t2.strValue;
			putT(t2);
			
			quats.add(newQuat); // New Quat
			
			msg1.value = t3;
		}
		 
		if(tempTuple.strContent.equals(";"))	// EXP end
			return msg1;
		else {
			// Error lack of ;
			return msg1;
			
		}	
		
	}
	
	// EXP1 -> EXP2 { *EXP2 | /EXP2 }
	private Msg EXP1() {
		Msg msg1 = EXP2();
		while(tempTuple.strContent.equals("*") || tempTuple.strContent.equals("/")) {
			T t3 = getT();
			Quaternary newQuat = new Quaternary(tempTuple.strContent,"","","T"+t3.number); 
			
			advance();
			Msg msg2 = EXP1();
			T t1 = msg1.value;
			T t2 = msg2.value;
			
			if(t1.isTemp) { 
				newQuat.s2 = "T"+t1.number;
			} else {
				newQuat.s2 = t1.strValue;
			}
			putT(t1);
			
			if(t2.isTemp)
				newQuat.s3 = "T"+t2.number;
			else
				newQuat.s3 = t2.strValue;
			putT(t2);
			
			quats.add(newQuat); // New Quat
			
			msg1.value = t3;
		}
		 
		if(tempTuple.strContent.equals(";"))	// EXP end
			return msg1;
		else {
			// Error lack ;
			return msg1;
		}	
		
	}
	
	// EXP2 -> i | (EXP) | ID
	private Msg EXP2() {
		Msg msg = new Msg('T');
		
		if(tempTuple.code == 4) { // ID
			T temT = getT();
			if(!idSet.contains(tempTuple.strContent)) {
				// Undefined ID
				errorHandle(9);
			}
			
			temT.strValue = tempTuple.strContent;
			temT.isTemp = false;
			msg.value = temT;
			advance();
			
			return msg;
		} else if(tempTuple.code == 3) { // Number
			T temT = getT();
			temT.strValue = tempTuple.strContent;
			temT.isTemp = false;
			msg.value = temT;
			advance();
			return msg;
		} else if(tempTuple.strContent.equals("(")) { // (EXP)
			advance();	// Read (
			msg = EXP();
			if(tempTuple.strContent.equals(")"))
				advance();	// Read )
			else {
				// Error lack of ")"
				errorHandle(12);
			}
			return msg;
		} else { // Error Not a right expression
			errorHandle(13);
			T temT = getT();
			temT.strValue = "NULL";
			temT.isTemp = false;
			msg.value = temT;
			return msg;
		}
	}
	
	// BEXP -> BEXP1 { and BEXP1 | or BEXP1 }
	private Msg BEXP() {
		Msg msg = BEXP1();
		while(tempTuple.strContent.equals("and") || tempTuple.strContent.equals("or")) {
			int M = quats.size();
			String op = tempTuple.strContent;
			advance();
			Msg msg2 = BEXP1();
			if(op.equals("and")) {
				msg.falselist = Merge(msg.falselist, msg2.falselist);
				BackPatch(msg.truelist,M);
				msg.truelist = msg2.truelist;
			} else { // or
				msg.truelist = Merge(msg.truelist, msg2.truelist);
				BackPatch(msg.falselist,M);
				msg.falselist = msg2.falselist;
			}
		}
		
		return msg;
	}
	
	// BEXP1 -> not BEXP1 | BEXP2
	private Msg BEXP1() {
		Msg msg;
		if(tempTuple.strContent.equals("not")) {
			advance();	// Read not
			msg = BEXP1();
			List tempList = msg.truelist;
			msg.truelist = msg.falselist;
			msg.falselist = tempList;
		} else { // Not a not
			msg = BEXP2();
		}
		return msg;
	}
	
	// BEXP2 -> EXP > EXP | EXP < EXP | EXP >= EXP | EXP <= EXP | EXP == EXP | EXP <> EXP
	private Msg BEXP2() {
		Msg msg = new Msg('B'); // Boolean expression
		Msg msg1 = EXP();
		String strOP = tempTuple.strContent;
		if(tempTuple.code == 2) {	// Token
			if(strOP.equals(">") || strOP.equals("<") || strOP.equals(">=") || 
					strOP.equals("<=") || strOP.equals("==") || strOP.equals("<>")) {
				strOP = tempTuple.strContent;
			} else {
				// Error: Not a right OP
				errorHandle(14);
				strOP = tempTuple.strContent;
			}
			
		} else {
			// Error: lack of OP
			errorHandle(15);
			strOP = "null";
		}
		
		advance();
		Msg msg2 = EXP();
		Quaternary quat1 = new Quaternary("","","","0");
		Quaternary quat2 = new Quaternary("J","-","-","0");
		msg.truelist = new List();
		msg.falselist = new List();
		
		quat1.s1 = "J"+strOP;
		
		T tempT = msg1.value;
		if(tempT.isTemp) { // Is a temp Value
			quat1.s2 = "T"+tempT.number;
		} else {
			quat1.s2 = tempT.strValue;
		}
		putT(tempT);
		tempT = msg2.value;
		if(tempT.isTemp) { // Is a temp value
			quat1.s3 = "T"+tempT.number;
		} else {
			quat1.s3 = tempT.strValue;
		}
		putT(tempT);
		
		msg.truelist.pQuat = quats.size();
		quats.add(quat1);
		msg.falselist.pQuat = quats.size();
		quats.add(quat2);
		
		return msg;
	}
	
	// l2'end -> l1, return l2
	private List Merge(List l1, List l2) {
		
		if(l1 == null && l2 == null) {
			return null;
		} else if(l1 == null) {
			return l2;
		} else if(l2 == null) {
			return l1;
		}
		
		List list = l2;
		while(list.next != null)
			list = list.next;
		
		list.next = l1;
		quats.get(list.pQuat).s4 = String.valueOf(l1.pQuat);
		return l2;
	}
	
	// l3 -> l2 -> l1, return l3
	private List Merge(List l1, List l2, List l3) {
		return Merge(Merge(l1,l2),l3);
	}
	
	// back patch l1 with intPQuat
	private void BackPatch(List l1, int pQuat) {
		if(l1 == null) {
		} 
		if(pQuat < 0) {
			// TODO Error invalid quaternary position
		}
		
		List list = l1;
		while(list!= null) {
			quats.get(list.pQuat).s4 = String.valueOf(pQuat);
			list = list.next;
		}
	}
	
	private class Msg {
		char type; // T,M,S,N,B
		List nextlist = null;
		List truelist = null;
		List falselist = null;
		T value;
		
		Msg(char type) {
			this.type = type;
		}
	}
	
	private class T {  
		int number;
		String strValue;
		boolean isTemp = true;
		T(int number) {
			this.number = number;
		}
		
	}
	
	// Source pool
	private T getT() {
		if(TSet.isEmpty()) {
			T t = new T(Tnum++);
			return t;
		} else { // Tset has T
			T t = TSet.iterator().next();
			TSet.remove(t);
			return t;
		}
	}
	
	private void putT(T t) {
		t.isTemp = true;
		t.strValue = "";
		TSet.add(t);
		t = null;
	}
	
	private class List {
		int pQuat = -1;
		List next = null;
	}

	public void outPut() {
		int num=0;
		for(int i=0; i<quats.size(); i++) {
			Quaternary q = quats.get(i);
			System.out.println(num++ + "\t" + q.s1+"\t"+q.s2+"\t"+q.s3+"\t"+q.s4);
		}
	}
	
	public void outPut(String filePath) {
		File quatFile = new File(filePath+"\\quats.txt");
		try {
			FileOutputStream out = new FileOutputStream(quatFile);
			out.write("LineNum\tQuaternaries\r\n".getBytes());
			int num = 0;
			for(Quaternary q : quats) {
				out.write((num++ + "\t(" + q.s1 + "\t" + q.s2 + "\t" + q.s3 + "\t" + q.s4 + ")\r\n").getBytes());
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void outSyt(String str) {
		System.out.println("Row:" + tempTuple.xy.row + "\t" + str);
	}
	
	public void outErrors(String folderpath) {
		File errorFile = new File(folderpath+"\\errors.txt");
		try {
			FileWriter writer = new FileWriter(errorFile, true); // Append
			errors.forEach(x->{
				try {
					writer.write(x+"\r\n");
				} catch (Exception e) {
				e.printStackTrace();
				}});
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
