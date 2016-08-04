package plain;

public class Line {
	//Объект класса Line содержит всего три поля - фраза, и две позиции
		private String string;
		private int ya;
		private int goo;

		
		public Line(String string, int ya, int goo){
			this.string = string;
			this.ya = ya;
			this.goo = goo;
		}

		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}

		public int getYa() {
			return ya;
		}

		public void setYa(int ya) {
			this.ya = ya;
		}

		public int getGoo() {
			return goo;
		}

		public void setGoo(int goo) {
			this.goo = goo;
		}
		
		public void printToConsole(){
			System.out.println(this.string + "\t" + this.ya + "\t" + this.goo);
		}
		
	
	}