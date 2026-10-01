CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE facilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE access_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    facility_id BIGINT NOT NULL REFERENCES facilities(id),
    status VARCHAR(20) NOT NULL,
    request_reason VARCHAR(1000),
    access_start_at TIMESTAMP NOT NULL,
    access_end_at TIMESTAMP NOT NULL,
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMP,
    reject_reason VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE access_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    facility_id BIGINT NOT NULL REFERENCES facilities(id),
    access_request_id BIGINT REFERENCES access_requests(id),
    result VARCHAR(20) NOT NULL,
    decision_reason VARCHAR(50) NOT NULL,
    accessed_at TIMESTAMP NOT NULL
);
