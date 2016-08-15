<%@page import="java.text.DateFormat"%>
<%@page import="java.time.Instant"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.LinkedHashMap"%>
<%@page import="java.io.File, summCounterModule.Reacher, singletons.ClientWorker, plain.Blank, plain.Optimizer, java.util.HashMap, java.util.Map.Entry, java.util.List, java.util.Locale, java.util.Calendar" %>
<%@page language="java" contentType="text/html; charset=UTF-8"%><%@page pageEncoding="UTF-8"%>
<%String lineBreaker="&#013;&#010;";
Calendar c = Calendar.getInstance();
String date = c.getDisplayName(Calendar.MONTH, Calendar.LONG_STANDALONE, new Locale.Builder().setLanguage("ru").setScript("Cyrl").build()) + ", " + c.get(Calendar.YEAR);

HashMap<String, Blank> blankList = (HashMap<String, Blank>)request.getAttribute("blanklist");
Blank selectedBlank = null;
if(blankList != null){
	selectedBlank = blankList.get(request.getParameter("blank"));
}

Optimizer selectedOptimizer = null;
Entry<String, String> selectedClient = null;

ClientWorker cw = (ClientWorker)request.getAttribute("clientworker");

Boolean prices = (request.getParameter("prices") != null );%>
<!DOCTYPE html>
<html>
<head>
<script type="text/javascript" src="./js/Chart.min.js"></script>
<link href="./css/style.css" rel="stylesheet">
<link rel="icon" href="favicon.ico" type="image/x-icon">
<link rel="shortcut icon" href="favicon.ico" type="image/x-icon">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>ReportMaker</title>
</head>
<body>

<div id="wrapper">
		<form name="blankform" class="region-form" accept-charset="utf-8" method="get">
		<div class="top"><div class="header">
			<h1>Report_Maker</h1>
		</div></div>
		<div class="header"><h2>Бланк</h2></div>
		<div class="content">
		<select class="input" name="blank">
		<%	String blankParameter = (String)request.getParameter("blank");
			out.println("<option disabled selected>Выберите бланк</option>");
			for(Entry<String, Blank> e : blankList.entrySet()){
				out.println("<option value=\"" + e.getKey() + "\"" +
							(blankParameter!= null && e.getKey().equals(blankParameter) ? " selected" : "") + ">" 
							+ e.getValue().getFullName() + "</option>");
			}
		%>
		</select>
		<br>	
		<%
		if(selectedBlank != null){
			String optParameter = (String)request.getParameter("opt");
			out.println("<select class=\"input\" name=\"opt\">");
			out.println("<option disabled selected>Выберете оптимизатора</option>");
			for(Optimizer opt : selectedBlank.getOptimizers()){
				out.println("<option value=\"" + opt.getNick() + "\"" +
						(opt.getNick().equals(optParameter) ? " selected" : "") + ">" 
						+ opt.getName() + "</option>");
				if(opt.getNick().equals(optParameter)){
					selectedOptimizer = opt;
				}
				
			}
			out.println("</select><br>");
		}
		%>
		<%
		if(cw != null && selectedBlank != null){
			String clParameter = (String)request.getParameter("client");
			out.println("<select class=\"input\" name=\"client\">");
			out.println("<option disabled selected>Выберете клиента(если есть в списке)</option>");
			for(Entry<String, String> cl : cw.getClients().entrySet()){
				out.println("<option value=\"" + cl.getKey() + "\"" +
						(cl.getKey().equals(clParameter) ? " selected" : "") + ">" 
						+ cl.getKey() + "</option>");
				if(cl.getKey().equals(clParameter)){
					selectedClient = cl;
				}
				
			}
			out.println("</select><br>");
		}
		%>
		<div class="checkbox"><input type="checkbox" name="prices"<%= (prices ? " checked" : "") %>>По выходу</div>
		
		</div>
		<div class="footer">
			<input class="button" type="submit" value="Подтвердить">
		</div>
		</form>
	<hr>
	
	<!-- График по выходу -->
	
	<%if(selectedClient != null){
		Reacher r = new Reacher(getServletContext().getRealPath("config" + File.separator + "reachers"), selectedClient.getKey());
		if (r.exists()){
			LinkedHashMap<Long, Float> stats = r.loadStats();
			DateFormat df = DateFormat.getInstance();
			out.println("<canvas id='reachChart' width='800' height='350'></canvas>");
			%><script>
				var ctx = document.getElementById("reachChart");
				var myChart = new Chart(ctx, {
				    type: 'bar',
				    data: {
				        labels: [<%
				        			for(Entry<Long, Float> ent : stats.entrySet()){
				        				out.print("\"" + df.format(Date.from(Instant.ofEpochMilli(ent.getKey()))) + "\"" + ",");
				     				}
				                 %>],
				        datasets: [{
				            label: 'Проценты по выходу фраз',
				            data: [<%
				        			for(Entry<Long, Float> ent : stats.entrySet()){
				        				out.print("\"" + ent.getValue() + "\"" + ",");
				     				}
				                 %>],
				            borderWidth: 1
				        }]
				    },
				    options: {
				        scales: {
				            yAxes: [{
				                ticks: {
				                    beginAtZero:true,
				                    max:100
				                }
				            }]
				        }
				    }
				});
			</script><%
		}	
	}%>
	
	<form name="reportform" class="region-form" method="post" action="reportupload" method="post" enctype="multipart/form-data">
		<input type="hidden" name="blankName" value="<%= (selectedBlank == null ? "" : selectedBlank.getName()) %>">
		<div class="top"><div class="header"><h2>Файл с Метрикой(.docx)</h2></div>
		<div class="content">
		<input class="upload" type="file" accept="application/vnd.openxmlformats-officedocument.wordprocessingml.document" name="uploadMetrica" required/>
		</div></div>
		<div class="header"><h3>Оптимизатор</h3></div>
		<div class="content"><input class="input" type="text" placeholder="Номер оптимизатора" name="optnumber" value="<%= (selectedOptimizer != null ? selectedOptimizer.getPhone() : "") %>"></div>
		<div class="content"><input class="input" type="text" placeholder="Email оптимизатора" name="optemail" value="<%= (selectedOptimizer != null ? selectedOptimizer.getMail() : "") %>"></div>
		<div class="header"><h3>Клиент</h3></div>
		<div class="content"><input class="input" type="text" placeholder="Сайт клиента" name="clientsite" value="<%=(selectedClient == null ? "" : selectedClient.getKey()) %>"></input></div>
		<div class="content"><input class="input" type="text" placeholder="Уважаемый XXX" name="clientappeal" value="<%=(selectedClient == null ? "" : selectedClient.getValue()) %>"></input></div>
		<div class="content"><input class="input" type="text" placeholder="Дата" name="date" value="<%=date%>"></input></div>
		<hr>
		<div class="top">
		<div class="header"><h3>Файл CSV с выгрузкой</h3></div>
		<div class="content"><input class="upload" type="file" accept="text/csv" name="uploadFile" required/></div>
		</div>
		<%if(prices) {%>
			<div class="top">
			<div class="header"><h3>Файл CSV с ценами</h3></div>
			<div class="content"><input class="upload" type="file" accept="text/csv" name="uploadPricesFile" required/></div>
			</div>
			<div class="header"><span>Регион продвижения</span></div>
			<div class="content"><input class="input" type="text" name="promoRegion" value="<%=(selectedBlank == null ? "" : selectedBlank.getFullName()) %>"></input></div>
			<div class="header"><h3>Настройки выгрузки</h3></div>
			<div class="header"><h4>Таблицы</h4></div>
			<div class="checkbox"><input type="checkbox" name="isSingles" checked>Фразы без добавлений |
			по выходу?<input type="checkbox" name="isSinglesReach" checked></div>
			<div class="checkbox"><input type="checkbox" name="isRostov" checked>Ростов-на-Дону |
			по выходу?<input type="checkbox" name="isRostovReach" checked></div>
			<div class="checkbox"><input type="checkbox" name="isCities">Другие города |
			по выходу?<input type="checkbox" name="isCitiesReach"></div>
			<div class="checkbox"><input type="checkbox" name="isAddWords">Дополнительные слова |
			по выходу?<input type="checkbox" name="isAddWordsReach"></div>		
			<div class="header"><h4>Доп. настройки</h4></div>
			<div class="checkbox"><input type="checkbox" name="isNeedTableChecking" checked>Проверять целостность таблиц (не всегда работает)</div>
			<div class="checkbox"><input type="checkbox" name="isBestLineToTop" checked>"Вытягивать" лучшие строки</div>
			<div class="header"><span>Топ:</span></div>
			<div class="content"><input class="input" type="number" name="topNum" value="10"></input></div>
			<div class="header"><span>А также оставлять до:</span></div>
			<div class="content"><input class="input" type="number" name="leaveNum" value="15"></input></div>
			<input type="hidden" name="tabletype" value="prices">
		<%}else{ %>
			<div class="header"><span>Регион продвижения</span></div>
			<div class="content"><input class="input" type="text" name="promoRegion" value="<%=(selectedBlank == null ? "" : selectedBlank.getFullName()) %>"></input></div>
			<div class="header"><h3>Настройки выгрузки</h3></div>
			<div class="header"><h4>Таблицы</h4></div>
			<div class="checkbox"><input type="checkbox" name="isSingles" checked>Фразы без добавлений |
			раздельно:<input type="checkbox" name="isSepSingles" checked>
			Бонус?<input type="checkbox" name="isSinglesBonus" checked></div>
			<div class="checkbox"><input type="checkbox" name="isRostov" checked>Ростов-на-Дону |
			раздельно:<input type="checkbox" name="isSepRostov">
			Бонус?<input type="checkbox" name="isRostovBonus"></div>
			<div class="checkbox"><input type="checkbox" name="isCities">Другие города |
			раздельно:<input type="checkbox" name="isSepOther">
			Бонус?<input type="checkbox" name="isCitiesBonus"></div>
			<div class="checkbox"><input type="checkbox" name="isAddWords">Дополнительные слова |
			раздельно:<input type="checkbox" name="isSepAddw">
			Бонус?<input type="checkbox" name="isAddWordsBonus"></div>
			
			<div class="header"><h4>Доп. настройки</h4></div>
			<div class="checkbox"><input type="checkbox" name="isNeedTableChecking" checked>Проверять целостность таблиц (не всегда работает)</div>
			<div class="checkbox"><input type="checkbox" name="isBestLineToTop" checked>"Вытягивать" лучшие строки</div>
			<div class="checkbox"><input type="checkbox" name="leaveEmpty">Оставлять "ненужные" строчки</div><br>
			<div class="header"><span>Последняя позиция бонус</span></div><div class="content"><input class="input" type="number" name="lastBonusPos" value="16"></input></div>
			<div class="header"><span>Последняя позиция остальные</span></div><div class="content"><input class="input" type="number" name="lastCitiesPos" value="28"></input></div>
			<input type="hidden" name="tabletype" value="usual">
		<%}%>
		
		<div class="footer"><input class="button" type="submit" value="Делать"></div>
	</form>
</div>		
	
</body>
</html>