package summCounterModule;
import java.util.ArrayList;


public class TableFixer {
	//ИЗБЫТОЧНОСТЬ | используется в getAdditionalPhrase, определяет количество строк, которые берутся для сравнения
	//Ну, я думаю, что 7 хватает, и производительность почти не падает
	private static final int redundancy = 7;
	
	
	//ЗАРАНЕЕ ЗНАЮ, ЧТО СДЕЛАЮ СТРЕМНЫЙ АЛГОРИТМ И ЧТО ЕГО МОЖНО СДЕЛАТЬ ЛУЧШЕ
	//(спустя 2 часа) ХМ, а может быть, не так уж и плохо
	
	//сравнивает "сырые" таблицы с таблицей БОНУСА, и при отсутствии какой-либо строки вставляем вместо нее пустую строку.
	//нужно для того, чтобы при сравнении таблиц, в случаее, если какая-либо фраза выпала, строки "не съезжали"
	//"сырые" таблицы - набор таблиц, которые мы напарсили с определенного блока, например, 2 таблицы для блока с городом ("Таганрог, "в Таганроге") или 4 для Ростова
	public static void checkTables(ArrayList<Table> tables, Table etalonTable){
		for(Table checkingTable : tables){
			 //если размер таблички совпадает с таковым у эталона, будем считать, что табличка полная
			String additionalPhrase = getAdditionalPhrase(checkingTable);
			
			if(checkingTable.getSize() == etalonTable.getSize()){
				System.out.println("ТАДААА|" + additionalPhrase + "' совпадает с эталоном из БОНУСа");
				continue;
			}else{
				System.out.println("ТАДААА|" + additionalPhrase + "' корявая, сейчас будет ПОФИКШЕНА!");
			}
			
			int fixNumber = 0;
			
			ArrayList<Line> etalonLines = etalonTable.getLines();
			
			
			String etalonString;
			
			for(int i = 0; i < etalonLines.size(); i++){
				etalonString = etalonLines.get(i).getString();
				
				if(i < checkingTable.getSize()){
					if(etalonString.equals(getBaseOfPhrase(checkingTable.getLine(i).getString(), additionalPhrase))){ //если строка на месте, смотрим следующую
						//U.println("ТАДААА строка на месте|" + checkingTable.getLine(i).getString());
						continue;
					}else{
						System.out.println("В '" + additionalPhrase + "' отсутствует строка '" + etalonString + "'");
						checkingTable.getLines().add(i, new Line(" ", 60, 60)); //если нет, вставляем пустую, которую сортировщик в РепортБилдере будет игнорить
						fixNumber++;
					}
				}else{
					checkingTable.getLines().add(new Line(" ", 60, 60));
				}
				
			}
			
			if(checkingTable.getSize() != etalonTable.getSize()){
				System.out.println(
						"Фиксер таблиц не справился, в результате в исправляемой таблице количиство строк неравно эталонному из Бонуса.\n"
						+ "Скорее всего, в таблице '" + additionalPhrase + "' нарушен порядок строк"
						);
				break;
			}else{
				System.out.println("||||Фиксер таблиц добавил " + fixNumber + " строк в '" + additionalPhrase + "', круто же?");
			}
		}
	}
	
	
	//получить доп. слово или город на основе таблицы (можно было бы на основе двух фраз, но тогда возможны ошибки
	//т.к. метод сравнивает посимвольно все строки справа налево, пока они совпадают,
	//но "базы" некоторых фраз так же могут оканчиваться одинаково
	//а у всей таблицы "базы" фраз врядли уж будут совпадать(ХОТЯ ХЗ)
	//*******
	//например: "клавиатуры в Ростове", "мышки в Ростове" - получим "в Ростове"
	public static String getAdditionalPhrase(Table table){	
		ArrayList<Line> lines = table.getLines();
		String firstLine = lines.get(0).getString();
		
		int magicNumber = -1; // номер буквы, с которой начинается доп. слово/город
		boolean match = true;
		
		for(int cIndex = 0; cIndex < firstLine.length()-1; cIndex++){  //проход по всем буквам первой строки с конца; cIndex - номер буквы С КОНЦА строки
			char currentChar = firstLine.charAt(firstLine.length() -1 - cIndex);
			
			//проход от второй строки до СМ. ВВЕРХУ КЛАССА, ЧТО ТАКОЕ redundancy
			//либо, если строк в таблице меньше, чем redundancy, до последней строки
			for(int i = 1; i < (table.getSize() - 1 < redundancy ? table.getSize() - 1 : redundancy); i++){ 
				
				String iString = lines.get(i).getString();
				
				char iStringChar = iString.charAt(iString.length() - 1 - cIndex);
				if(iStringChar != currentChar){
					match = false;
					break;
				}
				
			}
			if(!match){
				magicNumber = firstLine.length() - 1 - cIndex + 1 + 1; //длина минус индекс первого несовпадающего символа(с конца) плюс 1(следующий симол) плюс 1(т.к. там по идее пробел)
				break;
			}
		}
		
		if(match){
			System.out.println("Фиксер сравнил две строки одной таблицы, очень удивился, т.к. они ОДИНАКОВЫЕ");
		}
		
		String result = firstLine.substring(magicNumber);
		
		return result;
	}
	
	//Получить "базу" фразы с помощью фразы ЦЕЛИКОМ и известного доп. окончания (город, етс), которое можно узнать методом getAdditionalPhrase()
	//например, в "Трава зеленая Ростов" база "Трава зеленая"
	public static String getBaseOfPhrase(String fullPhrase , String additionalPhrase){
		if(!fullPhrase.endsWith(additionalPhrase)){
			System.out.println("Метода getBaseOfPhrase не нашел в строке '" + fullPhrase + "' оконцовочки '" + additionalPhrase + "'");
		}
		
		int addPhStart = fullPhrase.indexOf(additionalPhrase);
		
		return fullPhrase.substring(0, addPhStart - 1);
	}
}
