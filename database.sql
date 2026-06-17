CREATE DATABASE belajar_spring_restfull;

USE belajar_spring_restfull;

CREATE TABLE users (
    username VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    token VARCHAR(255) ,
    token_expired_at BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (username),
    UNIQUE KEY (token)
) ENGINE=InnoDB;

DESC users;


CREATE TABLE contacts (
    id VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    phone VARCHAR(100),
    email VARCHAR(100),
    PRIMARY KEY (id), -- <--- Ditambahkan tanda koma di sini
    CONSTRAINT fk_users_contacts FOREIGN KEY (username) REFERENCES users(username) -- <--- Ditambahkan kata CONSTRAINT
) ENGINE=InnoDB;


CREATE TABLE addresses(
    id VARCHAR(100) NOT NULL,
    contact_id VARCHAR(100) NOT NULL,
    street VARCHAR(255),
    city VARCHAR(100),
    province VARCHAR(100),
    country VARCHAR(100),
    postal_code VARCHAR(10),
    
    PRIMARY KEY (id),
    CONSTRAINT  fk_contacts_addresses FOREIGN KEY (contact_id) REFERENCES contacts(id)
    
)ENGINE=InnoDB;