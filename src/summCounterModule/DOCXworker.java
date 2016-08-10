package summCounterModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Body;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase.PStyle;

import org.docx4j.wml.R;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;

public class DOCXworker {
	
	private WordprocessingMLPackage template;
	
	private List<Object> allTables;
	private Tbl templateTable;
	private Tr templateRow;
	private Tr templateRow2;
	private Tr templateEndRow;		
	
	public DOCXworker(String sourceFileName) throws Docx4JException, JAXBException{
		try {
			this.template = WordprocessingMLPackage.load(new FileInputStream(new File(sourceFileName)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		//берем список всех таблиц
		this.allTables = getAllElementFromObject(this.template.getMainDocumentPart(), Tbl.class);
		//находим таблицы-образцы
		setTemplateTable();
	}


	//получить все элементы данного класса из объекта
	public static List<Object> getAllElementFromObject(Object obj, Class<?> toSearch) {
		List<Object> result = new ArrayList<Object>();
		if (obj instanceof JAXBElement)
			obj = ((JAXBElement<?>) obj).getValue();
		
		if (obj.getClass().equals(toSearch))
			result.add(obj);
		else if (obj instanceof ContentAccessor) {
			List<?> children = ((ContentAccessor) obj).getContent();
			for (Object child : children) {
				result.addAll(getAllElementFromObject(child, toSearch));
			}
		}
		return result;
	}
	
	//сохранить документ в ФС
	public void writeDocx(String savePath){
		File f = new File(savePath);
		try {
			template.save(f);
		} catch (Docx4JException e) {
			e.printStackTrace();
		}
		System.out.println("НАПЕЧАТАЛИ В ДОК");
	}
	
	
	//получить таблицу-образец 
	private void setTemplateTable() throws Docx4JException, JAXBException {
		Object tbl = this.allTables.get(0);
		this.templateTable = (Tbl) tbl;
		this.templateRow = (Tr) getAllElementFromObject(this.templateTable, Tr.class).get(1);
		this.templateRow2 = (Tr) getAllElementFromObject(this.templateTable, Tr.class).get(2);	
		this.templateEndRow = (Tr) XmlUtils.deepCopy((Tr) getAllElementFromObject(this.templateTable, Tr.class).get(3));
	}
	
	
	//добавление таблицы
	//возвращает кол-во вышедших фраз
	public int insertNewTable(Table table, String title, int topNum){
		int price = 0;
		int result = 0;// кол-во вышедших в топ строк
		
		ObjectFactory factory = Context.getWmlObjectFactory();
		
	    P pTitle = factory.createP(); 
	    
	    PPr pPr = factory.createPPr();
	    PStyle pStyle = factory.createPPrBasePStyle();
	    pStyle.setVal("Delta2");
        pPr.setPStyle(pStyle);
	    pTitle.setPPr(pPr);
	    R hrun = factory.createR();
	    Text htxt = new Text();
	    htxt.setValue(title);
	    hrun.getContent().add(htxt);
	    pTitle.getContent().add(hrun);
	    
	    
		Tbl newTbl = (Tbl) XmlUtils.deepCopy(this.templateTable);
		for(Line line : table.getLines()){
			addRowToTable(newTbl, line, topNum);
			if(line.getYa() <= topNum){
				result++;
				price += line.getPrice();
			}
				
		}
		removeFirstRow(newTbl);
		Tr summRow = (Tr) XmlUtils.deepCopy(this.templateEndRow);
		((Text)getAllElementFromObject(summRow, Text.class).get(1)).setValue("" + price);
		newTbl.getContent().add(summRow);
		
		template.getMainDocumentPart().getContent().add(pTitle); //параграф с названием таблицы
		template.getMainDocumentPart().getContent().add(factory.createP()); //пустой параграф-отступ
		template.getMainDocumentPart().getContent().add(newTbl);
		template.getMainDocumentPart().getContent().add(factory.createP());

		return result;
	}
	
	
	//добавить строку в таблицу
	public void addRowToTable(Tbl reviewtable, Line line, int topNum) {
		Tr workingRow;
		if(line.getYa() > topNum){
			workingRow = (Tr) XmlUtils.deepCopy(this.templateRow2);
		}else{
			workingRow = (Tr) XmlUtils.deepCopy(this.templateRow);
		}
		
		List<Object> textElements = getAllElementFromObject(workingRow, Text.class);
		Text text = (Text) textElements.get(0);
		text.setValue(line.getString());
		
		text = (Text) textElements.get(1);
		text.setValue("" + line.getYa());
		text = (Text) textElements.get(2);
		text.setValue("" + line.getPrice());
		
		reviewtable.getContent().add(reviewtable.getContent().size()-1, workingRow);
	}	
	
	//удалить таблицу-шаблон
	public void removeTemplateTable(){
		@SuppressWarnings("deprecation")
		Body body = this.template.getMainDocumentPart().getJaxbElement().getBody();
		body.getContent().remove(this.templateTable.getParent());
	}
	
	//удаляет первую и вторую строку таблицы(которая после заголовочной строки)
	private void removeFirstRow(Tbl table){
		table.getContent().remove(getAllElementFromObject(table.getParent(), Tr.class).get(2));
		table.getContent().remove(getAllElementFromObject(table.getParent(), Tr.class).get(1));
		table.getContent().remove(getAllElementFromObject(table.getParent(), Tr.class).get(table.getContent().size()-1));
	}
	
	public WordprocessingMLPackage getWMLP(){
		return this.template;
	}
}
