-- =============================================================
-- LifeLink Blood Bank & Donor Navigation System
-- SQLite Schema Notes
-- =============================================================
-- This project initializes the SQLite database automatically on startup.
-- This file is a legacy reference only; the runtime database is created by
-- DatabaseInitializer and stored in lifelink.db at the project root.
-- =============================================================

PRAGMA foreign_keys = ON;

-- =============================================================
-- TABLE: users
-- Central authentication table for every person/entity
-- =============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id       INT          NOT NULL AUTO_INCREMENT,
    username      VARCHAR(60)  NOT NULL,
    email         VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,          -- BCrypt hash
    role          ENUM(
                    'DONOR',
                    'RECIPIENT',
                    'BLOOD_BANK',
                    'HOSPITAL',
                    'ADMIN'
                  )            NOT NULL,
    status        ENUM(
                    'ACTIVE',
                    'INACTIVE',
                    'SUSPENDED'
                  )            NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id),
    UNIQUE  KEY uq_username (username),
    UNIQUE  KEY uq_email    (email),
    INDEX   idx_role        (role),
    INDEX   idx_status      (status)
) ENGINE=InnoDB;

-- =============================================================
-- TABLE: donors
-- Extended profile for users with role = 'DONOR'
-- =============================================================
CREATE TABLE IF NOT EXISTS donors (
    donor_id           INT          NOT NULL AUTO_INCREMENT,
    user_id            INT          NOT NULL,
    first_name         VARCHAR(60)  NOT NULL,
    last_name          VARCHAR(60)  NOT NULL,
    blood_group        ENUM(
                         'A+','A-',
                         'B+','B-',
                         'AB+','AB-',
                         'O+','O-'
                       )            NOT NULL,
    date_of_birth      DATE         NOT NULL,
    gender             ENUM('MALE','FEMALE','OTHER') NOT NULL,
    phone              VARCHAR(20)  NOT NULL,
    address            VARCHAR(255) NOT NULL,
    city               VARCHAR(80)  NOT NULL,
    latitude           DECIMAL(10,7) NULL,
    longitude          DECIMAL(10,7) NULL,
    last_donation_date DATE         NULL,
    availability       TINYINT(1)   NOT NULL DEFAULT 1,  -- 1=available
    eligibility_status ENUM(
                         'ELIGIBLE',
                         'INELIGIBLE',
                         'PENDING_CHECK'
                       )            NOT NULL DEFAULT 'ELIGIBLE',
    total_donations    INT          NOT NULL DEFAULT 0,

    PRIMARY KEY (donor_id),
    UNIQUE  KEY uq_donor_user    (user_id),
    INDEX   idx_blood_group      (blood_group),
    INDEX   idx_availability     (availability),
    INDEX   idx_eligibility      (eligibility_status),
    CONSTRAINT fk_donor_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- TABLE: recipients
-- Extended profile for users with role = 'RECIPIENT'
-- =============================================================
CREATE TABLE IF NOT EXISTS recipients (
    recipient_id INT          NOT NULL AUTO_INCREMENT,
    user_id      INT          NOT NULL,
    first_name   VARCHAR(60)  NOT NULL,
    last_name    VARCHAR(60)  NOT NULL,
    blood_group  ENUM(
                   'A+','A-',
                   'B+','B-',
                   'AB+','AB-',
                   'O+','O-'
                 )            NOT NULL,
    phone        VARCHAR(20)  NOT NULL,
    address      VARCHAR(255) NOT NULL,
    city         VARCHAR(80)  NOT NULL,
    latitude     DECIMAL(10,7) NULL,
    longitude    DECIMAL(10,7) NULL,

    PRIMARY KEY (recipient_id),
    UNIQUE  KEY uq_recipient_user (user_id),
    INDEX   idx_blood_group       (blood_group),
    CONSTRAINT fk_recipient_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- TABLE: hospitals
-- =============================================================
CREATE TABLE IF NOT EXISTS hospitals (
    hospital_id  INT          NOT NULL AUTO_INCREMENT,
    user_id      INT          NULL,                       -- optional linked account
    name         VARCHAR(120) NOT NULL,
    address      VARCHAR(255) NOT NULL,
    city         VARCHAR(80)  NOT NULL,
    latitude     DECIMAL(10,7) NULL,
    longitude    DECIMAL(10,7) NULL,
    phone        VARCHAR(20)  NOT NULL,
    email        VARCHAR(120) NULL,
    status       ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (hospital_id),
    INDEX idx_hospital_status (status),
    INDEX idx_hospital_city   (city),
    CONSTRAINT fk_hospital_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- TABLE: blood_banks
-- =============================================================
CREATE TABLE IF NOT EXISTS blood_banks (
    blood_bank_id INT          NOT NULL AUTO_INCREMENT,
    user_id       INT          NULL,                       -- optional linked account
    name          VARCHAR(120) NOT NULL,
    address       VARCHAR(255) NOT NULL,
    city          VARCHAR(80)  NOT NULL,
    latitude      DECIMAL(10,7) NULL,
    longitude     DECIMAL(10,7) NULL,
    phone         VARCHAR(20)  NOT NULL,
    email         VARCHAR(120) NULL,
    status        ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (blood_bank_id),
    INDEX idx_bank_status (status),
    INDEX idx_bank_city   (city),
    CONSTRAINT fk_blood_bank_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- TABLE: blood_inventory
-- Each row = a batch of one blood group at one blood bank
-- =============================================================
CREATE TABLE IF NOT EXISTS blood_inventory (
    inventory_id    INT          NOT NULL AUTO_INCREMENT,
    blood_bank_id   INT          NOT NULL,
    blood_group     ENUM(
                      'A+','A-',
                      'B+','B-',
                      'AB+','AB-',
                      'O+','O-'
                    )            NOT NULL,
    quantity        INT          NOT NULL DEFAULT 0
                                 CHECK (quantity >= 0),
    collection_date DATE         NOT NULL,
    expiry_date     DATE         NOT NULL,
    status          ENUM(
                      'AVAILABLE',
                      'RESERVED',
                      'EXPIRED',
                      'ISSUED'
                    )            NOT NULL DEFAULT 'AVAILABLE',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (inventory_id),
    INDEX idx_inv_bank        (blood_bank_id),
    INDEX idx_inv_blood_group (blood_group),
    INDEX idx_inv_status      (status),
    INDEX idx_inv_expiry      (expiry_date),
    CONSTRAINT fk_inventory_bank
        FOREIGN KEY (blood_bank_id) REFERENCES blood_banks(blood_bank_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- TABLE: blood_requests
-- Created by recipients or hospitals
-- =============================================================
CREATE TABLE IF NOT EXISTS blood_requests (
    request_id    INT          NOT NULL AUTO_INCREMENT,
    requester_id  INT          NOT NULL,               -- users.user_id
    blood_group   ENUM(
                    'A+','A-',
                    'B+','B-',
                    'AB+','AB-',
                    'O+','O-'
                  )            NOT NULL,
    quantity      INT          NOT NULL DEFAULT 1
                               CHECK (quantity > 0),
    hospital_id   INT          NULL,
    priority      ENUM(
                    'NORMAL',
                    'URGENT',
                    'EMERGENCY'
                  )            NOT NULL DEFAULT 'NORMAL',
    status        ENUM(
                    'PENDING',
                    'MATCHING',
                    'PARTIALLY_FULFILLED',
                    'FULFILLED',
                    'CANCELLED'
                  )            NOT NULL DEFAULT 'PENDING',
    request_date  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    required_date DATETIME     NULL,
    notes         TEXT         NULL,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (request_id),
    INDEX idx_req_requester  (requester_id),
    INDEX idx_req_blood      (blood_group),
    INDEX idx_req_priority   (priority),
    INDEX idx_req_status     (status),
    INDEX idx_req_date       (request_date),
    CONSTRAINT fk_req_requester
        FOREIGN KEY (requester_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_req_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(hospital_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- TABLE: donor_matches
-- Results produced by the matching engine for each request
-- =============================================================
CREATE TABLE IF NOT EXISTS donor_matches (
    match_id     INT           NOT NULL AUTO_INCREMENT,
    request_id   INT           NOT NULL,
    donor_id     INT           NOT NULL,
    match_score  DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    distance_km  DECIMAL(8,2)  NULL,
    status       ENUM(
                   'NOTIFIED',
                   'ACCEPTED',
                   'DECLINED',
                   'EXPIRED'
                 )             NOT NULL DEFAULT 'NOTIFIED',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (match_id),
    UNIQUE  KEY uq_match_req_donor (request_id, donor_id),
    INDEX   idx_match_status (status),
    CONSTRAINT fk_match_request
        FOREIGN KEY (request_id) REFERENCES blood_requests(request_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_match_donor
        FOREIGN KEY (donor_id) REFERENCES donors(donor_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- TABLE: donations
-- Actual donation records after a donor fulfils a request
-- =============================================================
CREATE TABLE IF NOT EXISTS donations (
    donation_id     INT     NOT NULL AUTO_INCREMENT,
    donor_id        INT     NOT NULL,
    blood_bank_id   INT     NULL,
    request_id      INT     NULL,
    donation_date   DATE    NOT NULL,
    blood_group     ENUM(
                      'A+','A-',
                      'B+','B-',
                      'AB+','AB-',
                      'O+','O-'
                    )       NOT NULL,
    quantity_ml     INT     NOT NULL DEFAULT 450
                            CHECK (quantity_ml > 0),
    status          ENUM(
                      'COMPLETED',
                      'FAILED',
                      'PENDING'
                    )       NOT NULL DEFAULT 'COMPLETED',
    notes           TEXT    NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (donation_id),
    INDEX idx_don_donor     (donor_id),
    INDEX idx_don_bank      (blood_bank_id),
    INDEX idx_don_date      (donation_date),
    CONSTRAINT fk_donation_donor
        FOREIGN KEY (donor_id) REFERENCES donors(donor_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_donation_bank
        FOREIGN KEY (blood_bank_id) REFERENCES blood_banks(blood_bank_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_donation_request
        FOREIGN KEY (request_id) REFERENCES blood_requests(request_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- TABLE: notifications
-- In-app notification messages for any user
-- =============================================================
CREATE TABLE IF NOT EXISTS notifications (
    notification_id INT          NOT NULL AUTO_INCREMENT,
    user_id         INT          NOT NULL,
    title           VARCHAR(120) NOT NULL,
    message         TEXT         NOT NULL,
    type            ENUM(
                      'EMERGENCY_REQUEST',
                      'DONOR_MATCH',
                      'LOW_STOCK',
                      'REQUEST_APPROVED',
                      'REQUEST_REJECTED',
                      'REQUEST_FULFILLED',
                      'DONATION_REMINDER',
                      'SYSTEM',
                      'GENERAL'
                    )            NOT NULL DEFAULT 'GENERAL',
    read_status     TINYINT(1)   NOT NULL DEFAULT 0,   -- 0=unread, 1=read
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (notification_id),
    INDEX idx_notif_user   (user_id),
    INDEX idx_notif_read   (read_status),
    INDEX idx_notif_type   (type),
    CONSTRAINT fk_notif_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- TABLE: audit_logs
-- Immutable record of all critical application actions
-- =============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id      INT          NOT NULL AUTO_INCREMENT,
    user_id     INT          NULL,                      -- NULL for system actions
    action      VARCHAR(80)  NOT NULL,
    entity_type VARCHAR(40)  NULL,                      -- e.g. "BloodRequest"
    entity_id   INT          NULL,
    description TEXT         NULL,
    ip_address  VARCHAR(45)  NULL,
    timestamp   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (log_id),
    INDEX idx_log_user      (user_id),
    INDEX idx_log_action    (action),
    INDEX idx_log_timestamp (timestamp),
    CONSTRAINT fk_log_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =============================================================
-- TABLE: app_config
-- Key-value store for runtime configuration
-- =============================================================
CREATE TABLE IF NOT EXISTS app_config (
    config_key   VARCHAR(80)  NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    description  VARCHAR(255) NULL,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (config_key)
) ENGINE=InnoDB;

-- =============================================================
-- SEED DATA: Default admin account
-- Password: Admin@1234  (BCrypt hash generated externally)
-- Change password on first login!
-- =============================================================
INSERT IGNORE INTO users
    (username, email, password_hash, role, status)
VALUES (
    'admin',
    'admin@lifelink.local',
    -- BCrypt hash of "Admin@1234" (cost=12)
    '$2a$12$eG6cGhiT0RJKu2OPo7y7F.3jGBRdSZl9m1tWBz5cFXU7a4C3Zz7Ve',
    'ADMIN',
    'ACTIVE'
);

-- =============================================================
-- SEED DATA: Default configuration values
-- =============================================================
INSERT IGNORE INTO app_config (config_key, config_value, description) VALUES
('inventory.low_stock_threshold',    '5',    'Units below this value trigger LOW STOCK warning'),
('inventory.expiry_warning_days',    '7',    'Days before expiry to trigger EXPIRING SOON warning'),
('donation.min_days_between',        '56',   'Minimum days a donor must wait between donations (8 weeks)'),
('matching.max_distance_km',         '50',   'Maximum km radius for donor matching'),
('matching.min_score_threshold',     '40',   'Minimum match score (0-100) to include a donor'),
('scheduler.inventory_check_mins',   '30',   'How often (minutes) the inventory monitor runs');

-- =============================================================
-- END OF SCHEMA
-- =============================================================
