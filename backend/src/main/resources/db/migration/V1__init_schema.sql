-- =============================================================
-- V1__init_schema.sql
-- SME Loan Origination Platform — Initial Schema
-- =============================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================
-- 1. USERS  (internal staff: RM, MANAGER, ADMIN)
-- =============================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(64)  NOT NULL UNIQUE,
    email           VARCHAR(128) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(128) NOT NULL,
    role            VARCHAR(32)  NOT NULL CHECK (role IN ('RM', 'MANAGER', 'ADMIN')),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_role     ON users (role);

-- =============================================================
-- 2. CUSTOMERS  (SME borrowers — onboarded via token)
-- =============================================================
CREATE TABLE customers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name        VARCHAR(256) NOT NULL,
    unified_business_no VARCHAR(20)  NOT NULL UNIQUE,   -- UBN / Business Registry No.
    legal_representative VARCHAR(128),
    industry_code       VARCHAR(16),
    established_date    DATE,
    registered_address  TEXT,
    contact_phone       VARCHAR(32),
    contact_email       VARCHAR(128),
    -- KYC status
    kyc_status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING'
                            CHECK (kyc_status IN ('PENDING', 'IN_PROGRESS', 'VERIFIED', 'FAILED')),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_ubn ON customers (unified_business_no);

-- =============================================================
-- 3. LOAN_APPLICATIONS  (core entity — drives the lifecycle)
-- =============================================================
CREATE TABLE loan_applications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_no      VARCHAR(32)  NOT NULL UNIQUE,   -- human-readable: LN-20240001
    customer_id         UUID         NOT NULL REFERENCES customers (id),
    assigned_rm_id      UUID         NOT NULL REFERENCES users (id),

    -- Requested loan terms
    requested_amount    NUMERIC(18, 2) NOT NULL,
    requested_currency  VARCHAR(3)   NOT NULL DEFAULT 'TWD',
    requested_tenor_months INT       NOT NULL,
    loan_purpose        VARCHAR(256),

    -- Lifecycle state machine
    status              VARCHAR(32)  NOT NULL DEFAULT 'DRAFT'
                            CHECK (status IN (
                                'DRAFT',
                                'PENDING_ONBOARDING',
                                'ONBOARDING_COMPLETED',
                                'CHECKS_IN_PROGRESS',
                                'CHECKS_COMPLETED',
                                'SCORING_IN_PROGRESS',
                                'SCORED',
                                'PENDING_MANUAL_REVIEW',
                                'APPROVED',
                                'CONDITIONALLY_APPROVED',
                                'REJECTED',
                                'OFFER_SENT',
                                'OFFER_ACCEPTED',
                                'DISBURSED',
                                'WITHDRAWN'
                            )),

    -- Onboarding token (for customer portal access)
    onboarding_token    VARCHAR(128) UNIQUE,
    onboarding_token_expires_at TIMESTAMPTZ,
    onboarding_completed_at     TIMESTAMPTZ,

    -- Review / decision
    reviewed_by_id      UUID REFERENCES users (id),
    reviewed_at         TIMESTAMPTZ,
    review_notes        TEXT,
    decision_reason     TEXT,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loan_app_customer    ON loan_applications (customer_id);
CREATE INDEX idx_loan_app_rm          ON loan_applications (assigned_rm_id);
CREATE INDEX idx_loan_app_status      ON loan_applications (status);
CREATE INDEX idx_loan_app_app_no      ON loan_applications (application_no);
CREATE INDEX idx_loan_app_token       ON loan_applications (onboarding_token);

-- =============================================================
-- 4. APPLICATION_PARTIES  (directors, guarantors, shareholders)
-- =============================================================
CREATE TABLE application_parties (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID         NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    party_type          VARCHAR(32)  NOT NULL CHECK (party_type IN ('DIRECTOR', 'GUARANTOR', 'SHAREHOLDER', 'AUTHORIZED_SIGNATORY')),
    full_name           VARCHAR(128) NOT NULL,
    id_number           VARCHAR(32),
    nationality         VARCHAR(64),
    ownership_pct       NUMERIC(5, 2),      -- for shareholders
    is_pep              BOOLEAN DEFAULT FALSE,   -- Politically Exposed Person flag
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_parties_application ON application_parties (application_id);

-- =============================================================
-- 5. DOCUMENTS  (uploaded files — stored in MinIO)
-- =============================================================
CREATE TABLE documents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID         NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    document_type       VARCHAR(64)  NOT NULL CHECK (document_type IN (
                            'BUSINESS_REGISTRATION',
                            'FINANCIAL_STATEMENT_Y1',
                            'FINANCIAL_STATEMENT_Y2',
                            'FINANCIAL_STATEMENT_Y3',
                            'TAX_RETURN',
                            'BANK_STATEMENT',
                            'ID_CARD_FRONT',
                            'ID_CARD_BACK',
                            'OTHER'
                        )),
    original_filename   VARCHAR(256) NOT NULL,
    storage_key         VARCHAR(512) NOT NULL,      -- MinIO object key
    content_type        VARCHAR(128),
    file_size_bytes     BIGINT,
    checksum_sha256     VARCHAR(64),
    uploaded_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_documents_application ON documents (application_id);
CREATE INDEX idx_documents_type        ON documents (document_type);

-- =============================================================
-- 6. EXTERNAL_CHECK_RESULTS  (JCIC, AML, Business Registry)
-- =============================================================
CREATE TABLE external_check_results (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID         NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    check_type          VARCHAR(32)  NOT NULL CHECK (check_type IN ('JCIC_CREDIT_BUREAU', 'AML_SCREENING', 'BUSINESS_REGISTRY')),
    provider_name       VARCHAR(64)  NOT NULL,   -- e.g. 'MOCK_JCIC_V1'
    status              VARCHAR(32)  NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'TIMEOUT')),

    -- Full request/response snapshots stored as JSONB for auditability
    request_payload     JSONB,
    response_payload    JSONB,

    -- Extracted key results
    result_code         VARCHAR(32),    -- e.g. 'PASS', 'FAIL', 'REFER'
    risk_level          VARCHAR(16),    -- LOW / MEDIUM / HIGH
    summary             TEXT,

    -- Timing
    requested_at        TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    duration_ms         INT,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ext_check_application ON external_check_results (application_id);
CREATE INDEX idx_ext_check_type        ON external_check_results (check_type);
CREATE INDEX idx_ext_check_status      ON external_check_results (status);
-- GIN index for JSONB query support
CREATE INDEX idx_ext_check_response_gin ON external_check_results USING GIN (response_payload);

-- =============================================================
-- 7. SCORE_RESULTS  (output of credit scoring engine)
-- =============================================================
CREATE TABLE score_results (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID         NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,
    total_score         NUMERIC(6, 2) NOT NULL,
    score_grade         VARCHAR(4)    NOT NULL,   -- AAA / AA / A / BBB / BB / B / CCC / D

    -- Dimension breakdown stored as JSONB
    score_breakdown     JSONB         NOT NULL,   -- { "credit_bureau": 245, "financial_ratio": 180, ... }
    weight_config       JSONB         NOT NULL,   -- snapshot of weights used at scoring time

    -- Automated decision
    auto_decision       VARCHAR(32)   NOT NULL CHECK (auto_decision IN ('AUTO_APPROVE', 'AUTO_REJECT', 'MANUAL_REVIEW')),
    decision_rationale  TEXT,

    scored_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    scored_by_engine    VARCHAR(64)   NOT NULL DEFAULT 'RULE_ENGINE_V1'
);

CREATE UNIQUE INDEX idx_score_results_application ON score_results (application_id);

-- =============================================================
-- 8. LOAN_OFFERS  (generated after approval)
-- =============================================================
CREATE TABLE loan_offers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID         NOT NULL REFERENCES loan_applications (id) ON DELETE CASCADE,

    -- Approved terms
    approved_amount     NUMERIC(18, 2) NOT NULL,
    currency            VARCHAR(3)    NOT NULL DEFAULT 'TWD',
    tenor_months        INT           NOT NULL,
    annual_interest_rate NUMERIC(6, 4) NOT NULL,   -- e.g. 0.0350 = 3.50%
    monthly_payment     NUMERIC(18, 2) NOT NULL,
    total_interest      NUMERIC(18, 2) NOT NULL,
    total_repayment     NUMERIC(18, 2) NOT NULL,
    origination_fee     NUMERIC(18, 2) NOT NULL DEFAULT 0,

    -- Offer validity
    offer_valid_until   DATE          NOT NULL,
    offer_pdf_key       VARCHAR(512),    -- MinIO key for the PDF contract

    -- Status
    status              VARCHAR(32)   NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'SENT', 'ACCEPTED', 'DECLINED', 'EXPIRED')),
    accepted_at         TIMESTAMPTZ,
    declined_at         TIMESTAMPTZ,

    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_loan_offers_application ON loan_offers (application_id);

-- =============================================================
-- 9. REPAYMENT_SCHEDULES  (amortization table)
-- =============================================================
CREATE TABLE repayment_schedules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_offer_id       UUID         NOT NULL REFERENCES loan_offers (id) ON DELETE CASCADE,
    installment_no      INT          NOT NULL,   -- 1-based month index
    due_date            DATE         NOT NULL,
    opening_balance     NUMERIC(18, 2) NOT NULL,
    principal_amount    NUMERIC(18, 2) NOT NULL,
    interest_amount     NUMERIC(18, 2) NOT NULL,
    total_payment       NUMERIC(18, 2) NOT NULL,
    closing_balance     NUMERIC(18, 2) NOT NULL,
    UNIQUE (loan_offer_id, installment_no)
);

CREATE INDEX idx_repayment_offer ON repayment_schedules (loan_offer_id);

-- =============================================================
-- SEED DATA — Demo users (password: demo1234)
-- BCrypt hash generated with cost 10:
--   demo1234 → $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- =============================================================
INSERT INTO users (id, username, email, password_hash, full_name, role) VALUES
(
    gen_random_uuid(),
    'rm_demo',
    'rm.demo@smeloan.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Demo Relationship Manager',
    'RM'
),
(
    gen_random_uuid(),
    'manager_demo',
    'manager.demo@smeloan.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Demo Credit Manager',
    'MANAGER'
),
(
    gen_random_uuid(),
    'admin_demo',
    'admin.demo@smeloan.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Demo System Administrator',
    'ADMIN'
);
