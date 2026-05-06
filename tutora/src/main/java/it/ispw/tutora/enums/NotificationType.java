package it.ispw.tutora.enums;

/**
 * Tipi di notifica del sistema TUTORA.
 * Specchia il campo ENUM della tabella notification.
 */
public enum NotificationType {
    APPLICATION_UPDATE,   // Esito application tutor → studente
    EXPERTISE_OFFER,      // Nuova offerta tutoring → admin
    LESSON_BOOKED,        // Nuova prenotazione → tutor
    LESSON_ACCEPTED,      // Tutor ha accettato → studente
    LESSON_REJECTED,      // Tutor ha rifiutato → studente
    PAYMENT_CONFIRMED,    // Pagamento confermato → studente
    NEW_REVIEW            // Nuova recensione → tutor
}