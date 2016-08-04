package plain;
import java.util.ArrayList;

public class Table {
	
	private ArrayList<Line> lines;
	
	
	public Table(){
		this.lines = new ArrayList<Line>();
	}
	
	public Table(ArrayList<Table> tables){
		this.lines = new ArrayList<Line>();
		for(Table table : tables){
			this.lines.addAll(table.getLines());
		}
	}
	
	public void addLine(Line line){
		this.lines.add(line);
	}
	
	
	public Line getLine(int num){
		return this.lines.get(num);
	}
	
	public int getSize(){
		return this.lines.size();
	}
	
	public ArrayList<Line> getLines(){
		return this.lines;
	}
	
	
	public static Table buildBestTable(ArrayList<Table> tables, int lastPos, boolean isMainGoo, boolean leaveEmpty, boolean isBestLineToTop){ //на вход эррейлист сырых табличек из CSV, крайн€€ позици€ и boolean €вл€етс€ ли главным столбиком google
		
		Table result = new Table();
		int tabSize = tables.get(0).getSize();
		
		for(int i = 0; i < tabSize; i++){ // в этом цикле по пор€дку проходимс€ по строчкам параллельно из таблиц, выбира€ лучшую
			Line bestLine = null;
			for(Table tab : tables){
				Line tempLine = tab.getLine(i);
				
				if(tempLine.getYa() == -1){ // -1 мен€ем на 60 дл€ удобности сравнени€ позиций
					tempLine.setYa(60);
				}
				if(tempLine.getGoo() == -1){
					tempLine.setGoo(60);
				}
				
				if(bestLine == null){ //перва€ из сравниваемых строчек по-умолчанию лучша€
					bestLine = tempLine;
				}else{
					if(!isMainGoo){	//если главна€ таблица не √угл, то сортируем таблицу с приоритетом по столбцу яндекса, иначе - по √углу
						if(tempLine.getYa() > bestLine.getYa()){ //если яндекс поз. выше чем у лучшей строчки, то сразу пропускаем дальнейшие сравнени€
							continue;
						}else if(tempLine.getYa() < bestLine.getYa()){ //если янд. поз. лучше, то строчка соответственно лучша€
							bestLine = tempLine;
						}else if(tempLine.getYa() == bestLine.getYa()){ // при одинаковых янд. свер€ем позиции по √углу
							if(tempLine.getGoo() < bestLine.getGoo()){
								bestLine = tempLine;
							}else{
								continue; // нет так нет
							}
						}
					}else{
						if(tempLine.getGoo() > bestLine.getGoo()){
							continue;
						}else if(tempLine.getGoo() < bestLine.getGoo()){
							bestLine = tempLine;
						}else if(tempLine.getGoo() == bestLine.getGoo()){
							if(tempLine.getYa() < bestLine.getYa()){
								bestLine = tempLine;
							}else{
								continue; // нет так нет
							}
						}
					}
					
				}
			}
			//сюда. ƒобавл€ть ли безрезультатные строки
			if(!leaveEmpty){
				if(!isMainGoo){
					if(bestLine.getYa() == 60 | bestLine.getYa() > lastPos) continue;
					/*если из всех сравниваемых строк не нашлось хоть одной с позицией яндекс не -1 
					и не > lastPos, то не добавл€ем; (continue - перейсти сразу в начало следующей итерации цикла) */
					
				}else{
					if(bestLine.getGoo() == 60 | bestLine.getGoo() > lastPos) continue; //аналогично, но при сортировке по √углу
				}
			}
				
			result.addLine(bestLine);
		}
		
		if(isBestLineToTop){
			result.bestLinetoTop(isMainGoo);
		}
		
		
		return result;
	}
	
	
	public void plusTable(Table table){
		lines.addAll(table.getLines());
	}
	
	public void bestLinetoTop(boolean isMainGoo){
		Line bestLine = null;
		if(lines.isEmpty()) return;
		for(Line line : lines){
			if(bestLine == null){
				bestLine = line;
				continue;
			}
			if(!isMainGoo){
				if(line.getYa() < bestLine.getYa()){
					bestLine = line;
				}else if(line.getYa() == bestLine.getYa()){
					if(line.getGoo() < bestLine.getGoo()){
						bestLine = line;
					}
				}
			}else{
				if(line.getGoo() < bestLine.getGoo()){
					bestLine = line;
				}else if(line.getGoo() == bestLine.getGoo()){
					if(line.getYa() < bestLine.getYa()){
						bestLine = line;
					}
				}
			}
			
		}
		lines.remove(bestLine);
		lines.add(0, bestLine);
	}
	
}
