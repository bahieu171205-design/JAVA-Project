CREATE TABLE authors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    country VARCHAR(100),
    note VARCHAR(500),
    PRIMARY KEY (id)
);

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_code UNIQUE (code)
);

CREATE TABLE publishers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(180) NOT NULL,
    address VARCHAR(250),
    email VARCHAR(150),
    PRIMARY KEY (id)
);

CREATE TABLE user_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(60) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_accounts_username UNIQUE (username),
    CONSTRAINT uk_user_accounts_email UNIQUE (email)
);

CREATE TABLE resource_materials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    inventory_code VARCHAR(40) NOT NULL,
    title VARCHAR(300) NOT NULL,
    isbn_issn VARCHAR(30),
    material_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    language VARCHAR(80),
    publish_year INTEGER,
    edition VARCHAR(80),
    call_number VARCHAR(80),
    pages INTEGER,
    quantity INTEGER NOT NULL,
    description TEXT,
    category_id BIGINT,
    publisher_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    trashed BOOLEAN NOT NULL,
    trashed_at DATETIME(6),
    trashed_by_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_resource_materials_inventory_code UNIQUE (inventory_code),
    CONSTRAINT fk_material_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_material_publisher FOREIGN KEY (publisher_id) REFERENCES publishers (id),
    CONSTRAINT fk_material_trashed_by FOREIGN KEY (trashed_by_id) REFERENCES user_accounts (id)
);

CREATE TABLE material_authors (
    material_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (material_id, author_id),
    CONSTRAINT fk_material_authors_material FOREIGN KEY (material_id)
        REFERENCES resource_materials (id) ON DELETE CASCADE,
    CONSTRAINT fk_material_authors_author FOREIGN KEY (author_id)
        REFERENCES authors (id)
);

CREATE TABLE material_favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_material_favorite_user_material UNIQUE (user_id, material_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_favorite_material FOREIGN KEY (material_id)
        REFERENCES resource_materials (id) ON DELETE CASCADE
);

CREATE TABLE material_shares (
    id BIGINT NOT NULL AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    shared_with_id BIGINT NOT NULL,
    shared_by_id BIGINT NOT NULL,
    shared_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_material_share_recipient UNIQUE (material_id, shared_with_id),
    CONSTRAINT fk_share_material FOREIGN KEY (material_id)
        REFERENCES resource_materials (id) ON DELETE CASCADE,
    CONSTRAINT fk_share_recipient FOREIGN KEY (shared_with_id) REFERENCES user_accounts (id),
    CONSTRAINT fk_share_actor FOREIGN KEY (shared_by_id) REFERENCES user_accounts (id)
);

CREATE TABLE acquisition_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_code VARCHAR(40) NOT NULL,
    proposed_title VARCHAR(300) NOT NULL,
    requester VARCHAR(150) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    quantity INTEGER NOT NULL,
    estimated_price DECIMAL(15, 2),
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    note VARCHAR(1000),
    requested_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_acquisition_requests_code UNIQUE (request_code)
);

CREATE INDEX idx_materials_updated_at ON resource_materials (updated_at);
CREATE INDEX idx_materials_status ON resource_materials (status);
CREATE INDEX idx_acquisitions_updated_at ON acquisition_requests (updated_at);
