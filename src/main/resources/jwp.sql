DROP TABLE IF EXISTS users;

CREATE TABLE users (
    userId varchar(20) NOT NULL,
    password varchar(20) NOT NULL,
    name varchar(20) NOT NULL,
    email varchar(50),
    PRIMARY KEY (userId)
);

INSERT INTO users VALUES ('admin', '1234', '관리자', 'admin@admin.com');