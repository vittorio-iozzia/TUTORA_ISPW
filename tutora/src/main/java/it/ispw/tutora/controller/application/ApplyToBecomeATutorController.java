package it.ispw.tutora.controller.application;

import it.ispw.tutora.bean.*;
import it.ispw.tutora.boundary.CertificateValidationBoundary;
import it.ispw.tutora.boundary.CertificateValidator;
import it.ispw.tutora.dao.*;
import it.ispw.tutora.dao.factory.DaoFactory;
import it.ispw.tutora.enums.ApplicationStatus;
import it.ispw.tutora.enums.ItemType;
import it.ispw.tutora.enums.NotificationType;
import it.ispw.tutora.enums.Status;
import it.ispw.tutora.exception.*;
import it.ispw.tutora.model.*;
import it.ispw.tutora.model.session.SessionManager;

import java.math.BigDecimal;
import java.util.logging.Level;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Controller applicativo per UC-2: Apply to Become a Tutor.
 *
 * -----------------------------------------------------------------------
 * Responsabilità
 * -----------------------------------------------------------------------
 * Coordina il flusso completo della candidatura:
 *   1. Caricamento categorie per la dashboard
 *   2. Caricamento requisiti per il form dinamico
 *   3. Invio candidatura con validazione documenti (CertificateValidationBoundary)
 *   4. Valutazione candidatura da parte dell'admin
 *   5. Consultazione candidature dello studente
 *   6. Consultazione candidature pendenti per l'admin
 *
 * -----------------------------------------------------------------------
 * Transazioni
 * -----------------------------------------------------------------------
 * submitApplication() e evaluateApplication() usano transazioni
 * esplicite (setAutoCommit false) per garantire atomicità:
 * se una delle operazioni fallisce tutto viene annullato con rollback.
 *
 */
public class ApplyToBecomeATutorController {

    private static final Logger LOGGER = Logger.getLogger(
            ApplyToBecomeATutorController.class.getName());

    private final CertificateValidator validator = new CertificateValidationBoundary();

    // ----------------------------------------------------------------
    // 1. loadCategories
    // ----------------------------------------------------------------

    /**
     * Carica tutte le categorie disponibili per la dashboard.
     */
    public List<CategoryBean> loadCategories()
            throws DatabaseException {

        CategoryDao dao = DaoFactory.getInstance().createCategoryDao();

        List<Category> categories = dao.findAll();
        List<CategoryBean> beans  = new ArrayList<>();

        for (Category c : categories) {
            CategoryBean bean = new CategoryBean();
            bean.setName(c.getName());
            bean.setDescription(c.getDescription());
            beans.add(bean);
        }
        return beans;
    }

    // ----------------------------------------------------------------
    // 2. loadRequirements
    // ----------------------------------------------------------------

    /**
     * Carica i requisiti di una categoria per costruire il form dinamico.
     * Chiamato quando lo studente clicca "Invia candidatura".
     */
    public List<RequirementBean> loadRequirements(String categoryName)
            throws DatabaseException, CategoryNotFoundException {

        CategoryDao dao = DaoFactory.getInstance().createCategoryDao();
        Category category = dao.findByNameWithRequirements(categoryName);

        List<RequirementBean> beans = new ArrayList<>();
        for (Requirement req : category.getRequirements()) {
            RequirementBean bean = new RequirementBean();
            bean.setName(req.getName());
            bean.setLabel(req.getLabel());
            bean.setDescription(req.getDescription());
            bean.setRequired(req.isRequired());
            bean.setItemType(req.getItemType());

            if (req instanceof TextRequirement tr) {
                bean.setMinChar(tr.getMinChar());
                bean.setMaxChar(tr.getMaxChar());
            }
            beans.add(bean);
        }
        return beans;
    }

    // ----------------------------------------------------------------
    // 3. submitApplication
    // ----------------------------------------------------------------

    /**
     * Invia la candidatura dello studente.
     *
     * Flusso:
     *   1. Verifica che lo studente non abbia già una candidatura attiva
     *   2. Valida i documenti tramite CertificateValidationBoundary (timer 3 min)
     *   3. Se validi: salva application + item + notifica admin (transazione)
     *   4. Se invalidi: lancia InvalidDocumentException
     *   5. Se timeout: lancia ValidationTimeoutException
     */
    public int submitApplication(TutorApplicationBean bean, String token)
            throws AuthenticationException,
            ValidationTimeoutException,
            ValidationServiceException,
            InvalidDocumentException,
            DuplicateApplicationException,
            DatabaseException {

        String studentUsername = requireUsername(token);

        // 1. valida i documenti tramite API esterna (timer 3 minuti)
        boolean valid = validator.validateDocuments(bean.getItems());
        if (!valid) {
            throw new InvalidDocumentException(
                    "One or more documents are invalid. Please resubmit.");
        }

        DaoFactory factory = DaoFactory.getInstance();
        TutorApplicationDao appDao = factory.createTutorApplicationDao();
        ApplicationItemDao itemDao = factory.createApplicationItemDao();
        DocumentDao documentDao = factory.createDocumentDao();
        NotificationDao notifDao = factory.createNotificationDao();

        try (Connection conn = factory.getConnection()) {
            if (conn != null) conn.setAutoCommit(false);
            return executeSubmit(conn, bean, studentUsername, appDao, itemDao, documentDao, notifDao);
        } catch (SQLException e) {
            throw new DatabaseException("Error submitting application.", e);
        }
    }

    private int executeSubmit(Connection conn,
                              TutorApplicationBean bean,
                              String studentUsername,
                              TutorApplicationDao appDao,
                              ApplicationItemDao itemDao,
                              DocumentDao documentDao,
                              NotificationDao notifDao)
            throws DuplicateApplicationException, DatabaseException {
        try {
            String subcategoryName = bean.getItems().stream()
                    .filter(i -> "subcategory".equals(i.getRequirementName())
                            && i.getTextContent() != null
                            && !i.getTextContent().isBlank())
                    .map(ApplicationItemBean::getTextContent)
                    .map(String::trim)
                    .findFirst()
                    .orElse(null);

            TutorApplication application = new TutorApplication(
                    0, bean.getCategoryName(), studentUsername,
                    LocalDateTime.now(), ApplicationStatus.SUBMITTED);
            application.setSubcategoryName(subcategoryName);
            int applicationId = appDao.insert(conn, application);
            for (ApplicationItemBean itemBean : bean.getItems()) {
                itemDao.insert(conn, buildItem(conn, itemBean, applicationId, documentDao));
            }
            sendNotificationToAdmin(conn, notifDao, studentUsername,
                    applicationId, bean.getCategoryName());
            if (conn != null) conn.commit();
            return applicationId;
        } catch (DuplicateApplicationException | DatabaseException e) {
            safeRollback(conn);
            throw e;
        } catch (Exception e) {
            safeRollback(conn);
            throw new DatabaseException("Unexpected error submitting application.", e);
        }
    }

    // ----------------------------------------------------------------
    // 4. evaluateApplication
    // ----------------------------------------------------------------

    /**
     * Valuta una candidatura da parte dell'admin.
     * Aggiorna lo status (ACCEPTED o REJECTED) e notifica lo studente.
     */
    public void evaluateApplication(ApplicationReviewBean bean, String token)
            throws AuthenticationException,
            AuthorizationException,
            ApplicationNotFoundException,
            InvalidApplicationStateException,
            DatabaseException {

        String adminUsername = requireAdmin(token);

        DaoFactory factory = DaoFactory.getInstance();
        TutorApplicationDao appDao = factory.createTutorApplicationDao();
        NotificationDao notifDao = factory.createNotificationDao();

        try (Connection conn = factory.getConnection()) {
            if (conn != null) conn.setAutoCommit(false);
            executeEvaluate(conn, bean, adminUsername, appDao, notifDao);
        } catch (SQLException e) {
            throw new DatabaseException("Error evaluating application.", e);
        }
    }

    private void executeEvaluate(Connection conn,
                                 ApplicationReviewBean bean,
                                 String adminUsername,
                                 TutorApplicationDao appDao,
                                 NotificationDao notifDao)
            throws ApplicationNotFoundException, InvalidApplicationStateException, DatabaseException {
        try {
            TutorApplication application = appDao.findById(conn, bean.getApplicationId());
            ApplicationStatus newStatus = bean.getStatus();
            applyStatusUpdate(application, newStatus);
            application.setAdminNotes(bean.getAdminNotes());
            application.setEvaluatedAt(LocalDateTime.now());
            appDao.updateStatus(conn, application);
            notifDao.markReadByTargetIdAndRecipient(conn, application.getId(), adminUsername);
            sendNotificationToStudent(conn, notifDao, adminUsername,
                    application.getStudentUsername(), application.getId(),
                    newStatus, application.getAdminNotes());

            if (newStatus == ApplicationStatus.ACCEPTED) {
                Tutor promoted = promoteStudentToTutor(conn, application.getStudentUsername());
                if (promoted != null) {
                    createInitialExpertise(conn, promoted,
                            application.getCategoryName(), application.getId());
                }
            }

            if (conn != null) conn.commit();
        } catch (ApplicationNotFoundException | InvalidApplicationStateException | DatabaseException e) {
            safeRollback(conn);
            throw e;
        } catch (Exception e) {
            safeRollback(conn);
            throw new DatabaseException("Unexpected error evaluating application.", e);
        }
    }

    private Tutor promoteStudentToTutor(Connection conn, String studentUsername)
            throws DatabaseException {
        try {
            DaoFactory factory = DaoFactory.getInstance();
            UserDao userDao = factory.createUserDao();
            TutorDao tutorDao = factory.createTutorDao();
            Tutor promoted = userDao.promoteToTutor(conn, studentUsername);
            insertTutorIfNeeded(conn, tutorDao, promoted);
            SessionManager.getInstance().markAsNewlyPromotedTutor(studentUsername);
            SessionManager.getInstance().invalidateSessionsForUser(studentUsername);
            return promoted;
        } catch (UserNotFoundException e) {
            LOGGER.log(Level.WARNING, "Cannot promote to tutor, student not found: {0}", studentUsername);
            return null;
        }
    }

    private void createInitialExpertise(Connection conn, Tutor tutor,
                                        String categoryName, int applicationId) {
        try {
            DaoFactory factory = DaoFactory.getInstance();
            ApplicationItemDao itemDao = factory.createApplicationItemDao();
            TutorExpertiseDao expertiseDao = factory.createTutorExpertiseDao();

            List<ApplicationItem> items = itemDao.findByApplicationId(conn, applicationId);

            String subcategoryName = items.stream()
                    .filter(i -> "subcategory".equals(i.getRequirementName()) && i instanceof TextItem ti && !ti.getTextContent().isBlank())
                    .map(i -> ((TextItem) i).getTextContent().trim())
                    .findFirst()
                    .orElse(categoryName);

            BigDecimal price = items.stream()
                    .filter(i -> "hourly_price".equals(i.getRequirementName()) && i instanceof TextItem)
                    .map(i -> {
                        try { return new BigDecimal(((TextItem) i).getTextContent().replace(",", ".")); }
                        catch (NumberFormatException e) { return null; }
                    })
                    .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                    .findFirst()
                    .orElse(new BigDecimal("30.00"));

            CategoryDao categoryDao = factory.createCategoryDao();
            Category category;
            try {
                category = categoryDao.findByNameWithRequirements(categoryName);
            } catch (CategoryNotFoundException e) {
                category = new Category(categoryName, "");
            }

            SubCategory subcategory = new SubCategory(subcategoryName, category, "");
            TutorExpertise expertise = new TutorExpertise(
                    tutor, subcategory, price, Status.APPROVED, LocalDateTime.now());
            expertiseDao.insertExpertise(conn, expertise);

        } catch (DuplicateTutorExpertiseException e) {
            LOGGER.info("Expertise already exists for this tutor/subcategory, skipping.");
        } catch (Exception e) {
            LOGGER.warning("Cannot create initial expertise: " + e.getMessage());
        }
    }

    private void insertTutorIfNeeded(Connection conn, TutorDao tutorDao, Tutor promoted)
            throws DatabaseException {
        try {
            tutorDao.insert(conn, promoted);
        } catch (DuplicateUserException ignored) {
            // DB mode: the tutor row was already created inside promoteToTutor()
        }
    }

    private void applyStatusUpdate(TutorApplication application, ApplicationStatus newStatus)
            throws InvalidApplicationStateException {
        try {
            application.updateStatus(newStatus);
        } catch (IllegalStateException e) {
            throw new InvalidApplicationStateException(e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // 5. loadApplicationDetail (admin)
    // ----------------------------------------------------------------

    /**
     * Carica il dettaglio completo di una candidatura (inclusi gli item)
     * per la revisione da parte dell'admin.
     */
    public TutorApplicationBean loadApplicationDetail(int applicationId, String token)
            throws AuthenticationException, AuthorizationException,
            ApplicationNotFoundException, DatabaseException {

        requireAdmin(token);

        DaoFactory factory = DaoFactory.getInstance();
        TutorApplicationDao appDao  = factory.createTutorApplicationDao();
        ApplicationItemDao  itemDao = factory.createApplicationItemDao();

        try (Connection conn = factory.getConnection()) {
            TutorApplication app   = appDao.findById(conn, applicationId);
            List<ApplicationItem> items = itemDao.findByApplicationId(conn, applicationId);

            TutorApplicationBean bean = new TutorApplicationBean();
            bean.setApplicationId(app.getId());
            bean.setCategoryName(app.getCategoryName());
            bean.setSubcategoryName(app.getSubcategoryName());
            bean.setStudentUsername(app.getStudentUsername());
            bean.setStatus(app.getStatus());
            bean.setCreationDate(app.getCreationDate());

            List<ApplicationItemBean> itemBeans = new ArrayList<>();
            for (ApplicationItem item : items) {
                ApplicationItemBean ib = new ApplicationItemBean();
                ib.setRequirementName(item.getRequirementName());
                if (item instanceof TextItem ti) {
                    ib.setItemType(ItemType.TEXT);
                    ib.setTextContent(ti.getTextContent());
                } else if (item instanceof DocumentItem di) {
                    ib.setItemType(ItemType.DOCUMENT);
                    ib.setOriginalFilename(di.getDocument().getOriginalFilename());
                    ib.setSizeBytes(di.getDocument().getSizeBytes());
                }
                itemBeans.add(ib);
            }
            bean.setItems(itemBeans);
            return bean;

        } catch (SQLException e) {
            throw new DatabaseException("Error loading application detail.", e);
        }
    }

    // ----------------------------------------------------------------
    // 6. loadMyApplications
    // ----------------------------------------------------------------

    /**
     * Carica tutte le candidature dello studente loggato.
     */
    public List<TutorApplicationBean> loadMyApplications(String token)
            throws AuthenticationException, DatabaseException {

        String studentUsername = requireUsername(token);

        DaoFactory factory = DaoFactory.getInstance();
        TutorApplicationDao dao = factory.createTutorApplicationDao();

        try (Connection conn = factory.getConnection()) {

            List<TutorApplication> applications =
                    dao.findByStudent(conn, studentUsername);

            return toApplicationBeans(applications);

        } catch (SQLException e) {
            throw new DatabaseException("Error loading applications.", e);
        }
    }

    // ----------------------------------------------------------------
    // 6. loadPendingApplications
    // ----------------------------------------------------------------

    /**
     * Carica le candidature in stato SUBMITTED per la dashboard admin.
     */
    public List<TutorApplicationBean> loadPendingApplications(String token)
            throws AuthenticationException, AuthorizationException, DatabaseException {

        requireAdmin(token);

        DaoFactory factory = DaoFactory.getInstance();
        TutorApplicationDao dao = factory.createTutorApplicationDao();

        try (Connection conn = factory.getConnection()) {

            List<TutorApplication> applications = dao.findByStatus(conn, ApplicationStatus.SUBMITTED);

            return toApplicationBeans(applications);

        } catch (SQLException e) {
            throw new DatabaseException("Error loading pending applications.", e);
        }
    }

    // ----------------------------------------------------------------
    // Helper privati
    // ----------------------------------------------------------------

    private void safeRollback(Connection conn) {
        if (conn == null) return;
        try {
            conn.rollback();
        } catch (SQLException e) {
            LOGGER.warning("Rollback failed: " + e.getMessage());
        }
    }

    /**
     * Restituisce lo username dell'utente associato al token.
     */
    private String requireUsername(String token) throws AuthenticationException {
        User user = SessionManager.getInstance().getCurrentUser(token);
        if (user == null) throw new AuthenticationException("Invalid or expired session.");
        return user.getUsername();
    }

    /**
     * Verifica che il token appartenga a un utente con ruolo Admin
     * e restituisce lo username per evitare un secondo lookup sulla sessione.
     */
    private String requireAdmin(String token)
            throws AuthenticationException, AuthorizationException {
        SessionManager sm = SessionManager.getInstance();
        if (!sm.isSessionValid(token))
            throw new AuthenticationException("Invalid or expired session.");
        if (!sm.getSession(token).isAdmin())
            throw new AuthorizationException("Admin role required.");
        return sm.getCurrentUser(token).getUsername();
    }

    /**
     * Costruisce un ApplicationItem dal Bean corrispondente.
     * Se l'item è di tipo DOCUMENT persiste prima il documento
     * tramite DocumentDao e poi crea il DocumentItem.
     */
    private ApplicationItem buildItem(Connection conn,
                                      ApplicationItemBean itemBean,
                                      int applicationId,
                                      DocumentDao documentDao)
            throws DatabaseException {

        if (itemBean.getItemType() == ItemType.TEXT) {
            return new TextItem(0, applicationId,
                    itemBean.getRequirementName(),
                    itemBean.getTextContent());
        }

        // DOCUMENT — persiste prima il file
        Document document = new Document(
                0,
                itemBean.getOriginalFilename(),
                UUID.randomUUID() + getExtension(itemBean.getOriginalFilename()),
                itemBean.getMimeType(),
                itemBean.getSizeBytes(),
                itemBean.getContent(),
                LocalDateTime.now()
        );

        int documentId = documentDao.insert(conn, document); // Dopo che viene inserito ID auto-incremented viene aggiornato dal db

        // Creo il nuovo document con id aggiornato
        Document savedDocument = new Document(
                documentId,
                document.getOriginalFilename(),
                document.getStoredFilename(),
                document.getMimeType(),
                document.getSizeBytes(),
                document.getContent(),
                document.getUploadedAt()
        );

        return new DocumentItem(0, applicationId,
                itemBean.getRequirementName(), savedDocument);
    }

    /**
     * Invia una notifica all'admin quando arriva una nuova candidatura.
     */
    private void sendNotificationToAdmin(Connection conn,
                                         NotificationDao notifDao,
                                         String studentUsername,
                                         int applicationId,
                                         String categoryName)
            throws DatabaseException {

        Notification notification = new Notification.Builder()
                .id(0)
                .recipientUsername("admin")
                .senderUsername(studentUsername)
                .message("New tutor application from '"
                        + studentUsername + "' for category '"
                        + categoryName + "'.")
                .type(NotificationType.APPLICATION_UPDATE)
                .targetId(applicationId)
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        notifDao.insert(conn, notification);
    }

    /**
     * Invia una notifica allo studente con l'esito della valutazione.
     */
    private void sendNotificationToStudent(Connection conn,
                                           NotificationDao notifDao,
                                           String adminUsername,
                                           String studentUsername,
                                           int applicationId,
                                           ApplicationStatus status,
                                           String adminNotes)
            throws DatabaseException {

        String base = status == ApplicationStatus.ACCEPTED
                ? "Your tutor application has been accepted! Welcome to TUTORA."
                : "Your tutor application has been rejected.";
        String message = (adminNotes != null && !adminNotes.isBlank())
                ? base + "\n\nAdmin notes: " + adminNotes.trim()
                : base;

        Notification notification = new Notification.Builder()
                .id(0)
                .recipientUsername(studentUsername)
                .senderUsername(adminUsername)
                .message(message)
                .type(NotificationType.APPLICATION_UPDATE)
                .targetId(applicationId)
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        notifDao.insert(conn, notification);
    }

    /**
     * Converte una lista di TutorApplication in lista di TutorApplicationBean.
     */
    private List<TutorApplicationBean> toApplicationBeans(
            List<TutorApplication> applications) {

        List<TutorApplicationBean> beans = new ArrayList<>();
        for (TutorApplication app : applications) {
            TutorApplicationBean bean = new TutorApplicationBean();
            bean.setApplicationId(app.getId());
            bean.setCategoryName(app.getCategoryName());
            bean.setStudentUsername(app.getStudentUsername());
            bean.setCreationDate(app.getCreationDate());
            bean.setStatus(app.getStatus());
            bean.setAdminNotes(app.getAdminNotes());
            bean.setEvaluatedAt(app.getEvaluatedAt());
            beans.add(bean);
        }
        return beans;
    }

    /**
     * Estrae l'estensione dal nome del file originale.
     * Es: "diploma.pdf" → ".pdf"
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}