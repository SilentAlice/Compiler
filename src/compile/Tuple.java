package compile;

class Tuple {
	
	int code = -1;
	String strContent = "";	// Define
	RowCol xy = new RowCol(0, 0);
	
	Tuple(int code, String strContent, final RowCol xy) {
		this.code = code;
		this.strContent = strContent;
		this.xy.col = xy.col;
		this.xy.row = xy.row;
	}
}