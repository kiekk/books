-- insert article, magazine and book
INSERT INTO article (title, id) VALUES ('CSS3 Tricks', 1);
INSERT INTO magazine (title, id) VALUES ('Java QA', 1);
INSERT INTO book (title, id) VALUES ('Java Coding Problems', 1);

DROP TRIGGER IF EXISTS Just_One_Of_Many;

CREATE TRIGGER Just_One_Of_Many BEFORE INSERT ON review FOR EACH ROW CALL "com.bookstore.trigger.ReviewTrigger";