package logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.docx4j.openpackaging.exceptions.Docx4JException;

import plain.Line;
import plain.Table;

public class ReportBuilder{
	


	private String sourceFileName;

	private Line currentLine;
	private boolean eof = false; //end of file
	private CSVReader csvReader;
	private Table bonusForEtalon = null;
	
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
	
	public ReportBuilder(String sourceFileName, HashMap<String, String> values) {
		//да, быдлокодю. НО СРОКИ!11!!
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
		
		csvReader.close();

		
		//СОХРАНЯЕМ DOCX
		dw.removeTemplateTable();

		System.out.println("Конвертация таблиц успешно завершена");
		
		return dw;
	}
	
	
	/**
	 * ДЕЛАТЬ БОНУС
	 */

	private void doSingles(boolean isNeed){
		if(!isNeed)
			return;
		currentLine = csvReader.readLine();
		checkEOF(); 
	
		bonusForEtalon = new Table();
		
		while(!csvReader.isTableSeparator(currentLine.getString()) & !eof){ //пока следующая строка не разделитель
			bonusForEtalon.addLine(currentLine);

			currentLine = csvReader.readLine();
			checkEOF();
			if(eof){
				break;
			}
		}
		
		
		ArrayList<Table> bonuses = new ArrayList<Table>(); 
		bonuses.add(bonusForEtalon);
		
		String bonusstring = "БОНУС: наличие позиций сайта по региональным запросам без указания города - показатель высокого качества проводимых мероприятий!";
		Table bonus = Table.buildBestTable(bonuses, lastBonusPos, false, this.leaveEmpty, this.isBestLineToTop);//лень переписывать метод под одну таблицу, поэтому выше делаем эррейлист на одну таблицу :D
		//для выгрузки в .docx
		if(isSepSingles){
			dw.insertNewTableWithTwoColumns(bonus, bonusstring, this.lastBonusPos);
		}else{
			dw.insertNewTable(bonus, bonusstring, this.lastBonusPos);
		}
		
		
		if(isSepSingles){
			bonus = Table.buildBestTable(bonuses, lastBonusPos, true, this.leaveEmpty, this.isBestLineToTop);
			//для выгрузки в .docx
			dw.insertNewTableWithTwoColumns(bonus, "GOOGLE", this.lastBonusPos);
		}
	}
	
	/**
	 * ДЕЛАТЬ РОСТОВ
	 */

	private void doRostov(boolean isNeed){
		if(!isNeed)
			return;
		
		if(eof)
			System.out.println("Ростов указан в отчете, но файл внезапно закончился((");
		
		ArrayList<Table> rostovTables = new ArrayList<Table>();
		
		for(int i = 0; i < 4; i++){ //4 таблицы
			Table table = new Table();
			
			currentLine = csvReader.readLine();
			checkEOF();
			while(!csvReader.isTableSeparator(currentLine.getString())){ //пока следующая строка не разделитель
				table.addLine(currentLine);
				currentLine = csvReader.readLine();
				checkEOF();
				if(eof){
					if(i == 3){
						break;//если файл заканчивается
					}
					else{
						System.out.println("Не хватает таблиц по Ростову, или они не разделены");
					}
				}
			}
			
			rostovTables.add(table);
		}
		
		//ПРОВЕРЯЕМ ЦЕЛОСТНОСТЬ РОСТОВА!
		if(isNeedTableChecking){
			if(this.bonusForEtalon != null){
				TableFixer.checkTables(rostovTables, this.bonusForEtalon);
			}else{
				System.out.println("Опция проверки целостности включена, но не было бонуса");
			}
		}
		
		
		Table rostovTABLE = Table.buildBestTable(rostovTables, lastCitiesPos, false, this.leaveEmpty, this.isBestLineToTop);
		//для выгрузки в .docx
		if(isSepRostov){
			dw.insertNewTableWithTwoColumns(rostovTABLE, "С добавлением города: Ростов-на-Дону", lastCitiesPos);
		}else{
			dw.insertNewTable(rostovTABLE, "С добавлением города: Ростов-на-Дону", lastCitiesPos);
		}
		
		if(isSepRostov){
			rostovTABLE = Table.buildBestTable(rostovTables, lastCitiesPos, true, this.leaveEmpty, this.isBestLineToTop);
			//для выгрузки в .docx
			dw.insertNewTableWithTwoColumns(rostovTABLE, "GOOGLE", lastCitiesPos);
		}
	}
	
	
	/**
	 * //ДЕЛАТЬ ГОРОДА + доп. слова

	 * СНАЧАЛА СОБРАТЬ УЖЕ ВСЕ ТАБЛИЦЫ, а потом брать по две и сравнивать их доп.слова. 
	 * например, у первой таблицы доп слово "xyz...", у второй "в xyz...e" где e - либо пустое, если город не склоняется в предложном падеже
	 * то берем у первой первые три символа(города даже при склонении оставляют же 3 символа?))
	 * и для второй проверяем равенство "[в|во] xyz...]
	 * если совпали - это все еще идут таблицы с городами, нет - начались доп. слова, запускаем отдельный метод doAddWords
	 */
	
	private void doCities(boolean isNeed){
		if(!isNeed)
			return;
		
		if(eof) 
			System.out.println("Другие города указан в отчете, но файл внезапно закончился((");
		
		ArrayList<Table> oneCityTables = new ArrayList<Table>();
		while(!eof){
			Table table = new Table();
			
			
			currentLine = csvReader.readLine();
			checkEOF();
			
			if(eof)
				System.out.println("Внезапный конец файла при просмотре других городов");
			
			while(!csvReader.isTableSeparator(currentLine.getString())){ //пока следующая строка не разделитель
				table.addLine(currentLine);
				currentLine = csvReader.readLine();
				checkEOF();
				if(eof){
					break;
				}
			}
			
			oneCityTables.add(table);
		}
		
		
		//СОБРАЛИ ВСЕ ТАБЛИЦЫ В КОНЦЕ ФАЙЛА, ДУ СТАФФ
		if(isNeedTableChecking){
			if(this.bonusForEtalon != null){
				TableFixer.checkTables(oneCityTables, this.bonusForEtalon);
			}else{
				System.out.println("Опция проверки целостности включена, но не было бонуса");
			}
		}
		
		if(!isAddWords & oneCityTables.size() % 2 == 1){
			System.out.println("В настройках указано отсутствие доп. слов, а таблиц по доп. городам - нечетное кол-во.\nЧто-то тут не так, проверь");
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
				//ЩА ваще магия попрет
				//Как раз то, что говорится в комменте над методом doCities
				//Можно было управиться двумя регекспами, но [в|во] вызывают допольнительные трудности, поэтому пошел легким путем
				Pattern p1;
				Matcher m1;
				Pattern p2;
				Matcher m2;
				if(city1add.length() < city2add.length()){
					p1 = Pattern.compile("^в " + city1add.substring(0, 2) + ".*$");
					m1 = p1.matcher(city2add.substring(0, 4));
					p2 = Pattern.compile("^во " + city1add.substring(0, 2) + ".*$");
					m2 = p2.matcher(city2add.substring(0, 5));
				}else{
					p1 = Pattern.compile("^в " + city2add.substring(0, 2) + ".*$");
					m1 = p1.matcher(city1add.substring(0, 4));
					p2 = Pattern.compile("^во " + city2add.substring(0, 2) + ".*$");
					m2 = p2.matcher(city1add.substring(0, 5));
				}
				
				
				if(m1.matches() | m2.matches()){ //если один из регэкспов совпал, значит, окончания - города, у одного в именит., у другого в предложном падеже с предлогом в|во
					ArrayList<Table> oneCityTable = new ArrayList<>();
					oneCityTable.add(city1);
					oneCityTables.remove(city1);
					oneCityTable.add(city2);
					oneCityTables.remove(city2);
					i-=2;
					
					Table cityForReal = Table.buildBestTable(oneCityTable, lastCitiesPos, false, this.leaveEmpty, this.isBestLineToTop);
					realCities.add(cityForReal);
					if(isSepOther){
						Table cityForRealGoo = Table.buildBestTable(oneCityTable, lastCitiesPos, true, this.leaveEmpty, this.isBestLineToTop);
						realCitiesGoo.add(cityForRealGoo);
					}
					
				}else{
					//две таблички не совпали по якобы городу в конце, значит, там не города а доп. слова(ну или таблички перепутаны, но тут я уже хз как это предусматривать, СЛОЖНА, СЛОЖНА)
					addWordsAreFound = true;
					System.out.println("Найдены доп. слова!");
					break;
				}
			
			}else{
				//явно осталась последняя табличка, скореее всего - с доп.словами
				addWordsAreFound = true;
				break;
			}
		}
		
		if(realCities.isEmpty()){
			System.out.println("Дополнительных городов не было найдено!");
		}else{
			Table citiesToPrint = new Table(realCities);
			
			//для выгрузки в .docx
			if(isSepOther){
				dw.insertNewTableWithTwoColumns(citiesToPrint, "С добавлением других городов", lastCitiesPos);
			}else{
				dw.insertNewTable(citiesToPrint, "С добавлением других городов", lastCitiesPos);
			}
			
			if(isSepOther){
				citiesToPrint = new Table(realCitiesGoo);
				//для выгрузки в .docx
				dw.insertNewTableWithTwoColumns(citiesToPrint, "GOOGLE", lastCitiesPos);
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
			Table addWord = Table.buildBestTable(temp, lastCitiesPos, false, this.leaveEmpty, this.isBestLineToTop);
			addWordsToPrint.plusTable(addWord);
			if(isSepAddw){
				addWord = Table.buildBestTable(temp, lastCitiesPos, true, this.leaveEmpty, this.isBestLineToTop);
				addWordsToPrintGoo.plusTable(addWord);
			}
		}
		//для выгрузки в .docx
		if(isSepAddw){
			dw.insertNewTableWithTwoColumns(addWordsToPrint, "С доп. словами", lastCitiesPos);
		}else{
			dw.insertNewTable(addWordsToPrint, "С доп. словами", lastCitiesPos);
		}

		if(isSepAddw){
			//для выгрузки в .docx
		dw.insertNewTableWithTwoColumns(addWordsToPrintGoo, "GOOGLE", lastCitiesPos);
		}
	}
	
	
	private void checkEOF(){ // метод проверяет, не закончился ли файл (файл закончИлся, когда CSVReader вместо очередной строчки возвращает null
		if(currentLine == null)
			eof = true;
	}
}
