package plain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Optimizer {
	private static Pattern p = Pattern.compile("(.*?)!(.*?)!(.*?)!(.*?)$");
	String nick;
	String name;
	String phone;
	String mail;
	
	public Optimizer(String line){
		Matcher m = p.matcher(line);
		m.find();
		this.nick = m.group(1);
		this.name = m.group(2);
		this.phone = m.group(3);
		this.mail = m.group(4);
	}
	
	public String getNick() {
		return nick;
	}
	public void setNick(String nick) {
		this.nick = nick;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getMail() {
		return mail;
	}
	public void setMail(String mail) {
		this.mail = mail;
	}
	
}
