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
		Line l = null;
		try{
			l = lines.get(num);
		}catch(IndexOutOfBoundsException e){
			for(Line li : this.lines){
				System.out.println(li.getString());
			}
		}
		return l;
	}
	
	public int getSize(){
		return this.lines.size();
	}
	
	public ArrayList<Line> getLines(){
		return this.lines;
	}
	
	
	public static Table buildBestTable(ArrayList<Table> tables, int lastPos, boolean isMainGoo, boolean leaveEmpty, boolean isBestLineToTop){ //�� ���� ��������� ����� �������� �� CSV, ������� ������� � boolean �������� �� ������� ��������� google
		
		Table result = new Table();
		int tabSize = tables.get(0).getSize();
		
		for(int i = 0; i < tabSize; i++){ // � ���� ����� �� ������� ���������� �� �������� ����������� �� ������, ������� ������
			Line bestLine = null;
			for(Table tab : tables){
				Line tempLine = tab.getLine(i);
				
				if(tempLine.getYa() == -1){ // -1 ������ �� 60 ��� ��������� ��������� �������
					tempLine.setYa(60);
				}
				if(tempLine.getGoo() == -1){
					tempLine.setGoo(60);
				}
				
				if(bestLine == null){ //������ �� ������������ ������� ��-��������� ������
					bestLine = tempLine;
				}else{
					if(!isMainGoo){	//���� ������� ������� �� ����, �� ��������� ������� � ����������� �� ������� �������, ����� - �� �����
						if(tempLine.getYa() > bestLine.getYa()){ //���� ������ ���. ���� ��� � ������ �������, �� ����� ���������� ���������� ���������
							continue;
						}else if(tempLine.getYa() < bestLine.getYa()){ //���� ���. ���. �����, �� ������� �������������� ������
							bestLine = tempLine;
						}else if(tempLine.getYa() == bestLine.getYa()){ // ��� ���������� ���. ������� ������� �� �����
							if(tempLine.getGoo() < bestLine.getGoo()){
								bestLine = tempLine;
							}else{
								continue; // ��� ��� ���
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
								continue; // ��� ��� ���
							}
						}
					}
					
				}
			}
			//����. ��������� �� ��������������� ������
			if(!leaveEmpty){
				if(!isMainGoo){
					if(bestLine.getYa() == 60 | bestLine.getYa() > lastPos) continue;
					/*���� �� ���� ������������ ����� �� ������� ���� ����� � �������� ������ �� -1 
					� �� > lastPos, �� �� ���������; (continue - �������� ����� � ������ ��������� �������� �����) */
					
				}else{
					if(bestLine.getGoo() == 60 | bestLine.getGoo() > lastPos) continue; //����������, �� ��� ���������� �� �����
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
