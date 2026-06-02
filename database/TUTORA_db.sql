-- ============================================================
--  TUTORA_db  –  Schema Definitivo
--  Progetto ISPW  |  Ingegneria del Software
-- ============================================================
--  Convenzioni:
--    • Chiavi surrogate INT AUTO_INCREMENT su entità deboli o con PK fragile
--    • Valori monetari:  DECIMAL(10,2)            (mai DOUBLE/FLOAT)
--    • Password:         VARCHAR(255)              (hash BCrypt / Argon2)
--    • Timestamp:        DATETIME DEFAULT CURRENT_TIMESTAMP
--    • Nomi colonna:     snake_case
--    • Nomi tabella:     snake_case, singolare
--    • ENUM solo per insiemi di valori chiusi e stabili
--    • Indici espliciti sulle colonne più frequentemente filtrate
-- ============================================================

CREATE DATABASE IF NOT EXISTS `TUTORA_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;   -- MySQL 8+: collation Unicode corretta (utf8mb4_general_ci
                                --   tratta ä=a, ö=o ecc.)

USE `TUTORA_db`;

SET SQL_MODE   = 'NO_AUTO_VALUE_ON_ZERO,STRICT_TRANS_TABLES';
SET time_zone  = '+00:00';
SET NAMES utf8mb4;

START TRANSACTION;

-- ============================================================
-- 1. UTENTI E RUOLI
-- ============================================================

-- Tabella base condivisa da tutti i ruoli (pattern Table-Per-Subclass).
-- is_active implementa il soft-delete: disabilitare un utente conserva
-- lo storico di booking e recensioni senza cancellazione fisica.
CREATE TABLE `user` (
  `username`       VARCHAR(100) NOT NULL,
  `email`          VARCHAR(150) NOT NULL,
  `name`           VARCHAR(50)  NOT NULL,
  `surname`        VARCHAR(50)  NOT NULL,
  -- Memorizzare SOLO l'hash (BCrypt / Argon2), mai la password in chiaro
  `password_hash`  VARCHAR(255) NOT NULL,
  `role`           ENUM('Admin','Tutor','Student') NOT NULL,
  `description`    VARCHAR(500) DEFAULT NULL,
  `is_active`      BOOLEAN      NOT NULL DEFAULT TRUE,
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                   ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`username`),
  UNIQUE KEY `uq_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `admin` (
  `username`  VARCHAR(100) NOT NULL,
  PRIMARY KEY (`username`),
  CONSTRAINT `fk_admin_user`
    FOREIGN KEY (`username`) REFERENCES `user`(`username`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `student` (
  `username`  VARCHAR(100)  NOT NULL,
  `budget`    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  PRIMARY KEY (`username`),
  CONSTRAINT `fk_student_user`
  FOREIGN KEY (`username`) REFERENCES `user`(`username`) ON DELETE CASCADE,
  CONSTRAINT `chk_student_budget`
  CHECK (`budget` >= 0.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- rating e rating_count sono mantenuti sincronizzati automaticamente
-- dai trigger trg_review_after_insert / update / delete (sezione 13).
CREATE TABLE `tutor` (
  `username`      VARCHAR(100) NOT NULL,
  `rating`        DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  `rating_count`  INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`username`),
  CONSTRAINT `fk_tutor_user`
  FOREIGN KEY (`username`) REFERENCES `user`(`username`) ON DELETE CASCADE,
  CONSTRAINT `chk_tutor_rating`
  CHECK (`rating` BETWEEN 0.00 AND 5.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 2. CATEGORIE E SOTTOCATEGORIE
-- ============================================================

CREATE TABLE `category` (
  `name`         VARCHAR(100) NOT NULL,
  `description`  VARCHAR(300) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `subcategory` (
  `name`           VARCHAR(100) NOT NULL,
  `category_name`  VARCHAR(100) NOT NULL,
  `description`    VARCHAR(300) DEFAULT NULL,
  PRIMARY KEY (`name`),
  KEY `idx_subcategory_category` (`category_name`),
  CONSTRAINT `fk_subcategory_category`
  FOREIGN KEY (`category_name`) REFERENCES `category`(`name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 3. COMPETENZE TUTOR, PREZZI E TAG
-- ============================================================

-- Un tutor può insegnare una o più sottocategorie.
-- status = 'Approved' va verificato a livello applicativo prima di
-- consentire la creazione di nuovi slot lezione.
CREATE TABLE `tutor_expertise` (
  `tutor_username`    VARCHAR(100)  NOT NULL,
  `subcategory_name`  VARCHAR(100)  NOT NULL,
  `hourly_price`      DECIMAL(10,2) NOT NULL,
  `status`            ENUM('Pending','Approved','Rejected') NOT NULL DEFAULT 'Pending',
  `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                      ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`tutor_username`, `subcategory_name`),
  -- Copre: "tutor approvati per la materia X" (query di ricerca principale)
  KEY `idx_expertise_subcat_status` (`subcategory_name`, `status`),
  CONSTRAINT `fk_expertise_tutor`
  FOREIGN KEY (`tutor_username`)   REFERENCES `tutor`(`username`)      ON DELETE CASCADE,
  CONSTRAINT `fk_expertise_sub`
  FOREIGN KEY (`subcategory_name`) REFERENCES `subcategory`(`name`)    ON DELETE CASCADE,
  CONSTRAINT `chk_expertise_price`
  CHECK (`hourly_price` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tag` (
  `name`  VARCHAR(50) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Associazione M:N tra tag e una specifica competenza del tutor
CREATE TABLE `expertise_tag` (
  `tutor_username`    VARCHAR(100) NOT NULL,
  `subcategory_name`  VARCHAR(100) NOT NULL,
  `tag_name`          VARCHAR(50)  NOT NULL,
  PRIMARY KEY (`tutor_username`, `subcategory_name`, `tag_name`),
  CONSTRAINT `fk_etag_expertise`
  FOREIGN KEY (`tutor_username`, `subcategory_name`)
  REFERENCES `tutor_expertise`(`tutor_username`, `subcategory_name`) ON DELETE CASCADE,
  CONSTRAINT `fk_etag_tag`
  FOREIGN KEY (`tag_name`) REFERENCES `tag`(`name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4. REQUISITI PER CATEGORIA  (usati nel form Apply to Become a Tutor)
-- ============================================================

-- Requisito di tipo documento (upload file)
CREATE TABLE `document_requirement` (
  `category_name`  VARCHAR(100) NOT NULL,
  `name`           VARCHAR(100) NOT NULL,
  `label`          VARCHAR(50)  NOT NULL,
  `description`    VARCHAR(200) DEFAULT NULL,
  `is_required`    BOOLEAN      NOT NULL DEFAULT TRUE,
  PRIMARY KEY (`category_name`, `name`),
  CONSTRAINT `fk_docreq_category`
  FOREIGN KEY (`category_name`) REFERENCES `category`(`name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Requisito di tipo testo libero
CREATE TABLE `text_requirement` (
  `category_name`  VARCHAR(100) NOT NULL,
  `name`           VARCHAR(100) NOT NULL,
  `label`          VARCHAR(50)  NOT NULL,
  `description`    VARCHAR(200) DEFAULT NULL,
  `min_char`       INT UNSIGNED NOT NULL DEFAULT 0,
  `max_char`       INT UNSIGNED NOT NULL DEFAULT 1000,
  `is_required`    BOOLEAN      NOT NULL DEFAULT TRUE,
  PRIMARY KEY (`category_name`, `name`),
  CONSTRAINT `fk_textreq_category`
  FOREIGN KEY (`category_name`) REFERENCES `category`(`name`) ON DELETE CASCADE,
  CONSTRAINT `chk_textreq_chars`
  CHECK (`max_char` >= `min_char`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 5. DOCUMENTI  (file caricati durante un'application)
-- ============================================================

-- stored_filename è un nome UUID generato in Java per prevenire collisioni.
-- ATTENZIONE: il DAO non deve mai eseguire SELECT * su questa tabella —
-- il campo content può raggiungere decine di MB. Selezionare sempre colonne
-- esplicite (id, original_filename, mime_type, size_bytes, …).
-- content è NULLABLE: quando si usa lo storage su filesystem il DB
-- conserva solo i metadati, mentre il file fisico è su disco.
CREATE TABLE `document` (
  `id`                INT              NOT NULL AUTO_INCREMENT,
  `original_filename` VARCHAR(255)     NOT NULL,
  `stored_filename`   VARCHAR(255)     NOT NULL,   -- UUID.ext generato in Java
  `mime_type`         VARCHAR(100)     NOT NULL,
  `size_bytes`        BIGINT UNSIGNED  NOT NULL,   -- unsigned: dimensione sempre >= 0
  `content`           LONGBLOB         DEFAULT NULL, -- NULL se storage su filesystem
  `uploaded_at`       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_stored_filename` (`stored_filename`),
  CONSTRAINT `chk_document_size`
  CHECK (`size_bytes` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 6. APPLICATION TUTOR  (UC-2: Apply to Become a Tutor)
-- ============================================================

-- active_key garantisce al massimo una application attiva (Draft/Submitted)
-- per studente+categoria senza colonne GENERATED (non portabili).
-- Regole imposte dal DAO Java a ogni INSERT/UPDATE:
--   status IN ('Draft','Submitted')   -> active_key = student_username
--   status IN ('Accepted','Rejected') -> active_key = CONCAT(id, '_closed')
-- Il vincolo UNIQUE su (category_name, active_key) blocca il secondo
-- INSERT attivo; i valori '_closed' non collidono mai tra loro.
CREATE TABLE `tutor_application` (
  `id`               INT          NOT NULL AUTO_INCREMENT,
  `category_name`    VARCHAR(100) NOT NULL,
  `student_username` VARCHAR(100) NOT NULL,
  `active_key`       VARCHAR(120) NOT NULL,
  `creation_date`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status`           ENUM('Draft','Submitted','Accepted','Rejected') NOT NULL DEFAULT 'Draft',
  `admin_notes`      VARCHAR(500) DEFAULT NULL,
  `evaluated_at`     DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_one_active_application`  (`category_name`, `active_key`),
  KEY `idx_application_status`            (`status`),
  KEY `idx_application_student_status`    (`student_username`, `status`),
  CONSTRAINT `fk_application_category`
  FOREIGN KEY (`category_name`)    REFERENCES `category`(`name`)    ON DELETE RESTRICT,
  CONSTRAINT `fk_application_student`
  FOREIGN KEY (`student_username`) REFERENCES `student`(`username`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Risposta singola a un requisito all'interno di un'application.
-- Il CHECK garantisce mutua esclusività:
--   Text     -> text_content NOT NULL, document_id NULL
--   Document -> document_id  NOT NULL, text_content NULL
CREATE TABLE `application_item` (
  `id`               INT          NOT NULL AUTO_INCREMENT,
  `application_id`   INT          NOT NULL,
  `requirement_name` VARCHAR(100) NOT NULL,
  `item_type`        ENUM('Text','Document') NOT NULL,
  `text_content`     TEXT         DEFAULT NULL,
  `document_id`      INT          DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_item_per_req` (`application_id`, `requirement_name`),
  CONSTRAINT `fk_item_application`
  FOREIGN KEY (`application_id`) REFERENCES `tutor_application`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_item_document`
  FOREIGN KEY (`document_id`)    REFERENCES `document`(`id`)          ON DELETE RESTRICT,
  CONSTRAINT `chk_item_content`
  CHECK (
      (`item_type` = 'Text'     AND `text_content` IS NOT NULL AND `document_id` IS NULL) OR
      (`item_type` = 'Document' AND `document_id`  IS NOT NULL AND `text_content` IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 7. LEZIONI  (UC-1: Book Tutor)
-- ============================================================

-- Una lesson rappresenta un singolo slot di disponibilità del tutor.
-- listed_price è il prezzo snapshot al momento della creazione dello slot.
--
-- NOTA – Lesson Overlap: il DB non impedisce sovrapposizioni temporali
-- (MySQL non supporta indici parziali su range di date).
-- La guardia anti-overlap va implementata nel DAO con questa query
-- prima di ogni INSERT:
--
--   SELECT COUNT(*) FROM lesson
--   WHERE tutor_username = ?
--     AND status NOT IN ('Cancelled')
--     AND start_time < ?   -- end_time del nuovo slot
--     AND end_time   > ?   -- start_time del nuovo slot
--
-- Se COUNT > 0, l'INSERT deve essere rifiutato con un'eccezione applicativa.
-- L'indice idx_lesson_overlap_check copre esattamente questa query.
CREATE TABLE `lesson` (
  `id`               INT           NOT NULL AUTO_INCREMENT,
  `tutor_username`   VARCHAR(100)  NOT NULL,
  `subcategory_name` VARCHAR(100)  NOT NULL,
  `start_time`       DATETIME      NOT NULL,
  `end_time`         DATETIME      NOT NULL,
  `is_remote`        BOOLEAN       NOT NULL DEFAULT TRUE,
  `listed_price`     DECIMAL(10,2) NOT NULL,
  `status`           ENUM('Available','Booked','Completed','Cancelled') NOT NULL DEFAULT 'Available',
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                     ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- Copre la guardia anti-overlap (tutor_username, status, start_time, end_time)
  KEY `idx_lesson_overlap_check`  (`tutor_username`,   `status`, `start_time`, `end_time`),
  -- Copre la ricerca lezioni disponibili per materia ordinate per data
  KEY `idx_lesson_browse`         (`subcategory_name`, `status`, `start_time`),
  CONSTRAINT `fk_lesson_expertise`
  FOREIGN KEY (`tutor_username`, `subcategory_name`)
  REFERENCES `tutor_expertise`(`tutor_username`, `subcategory_name`) ON DELETE RESTRICT,
  CONSTRAINT `chk_lesson_times`
  CHECK (`end_time` > `start_time`),
  CONSTRAINT `chk_lesson_price`
  CHECK (`listed_price` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 8. PRENOTAZIONI
-- ============================================================

-- Una booking collega uno studente a una lezione e registra il pagamento.
-- price_paid è uno snapshot fissato al momento della prenotazione.
--
-- IMPORTANTE: entrambe le FK usano ON DELETE RESTRICT perché i dati
-- finanziari non devono mai essere cancellati a cascata.
-- Il soft-delete su user.is_active è la strada corretta per disattivare
-- un utente senza perdere lo storico dei pagamenti.
CREATE TABLE `booking` (
  `id`               INT           NOT NULL AUTO_INCREMENT,
  `lesson_id`        INT           NOT NULL,
  `student_username` VARCHAR(100)  NOT NULL,
  `booked_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `price_paid`       DECIMAL(10,2) NOT NULL,
  `payment_status`   ENUM('Pending','Paid','Refunded') NOT NULL DEFAULT 'Pending',
  `payment_ref`      VARCHAR(100)  DEFAULT NULL,
  `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                     ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_booking_lesson` (`lesson_id`),
  KEY `idx_booking_student` (`student_username`),
  KEY `idx_booking_payment` (`payment_status`),
  CONSTRAINT `fk_booking_lesson`
    FOREIGN KEY (`lesson_id`)        REFERENCES `lesson`(`id`)        ON DELETE RESTRICT,
  -- RESTRICT (non CASCADE): lo storico dei pagamenti non si cancella mai
  CONSTRAINT `fk_booking_student`
    FOREIGN KEY (`student_username`) REFERENCES `student`(`username`) ON DELETE RESTRICT,
  CONSTRAINT `chk_booking_price`
    CHECK (`price_paid` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 9. RECENSIONI
-- ============================================================

CREATE TABLE `review` (
  `id`               INT               NOT NULL AUTO_INCREMENT,
  `booking_id`       INT               NOT NULL,
  `student_username` VARCHAR(100)      NOT NULL,
  `tutor_username`   VARCHAR(100)      NOT NULL,
  `rating`           TINYINT UNSIGNED  NOT NULL,  -- unsigned: sempre 1–5
  `comment`          TEXT              DEFAULT NULL,
  `created_at`       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_review_booking` (`booking_id`),
  KEY `idx_review_tutor`         (`tutor_username`),
  CONSTRAINT `fk_review_booking`
  FOREIGN KEY (`booking_id`)       REFERENCES `booking`(`id`)          ON DELETE RESTRICT,
  CONSTRAINT `fk_review_student`
  FOREIGN KEY (`student_username`) REFERENCES `student`(`username`)    ON DELETE RESTRICT,
  CONSTRAINT `fk_review_tutor`
  FOREIGN KEY (`tutor_username`)   REFERENCES `tutor`(`username`)      ON DELETE RESTRICT,
  CONSTRAINT `chk_review_rating`
  CHECK (`rating` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 10. NOTIFICHE
-- ============================================================

-- sender_username è nullable: NULL indica una notifica generata dal sistema
-- (es. conferma pagamento automatica, scadenza slot).
-- target_id è una referenza polimorfica senza FK: il tipo dell'entità
-- referenziata si deduce dal campo type (application_id, lesson_id, booking_id…).
CREATE TABLE `notification` (
  `id`                 INT          NOT NULL AUTO_INCREMENT,
  `recipient_username` VARCHAR(100) NOT NULL,
  `sender_username`    VARCHAR(100) DEFAULT NULL,
  `message`            VARCHAR(500) NOT NULL,
  `type`               ENUM(
                         'Application_Update',
                         'Expertise_Offer',
                         'Lesson_Booked',
                         'Lesson_Accepted',
                         'Lesson_Rejected',
                         'Payment_Confirmed',
                         'New_Review'
                       ) NOT NULL,
  `target_id`          INT          DEFAULT NULL,
  `timestamp`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_read`            BOOLEAN      NOT NULL DEFAULT FALSE,
  PRIMARY KEY (`id`),
  KEY `idx_notif_recipient` (`recipient_username`),
  -- Copre la query inbox: WHERE recipient=? AND is_read=? ORDER BY timestamp DESC
  KEY `idx_notif_inbox` (`recipient_username`, `is_read`, `timestamp`),
  CONSTRAINT `fk_notif_recipient`
  FOREIGN KEY (`recipient_username`) REFERENCES `user`(`username`) ON DELETE CASCADE,
  CONSTRAINT `fk_notif_sender`
  FOREIGN KEY (`sender_username`)    REFERENCES `user`(`username`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 11. PREFERENZE E INTERESSI DELLO STUDENTE
-- ============================================================

CREATE TABLE `student_preference` (
  `student_username`  VARCHAR(100) NOT NULL,
  `tutor_username`    VARCHAR(100) NOT NULL,
  PRIMARY KEY (`student_username`, `tutor_username`),
  CONSTRAINT `fk_pref_student`
  FOREIGN KEY (`student_username`) REFERENCES `student`(`username`) ON DELETE CASCADE,
  CONSTRAINT `fk_pref_tutor`
  FOREIGN KEY (`tutor_username`)   REFERENCES `tutor`(`username`)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `student_interest` (
  `student_username`  VARCHAR(100) NOT NULL,
  `category_name`     VARCHAR(100) NOT NULL,
  PRIMARY KEY (`student_username`, `category_name`),
  CONSTRAINT `fk_interest_student`
  FOREIGN KEY (`student_username`) REFERENCES `student`(`username`) ON DELETE CASCADE,
  CONSTRAINT `fk_interest_category`
  FOREIGN KEY (`category_name`)    REFERENCES `category`(`name`)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 12. MESSAGGI DI CHAT
-- ============================================================

-- Tabella per la chat in tempo reale tra utenti.
-- sender_username e recipient_username referenziano user con ON DELETE CASCADE
-- così i messaggi vengono rimossi automaticamente se l'utente viene eliminato.
-- sent_at usa DEFAULT CURRENT_TIMESTAMP come fallback, ma viene valorizzato
-- esplicitamente dal DAO con il timestamp generato dall'applicazione.
CREATE TABLE `message` (
  `id`                 INT           NOT NULL AUTO_INCREMENT,
  `sender_username`    VARCHAR(100)  NOT NULL,
  `recipient_username` VARCHAR(100)  NOT NULL,
  `content`            VARCHAR(2000) NOT NULL,
  `sent_at`            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_read`            BOOLEAN       NOT NULL DEFAULT FALSE,
  PRIMARY KEY (`id`),
  -- Copre le query di conversazione bidirezionale: (A→B) e (B→A)
  KEY `idx_message_conversation` (`sender_username`, `recipient_username`),
  -- Copre il conteggio non letti per destinatario: WHERE recipient=? AND is_read=?
  KEY `idx_message_unread` (`recipient_username`, `is_read`),
  CONSTRAINT `fk_msg_sender`
    FOREIGN KEY (`sender_username`)    REFERENCES `user`(`username`) ON DELETE CASCADE,
  CONSTRAINT `fk_msg_recipient`
    FOREIGN KEY (`recipient_username`) REFERENCES `user`(`username`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 13. DATI DI ESEMPIO
-- ============================================================

-- Password di tutti gli utenti demo: Demo1234
-- Hash BCrypt cost 10: $2a$10$aVZ17z4b/dBhn1/wCFUq..tDByVXnTt8VUzGUbRJ2P8TeOdM9RMDG
INSERT INTO `user` (`username`, `email`, `name`, `surname`, `password_hash`, `role`) VALUES
  ('admin_ale',     'admin@tutora.com',        'Alessio',  'Dainelli', '$2a$10$aVZ17z4b/dBhn1/wCFUq..tDByVXnTt8VUzGUbRJ2P8TeOdM9RMDG', 'Admin'),
  ('tutor_vitto',   'vitto@tutora.com',        'Vittorio', 'Iozzia',   '$2a$10$aVZ17z4b/dBhn1/wCFUq..tDByVXnTt8VUzGUbRJ2P8TeOdM9RMDG', 'Tutor'),
  ('student_luigi', 'luigi.verdi@tutora.com',  'Luigi',    'Verdi',    '$2a$10$aVZ17z4b/dBhn1/wCFUq..tDByVXnTt8VUzGUbRJ2P8TeOdM9RMDG', 'Student'),
  ('student_marco', 'marco.rossi@tutora.com',  'Marco',    'Rossi',    '$2a$10$aVZ17z4b/dBhn1/wCFUq..tDByVXnTt8VUzGUbRJ2P8TeOdM9RMDG', 'Student');

INSERT INTO `admin`   (`username`) VALUES ('admin_ale');

INSERT INTO `student` (`username`, `budget`) VALUES
  ('student_luigi', 150.00),
  ('student_marco', 200.00);

INSERT INTO `tutor`   (`username`, `rating`, `rating_count`) VALUES
  ('tutor_vitto', 0.00, 0);

INSERT INTO `category` (`name`, `description`) VALUES
  ('Music',       'Musical instrument lessons and music theory'),
  ('Photography', 'Photography technique and post-production'),
  ('Sport',       'Athletic training and sports coaching');

INSERT INTO `subcategory` (`name`, `category_name`, `description`) VALUES
  ('Saxophone',    'Music',       'Classical, jazz and blues saxophone'),
  ('Jazz Guitar',  'Music',       'Jazz guitar and improvisation'),
  ('Photography',  'Photography', 'Composition and photography technique');

INSERT INTO `tag` (`name`) VALUES
  ('#Sax'), ('#Blues'), ('#Jazz'), ('#InPerson'), ('#Online'), ('#Intermediate'), ('#Pro');

INSERT INTO `tutor_expertise` (`tutor_username`, `subcategory_name`, `hourly_price`, `status`) VALUES
  ('tutor_vitto', 'Saxophone', 30.00, 'Approved');

INSERT INTO `expertise_tag` (`tutor_username`, `subcategory_name`, `tag_name`) VALUES
  ('tutor_vitto', 'Saxophone', '#Sax'),
  ('tutor_vitto', 'Saxophone', '#Blues'),
  ('tutor_vitto', 'Saxophone', '#InPerson');

-- Requisiti dell'application per tutte le categorie
INSERT INTO `document_requirement` (`category_name`, `name`, `label`, `description`, `is_required`) VALUES
  ('Music',       'music_cert',    'Diploma / Certificate', 'Music school diploma or conservatory certificate', TRUE),
  ('Music',       'id_document',   'Identity document',     'Valid national ID or passport',                    TRUE),
  ('Photography', 'portfolio',     'Portfolio',             'Upload a sample of your photographic work',        TRUE),
  ('Sport',       'certification', 'Sports certification',  'Upload your coaching or sports certification',     TRUE);

INSERT INTO `text_requirement` (`category_name`, `name`, `label`, `description`, `min_char`, `max_char`, `is_required`) VALUES
  ('Music',       'bio',          'Biography',          'Describe your musical background and experience',                                    50, 800, TRUE),
  ('Music',       'teaching_exp', 'Teaching experience','Describe your experience as a music teacher',                                        0,  600, FALSE),
  ('Music',       'subcategory',  'Subcategory',        'Which instrument do you want to teach?',                                             2,  100, TRUE),
  ('Photography', 'bio',          'Biography',          'Describe your photography experience',                                              50, 800, TRUE),
  ('Photography', 'subcategory',  'Subcategory',        'What specific area do you want to teach? (e.g., Portrait, Landscape, Videomaking)',  2,  100, TRUE),
  ('Sport',       'bio',          'Biography',          'Describe your sports background',                                                   50, 800, TRUE),
  ('Sport',       'subcategory',  'Subcategory',        'What sport do you want to teach? (e.g., Tennis, Swimming, Football)',                2,  100, TRUE);

-- Tre lezioni Saxophone completate nel passato (abilitano la funzionalità review)
-- IDs 1,2,3 sono garantiti su DB fresco (primo gruppo di INSERT in lesson).
INSERT INTO `lesson` (`tutor_username`, `subcategory_name`, `start_time`, `end_time`,
                      `is_remote`, `listed_price`, `status`) VALUES
  ('tutor_vitto', 'Saxophone', '2025-05-01 10:00:00', '2025-05-01 11:00:00', TRUE,  30.00, 'Completed'),
  ('tutor_vitto', 'Saxophone', '2025-05-08 15:00:00', '2025-05-08 16:00:00', FALSE, 30.00, 'Completed'),
  ('tutor_vitto', 'Saxophone', '2025-05-15 10:00:00', '2025-05-15 11:00:00', TRUE,  30.00, 'Completed');

-- Prenotazioni pagate di student_marco per quelle lezioni
INSERT INTO `booking` (`lesson_id`, `student_username`, `price_paid`, `payment_status`, `payment_ref`) VALUES
  (1, 'student_marco', 30.00, 'Paid', 'PAY-DEMO-001'),
  (2, 'student_marco', 30.00, 'Paid', 'PAY-DEMO-002'),
  (3, 'student_marco', 30.00, 'Paid', 'PAY-DEMO-003');

-- Lezioni disponibili per testare il flusso BookTutor con student_luigi (budget 150)
-- ID 4 e 5 garantiti su DB fresco (seguono i 3 Completed sopra)
INSERT INTO `lesson` (`tutor_username`, `subcategory_name`, `start_time`, `end_time`,
                      `is_remote`, `listed_price`, `status`) VALUES
  ('tutor_vitto', 'Saxophone', '2026-07-10 10:00:00', '2026-07-10 11:00:00', FALSE, 30.00, 'Available'),
  ('tutor_vitto', 'Saxophone', '2026-07-15 15:00:00', '2026-07-15 17:00:00', TRUE,  30.00, 'Available');

COMMIT;

-- ============================================================
-- 14. TRIGGER – AGGIORNAMENTO AUTOMATICO RATING TUTOR
-- ============================================================
-- I trigger mantengono tutor.rating e tutor.rating_count sempre
-- sincronizzati con la tabella review, eliminando il rischio di
-- race condition e inconsistenza rispetto al calcolo lato Java.
--
-- Tutti e tre i trigger usano il ricalcolo completo con AVG() per
-- evitare l'accumulo di errori di arrotondamento che si avrebbe con
-- la formula incrementale applicata ripetutamente (ogni arrotondamento
-- a 2 cifre introduce un errore che si propaga nel voto successivo).
-- ============================================================

DELIMITER $$

-- Trigger 1: nuova recensione inserita
CREATE TRIGGER `trg_review_after_insert`
AFTER INSERT ON `review`
FOR EACH ROW
BEGIN
  UPDATE `tutor`
  SET
    `rating_count` = (SELECT COUNT(*)
                      FROM `review` r WHERE r.`tutor_username` = NEW.`tutor_username`),
    `rating`       = (SELECT ROUND(AVG(r.`rating`), 2)
                      FROM `review` r WHERE r.`tutor_username` = NEW.`tutor_username`)
  WHERE `username` = NEW.`tutor_username`;
END$$

-- Trigger 2: voto di una recensione esistente modificato
CREATE TRIGGER `trg_review_after_update`
AFTER UPDATE ON `review`
FOR EACH ROW
BEGIN
  UPDATE `tutor`
  SET
    `rating_count` = (SELECT COUNT(*)
                      FROM `review` r WHERE r.`tutor_username` = NEW.`tutor_username`),
    `rating`       = (SELECT ROUND(AVG(r.`rating`), 2)
                      FROM `review` r WHERE r.`tutor_username` = NEW.`tutor_username`)
  WHERE `username` = NEW.`tutor_username`;
END$$

-- Trigger 3: recensione eliminata
CREATE TRIGGER `trg_review_after_delete`
AFTER DELETE ON `review`
FOR EACH ROW
BEGIN
  UPDATE `tutor`
  SET
    `rating_count` = (SELECT COUNT(*)
                      FROM `review` r WHERE r.`tutor_username` = OLD.`tutor_username`),
    `rating`       = (SELECT IFNULL(ROUND(AVG(r.`rating`), 2), 0.00)
                      FROM `review` r WHERE r.`tutor_username` = OLD.`tutor_username`)
  WHERE `username` = OLD.`tutor_username`;
END$$

DELIMITER ;

-- ============================================================
-- NOTE IMPLEMENTATIVE PER IL CODICE JAVA
-- ============================================================
-- 1. PASSWORD
--    Dipendenza (pom.xml):  org.mindrot:jbcrypt:0.4
--    Hash:      String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
--    Verifica:  boolean ok  = BCrypt.checkpw(rawPassword, storedHash);
--
-- 2. STORAGE DOCUMENTI
--    Generare stored_filename con UUID.randomUUID() + estensione originale
--    prima di ogni INSERT per garantire unicità assoluta.
--    Il DAO deve sempre selezionare colonne esplicite da `document`:
--    mai SELECT * (il LONGBLOB può pesare decine di MB).
--    Per caricare il file: SELECT id, original_filename, mime_type,
--      size_bytes, content FROM document WHERE id = ?
--
-- 3. RATING TUTOR
--    Il ricalcolo è gestito automaticamente dai trigger SQL
--    (trg_review_after_insert / update / delete).
--    Il layer Java NON deve aggiornare tutor.rating manualmente:
--    è sufficiente eseguire l'INSERT/UPDATE/DELETE su review
--    all'interno di una transazione e il trigger farà il resto.
--
-- 4. SOFT DELETE
--    Tutte le query di ricerca utenti devono includere
--    WHERE u.is_active = TRUE.
--    Le FK con ON DELETE CASCADE gestiscono la pulizia dei dati figli
--    solo per eliminazioni fisiche (da non usare su utenti attivi).
--
-- 5. GUARDIA EXPERTISE
--    Prima di creare uno slot lezione, verificare:
--      SELECT status FROM tutor_expertise
--      WHERE tutor_username = ? AND subcategory_name = ?
--    e rifiutare se status != 'Approved'.
--
-- 6. GUARDIA ANTI-OVERLAP LEZIONI
--    Prima  INSERT in lesson, eseguire (con SELECT … FOR UPDATE
--    sulla riga del tutor per evitare race condition):
--      SELECT COUNT(*) FROM lesson
--      WHERE tutor_username = ?
--        AND status NOT IN ('Cancelled')
--        AND start_time < ?   -- end_time del nuovo slot
--        AND end_time   > ?   -- start_time del nuovo slot
--    Se COUNT > 0, lanciare un'eccezione applicativa.
--    L'indice idx_lesson_overlap_check rende questa query O(log n).
--
-- 7. NOTIFICHE DI SISTEMA
--    Usare sender_username = NULL per notifiche generate automaticamente
--    (conferme di pagamento, promemoria, scadenze, ecc.).
--    target_id referenzia l'entità correlata (application_id, lesson_id,
--    booking_id…) in base al campo type; non ha FK per design intenzionale
--    (referenza polimorfica).
--
-- 8. GESTIONE active_key  (DAO TutorApplicationDAO)
--    In INSERT (Draft):
--      active_key = student_username
--    In UPDATE verso Submitted:
--      active_key = student_username  (invariato, rimane attiva)
--    In UPDATE verso Accepted / Rejected:
--      active_key = CONCAT(id, '_closed')
--    Il vincolo UNIQUE (category_name, active_key) blocca qualsiasi
--    secondo INSERT attivo per la stessa coppia categoria+studente.
--    I valori '_closed' non collidono mai tra loro perché id è univoco.
--
-- 9. BOOKING – DATI FINANZIARI
--    fk_booking_student usa ON DELETE RESTRICT (non CASCADE).
--    Per disattivare uno studente usare il soft-delete (is_active = FALSE):
--    non eliminare mai fisicamente un utente che ha booking o review.
-- ============================================================
