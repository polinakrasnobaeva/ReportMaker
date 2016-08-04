package logic;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import plain.Line;

public class CSVReader {
private BufferedReader bReader;
private Pattern dataPattern;
private Matcher m;
private Pattern separatorPattern;
	
	public CSVReader(String readFileName) {
		try {
			FileInputStream fis = new FileInputStream(readFileName);
			InputStreamReader isr = new InputStreamReader(fis, "windows-1251");
			bReader = new BufferedReader(isr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		separatorPattern = Pattern.compile("^[0-9]{1,}$"); // сепаратор выглядит либо как куча цифр типа "20000000214124"
		
		dataPattern = skipLine(); //скипаем заголовочную строчку и заодно получаем паттерн
		
	}

	//считывает строку CSV и возвращает уже объект класса Line
	public Line readLine() {
		String result= "";
		try {
			result = bReader.readLine();

			if(result == null){
				return null;
			}
			
			
			String string;
			int ya;
			int goo;
			
			
			m = dataPattern.matcher(result);  
			

			if(m.find()){
				string = m.group(1).replace('\\', '0').replace('/', '0'); //реплейсы для фикса сепараторов аля Tanyusha-style
				try{// трай кэтчи на случай, если не удается распарсить число из ячейки с позицией. 
					ya = Integer.valueOf(m.group(2));
				}catch(NumberFormatException e){
					ya = 60;
				}
				try{
					goo = Integer.valueOf(m.group(3));
				}catch(NumberFormatException e){
					goo = 60;
				}	
				Line line = new Line(string, ya, goo);
				return line;
			}else{
				System.out.println("ЧС|" + result);
				System.out.println(m.pattern().toString());
				System.out.println("Ошибка чтения строки\nСтрока не подходит паттерн");
				return null;
			}
			
			
		} catch (IOException e) {
			System.out.println("ПОГАНАЯ СТРОКА||" + result);
			try {
				bReader.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("Произошла ошибка при вычитывании из файла.");
			return null;
		}		
	}
	
	//возвращает нужный паттерн для чтения последующих строк, определяется по первой строке CSV, являющегося заголовком таблицы
	private Pattern skipLine(){
		int stringBySeparatorType = 0;
		String line = null;
		try {
			line = bReader.readLine();
		} catch (IOException e) {
			System.out.println("Не удалось скипнуть строку");
			try {
				bReader.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("Произошла ошибка при вычитывании из файла.");
		}
		Pattern p = Pattern.compile("^\".*?\";\".*?\".*$"); //самый обычный
		Matcher m = p.matcher(line);
		if(m.matches()){
			stringBySeparatorType = 1;
		}else{
			p = Pattern.compile("^.*?,.*?,.*$"); //когда разделитель - запятая
			m = p.matcher(line);
			if(m.matches()){
				stringBySeparatorType = 2;
			}else{
				p = Pattern.compile("^.*?\t.*?\t.*$"); //когда разделитель - табуляция
				m = p.matcher(line);
				if(m.matches()){
					stringBySeparatorType = 3;
				}else{
					stringBySeparatorType = 0;
				}
			}
		}
		
		boolean isThereISCheckedColumn = line.substring(0, 15).contains("IsChecked");
		if(isThereISCheckedColumn){
			System.out.println("Первый столбик IsChecked есть");
		}else{
			System.out.println("Первого столбика IsChecked нет");
		}
		switch(stringBySeparatorType){
			// при выгрузке из КК разделитель - ;
			case 1 : {
				System.out.println("Паттерн - с разделителем ';'");
				if(isThereISCheckedColumn){
					return Pattern.compile("^(?:\"[Ff]alse\"|\"[Tt]rue\");\"(.*?)\";\"(.*?)\";\"(.*?)\"(:?.*$|$)");
				}else{
					return Pattern.compile("^\"(.*?)\";\"(.*?)\";\"(.*?)\"(:?.*$|$)");
				}
			}
			//когда правишь CSV Экселем, он совершенно меняет представление файла. Вместо ';' - ',' а кавычки убираются
			//(но не всегда. Если в ячейке есть запятые, то кавычки остаются, чтобы не спутать с разделителем ячейки)
			case 2 : {
				System.out.println("Паттерн - с разделителем ',', возможны кавычки иногда");
				if(isThereISCheckedColumn){
					return Pattern.compile("^(?:[Ff]alse|[Tt]rue),(\".*?,.*?\"|.*?),(\".+?,.+?\"|.*?),(\".+?,.+?\"|.*?)(:?,.*$|$)");
				}else{
					return Pattern.compile("^(\".*?,.*?\"|.*?),(\".+?,.+?\"|.*?),(\".+?,.+?\"|.*?)(:?,.*$|$)");
				}
			}
			//а когда бл%$# правишь либре офисом, он ваще табуляциями ячейки делит лол
			case 3 : {
				System.out.println("Паттерн - с разделителем 'табуляция'");
				if(isThereISCheckedColumn){
					return Pattern.compile("^(?:[Ff]alse|[Tt]rue)\t(.*?)\t(.*?)\t(.*?)(:?\t.*$|$)");
				}else{
					return Pattern.compile("^(.*?)\t(.*?)\t(.*?)(:?\t.*$|$)");
				}
			}	
		}
		//ну тут уже смиренно ловим нуллпоинтерэксепшен
		return null;
	}
	
	
	
	public void close()
	{
		try {
			bReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean isTableSeparator(String string){
		Matcher m = separatorPattern.matcher(string); 
		return m.matches();
	}
	
}
