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
public int lineCount = 0;
	
	public CSVReader(String readFileName) {
		try {
			FileInputStream fis = new FileInputStream(readFileName);
			InputStreamReader isr = new InputStreamReader(fis, "windows-1251");
			bReader = new BufferedReader(isr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		separatorPattern = Pattern.compile("^[0-9]{1,}$"); // ��������� �������� ���� ��� ���� ���� ���� "20000000214124"
		
		dataPattern = skipLine(); //������� ������������ ������� � ������ �������� �������
	}


	//��������� ������ CSV � ���������� ��� ������ ������ Line
	public Line readLine() {
		String result= "";
		try {
			result = bReader.readLine();
			
			if(result == null){
				return null;
			}
			lineCount++;
			
			String string;
			int ya;
			int goo;
			
			
			m = dataPattern.matcher(result);  
			

			if(m.find()){
				string = m.group(1).replace('\\', '0').replace('/', '0'); //�������� ��� ����� ����������� ��� Tanyusha-style
				try{// ���� ����� �� ������, ���� �� ������� ���������� ����� �� ������ � ��������. 
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
				System.out.println("��|" + result);
				System.out.println(m.pattern().toString());
				System.out.println("������ ������ ������\n������ �� ��������� �������");
				return null;
			}
			
			
		} catch (IOException e) {
			System.out.println("������� ������: " + result);
			try {
				bReader.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("��������� ������ ��� ����������� �� �����.");
			return null;
		}		
	}
	
	//���������� ������ ������� ��� ������ ����������� �����, ������������ �� ������ ������ CSV, ����������� ���������� �������
	private Pattern skipLine(){
		int stringBySeparatorType = 0;
		String line = null;
		try {
			line = bReader.readLine();
		} catch (IOException e) {
			System.out.println("�� ������� �������� ������");
			try {
				bReader.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("��������� ������ ��� ����������� �� �����.");
		}
		Pattern p = Pattern.compile("^\".*?\";\".*?\".*$"); //����� �������
		Matcher m = p.matcher(line);
		if(m.matches()){
			stringBySeparatorType = 1;
		}else{
			p = Pattern.compile("^.*?,.*?,.*$"); //����� ����������� - �������
			m = p.matcher(line);
			if(m.matches()){
				stringBySeparatorType = 2;
			}else{
				p = Pattern.compile("^.*?\t.*?\t.*$"); //����� ����������� - ���������
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
			System.out.println("������ ������� IsChecked ����");
		}else{
			System.out.println("������� �������� IsChecked ���");
		}
		switch(stringBySeparatorType){
			// ��� �������� �� �� ����������� - ;
			case 1 : {
				System.out.println("������� - � ������������ ';'");
				if(isThereISCheckedColumn){
					return Pattern.compile("^(?:\"[Ff]alse\"|\"[Tt]rue\");\"(.*?)\";\"(.*?)\";\"(.*?)\"(:?.*$|$)");
				}else{
					return Pattern.compile("^\"(.*?)\";\"(.*?)\";\"(.*?)\"(:?.*$|$)");
				}
			}
			//����� ������� CSV �������, �� ���������� ������ ������������� �����. ������ ';' - ',' � ������� ���������
			//(�� �� ������. ���� � ������ ���� �������, �� ������� ��������, ����� �� ������� � ������������ ������)
			case 2 : {
				System.out.println("������� - � ������������ ',', �������� ������� ������");
				if(isThereISCheckedColumn){
					return Pattern.compile("^(?:[Ff]alse|[Tt]rue),(\".*?,.*?\"|.*?),(\".+?,.+?\"|.*?),(\".+?,.+?\"|.*?)(:?,.*$|$)");
				}else{
					return Pattern.compile("^(\".*?,.*?\"|.*?),(\".+?,.+?\"|.*?),(\".+?,.+?\"|.*?)(:?,.*$|$)");
				}
			}
			//� ����� ��%$# ������� ����� ������, �� ���� ����������� ������ ����� ���
			case 3 : {
				System.out.println("������� - � ������������ '���������'");
				if(isThereISCheckedColumn){
					return Pattern.compile("^(?:[Ff]alse|[Tt]rue)\t(.*?)\t(.*?)\t(.*?)(:?\t.*$|$)");
				}else{
					return Pattern.compile("^(.*?)\t(.*?)\t(.*?)(:?\t.*$|$)");
				}
			}
		}
		//�� ��� ��� �������� ����� �������������������
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
