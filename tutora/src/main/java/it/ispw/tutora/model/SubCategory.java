package it.ispw.tutora.model;

public class SubCategory {
    private final String name;
    private final Category parentCategory;
    private final String description;
    public SubCategory(String name, Category parentCategory, String description){
        this.name=name;
        this.parentCategory = parentCategory;
        this.description=description;
    }
    public String getName(){
        return name;
    }
    public Category getParentName() {
        return parentCategory;
    }
    public String getDescription(){
        return description;
    }
    @Override
    public String toString() {
        String categoryName = parentCategory != null ? parentCategory.getName() : "N/A";
        return name + " (" + categoryName + ")";
    }
}
