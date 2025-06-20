-- DML
CREATE TABLE IF NOT EXISTS `author` (
                                        `id` BIGINT NOT NULL AUTO_INCREMENT,
                                        `age` INTEGER NOT NULL,
                                        `genre` VARCHAR(255),
    `name` VARCHAR(255),
    PRIMARY KEY (`id`));

CREATE TABLE IF NOT EXISTS `book` (
                                      `id` BIGINT NOT NULL AUTO_INCREMENT,
                                      `isbn` VARCHAR(255),
    `title` VARCHAR(255),
    `author_id` BIGINT,
    PRIMARY KEY (`id`));
ALTER TABLE `book` ADD CONSTRAINT FK_AUTHOR FOREIGN KEY (`author_id`) REFERENCES author(`id`);