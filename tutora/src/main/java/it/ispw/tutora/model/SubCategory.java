package it.ispw.tutora.model;

public class SubCategory {
    private final String name;
    private final Category parentname;
    private final String description;
    public SubCategory(String name, Category parentname, String description){
        this.name=name;
        this.parentname=parentname;
        this.description=description;
    }
    public String getName(){
        return name;
    }
    public Category getParentName() {
        return parentname;
    }
    public String getDescription(){
        return description;
    }
    @Override
    public String toString() {
        return name + " (" + parentname.getName() + ")";
    }
}
