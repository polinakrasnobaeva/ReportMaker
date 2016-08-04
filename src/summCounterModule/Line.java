package summCounterModule;

public class Line {
	//Объект класса Line содержит всего три поля - фраза, и две позиции
		private String string;
		private int ya;
		private int price;

		public Line(String string, int ya, int price){
			this.string = string;
			this.ya = ya;
			this.price = price;
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

		public int getPrice() {
			return price;
		}

		public void setPrice(int price) {
			this.price = price;
		}

		public void printToConsole(){
			System.out.println(this.string + "\t" + this.ya + "\t" + this.price);
		}
	}