package compile;

import java.io.File;

public class FileHandler {
	
	public File reserve;
	public File token;
	public File input;
	public FileHandler(String folderPath) {
		try {
			// Check every file
			File file = new File(folderPath);
			if(file.isDirectory()) {
				int fileNum = 0;
				File[] fileList = file.listFiles();
				for(File f : fileList) {
					switch (f.getName()) {
					case "reserve.ini":
						reserve = f;
						fileNum++;
						break;
					case "input.txt":
						input = f;
						fileNum++;
						break;
					case "token.ini":
						token = f;
						fileNum++;
						break;
					}	
				}
				if(fileNum < 3) {
					System.out.println("Files Not Enough!");
					throw(new Exception());
				}
			} else {
				throw(new Exception());
			}
		} catch (Exception e) {
			System.out.println("Directory or Files Error");
			return ;
		}
		
	}

}
