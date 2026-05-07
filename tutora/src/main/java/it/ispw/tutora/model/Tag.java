package it.ispw.tutora.model;

/**
 * Entity che rappresenta un tag associato a una competenza tutor
 * (es. #Sax, #Blues, #InPresenza).
 *
 * Corrisponde alla tabella tag del DB.
 *
 * Classe immutabile: un tag non cambia mai dopo la creazione.
 */
public class Tag {

    private final String name;

    public Tag(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}
