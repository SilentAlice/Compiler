package xjtu.se.compile;

public class Main {

	public static void main(String[] args) {
		FileHandler fileHandler = new FileHandler("T:/Compile");
		FSM fsm = new FSM(fileHandler.reserve, fileHandler.token, fileHandler.input);
		try {
			fsm.Start();
			fsm.outPut();
		} catch (Exception e) {e.printStackTrace();};
	}

}
