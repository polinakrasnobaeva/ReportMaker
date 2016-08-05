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
	
	

	private int topNum;
	private int leaveNum;
	
	private boolean isNeedTableChecking;
	private boolean isBestLineToTop;
	
	private int totalPhraseCount = 0;

	private String promoRegion;
	
	public ReportBuilder(String csvFileName, String priceFileName, HashMap<String, String> values) {
		
		this.csvFileName = csvFileName;
		this.priceFileName = priceFileName;
		
		this.isSingles = values.containsKey("isSingles");
		this.isRostov = values.containsKey("isRostov");
		this.isCities = values.containsKey("isCities");
		this.isAddWords = values.containsKey("isAddWords");
	
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

		
		//СОХРАНЯЕМ DOCX
		dw.removeTemplateTable();

		System.out.println("Конвертация таблиц успешно завершена");
		System.out.println("ВСЕГО ФРАЗ: " + this.totalPhraseCount);
		
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
	
		singlesForEtalon = new Table();
		
		while(!csvReader.isTableSeparator(currentLine.getString()) & !eof){ //пока следующая строка не разделитель
			this.totalPhraseCount++;
			
			singlesForEtalon.addLine(currentLine);
			
			currentLine = csvReader.readLine();
			checkEOF();
			if(eof){
				break;
			}
		}
		
		ArrayList<Table> singles = new ArrayList<Table>(); 
		singles.add(singlesForEtalon);
		
		Table single = Table.buildBestTable(singles, this.leaveNum, this.isBestLineToTop);//лень переписывать метод под одну таблицу, поэтому выше делаем эррейлист на одну таблицу :D
		//для выгрузки в .docx
		dw.insertNewTable(single, StaticStrings.singlesString + this.promoRegion, this.topNum);
		
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
			if(this.singlesForEtalon != null){
				TableFixer.checkTables(rostovTables, this.singlesForEtalon);
			}else{
				System.out.println("Опция проверки целостности включена, но не было бонуса");
			}
		}
		//для счёта кол-ва фраз
		this.totalPhraseCount += rostovTables.get(0).getSize();
		
		Table rostovTABLE = Table.buildBestTable(rostovTables, this.leaveNum, this.isBestLineToTop);
		//для выгрузки в .docx
		dw.insertNewTable(rostovTABLE, StaticStrings.rostovString,  this.topNum);
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
			if(this.singlesForEtalon != null){
				TableFixer.checkTables(oneCityTables, this.singlesForEtalon);
			}else{
				System.out.println("Опция проверки целостности включена, но не было бонуса");
			}
		}
		
		if(!isAddWords & oneCityTables.size() % 2 == 1){
			System.out.println("В настройках указано отсутствие доп. слов, а таблиц по доп. городам - нечетное кол-во.\nЧто-то тут не так, проверь");
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
					
					Table cityForReal = Table.buildBestTable(oneCityTable, this.leaveNum, this.isBestLineToTop);
					realCities.add(cityForReal);
					//для счёта кол-ва фраз
					this.totalPhraseCount += oneCityTable.size();

					
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
			
			dw.insertNewTable(citiesToPrint, StaticStrings.citiesString, this.topNum);
			
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
			//для счёта кол-ва фраз
			this.totalPhraseCount += tab.getSize();
			
			Table addWord = Table.buildBestTable(temp, this.leaveNum, this.isBestLineToTop);
			addWordsToPrint.plusTable(addWord);
		}
		dw.insertNewTable(addWordsToPrint, StaticStrings.addWordsString, this.topNum);

	}
	
	
	private void checkEOF(){ // метод проверяет, не закончился ли файл (файл закончИлся, когда CSVReader вместо очередной строчки возвращает null
		if(currentLine == null)
			eof = true;
	}
}
