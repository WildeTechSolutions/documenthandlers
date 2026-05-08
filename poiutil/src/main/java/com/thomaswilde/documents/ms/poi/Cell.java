package com.thomaswilde.documents.ms.poi;

public class Cell {
	private int row, col;
	private TemplateCategory category;
	private String sqlTable;
	private String objectProperty;

	public Cell(String sqlTable, String objectProperty, int row, int col) {
		this.sqlTable = sqlTable;
		this.objectProperty = objectProperty;
		this.row = row;
		this.col = col;
	}

	public Cell(TemplateCategory category, int row, int col) {
		this.category = category;
		this.row = row;
		this.col = col;
	}

	public Cell(int row, int col) {
		this.row = row;
		this.col = col;
	}

	public Cell() {}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public int getCol() {
		return col;
	}

	public void setCol(int col) {
		this.col = col;
	}

	public TemplateCategory getCategory() {
		return category;
	}

	public void setCategory(TemplateCategory category) {
		this.category = category;
	}

	public String getSqlTable() {
		return sqlTable;
	}

	public void setSqlTable(String sqlTable) {
		this.sqlTable = sqlTable;
	}

	public String getObjectProperty() {
		return objectProperty;
	}

	public void setObjectProperty(String objectProperty) {
		this.objectProperty = objectProperty;
	}
}
