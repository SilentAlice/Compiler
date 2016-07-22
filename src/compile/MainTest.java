package compile;

public class MainTest {


	public static void main(String[] args) {
		FileHandler fileHandler = new FileHandler("T:/Compile");
		FSM fsm = new FSM(fileHandler.reserve, fileHandler.token, fileHandler.input);
		
		try {
			fsm.Start();
			fsm.outPut();;
			Analyzer analyzer = new Analyzer(fsm.getTuples());
			analyzer.Run();
			analyzer.outPut();
			analyzer.outPut(fsm.getFolderPath());
			analyzer.outErrors(fsm.getFolderPath());
		} catch (Exception e) {e.printStackTrace();};

	}

}
