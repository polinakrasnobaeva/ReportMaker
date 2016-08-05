package plain;

public class BonusEntry{
	private Table table;
	private boolean isOneColumn;
	private String title;
	private int lastPos;
	
	public BonusEntry(Table table, boolean isOneColumn, String title, int lastPos){
		this.setTable(table);
		this.setOneColumn(isOneColumn);
		this.setTitle(title);
		this.setLastPos(lastPos);
	}
	public Table getTable() {
		return table;
	}
	public void setTable(Table table) {
		this.table = table;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getLastPos() {
		return lastPos;
	}
	public void setLastPos(int lastPos) {
		this.lastPos = lastPos;
	}
	public boolean isOneColumn() {
		return isOneColumn;
	}
	public void setOneColumn(boolean isOneColumn) {
		this.isOneColumn = isOneColumn;
	}
}
