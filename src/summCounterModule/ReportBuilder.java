package summCounterModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.docx4j.openpackaging.exceptions.Docx4JException;

import Utils.StaticStrings;


public class ReportBuilder{
	
	private String csvFileName;
	private String priceFileName;

	private Line currentLine;
	private boolean eof = false; //end of file
	private CSVReader csvReader;
	private Table singlesForEtalon = null;
	
	private DOCXworker dw;
	
	private boolean isSingles;
	private boolean isRostov;
	private boolean isCities;
	private boolean isAddWords;
	
	private boolean isSinglesReach;
	private boolean isRostovReach;
	private boolean isCitiesReach;
	private boolean isAddWordsReach;

	private int topNum;
	private int leaveNum;
	
	private boolean isNeedTableChecking;
	private boolean isBestLineToTop;
	
	public int totalPhraseCount = 0;
	public int topPhraseCount = 0;

	private String promoRegion;
	
	public ReportBuilder(String csvFileName, String priceFileName, HashMap<String, String> values) {
		
		this.csvFileName = csvFileName;
		this.priceFileName = priceFileName;
		
		this.isSingles = values.containsKey("isSingles");
		this.isRostov = values.containsKey("isRostov");
		this.isCities = values.containsKey("isCities");
		this.isAddWords = values.containsKey("isAddWords");
		
		this.isSinglesReach = values.containsKey("isSinglesReach");
		this.isRostovReach = values.containsKey("isRostovReach");
		this.isCitiesReach = values.containsKey("isCitiesReach");
		this.isAddWordsReach = values.containsKey("isAddWordsReach");
	
		this.topNum = Integer.valueOf(values.get("topNum"));
		this.leaveNum = Integer.valueOf(values.get("leaveNum"));
		
		this.isNeedTableChecking = values.containsKey("isNeedTableChecking");
		this.isBestLineToTop = values.containsKey("isBestLineToTop");
		
		this.promoRegion = values.get("promoRegion");

	}
	
	
	public DOCXworker buildReport(String docExamplePath){

		csvReader = new CSVReader(this.csvFileName, this.priceFileName);

		//for DOCX
		dw = null;
		try {
			dw = new DOCXworker(docExamplePath);
		} catch (Docx4JException | JAXBException e) {
			e.printStackTrace();
		}

		doSingles(isSingles);
		doRostov(isRostov);
		doCities(isCities | isAddWords);
		
		csvReader.close();

		
		//��������� DOCX
		dw.removeTemplateTable();

		System.out.println("����������� ������ ������� ���������");
		System.out.println("����� ����: " + this.totalPhraseCount);
		System.out.println("� ��� ����: " + this.topPhraseCount);
		return dw;
	}
	
	
	/**
	 * ������ ������ �����
	 */

	private void doSingles(boolean isNeed){
		if(!isNeed)
			return;
		currentLine = csvReader.readLine();
		checkEOF(); 
	
		singlesForEtalon = new Table();
		
		while(!csvReader.isTableSeparator(currentLine.getString()) & !eof){ //���� ��������� ������ �� �����������		
			singlesForEtalon.addLine(currentLine);
			
			currentLine = csvReader.readLine();
			checkEOF();
			if(eof){
				break;
			}
		}
		
		ArrayList<Table> singles = new ArrayList<Table>(); 
		singles.add(singlesForEtalon);
		
		
		Table single = Table.buildBestTable(singles, this.leaveNum, this.isBestLineToTop);//���� ������������ ����� ��� ���� �������, ������� ���� ������ ��������� �� ���� ������� :D
		//��� �������� � .docx
		int top = dw.insertNewTable(single, StaticStrings.singlesString + this.promoRegion, this.topNum);
		
		if(this.isSinglesReach){
			this.totalPhraseCount += singlesForEtalon.getSize(); // ������� ���-�� ����� � �������, ���� ��� �� ������
			this.topPhraseCount += top;
		}
		
	}
	
	/**
	 * ������ ������
	 */

	private void doRostov(boolean isNeed){
		if(!isNeed)
			return;
		
		if(eof)
			System.out.println("������ ������ � ������, �� ���� �������� ����������((");
		
		ArrayList<Table> rostovTables = new ArrayList<Table>();
		
		for(int i = 0; i < 4; i++){ //4 �������
			Table table = new Table();
			
			currentLine = csvReader.readLine();
			checkEOF();
			while(!csvReader.isTableSeparator(currentLine.getString())){ //���� ��������� ������ �� �����������
				
				table.addLine(currentLine);
				currentLine = csvReader.readLine();
				checkEOF();
				if(eof){
					if(i == 3){
						break;//���� ���� �������������
					}
					else{
						System.out.println("�� ������� ������ �� �������, ��� ��� �� ���������");
					}
				}
			}
			
			rostovTables.add(table);
		}
		
		//��������� ����������� �������!
		if(isNeedTableChecking){
			if(this.singlesForEtalon != null){
				TableFixer.checkTables(rostovTables, this.singlesForEtalon);
			}else{
				System.out.println("����� �������� ����������� ��������, �� �� ���� ������");
			}
		}
		
		
		Table rostovTABLE = Table.buildBestTable(rostovTables, this.leaveNum, this.isBestLineToTop);
		//��� �������� � .docx
		int top = dw.insertNewTable(rostovTABLE, StaticStrings.rostovString,  this.topNum);
		
		//��� ����� ���-�� ����
		if(this.isRostovReach){
			this.totalPhraseCount += rostovTables.get(0).getSize();
			this.topPhraseCount += top;
		}
	}
	
	
	/**
	 * //������ ������ + ���. �����

	 * ������� ������� ��� ��� �������, � ����� ����� �� ��� � ���������� �� ���.�����. 
	 * ��������, � ������ ������� ��� ����� "xyz...", � ������ "� xyz...e" ��� e - ���� ������, ���� ����� �� ���������� � ���������� ������
	 * �� ����� � ������ ������ ��� �������(������ ���� ��� ��������� ��������� �� 3 �������?))
	 * � ��� ������ ��������� ��������� "[�|��] xyz...]
	 * ���� ������� - ��� ��� ��� ���� ������� � ��������, ��� - �������� ���. �����, ��������� ��������� ����� doAddWords
	 */
	
	private void doCities(boolean isNeed){
		if(!isNeed)
			return;
		
		if(eof) 
			System.out.println("������ ������ ������ � ������, �� ���� �������� ����������((");
		
		ArrayList<Table> oneCityTables = new ArrayList<Table>();
		while(!eof){
			Table table = new Table();
			
			
			currentLine = csvReader.readLine();
			checkEOF();
			
			if(eof)
				System.out.println("��������� ����� ����� ��� ��������� ������ �������");
			
			while(!csvReader.isTableSeparator(currentLine.getString())){ //���� ��������� ������ �� �����������
				table.addLine(currentLine);
				currentLine = csvReader.readLine();
				checkEOF();
				if(eof){
					break;
				}
			}
			
			oneCityTables.add(table);
		}
		
		
		//������� ��� ������� � ����� �����, �� �����
		if(isNeedTableChecking){
			if(this.singlesForEtalon != null){
				TableFixer.checkTables(oneCityTables, this.singlesForEtalon);
			}else{
				System.out.println("����� �������� ����������� ��������, �� �� ���� ������");
			}
		}
		
		if(!isAddWords & oneCityTables.size() % 2 == 1){
			System.out.println("� ���������� ������� ���������� ���. ����, � ������ �� ���. ������� - �������� ���-��.\n���-�� ��� �� ���, �������");
		}
		
		boolean addWordsAreFound = false;
		
		ArrayList<Table> realCities = new ArrayList<>();
		for(int i = 0; i < oneCityTables.size(); i+=2){
			if(i+1 < oneCityTables.size()){
				Table city1 = oneCityTables.get(i);
				String city1add = TableFixer.getAdditionalPhrase(city1);
				Table city2 = oneCityTables.get(i+1);
				String city2add = TableFixer.getAdditionalPhrase(city2);
				System.out.println(city1add + ";" + city2add);
				//�� ���� ����� ������
				//��� ��� ��, ��� ��������� � �������� ��� ������� doCities
				//����� ���� ���������� ����� ����������, �� [�|��] �������� ��������������� ���������, ������� ����� ������ �����
				Pattern p1;
				Matcher m1;
				Pattern p2;
				Matcher m2;
				if(city1add.length() < city2add.length()){
					p1 = Pattern.compile("^� " + city1add.substring(0, 2) + ".*$");
					m1 = p1.matcher(city2add.substring(0, 4));
					p2 = Pattern.compile("^�� " + city1add.substring(0, 2) + ".*$");
					m2 = p2.matcher(city2add.substring(0, 5));
				}else{
					p1 = Pattern.compile("^� " + city2add.substring(0, 2) + ".*$");
					m1 = p1.matcher(city1add.substring(0, 4));
					p2 = Pattern.compile("^�� " + city2add.substring(0, 2) + ".*$");
					m2 = p2.matcher(city1add.substring(0, 5));
				}
				
				
				if(m1.matches() | m2.matches()){ //���� ���� �� ��������� ������, ������, ��������� - ������, � ������ � ������., � ������� � ���������� ������ � ��������� �|��
					ArrayList<Table> oneCityTable = new ArrayList<>();
					oneCityTable.add(city1);
					oneCityTables.remove(city1);
					oneCityTable.add(city2);
					oneCityTables.remove(city2);
					i-=2;
					
					//��� ����� ���-�� ����
					if(this.isCitiesReach){
						this.totalPhraseCount += city1.getSize();
					}
					
					Table cityForReal = Table.buildBestTable(oneCityTable, this.leaveNum, this.isBestLineToTop);
					realCities.add(cityForReal);
					

					
				}else{
					//��� �������� �� ������� �� ����� ������ � �����, ������, ��� �� ������ � ���. �����(�� ��� �������� ����������, �� ��� � ��� �� ��� ��� ���������������, ������, ������)
					addWordsAreFound = true;
					System.out.println("������� ���. �����!");
					break;
				}
			
			}else{
				//���� �������� ��������� ��������, ������� ����� - � ���.�������
				addWordsAreFound = true;
				break;
			}
		}
		
		if(realCities.isEmpty()){
			System.out.println("�������������� ������� �� ���� �������!");
		}else{
			Table citiesToPrint = new Table(realCities);
			
			int top = dw.insertNewTable(citiesToPrint, StaticStrings.citiesString, this.topNum);
			if(this.isCitiesReach){
				this.topPhraseCount += top;
			}
		}
		
		if(addWordsAreFound){
			doAddWords(oneCityTables);
		}
		
	}

	private void doAddWords(ArrayList<Table> tables){
		Table addWordsToPrint = new Table();

		for(Table tab : tables){
			ArrayList<Table> temp = new ArrayList<>();
			temp.add(tab);
			//��� ����� ���-�� ����
			if(this.isAddWordsReach){
				this.totalPhraseCount += tab.getSize();
			}
			
			Table addWord = Table.buildBestTable(temp, this.leaveNum, this.isBestLineToTop);
			addWordsToPrint.plusTable(addWord);
		}
		int top = dw.insertNewTable(addWordsToPrint, StaticStrings.addWordsString, this.topNum);
		if(this.isAddWordsReach){
			this.topPhraseCount += top;
		}
	}
	
	
	private void checkEOF(){ // ����� ���������, �� ���������� �� ���� (���� ����������, ����� CSVReader ������ ��������� ������� ���������� null
		if(currentLine == null)
			eof = true;
	}
}
