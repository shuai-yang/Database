/* Put all table create statements in this file as well as any UDFs */

CREATE DATABASE IF NOT EXISTS cs_410_final_project;
USE cs_410_final_project;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS classes;
DROP TABLE IF EXISTS class_enrollment;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS class_category;
DROP TABLE IF EXISTS assignments;
DROP TABLE IF EXISTS assignment_grade;
DROP FUNCTION IF EXISTS claculate_total_grade;
DROP FUNCTION IF EXISTS claculate_attempted_grade;

CREATE TABLE IF NOT EXISTS students(
	 student_id INT AUTO_INCREMENT,
	 username VARCHAR(50) NOT NULL,
	 name VARCHAR(50),
	 PRIMARY KEY (student_id)
);
CREATE TABLE IF NOT EXISTS classes(
	 class_id INT AUTO_INCREMENT,
	 course_number VARCHAR(50) NOT NULL,
	 term VARCHAR(50),
	 description VARCHAR(1000),
	 section_number INT(50),
	 PRIMARY KEY(class_id)
);
CREATE TABLE IF NOT EXISTS class_enrollment(
	 student_id INT, 
	 class_id INT,
	 PRIMARY KEY(student_id, class_id),
	 FOREIGN KEY(student_id) REFERENCES students(student_id),
	 FOREIGN KEY(class_id) REFERENCES classes(class_id)
);
CREATE TABLE IF NOT EXISTS categories(
	 category_id INT AUTO_INCREMENT,
	 name VARCHAR(50) NOT NULL,
	 PRIMARY KEY (category_id)
);
CREATE TABLE IF NOT EXISTS class_category(
	 class_id INT, 
	 category_id INT,
	 weight INT,
     PRIMARY KEY(class_id, category_id),
	 FOREIGN KEY(class_id) REFERENCES classes(class_id),
	 FOREIGN KEY(category_id) REFERENCES categories(category_id)
);
CREATE TABLE IF NOT EXISTS assignments(
	assignment_id INT AUTO_INCREMENT,
	name VARCHAR(50) NOT NULL,
	description VARCHAR(1000),
	point_value INT DEFAULT 0, 
	category_id INT,
	class_id INT,
	PRIMARY KEY(assignment_id),
	FOREIGN KEY(category_id) REFERENCES categories(category_id),
	FOREIGN KEY(class_id) REFERENCES classes(class_id)
);
CREATE TABLE IF NOT EXISTS assignment_grade(
	student_id INT,
	assignment_id INT,
	grade DEC(5,2) DEFAULT NULL,
    PRIMARY KEY(student_id, assignment_id),
	FOREIGN KEY(student_id) REFERENCES students(student_id),
	FOREIGN KEY(assignment_id) REFERENCES assignments(assignment_id)
);