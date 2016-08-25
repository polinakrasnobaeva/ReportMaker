package logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.docx4j.openpackaging.exceptions.Docx4JException;

import Utils.StaticStrings;
import plain.BonusEntry;
import plain.Line;
import plain.Table;

public class ReportBuilder{
	
	private String sourceFileName;

	private Line currentLine;
	private boolean eof = false; //end of file
	private CSVReader csvReader;
	private Table singleForEtalon = null;
	
	private DOCXworker dw;
	
	private boolean isSingles;
	private boolean isRostov;
	private boolean isCities;
	private boolean isAddWords;

	private int lastBonusPos;
	private int lastCitiesPos;
	
	private boolean isSepSingles;
	private boolean isSepRostov;
	private boolean isSepOther;
	private boolean isSepAddw;
	
	private boolean isNeedTableChecking;
	private boolean leaveEmpty;
	private boolean isBestLineToTop;
	
	private ArrayList<BonusEntry> tablesForBonus = new ArrayList<>();
	private boolean isSinglesBonus;
	private boolean isRostovBonus;
	private boolean isCitiesBonus;
	private boolean isAddWordsBonus;
	
	public int totalPhraseCount = 0;
	public int topPhraseCount = 0;
	
	private String promoRegion;
	
	public ReportBuilder(String sourceFileName, HashMap<String, String> values) {
		//��, ���������. �� �����!11!!
		this.sourceFileName = sourceFileName;
		this.isSingles = values.containsKey("isSingles");
		this.isRostov = values.containsKey("isRostov");
		this.isCities = values.containsKey("isCities");
		this.isAddWords = values.containsKey("isAddWords");
		this.lastBonusPos = Integer.valueOf(values.get("lastBonusPos"));
		this.lastCitiesPos = Integer.valueOf(values.get("lastCitiesPos"));
		this.isSepSingles = values.containsKey("isSepSingles");
		this.isSepRostov = values.containsKey("isSepRostov");
		this.isSepOther = values.containsKey("isSepOther");
		this.isSepAddw = values.containsKey("isSepAddw");
		this.isNeedTableChecking = values.containsKey("isNeedTableChecking");
		this.leaveEmpty = values.containsKey("leaveEmpty");
		this.isBestLineToTop = values.containsKey("isBestLineToTop");
		
		this.isSinglesBonus = values.containsKey("isSinglesBonus");
		this.isRostovBonus = values.containsKey("isRostovBonus");
		this.isCitiesBonus = values.containsKey("isCitiesBonus");
		this.isAddWordsBonus = values.containsKey("isAddWordsBonus");
		
		this.promoRegion = values.get("promoRegion");
	}
	
	
	public DOCXworker buildReport(String docExamplePath){

		csvReader = new CSVReader(this.sourceFileName);

		//for DOCX
		dw = null;
		try {
			dw = new DOCXworker(docExamplePath, " ");
		} catch (Docx4JException | JAXBException e) {
			e.printStackTrace();
		}

		doSingles(isSingles);
		doRostov(isRostov);
		doCities(isCities | isAddWords);
		
		dw.insertBonusTables(tablesForBonus, StaticStrings.bonusString);
		
		csvReader.close();

		
		//��������� DOCX
		dw.removeTemplateTable();

		System.out.println("����������� ������ ������� ���������");
		
		return dw;
	}
	
	
	/**
	 * ������ �����
	 */

	private void doSingles(boolean isNeed){
		if(!isNeed)
			return;
		currentLine = csvReader.readLine();
		checkEOF(); 
	
		singleForEtalon = new Table();
		
		while(!csvReader.isTableSeparator(currentLine.getString()) & !eof){ //���� ��������� ������ �� �����������
			singleForEtalon.addLine(currentLine);

			currentLine = csvReader.readLine();
			checkEOF();
			if(eof){
				break;
			}
		}
		
		
		ArrayList<Table> singles = new ArrayList<Table>(); 
		singles.add(singleForEtalon);
		

		if(this.isSinglesBonus){
			Table single = Table.buildBestTable(singles, this.lastBonusPos, false, this.leaveEmpty, this.isBestLineToTop);//���� ������������ ����� ��� ���� �������, ������� ���� ������ ��������� �� ���� ������� :D
			
			if(isSepSingles){
				this.tablesForBonus.add(new BonusEntry(single, true, StaticStrings.yandexString +  StaticStrings.singlesBonusString, this.lastBonusPos));
				single = Table.buildBestTable(singles, this.lastBonusPos, true, this.leaveEmpty, this.isBestLineToTop);
				this.tablesForBonus.add(new BonusEntry(single, true, StaticStrings.googleString +  StaticStrings.singlesBonusString, this.lastBonusPos));
			}else{
				this.tablesForBonus.add(new BonusEntry(single, false, StaticStrings.singlesBonusString, this.lastBonusPos));
			}
		}else{
			int top = 0;
			
			Table single = Table.buildBestTable(singles, this.lastCitiesPos, false, this.leaveEmpty, this.isBestLineToTop);//���� ������������ ����� ��� ���� �������, ������� ���� ������ ��������� �� ���� ������� :D
			
			if(isSepSingles){
				top = dw.insertNewTableWithTwoColumns(single, StaticStrings.singlesString + this.promoRegion, this.lastCitiesPos, false);
				single = Table.buildBestTable(singles, this.lastCitiesPos, true, this.leaveEmpty, this.isBestLineToTop);
				dw.insertNewTableWithTwoColumns(single, StaticStrings.googleString, this.lastCitiesPos, false);
			}else{
				top = dw.insertNewTable(single, StaticStrings.singlesString + this.promoRegion, this.lastCitiesPos, false);
			}
			
			this.totalPhraseCount += singleForEtalon.getSize(); // ������� ���-�� ����� � �������, ���� ��� �� ������
			this.topPhraseCount += top;
		}
		
		if(!this.isSinglesBonus){
			
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
			if(this.singleForEtalon != null){
				TableFixer.checkTables(rostovTables, this.singleForEtalon);
			}else{
				System.out.println("����� �������� ����������� ��������, �� �� ���� ������");
			}
		}
		
		if(this.isRostovBonus){
			Table rostovTABLE = Table.buildBestTable(rostovTables, lastBonusPos, false, false, this.isBestLineToTop);
			
			if(isSepRostov){
				this.tablesForBonus.add(new BonusEntry(rostovTABLE, true, StaticStrings.yandexString + StaticStrings.rostovBonusString, this.lastBonusPos));
				rostovTABLE = Table.buildBestTable(rostovTables, this.lastBonusPos, true, false, this.isBestLineToTop);
				this.tablesForBonus.add(new BonusEntry(rostovTABLE, true, StaticStrings.googleString + StaticStrings.rostovBonusString, this.lastBonusPos));
			}else{
				this.tablesForBonus.add(new BonusEntry(rostovTABLE, false, StaticStrings.rostovBonusString, this.lastBonusPos));
			}
		}else{
			int top = 0;
			
			Table rostovTABLE = Table.buildBestTable(rostovTables, lastCitiesPos, false, this.leaveEmpty, this.isBestLineToTop);
			
			if(isSepRostov){
				top = dw.insertNewTableWithTwoColumns(rostovTABLE, StaticStrings.rostovString, this.lastCitiesPos, false);
				rostovTABLE = Table.buildBestTable(rostovTables, this.lastCitiesPos, true, this.leaveEmpty, this.isBestLineToTop);
				dw.insertNewTableWithTwoColumns(rostovTABLE, StaticStrings.googleString, this.lastCitiesPos, false);
			}else{
				top = dw.insertNewTable(rostovTABLE, StaticStrings.rostovString, this.lastCitiesPos, false);
			}
			
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
			if(this.singleForEtalon != null){
				TableFixer.checkTables(oneCityTables, this.singleForEtalon);
			}else{
				System.out.println("����� �������� ����������� ��������, �� �� ���� ������");
			}
		}
		
		if(!isAddWords & oneCityTables.size() % 2 == 1){
			System.out.println("� ���������� ������� ���������� ���. ����, � ������ �� ���. ������� - �������� ���-��.\n���-�� ��� �� ���, �������");
		}
		
		boolean addWordsAreFound = false;
		
		ArrayList<Table> realCities = new ArrayList<>();
		ArrayList<Table> realCitiesGoo = new ArrayList<>();
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
					
					if(this.isCitiesBonus){
						Table cityForReal = Table.buildBestTable(oneCityTable, lastBonusPos, false, false, this.isBestLineToTop);
						realCities.add(cityForReal);
						if(isSepOther){
							Table cityForRealGoo = Table.buildBestTable(oneCityTable, lastBonusPos, true, false, this.isBestLineToTop);
							realCitiesGoo.add(cityForRealGoo);
						}
					}else{
						//��� �����
						this.totalPhraseCount += city1.getSize();
						
												
						Table cityForReal = Table.buildBestTable(oneCityTable, lastCitiesPos, false, this.leaveEmpty, this.isBestLineToTop);
						realCities.add(cityForReal);
						if(isSepOther){
							Table cityForRealGoo = Table.buildBestTable(oneCityTable, lastCitiesPos, true, this.leaveEmpty, this.isBestLineToTop);
							realCitiesGoo.add(cityForRealGoo);
						}
					}
					
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
			
			//��� �������� � .docx
			if(this.isCitiesBonus){
				if(isSepOther){
					this.tablesForBonus.add(new BonusEntry(citiesToPrint, true, StaticStrings.yandexString + StaticStrings.citiesBonusString, this.lastBonusPos));
					citiesToPrint = new Table(realCitiesGoo);
					this.tablesForBonus.add(new BonusEntry(citiesToPrint, true, StaticStrings.googleString + StaticStrings.citiesBonusString, this.lastBonusPos));
				}else{
					this.tablesForBonus.add(new BonusEntry(citiesToPrint, false, StaticStrings.citiesBonusString, this.lastBonusPos));
				}
			}else{
				int top = 0;
				if(isSepOther){
					top = dw.insertNewTableWithTwoColumns(citiesToPrint, StaticStrings.citiesString, this.lastCitiesPos, false);
					citiesToPrint = new Table(realCitiesGoo);
					dw.insertNewTableWithTwoColumns(citiesToPrint, StaticStrings.googleString, this.lastCitiesPos, false);
				}else{
					top = dw.insertNewTable(citiesToPrint, StaticStrings.citiesString, this.lastCitiesPos, false);
				}
				this.topPhraseCount += top;
			}
						
		}
		
		if(addWordsAreFound){
			doAddWords(oneCityTables);
		}
		
	}

	private void doAddWords(ArrayList<Table> tables){
		Table addWordsToPrint = new Table();
		Table addWordsToPrintGoo = new Table();
		for(Table tab : tables){
			ArrayList<Table> temp = new ArrayList<>();
			temp.add(tab);
			//��� �����
			if(!this.isAddWordsBonus){
				this.totalPhraseCount += tab.getSize();
			}
			
			Table addWord = Table.buildBestTable(temp, lastCitiesPos, false, this.leaveEmpty, this.isBestLineToTop);
			addWordsToPrint.plusTable(addWord);
			if(isSepAddw){
				addWord = Table.buildBestTable(temp, lastCitiesPos, true, this.leaveEmpty, this.isBestLineToTop);
				addWordsToPrintGoo.plusTable(addWord);
			}
		}
		if(this.isAddWordsBonus){
			if(isSepAddw){
				this.tablesForBonus.add(new BonusEntry(addWordsToPrint, true, StaticStrings.yandexString + StaticStrings.addWordsBonusString, this.lastBonusPos));
				this.tablesForBonus.add(new BonusEntry(addWordsToPrintGoo, true, StaticStrings.googleString + StaticStrings.addWordsBonusString, this.lastBonusPos));
			}else{
				this.tablesForBonus.add(new BonusEntry(addWordsToPrint, false, StaticStrings.addWordsBonusString, this.lastBonusPos));
			}
		}else{
			int top = 0;
			if(isSepAddw){
				top = dw.insertNewTableWithTwoColumns(addWordsToPrint, StaticStrings.addWordsString, this.lastCitiesPos, false);
				dw.insertNewTableWithTwoColumns(addWordsToPrintGoo, StaticStrings.googleString, this.lastCitiesPos, false);
			}else{
				top = dw.insertNewTable(addWordsToPrint, StaticStrings.addWordsString, this.lastCitiesPos, false);
			}
			this.topPhraseCount += top;
		}

	}
	
	
	private void checkEOF(){ // ����� ���������, �� ���������� �� ���� (���� ����������, ����� CSVReader ������ ��������� ������� ���������� null
		if(currentLine == null)
			eof = true;
	}
}
