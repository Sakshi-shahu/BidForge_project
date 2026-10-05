-- OTP is NOT stored in Users. Email OTP is verified server-side using a short-lived signed recovery flow token.
CREATE DATABASE IF NOT EXISTS bidforge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bidforge;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS quotation_items;
DROP TABLE IF EXISTS rfq_history;
DROP TABLE IF EXISTS FinalizedQuotations;
DROP TABLE IF EXISTS RFQVendors;
DROP TABLE IF EXISTS RFQQuotations;
DROP TABLE IF EXISTS RFQItem;
DROP TABLE IF EXISTS RFQs;
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS Users;

CREATE TABLE Users (
    UserId INT NOT NULL AUTO_INCREMENT,
    FullName VARCHAR(255) NOT NULL,
    Email VARCHAR(255) NOT NULL,
    PasswordHash LONGTEXT NOT NULL,
    Role VARCHAR(30) NOT NULL,
    IsFirstTimeLogin TINYINT(1) NOT NULL DEFAULT 1,
    SecretKey LONGTEXT NULL,
    TotpEnabled TINYINT(1) NOT NULL DEFAULT 0,
    TokenVersion BIGINT NOT NULL DEFAULT 1,
    CreatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (UserId),
    UNIQUE KEY UK_Users_Email (Email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    refresh_token_hash VARCHAR(128) NOT NULL,
    revoked TINYINT(1) NOT NULL DEFAULT 0,
    expires_at TIMESTAMP(6) NOT NULL,
    token_version BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_user (user_id),
    UNIQUE KEY uk_refresh_hash (refresh_token_hash),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES Users(UserId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE RFQs (
    RFQId INT NOT NULL AUTO_INCREMENT,
    RFQNo VARCHAR(100) NOT NULL,
    IndentNo VARCHAR(100) NOT NULL,
    RFQLineNo VARCHAR(100) NULL,
    ItemNo VARCHAR(100) NULL,
    ItemName VARCHAR(255) NULL,
    ReqQty INT NULL,
    Description LONGTEXT NULL,
    DeliveryLocation VARCHAR(255) NULL,
    UOM VARCHAR(100) NULL,
    ReqDeliveryDate DATETIME(6) NULL,
    BidDate DATETIME(6) NULL,
    ExpiryDateofBid DATETIME(6) NULL,
    Mobile VARCHAR(50) NULL,
    ContactPerson VARCHAR(255) NULL,
    Status VARCHAR(30) NOT NULL,
    UserId INT NOT NULL,
    PRIMARY KEY (RFQId),
    UNIQUE KEY UK_RFQs_RfqNo (RFQNo),
    KEY IX_RFQs_UserId (UserId),
    CONSTRAINT FK_RFQs_Users_UserId FOREIGN KEY (UserId) REFERENCES Users(UserId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE RFQItem (
    ItemId INT NOT NULL AUTO_INCREMENT,
    RFQId INT NOT NULL,
    RFQLineNo VARCHAR(100) NOT NULL,
    ItemNo VARCHAR(100) NULL,
    FactoryCode VARCHAR(100) NULL,
    ItemName VARCHAR(255) NOT NULL,
    ReqQty INT NOT NULL,
    UOM VARCHAR(100) NOT NULL,
    ReqDeliveryDate DATETIME(6) NOT NULL,
    DeliveryLocation VARCHAR(255) NOT NULL,
    Description LONGTEXT NULL,
    PRIMARY KEY (ItemId),
    KEY IX_RFQItem_RFQId (RFQId),
    CONSTRAINT FK_RFQItem_RFQs_RFQId FOREIGN KEY (RFQId) REFERENCES RFQs(RFQId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE RFQVendors (
    Id INT NOT NULL AUTO_INCREMENT,
    RFQId INT NOT NULL,
    VendorId INT NOT NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY UK_RFQVendors_RfqVendor (RFQId, VendorId),
    KEY IX_RFQVendors_VendorId (VendorId),
    CONSTRAINT FK_RFQVendors_RFQs_RFQId FOREIGN KEY (RFQId) REFERENCES RFQs(RFQId) ON DELETE CASCADE,
    CONSTRAINT FK_RFQVendors_Users_VendorId FOREIGN KEY (VendorId) REFERENCES Users(UserId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE RFQQuotations (
    QuotationId INT NOT NULL AUTO_INCREMENT,
    BidNo VARCHAR(100) NULL,
    QuotedAmount DECIMAL(18,4) NULL,
    DeliveryDate DATETIME(6) NULL,
    PaymentTerms VARCHAR(255) NULL,
    Remarks LONGTEXT NULL,
    Status VARCHAR(30) NOT NULL,
    SubmittedDate DATETIME(6) NULL,
    InvoiceGenerated TINYINT(1) NOT NULL DEFAULT 0,
    RFQId INT NOT NULL,
    VendorId INT NOT NULL,
    PRIMARY KEY (QuotationId),
    UNIQUE KEY UK_RFQQuotations_RfqVendor (RFQId, VendorId),
    KEY IX_RFQQuotations_VendorId (VendorId),
    CONSTRAINT FK_RFQQuotations_RFQs_RFQId FOREIGN KEY (RFQId) REFERENCES RFQs(RFQId),
    CONSTRAINT FK_RFQQuotations_Users_VendorId FOREIGN KEY (VendorId) REFERENCES Users(UserId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quotation_items (
    id INT NOT NULL AUTO_INCREMENT,
    quotation_id INT NOT NULL,
    rfq_item_id INT NOT NULL,
    unit_price DECIMAL(18,4) NOT NULL,
    gst_percent DECIMAL(8,2) NOT NULL DEFAULT 0,
    gst_amount DECIMAL(18,4) NOT NULL DEFAULT 0,
    line_total DECIMAL(18,4) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quote_item (quotation_id, rfq_item_id),
    CONSTRAINT fk_quote_item_quote FOREIGN KEY (quotation_id) REFERENCES RFQQuotations(QuotationId) ON DELETE CASCADE,
    CONSTRAINT fk_quote_item_rfq_item FOREIGN KEY (rfq_item_id) REFERENCES RFQItem(ItemId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE FinalizedQuotations (
    FinalId INT NOT NULL AUTO_INCREMENT,
    FinalizedDate DATETIME(6) NULL,
    RFQId INT NOT NULL,
    QuotationId INT NOT NULL,
    PRIMARY KEY (FinalId),
    UNIQUE KEY IX_FinalizedQuotations_QuotationId (QuotationId),
    UNIQUE KEY IX_FinalizedQuotations_RFQId (RFQId),
    CONSTRAINT FK_FinalizedQuotations_RFQQuotations_QuotationId FOREIGN KEY (QuotationId) REFERENCES RFQQuotations(QuotationId),
    CONSTRAINT FK_FinalizedQuotations_RFQs_RFQId FOREIGN KEY (RFQId) REFERENCES RFQs(RFQId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE rfq_history (
    id INT NOT NULL AUTO_INCREMENT,
    rfq_id INT NOT NULL,
    action VARCHAR(40) NOT NULL,
    old_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NULL,
    previous_closing_date DATETIME(6) NULL,
    new_closing_date DATETIME(6) NULL,
    actor_id INT NULL,
    created_at DATETIME(6) NOT NULL,
    remarks VARCHAR(500) NULL,
    PRIMARY KEY (id),
    KEY idx_rfq_history_rfq (rfq_id),
    CONSTRAINT fk_rfq_history_rfq FOREIGN KEY (rfq_id) REFERENCES RFQs(RFQId) ON DELETE CASCADE,
    CONSTRAINT fk_rfq_history_actor FOREIGN KEY (actor_id) REFERENCES Users(UserId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
