package Servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.MimeUtility;

import logic.ReportAssembler;
import logic.ReportBuilder;
import summCounterModule.DOCXworker;

public class UploadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	private Random random = new Random(System.currentTimeMillis());
	
    public UploadServlet() {
        super();
    }


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		HashMap<String, String> values = new HashMap<>();
		String csvPath = null;
		String metricaPath = null;
		String csvPricesPath = null;
		
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if (!isMultipart) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(1024*1024);
		File tempDir = (File)getServletContext().getAttribute("javax.servlet.context.tempdir");
		factory.setRepository(tempDir);
		ServletFileUpload upload = new ServletFileUpload(factory);
		upload.setSizeMax(1024 * 1024 * 10);
		upload.setHeaderEncoding("cp1251");
	
		try {
			List<FileItem> items = upload.parseRequest(request);
			Iterator<FileItem> iter = items.iterator();
			
			while (iter.hasNext()) {
			    FileItem item = (FileItem) iter.next();
 
			    if (item.isFormField()) {
			    	//если принимаемая часть данных является полем формы
			    	values.put(item.getFieldName(), item.getString("utf-8"));
			    } else {
			    	//в противном случае рассматриваем как файл
			    	if(item.getFieldName().equals("uploadMetrica")){
			    		metricaPath = processUploadedFile(item, "_metrica.docx");
			    	}else if(item.getFieldName().equals("uploadFile")){
			    		csvPath = processUploadedFile(item, "_tables.csv");
			    	}else if(item.getFieldName().equals("uploadPricesFile")){
			    		csvPricesPath = processUploadedFile(item, "_prices.csv");
			    	}
			    	
			    }
			}			
		} catch (Exception e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		String examplePath = getServletContext().getRealPath("config" + File.separator + "EXAMPLES" + File.separator + values.get("blankName") + ".docx");
		String resultFileName = getServletContext().getRealPath("result" + File.separator + "REPORT_" + random.nextInt() + ".docx");
		String docExamplePath = "";
		System.out.println(examplePath);
		//создание файла docx с таблицами
        
        
        
		if(((String)values.get("tabletype")).equals("usual")){
			docExamplePath = getServletContext().getRealPath("config" + File.separator + "EXAMPLE.docx");
			ReportBuilder tableBuilder = new ReportBuilder(csvPath, values);
			WordprocessingMLPackage tableWMLP = tableBuilder.buildReport(docExamplePath).getWMLP();
			//объединение
			try {
				ReportAssembler.assebmle(examplePath, tableWMLP, metricaPath, resultFileName, values, getServletContext().getRealPath(File.separator));
			} catch (Docx4JException e) {
				
				e.printStackTrace();
			} catch (JAXBException e) {
				
				e.printStackTrace();
			}
		}else if(((String)values.get("tabletype")).equals("prices")){
			docExamplePath = getServletContext().getRealPath("config" + File.separator + "priceEXAMPLE.docx");
			summCounterModule.ReportBuilder tableBuilder = new summCounterModule.ReportBuilder(csvPath, csvPricesPath, values);
			DOCXworker dw = tableBuilder.buildReport(docExamplePath);
			System.out.println("ВЫШЛО ФРАЗ: " + dw.getTopPhraseCount());
			WordprocessingMLPackage tableWMLP = dw.getWMLP();
			try {
				ReportAssembler.assebmle(examplePath, tableWMLP, metricaPath, resultFileName, values, getServletContext().getRealPath(File.separator));
			} catch (Docx4JException e) {
				
				e.printStackTrace();
			} catch (JAXBException e) {
				
				e.printStackTrace();
			}
			File csvPricesFile = new File(csvPricesPath);
			csvPricesFile.delete();
		}
		File csvFile = new File(csvPath);
		File metricaFile = new File(metricaPath);
        csvFile.delete();
        metricaFile.delete();
		
		
		///отправка файла на скачивание
			
		File resultFile = new File(resultFileName);
		
        String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        response.setContentType(fileType);
        String filename = "ОТЧЕТ - " + values.get("clientsite") + " - " + values.get("date").substring(0, values.get("date").indexOf(',')) + ".docx";
        filename = MimeUtility.encodeText(filename, "utf-8", "B");
        
        response.setHeader("Content-disposition","attachment; "
        		+ "filename=\"" + filename + "\"");

        OutputStream out = response.getOutputStream();
        FileInputStream in = new FileInputStream(resultFile);
        byte[] buffer = new byte[4096];
        int length;
        while ((length = in.read(buffer)) > 0){
           out.write(buffer, 0, length);
        }
        in.close();
        out.flush();
        
        resultFile.delete();

        MainServlet.cw.addClient(values.get("clientsite").replace("http://", "").replace("/", "").replace("www", ""), values.get("clientappeal"));
		
	}
	
	private String processUploadedFile(FileItem item, String extension) throws Exception {
		File uploadedFile = null;
		String path;
		do{
			path = getServletContext().getRealPath("/upload/" + random.nextInt() + extension);					
			uploadedFile = new File(path);		
		}while(uploadedFile.exists());
		uploadedFile.createNewFile();
		item.write(uploadedFile);
		return path;
	}
	
}
