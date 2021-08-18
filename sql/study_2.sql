CREATE SCHEMA nodejs;

USE nodejs;

CREATE TABLE nodejs.users (
	id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL,
    age INT UNSIGNED NOT NULL,
    married TINYINT NOT NULL,
    comment TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT now(),
	PRIMARY KEY(id),
    UNIQUE INDEX name_UNIQUE (name ASC))
    COMMENT = '사용자 정보'
    DEFAULT CHARSET=utf8
    ENGINE=InnoDB;
    
CREATE TABLE nodejs.comments (
	id INT NOT NULL AUTO_INCREMENT,
    commenter INT NOT NULL,
    comment VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT now(),
    PRIMARY KEY(id),
    INDEX commenter_idx (commenter ASC),
    CONSTRAINT commenter
    FOREIGN KEY (commenter)
    REFERENCES nodejs.users (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
    COMMENT = '댓글'
    DEFAULT CHARSET=utf8
    ENGINE=InnoDB;

SHOW TABLES;

INSERT INTO users (name, age, married, comment)
VALUES ('zero', 24, 0, '자기소개1');

INSERT INTO users (name, age, married, comment)
VALUES ('nero', 32, 1, '자기소개2');

INSERT INTO comments (commenter, comment) 
VALUES (1, '안녕하세요. zero의 댓글입니다');

SELECT * FROM users;
SELECT * FROM comments;
SELECT name, married FROM users;
SELECT name, age FROM users WHERE married = 1 AND age > 30;
SELECT id, name FROM users WHERE married = 0 OR age > 30;
SELECT id, name FROM users ORDER BY age DESC;
SELECT id, name FROM users ORDER BY age DESC LIMIT 1;
SELECT id, name FROM users ORDER BY age DESC LIMIT 1 OFFSET 1;

UPDATE users
SET comment = '바꿀 내용'
where id = 2;

