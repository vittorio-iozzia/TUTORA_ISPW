package it.ispw.tutora.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity di dominio che rappresenta una categoria di tutoraggio
 * (es. Music, Photography, Sport).
 *
 * Contiene la lista dei requisiti che uno studente deve soddisfare
 * per candidarsi come tutor in questa categoria (UC-2).
 */
public class Category {

    private final String name;
    private final String description;
    private final List<Requirement> requirements;

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
        this.requirements = new ArrayList<>();
    }

    public void addRequirement(Requirement requirement) {
        requirements.add(requirement);
    }

    /**
     * Restituisce una vista non modificabile dei requisiti.
     * Il chiamante non può alterare la lista interna.
     */
    public List<Requirement> getRequirements() {
        return Collections.unmodifiableList(requirements);
    }

    public boolean hasRequirements() {
        return !requirements.isEmpty();
    }

    public String getName() { return name; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "Category{name='" + name + "'}";
    }
}