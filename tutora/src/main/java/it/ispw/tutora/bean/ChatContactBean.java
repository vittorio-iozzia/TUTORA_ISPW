package it.ispw.tutora.bean;

/**
 * Bean di trasferimento per un contatto della chat.
 * Prodotto da {@link it.ispw.tutora.controller.application.ChatController}
 * e consumato da {@link it.ispw.tutora.controller.graphic.ChatGfxController}.
 */
public class ChatContactBean {

    private final String username;
    private final String fullName;
    private final String role;

    public ChatContactBean(String username, String fullName, String role) {
        this.username = username;
        this.fullName = fullName;
        this.role     = role;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole()     { return role; }
}
