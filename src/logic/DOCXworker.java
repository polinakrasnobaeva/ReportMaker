package logic;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
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

import Utils.StaticStrings;
import plain.BonusEntry;
import plain.Line;
import plain.Table;

import org.docx4j.wml.R;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;

public class DOCXworker {
	
	private WordprocessingMLPackage template;
	private String resultFileName;
	
	private List<Object> allTables;
	private Tbl templateTable;
	private Tr templateRow;
	private Tbl templateTable2;
	private Tr templateRow2;
		
	
	public DOCXworker(String sourceFileName, String resultFileName) throws Docx4JException, JAXBException{
		this.resultFileName = resultFileName;
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
	public void writeDocx(){
		File f = new File(this.resultFileName);
		try {
			template.save(f);
		} catch (Docx4JException e) {
			e.printStackTrace();
		}
		System.out.println("НАПЕЧАТАЛИ В ДОК");
	}
	
	
	//получить таблицу-образец 
	private void setTemplateTable() throws Docx4JException, JAXBException {
		Iterator<Object> iterator = this.allTables.iterator();
		Object tbl = iterator.next();
		//первая таблица с тремя стобцами
		this.templateTable = (Tbl) tbl;
		this.templateRow = (Tr) getAllElementFromObject(this.templateTable, Tr.class).get(1);
		//вторая таблица с двумя столбцами
		tbl = iterator.next();
		this.templateTable2 = (Tbl) tbl;
		this.templateRow2 = (Tr) getAllElementFromObject(this.templateTable2, Tr.class).get(1);
			
	}
	
	
	
	//добавление таблицы 
	public void insertNewTable(Table table, String title, int lastPos, boolean isBonus){
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
			addRowToTable(newTbl, line, false, false, lastPos);
		}
		removeFirstRow(newTbl);
		
		if(!isBonus){
			template.getMainDocumentPart().getContent().add(pTitle); //параграф с названием таблицы
		}
		template.getMainDocumentPart().getContent().add(factory.createP()); //пустой параграф-отступ
		template.getMainDocumentPart().getContent().add(newTbl);
		template.getMainDocumentPart().getContent().add(factory.createP());

	}
	
//добавление таблицы с двумя столбцами
	public void insertNewTableWithTwoColumns(Table table, String title, int lastPos, boolean isBonus){
		ObjectFactory factory = Context.getWmlObjectFactory();
		
		boolean isGoogle = title.equals(StaticStrings.googleString);
		
		if(!isGoogle && !isBonus){

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
		    
	    	template.getMainDocumentPart().getContent().add(pTitle); //параграф с названием таблицы
		    
		}
		
		
		Tbl newTbl = (Tbl) XmlUtils.deepCopy(this.templateTable2);
		Tr header = (Tr) getAllElementFromObject(newTbl, Tr.class).get(0);
		List<Object> textElements = getAllElementFromObject(header, Text.class);
		Text systemText = (Text) textElements.get(1);
		if(isGoogle){
			systemText.setValue("Google");
		}else{
			systemText.setValue("Яндекс");
		}
		
		
		for(Line line : table.getLines()){
			addRowToTable(newTbl, line, true, isGoogle, lastPos);
		}
		removeFirstRow(newTbl);
		
		
		template.getMainDocumentPart().getContent().add(factory.createP()); //пустой параграф-отступ
		template.getMainDocumentPart().getContent().add(newTbl);
		template.getMainDocumentPart().getContent().add(factory.createP());

	}
	
	public void insertBonusTables(ArrayList<BonusEntry> tablesForBonus, String title){
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
		
	    template.getMainDocumentPart().getContent().add(pTitle); //параграф с названием таблицы
	    
		for(BonusEntry be : tablesForBonus){
			if(be.isOneColumn()){
				insertNewTableWithTwoColumns(be.getTable(), (be.getTitle().contains(StaticStrings.googleString) ? StaticStrings.googleString : "YANDEX"), be.getLastPos(), true);
			}else{
				insertNewTable(be.getTable(), be.getTitle(), be.getLastPos(), true);
			}
		}
	}
	
	
	//добавить строку в таблицу
	public void addRowToTable(Tbl reviewtable, Line line, boolean twoColumns, boolean isGoogle, int lastPos) {
		Tr workingRow;
		if(twoColumns){
			workingRow = (Tr) XmlUtils.deepCopy(this.templateRow2);
		}else{
			workingRow = (Tr) XmlUtils.deepCopy(this.templateRow);
		}
		
		List<Object> textElements = getAllElementFromObject(workingRow, Text.class);
		Text text = (Text) textElements.get(0);
		text.setValue(line.getString());
		
		text = (Text) textElements.get(1);
		String goo = (line.getGoo() > lastPos ? "" : "" + line.getGoo());
		String ya = (line.getYa() > lastPos ? "" : "" + line.getYa());
		if(twoColumns){
			text.setValue(isGoogle ? goo : ya);
		}else{
			text.setValue("" + ya);
			text = (Text) textElements.get(2);
			text.setValue("" + goo);
		}
		 
		reviewtable.getContent().add(workingRow);
	}	
	
	//удалить таблицу-шаблон
	public void removeTemplateTable(){
		@SuppressWarnings("deprecation")
		Body body = this.template.getMainDocumentPart().getJaxbElement().getBody();
		body.getContent().remove(this.templateTable.getParent());
		body.getContent().remove(this.templateTable2.getParent());

	}
	
	//удаляет первую строку таблицы(которая после заголовочной строки)
	private void removeFirstRow(Tbl table){
		table.getContent().remove(getAllElementFromObject(table.getParent(), Tr.class).get(1));
	}
	
	public WordprocessingMLPackage getWMLP(){
		return this.template;
	}
}
