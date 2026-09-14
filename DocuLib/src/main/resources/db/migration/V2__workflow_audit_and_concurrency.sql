ALTER TABLE resource_materials
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE acquisition_requests
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE code_sequences (
    sequence_name VARCHAR(30) NOT NULL,
    current_year INTEGER NOT NULL,
    next_value BIGINT NOT NULL,
    PRIMARY KEY (sequence_name)
);

INSERT INTO code_sequences (sequence_name, current_year, next_value)
VALUES ('MATERIAL', 0, 1);

INSERT INTO code_sequences (sequence_name, current_year, next_value)
VALUES ('ACQUISITION', 0, 1);

CREATE TABLE acquisition_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    request_code VARCHAR(40) NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    actor_username VARCHAR(100) NOT NULL,
    note VARCHAR(1000),
    changed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_acquisition_history_request_time
    ON acquisition_status_history (request_id, changed_at);

CREATE TABLE material_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    inventory_code VARCHAR(40) NOT NULL,
    material_title VARCHAR(300) NOT NULL,
    action VARCHAR(30) NOT NULL,
    actor_username VARCHAR(100) NOT NULL,
    details VARCHAR(1000),
    occurred_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_material_audit_material_time
    ON material_audit_logs (material_id, occurred_at);
