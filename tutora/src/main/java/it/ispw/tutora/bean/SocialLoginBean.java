package it.ispw.tutora.bean;

/**
 * Transport object per il profilo utente restituito da un provider OAuth2.
 * Usato dalle Boundary (GoogleAuthBoundary, MetaAuthBoundary) per passare
 * i dati al SocialLoginController.
 */
public class SocialLoginBean {

    private final String provider;
    private final String oauthId;
    private final String email;
    private final String name;
    private final String surname;

    public SocialLoginBean(String provider, String oauthId,
                           String email, String name, String surname) {
        this.provider = provider;
        this.oauthId  = oauthId;
        this.email    = email;
        this.name     = name;
        this.surname  = surname;
    }

    public String getProvider() { return provider; }
    public String getOauthId()  { return oauthId; }
    public String getEmail()    { return email; }
    public String getName()     { return name; }
    public String getSurname()  { return surname; }
}
