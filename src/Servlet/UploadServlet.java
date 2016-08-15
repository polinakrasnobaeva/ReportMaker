package Servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
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
import summCounterModule.Reacher;



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
			    	values.put(item.getFieldName(), item.getString("utf-8"));
			    } else {
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
        
        int topCount = 0;
        int totalCount = 1;
        
		if(((String)values.get("tabletype")).equals("usual")){
			docExamplePath = getServletContext().getRealPath("config" + File.separator + "EXAMPLE.docx");
			logic.ReportBuilder tableBuilder = new logic.ReportBuilder(csvPath, values);
			WordprocessingMLPackage tableWMLP = tableBuilder.buildReport(docExamplePath).getWMLP();
			try {
				ReportAssembler.assebmle(examplePath, tableWMLP, metricaPath, resultFileName, values, getServletContext().getRealPath(File.separator));
			} catch (Docx4JException e) {
				
				e.printStackTrace();
			} catch (JAXBException e) {
				
				e.printStackTrace();
			}
			topCount = tableBuilder.topPhraseCount;
			totalCount = tableBuilder.totalPhraseCount;
		}else if(((String)values.get("tabletype")).equals("prices")){
			docExamplePath = getServletContext().getRealPath("config" + File.separator + "priceEXAMPLE.docx");
			summCounterModule.ReportBuilder tableBuilder = new summCounterModule.ReportBuilder(csvPath, csvPricesPath, values);
			summCounterModule.DOCXworker dw = tableBuilder.buildReport(docExamplePath);
			
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
			
			topCount = tableBuilder.topPhraseCount;
			totalCount = tableBuilder.totalPhraseCount;
		}
		
		//пушим статы
		String clientSite = (new String(values.get("clientsite").getBytes(), "cp1251")).replace("http://", "").replace("https://", "").replace("/", "").replace("www.", "");
		System.out.println("СТРОКА: " + clientSite);
		
		Reacher r = new Reacher(getServletContext().getRealPath("config" + File.separator + "reachers"), clientSite);
		LinkedHashMap<Long, Float> stats = r.loadStats();
		Calendar currentC = Calendar.getInstance();
		if(stats != null && stats.size() > 0){
			Entry <Long, Float> lastEntry = null;
			lastEntry = null;
			for(Entry<Long, Float> st : stats.entrySet()){
				lastEntry = st;
			}
			Calendar lastC = Calendar.getInstance();
			if(lastEntry != null){
				lastC.setTime(Date.from(Instant.ofEpochMilli(lastEntry.getKey())));
				if(lastC.get(Calendar.YEAR) == currentC.get(Calendar.YEAR) && lastC.get(Calendar.WEEK_OF_YEAR) == currentC.get(Calendar.WEEK_OF_YEAR)){
					stats.remove(lastEntry.getKey());
				}
			}

		}else{
			stats = new LinkedHashMap<>();
		}
					
		stats.put(currentC.getTimeInMillis(), (float)topCount / totalCount * 100);
		System.out.println(getServletContext().getRealPath("config" + File.separator + "reachers") + File.separator 
						+ clientSite + ".txt");
		Reacher.pushStats(
				getServletContext().getRealPath("config" + File.separator + "reachers" + File.separator 
						+ clientSite + ".txt"), stats);
		System.out.println("" + topCount + "\t" + totalCount);
		////////////////////////////////////////////
		
		File csvFile = new File(csvPath);
		File metricaFile = new File(metricaPath);
        csvFile.delete();
        metricaFile.delete();
		
			
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

        MainServlet.cw.addClient(values.get("clientsite").replace("http://", "").replace("https://", "").replace("/", "").replace("www.", ""), values.get("clientappeal"));
		
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
