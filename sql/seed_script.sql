/*Run this SQL file to delete existing data and populate your database with sample data.*/

USE cs_410_final_project;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE students;
TRUNCATE TABLE classes;
TRUNCATE TABLE class_enrollment;
TRUNCATE TABLE categories;
TRUNCATE TABLE class_category;
TRUNCATE TABLE assignments;
TRUNCATE TABLE assignment_grade;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO students(student_id, username, name)
VALUES
 (1,  'katej', 'Kate Johnson'),
 (2,  'clarew', 'Clare Williams'); 

INSERT INTO classes(class_id, course_number, term, description, section_number)
VALUES
 (1, 'CS410', 'SP22', 'Database', 1),
 (2, 'CS425', 'SP20', 'Network', 2),
 (3, 'CS421', 'FA21', 'Algorithms', 1 );
 
INSERT INTO class_enrollment(student_id, class_id)
VALUES
	(1, 2),
    (1, 3),
    (2, 3);

INSERT INTO categories(category_id, name)
VALUES
	(1, 'homework'),
	(2, 'midterm'),
    (3, 'project'),
    (4, 'final exam');

INSERT INTO class_category(class_id, category_id, weight)
VALUES
	(1, 3, 100),
    (2, 1, 25),
    (2, 2, 25),
	(2, 4, 50),
    (3, 4, 100);
    
INSERT INTO assignments(assignment_id, name, description, point_value, category_id, class_id)
VALUES
    (1, 'project', 'final project', 100, 3, 1),
	(2, 'hw1', 'module1-3', 80, 1, 2),
    (3, 'hw2', 'module4,5', 100, 1, 2),	
    (4, 'midterm', 'all modules', 50, 2, 2),
    (5, 'final exam', 'all modules', 100, 4, 2),	
    (6, 'final exam', 'all modules', 100, 4, 3);

INSERT INTO assignment_grade(assignment_id, student_id, grade)
VALUES
	(2, 1, 79.00),
    (3, 1, 89.00),
    (4, 1, 46.50),
    (5, 1, 94.00),
    (6, 1, 95.55),
    (6, 2, 90.00);
    
    