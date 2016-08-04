package plain;

import java.util.List;


public class Blank {
	
	private String name;
	private List<Optimizer> optimizers;
	private String fullName;
	
	public Blank(String name){
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Optimizer> getOptimizers() {
		return optimizers;
	}
	public void setOptimizers(List<Optimizer> optimizers) {
		this.optimizers = optimizers;
	}
	
	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	
	
}
