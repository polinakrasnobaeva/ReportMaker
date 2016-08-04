package logic;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.contenttype.ContentType;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.AlternativeFormatInputPart;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.Body;
import org.docx4j.wml.CTAltChunk;
import org.docx4j.wml.P;
import org.docx4j.wml.P.Hyperlink;

import org.docx4j.wml.Text;

public class ReportAssembler {
	
	@SuppressWarnings("deprecation")
	public static void assebmle(String sourceFileName, WordprocessingMLPackage WMLPTables, String sourceMetricaFileName, String resultFileName,
			 HashMap<String, String> values, String root
			) throws Docx4JException, JAXBException, IOException{
		//TODO сделать копирование шаблона
		
		WordprocessingMLPackage doc = WordprocessingMLPackage.load(new FileInputStream(new File(sourceFileName)));
		List<Object> allParagraphs = DOCXworker.getAllElementFromObject(doc.getMainDocumentPart(), P.class);
		
		//Имя сайта
		Text string = (Text)findByPlaceholderText(allParagraphs, "phsitename");	
		String siteName = values.get("clientsite");
		string.setValue(siteName.startsWith("www.") ? siteName : "www." + siteName);
		//месяц и год отчета
		string = (Text)findByPlaceholderText(allParagraphs, "phmonth");
		string.setValue(values.get("date"));
		//к кому обращаемся
		string = (Text)findByPlaceholderText(allParagraphs, "phdirectorname");
		string.setValue(values.get("clientappeal") + ",");
		//вставляем таблицы из файла
		addContentToFileAt(doc, WMLPTables, "phtableplace");
		
		
		//-----вставляем метрику
		//вытаскиваем из docx afchunk.mht
		ZipInputStream zin = new ZipInputStream(new FileInputStream(sourceMetricaFileName));
        ZipEntry entry;
        String mhtmlContent = "";
        while ((entry = zin.getNextEntry()) != null) {
        	if(entry.getName().equals("word/afchunk.mht")){
        		mhtmlContent = IOUtils.toString(zin, "utf-8");
        		zin.closeEntry();
                break;
        	}
            zin.closeEntry();
        }
        zin.close();
        
        //дописываем стили в afchunk
        FileInputStream fis = new FileInputStream(root + File.separator + "config" + File.separator + "_styles_api.css");
		InputStreamReader isr = new InputStreamReader(fis);
		BufferedReader bReader = new BufferedReader(isr);
		String stylesContent = "";
		String buf = "";
		while((buf = bReader.readLine()) != null){
			stylesContent += buf + "\n";
		};
		bReader.close();
		mhtmlContent = mhtmlContent.replace("<style>@import 'http://xn----7sbkbf0bzcxeva.xn--p1ai/_css/_styles_api.css'</style>", 
        		"\n<style>\n" + stylesContent + "\n</style>\n");
		
		//вставляем чанк в документ
        AlternativeFormatInputPart afiPart = new AlternativeFormatInputPart(new PartName("/afchunk.mht")); 
        afiPart.setBinaryData(mhtmlContent.getBytes("UTF-8")); 
        afiPart.setContentType(new ContentType("message/rfc822")); 
        Relationship altChunkRel = doc.getMainDocumentPart().addTargetPart(afiPart); 
        CTAltChunk ac = Context.getWmlObjectFactory().createCTAltChunk();
        ac.setId(altChunkRel.getId()); 
        doc.getContentTypeManager().addDefaultContentType("mht", "message/rfc822");
        doc.getMainDocumentPart().convertAltChunks();
 
        P p = findByPlaceholderParagraph(allParagraphs, "phmetrica");

        int n = doc.getMainDocumentPart().getContent().indexOf(p) - 1;
        doc.getMainDocumentPart().getContent().add(n, ac); 
        doc.getMainDocumentPart().getJaxbElement().getBody().getContent().remove(p);
        
        //------и это все чтобы вставить метрику
        
        
		//вставляем номер оптимизатора
		string = (Text)findByPlaceholderText(allParagraphs, "phphonenumber");
		string.setValue(values.get("optnumber"));
		//вставляем эл. почту
		Hyperlink link = createHyperlink(doc, values.get("optemail"));
		P paragraph = findByPlaceholderParagraph(allParagraphs, "phmailto");
		paragraph.getParagraphContent().add(link);
		Text t = findByPlaceholderText(allParagraphs, "phmailto");
		t.setValue("");
		//сохраняем в новый док
		writeDocx(doc, resultFileName);
	}
	
	private static Text findByPlaceholderText(List<Object> objectList, String phText) throws Docx4JException, JAXBException {
		for (Iterator<Object> iterator = objectList.iterator(); iterator.hasNext();) {
			Object obj = iterator.next();
			List<?> textElements = DOCXworker.getAllElementFromObject(obj, Text.class);
			for (Object text : textElements) {
				Text textElement = (Text) text;
				if (textElement.getValue() != null && textElement.getValue().equals(phText)){
					System.out.println("Текст параграфа с плейсхолдером " + phText + " нашелся");
					return textElement;
				}
			}
		}
		System.out.println("Параграф с плейсхолдером " + phText + " не нашелся");
		return null;
	}
	
	private static P findByPlaceholderParagraph(List<Object> objectList, String phText) throws Docx4JException, JAXBException {
		for (Iterator<Object> iterator = objectList.iterator(); iterator.hasNext();) {
			Object obj = iterator.next();
			List<?> textElements = DOCXworker.getAllElementFromObject(obj, Text.class);
			for (Object text : textElements) {
				Text textElement = (Text) text;
				if (textElement.getValue() != null && textElement.getValue().equals(phText)){
					System.out.println("Параграф с плейсхолдером " + phText + " нашелся");
					return (P) obj;
				}
			}
		}
		System.out.println("Параграф с плейсхолдером " + phText + " не нашелся");
		return null;
	}
	
	private static void writeDocx(WordprocessingMLPackage doc, String saveFilePath){
		File f = new File(saveFilePath);
		try {
			doc.save(f);
		} catch (Docx4JException e) {
			System.out.println("Не удалось сохранить в файл");
		}
		System.out.println("НАПЕЧАТАЛИ В ДОК");
	}
	
	private static void addContentToFileAt(WordprocessingMLPackage whereTo, WordprocessingMLPackage whereFrom, String placeholderText) throws Docx4JException, JAXBException{
		List<Object> docTablecContent = whereFrom.getMainDocumentPart().getContent();
		
		List<Object> allParagraphs = DOCXworker.getAllElementFromObject(whereTo.getMainDocumentPart(), P.class);
		P p = findByPlaceholderParagraph(allParagraphs, placeholderText);
		int n = whereTo.getMainDocumentPart().getContent().indexOf(p);
		n--;
		
		for(Object o : docTablecContent){
			whereTo.getMainDocumentPart().getContent().add(n++, o);
		}
		
		@SuppressWarnings("deprecation")
		Body body = whereTo.getMainDocumentPart().getJaxbElement().getBody(); //удаляем плейсхолдер
		body.getContent().remove(p);
	}
	
	private static Hyperlink createHyperlink(WordprocessingMLPackage wordMLPackage, String url) {
		
		try {
			org.docx4j.relationships.ObjectFactory factory =
				new org.docx4j.relationships.ObjectFactory();
			
			org.docx4j.relationships.Relationship rel = factory.createRelationship();
			rel.setType( Namespaces.HYPERLINK  );
			rel.setTarget("mailto:" + url);
			rel.setTargetMode("External");  
									
			wordMLPackage.getMainDocumentPart().getRelationshipsPart().addRelationship(rel);
			
			String hpl = "<w:hyperlink r:id=\"" + rel.getId() + "\" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
            "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" >" +
            "<w:r>" +
            "<w:rPr>" +
			"<w:rStyle w:val=\"a4\" />" +
            "<w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\" w:cs=\"Arial\" />" +
            "<w:i w:val=\"0\" />" +
            "<w:iCs w:val=\"0\" />" +
            "<w:sz w:val=\"22\" />" +
            "</w:rPr>" +
            "<w:t>" + url + "</w:t>" +
            "</w:r>" +
            "</w:hyperlink>";

			return (Hyperlink)XmlUtils.unmarshalString(hpl);
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
}