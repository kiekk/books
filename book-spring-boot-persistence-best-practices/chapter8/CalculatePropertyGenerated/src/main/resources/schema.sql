DROP TABLE IF EXISTS `book`;

CREATE TABLE IF NOT EXISTS `book`
(
    `id`         BIGINT           NOT NULL AUTO_INCREMENT,
    `title`      VARCHAR(255),
    `isbn`       VARCHAR(255),
    `price`      DOUBLE PRECISION NOT NULL,
    `discounted` DOUBLE GENERATED ALWAYS AS ((`price` - `price` * 0.25)) STORED,
    PRIMARY KEY (`id`)
);