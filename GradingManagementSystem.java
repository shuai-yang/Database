import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradingManagementSystem {
	static int active_class_id = -1;

	/** Class Management: 
	 *  list-classes
	 *  list classes with the # of students in each
	 */
	public static void listAllClassesWithNumStudents(){
		Connection connection = null;
	    Statement sqlStatement = null;
	
		try {
			//Get connection
	    	connection = Database.getDatabaseConnection();
	    	//Query
	    	sqlStatement = connection.createStatement();
	    	String sql = "SELECT  c.class_id, course_number, term, section_number, description, COUNT(student_id) \r\n"
	    			 +"FROM classes c \r\n"
	    			 +"LEFT JOIN class_enrollment r \r\n"
	    			 +"ON r.class_id=c.class_id\r\n"
	    			 +"GROUP BY c.class_id, course_number, term, section_number, description;";
	    	ResultSet rs = sqlStatement.executeQuery(sql); 
	    	//Process
        	System.out.println("Class ID | Course Number| Term | Section Number | Description | Number of Students");
        	System.out.println("-".repeat(128));
        	while(rs.next()) { 
        		int class_id = rs.getInt("class_id"); 
        		String course_number = rs.getString("course_number"); 
        		String term = rs.getString("term"); 
        		int section_number = rs.getInt("section_number"); 
        		String des = rs.getString("description");
        		int number_of_students = rs.getInt("COUNT(student_id)"); 
        		System.out.format("%10d%10s%10s%10d       %10s  %10d\n", 
        				class_id, course_number,term, section_number, des, number_of_students );
        	}
        	//Close result
       	 	rs.close();
		}
		catch(SQLException sqlException){
			System.out.println("Failed to list all classes with number of students");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlStatement != null)
                    sqlStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}
	
	/** Class Management: 
	 *  new-class CS410 Sp22 1 "Databases" 
	 *  create a class with course number, term, section number, description
	 */
	public static void createNewClass(String newCourseNum, String newTerm, String newSection, String newDescription){
		Connection connection = null;
		PreparedStatement sqlPreparedStatement = null;
	
		try {
			 //Get connection
	    	 connection = Database.getDatabaseConnection();
	    	 //Query
	    	 String sql = "INSERT INTO\r\n "
	    			 +"classes(course_number, term, section_number,description )\r\n"
	    			 +"VALUES(?,?,?,?);";
	    	 sqlPreparedStatement = connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
        	 
        	 sqlPreparedStatement.setString(1, newCourseNum);
        	 sqlPreparedStatement.setString(2, newTerm);
        	 sqlPreparedStatement.setString(3, newSection);
        	 sqlPreparedStatement.setString(4, newDescription);
        	 
        	 int impactedRows = sqlPreparedStatement.executeUpdate(); 
        	 
        	 if (impactedRows == 0) {
                 throw new SQLException("Creating new class failed, no rows affected.");
             }
        	 ResultSet generatedKey = sqlPreparedStatement.getGeneratedKeys();
        	 int class_id = -1;
        	 if (generatedKey.next()) {
        		 class_id = generatedKey.getInt(1);
        	 }
	    	//Process
        	System.out.println("Class ID | Course Number| Term | Section Number | Description");
        	System.out.println("-".repeat(128));
        	System.out.format("%10d%10s%10s%10s        %10s\n", 
    				class_id, newCourseNum, newTerm, newSection, newDescription);    
        	//Close result
        	generatedKey.close();
	    }catch(SQLException sqlException){
			System.out.println("Failed to create a new class with class information");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlPreparedStatement != null)
                	sqlPreparedStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}
	
	/** Class Management: Active a class
	 *  select-class CS410
	 *  selects the only section of CS410 in the most recent term, if there is only one such section;
	 *  if there are multiple sections it fails.
	 */
	public static void activateClass(String myCourseNum) {
		Connection connection = null;
		PreparedStatement sqlPreparedStatement = null;

        try {
        	connection = Database.getDatabaseConnection();
        	String sql = "select course_number, term, description, count(section_number)\r\n "
	       			 + "from classes \r\n"
	       			 + "where substr(term,3,2) = "
	       			 + "(select  max(substr(term,3,2)) from classes where course_number = ?)\r\n"
        		     + "and substr(term,1,2)=(\r\n"
        		     + "    select min(substr(term,1,2))\r\n"
        		     + "    from classes\r\n"
        		     + "    where course_number = ? and substr(term,3,2) = "
        		     		+ "(select max(substr(term,3,2)) from classes where course_number = ?)\r\n"
        		     + ")\r\n"
        	         + "group by course_number, term, description;";
        	sqlPreparedStatement = connection.prepareStatement(sql);
        	sqlPreparedStatement.setString(1, myCourseNum);
        	sqlPreparedStatement.setString(2, myCourseNum);
        	sqlPreparedStatement.setString(3, myCourseNum);
        	
        	ResultSet rs = sqlPreparedStatement.executeQuery(); 
        	if(!rs.next()) {
        		System.out.println(myCourseNum + " doesn't exit in the database");
        		return;
        	};
			int number_of_sections = rs.getInt("count(section_number)");
			String term = rs.getString("term");
	    	if(number_of_sections> 1){
	    		 System.out.println("There are "+number_of_sections+" sections of " + myCourseNum 
	    				 + " in the latest term " + term +". Failed to activate a class."); 
	    	}else {
	    		 System.out.println("Class is activated successfully!\n"); 
	    		 sql = "select *\r\n "
		       			 + "from classes \r\n"
		       			 + "where course_number = ? and substr(term,3,2) = "
		       			 		+ "(select max(substr(term,3,2)) from classes where course_number = ?)\r\n"
	        		     + "and substr(term,1,2)=(\r\n"
	        		     + "    select min(substr(term,1,2))\r\n"
	        		     + "    from classes\r\n"
	        		     + "    where course_number = ? and substr(term,3,2) = "
	        		     		+ "(select  max(substr(term,3,2)) from classes where course_number = ?)\r\n"
	        		     + ")\r\n"; 
	    		 sqlPreparedStatement = connection.prepareStatement(sql);
	         	 sqlPreparedStatement.setString(1, myCourseNum);
	         	 sqlPreparedStatement.setString(2, myCourseNum);
	         	 sqlPreparedStatement.setString(3, myCourseNum);
	         	 sqlPreparedStatement.setString(4, myCourseNum);
	         	 
	         	 rs = sqlPreparedStatement.executeQuery(); 
	    		 rs.next();
	         	 System.out.println("Class ID | Course Number| Term | Section Number | Description ");
	         	 System.out.println("-".repeat(128));
	     		 int class_id = rs.getInt("class_id"); 
	     		 active_class_id=class_id;
	     		 String course_number = rs.getString("course_number"); 
	     		 int section_number = rs.getInt("section_number"); 
	     		 String des = rs.getString("description");
	     		 System.out.format("%10d%10s%10s%10d        %10s\n", 
	    				class_id, course_number, term, section_number, des); 
	     		try {
	                if (sqlPreparedStatement != null)sqlPreparedStatement.close();
	            } catch (SQLException se2) {
	            }
	    	 }	
             rs.close();
        }catch (SQLException sqlException) {
            System.out.println("Failed to activate a class");
            System.out.println(sqlException.getMessage());
        }finally {
	         try {
	             if (sqlPreparedStatement != null)sqlPreparedStatement.close();
	         } catch (SQLException se2) {
	         }
	         try {
	             if (connection != null)
	                 connection.close();
	         } catch (SQLException se) {
	             se.printStackTrace();
	         }
	   }
	}

	/** Class Management: Active a class
	 *  select-class CS410 Sp22
	 *  selects the only section of CS410 in Spring 2022;
	 *  if there are multiple such sections, it fails.
	 */
	public static void activateClass(String myCourseNum, String myTerm) {
		Connection connection = null;
		PreparedStatement sqlPreparedStatement = null;
		PreparedStatement sqlPreparedStatement2 = null;
		
        try {
        	connection = Database.getDatabaseConnection();
        	String sql = "select course_number, term, description, count(section_number)\r\n "
	       			 + "from classes \r\n"
	       			 + "where course_number = ? and term = ? \r\n"
        	         + "group by course_number, term, description;";
        	sqlPreparedStatement = connection.prepareStatement(sql);
        	sqlPreparedStatement.setString(1, myCourseNum);
        	sqlPreparedStatement.setString(2, myTerm);
        	
        	ResultSet rs = sqlPreparedStatement.executeQuery(); 
        	if(!rs.next()) {
        		System.out.println("No " + myCourseNum + " in term " + myTerm);
        		return;
        	};
			int number_of_sections = rs.getInt("count(section_number)");
			String term = rs.getString("term");
	    	if(number_of_sections> 1){
	    		 System.out.println("There are "+number_of_sections+" sections of " + myCourseNum 
	    				 + " in the latest term " + term +". Failed to activate a class."); 
	    	}else {
	    		 System.out.println("Class is activated successfully!\n"); 
	    		 sql = "select *\r\n "
		       			 + "from classes \r\n"
		       			 + "where course_number = ? and term = ?;";
	    		 sqlPreparedStatement2 = connection.prepareStatement(sql);
	         	 sqlPreparedStatement2.setString(1, myCourseNum);
	         	 sqlPreparedStatement2.setString(2, myTerm);
	         	
	    		 rs = sqlPreparedStatement2.executeQuery(); 
	    		 rs.next();
	         	 System.out.println("Class ID | Course Number| Term | Section Number | Description ");
	         	 System.out.println("-".repeat(128));
	     		 int class_id = rs.getInt("class_id"); 
	     		 active_class_id=class_id;
	     		 String course_number = rs.getString("course_number"); 
	     		 int section_number = rs.getInt("section_number"); 
	     		 String des = rs.getString("description");
	     		 System.out.format("%10d%10s%10s%10d        %10s\n", 
	    				class_id, course_number, term, section_number, des); 
	    	 }	
             rs.close();
        }catch (SQLException sqlException) {
            System.out.println("Failed to activate a class");
            System.out.println(sqlException.getMessage());
        }finally {
	         try {
	             if (sqlPreparedStatement != null) sqlPreparedStatement.close();
	             if (sqlPreparedStatement2 != null) sqlPreparedStatement.close();
	         } catch (SQLException se2) {
	         }
	         try {
	             if (connection != null)
	                 connection.close();
	         } catch (SQLException se) {
	             se.printStackTrace();
	         }
       }
	}
	
	/** Class Management: Active a class
	 *  select-class CS410 Sp22 1
	 *  selects a specific section.
	 */
	public static void activateClass(String myCourseNum, String myTerm, String mySection) {
		Connection connection = null;
		PreparedStatement sqlPreparedStatement = null;

        try {
        	connection = Database.getDatabaseConnection();
        	String sql = "select*\r\n "
	       			 + "from classes \r\n"
	       			 + "where course_number = ? and term = ? and section_number= ?;"; 
        	sqlPreparedStatement = connection.prepareStatement(sql);
        	sqlPreparedStatement.setString(1, myCourseNum);
        	sqlPreparedStatement.setString(2, myTerm);
        	sqlPreparedStatement.setString(3, mySection);
        	
        	ResultSet rs = sqlPreparedStatement.executeQuery(); 
        	if(!rs.next()) {
        		System.out.println("No " + myCourseNum +" section "+ mySection + " in term " + myTerm);
        		return;
        	};
    		System.out.println("Class is activated successfully!\n"); 
    		//rs.next();
         	System.out.println("Class ID | Course Number| Term | Section Number | Description ");
         	System.out.println("-".repeat(128));
     		int class_id = rs.getInt("class_id"); 
     		active_class_id=class_id;
     		String course_number = rs.getString("course_number");
     		String term = rs.getString("term"); 
     		int section_number = rs.getInt("section_number"); 
     		String des = rs.getString("description");
     		System.out.format("%10d%10s%10s%10d        %10s\n", 
    				class_id, course_number, term, section_number, des);
            rs.close();
        }catch (SQLException sqlException) {
            System.out.println("Failed to activate a class");
            System.out.println(sqlException.getMessage());
        } finally {
	         try {
	             if (sqlPreparedStatement != null)
	                 sqlPreparedStatement.close();
	         } catch (SQLException se2) {
	         }
	         try {
	             if (connection != null)
	                 connection.close();
	         } catch (SQLException se) {
	             se.printStackTrace();
	         }
       }
	}
	
	/** Class Management: 
	 *  show-class
	 *  shows the currently-active class
	 */
	public static void showClass() {
		Connection connection = null;
	    Statement sqlStatement = null;
	    //System.out.println(active_class_id);
		try {
	    	connection = Database.getDatabaseConnection();
	    	sqlStatement = connection.createStatement();
	    	String sql = "SELECT  *\r\n"
	    			 +"FROM classes \r\n"
	    			 +"WHERE class_id = "+active_class_id+";";
	    	ResultSet rs = sqlStatement.executeQuery(sql); 
        	System.out.println("Class ID | Course Number| Term | Section Number | Description ");
        	System.out.println("-".repeat(128));
        	while(rs.next()) { 
        		int class_id = rs.getInt("class_id"); 
        		String course_number = rs.getString("course_number"); 
        		String term = rs.getString("term"); 
        		int section_number = rs.getInt("section_number"); 
        		String des = rs.getString("description");
        		System.out.format("%10d%10s%10s%10d        %10s\n", 
	    				class_id, course_number, term, section_number, des);
        	}
       	 	rs.close();
		}
		catch(SQLException sqlException){
			System.out.println("Failed to show the currently-active class");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlStatement != null)
                    sqlStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}	
	
	/** Category and Assignment Management(context of the currently active class): 
	 *  add-category name weight (add-category homework 35)
	 *  add a new category with weight
	 */
	public static void addCategoriesWithWeights(String categoryName, String myWeight){
		Connection connection = null;
		PreparedStatement sqlPreparedStatement = null;
	
		try {
	    	 connection = Database.getDatabaseConnection();
	    	 //get category_id using categoryName
	    	 String sql = "select category_id\r\n "
	    			 +"from categories\r\n"
	    			 +"where name = ?;";
	    	 sqlPreparedStatement = connection.prepareStatement(sql);
	         sqlPreparedStatement.setString(1, categoryName);
	        	
	    	 ResultSet rs = sqlPreparedStatement.executeQuery(); 
	    	 if(!rs.next()) {
	        		System.out.println(categoryName +" doesn't exist in categories table.");
	        		return;
	         };
	         String categoryId = rs.getString("category_id");
             
	    	 sql = "INSERT INTO\r\n "
	    			 +"class_category\r\n"
	    			 +"VALUES(?, ?, ?);";
	    	 sqlPreparedStatement = connection.prepareStatement(sql);
	    	 sqlPreparedStatement.setInt(1, active_class_id);
        	 sqlPreparedStatement.setString(2, categoryId);
        	 sqlPreparedStatement.setString(3, myWeight);
        	 //System.out.println(sqlPreparedStatement);
        	 
        	 int impactedRows = sqlPreparedStatement.executeUpdate(); 
        	 if (impactedRows == 0) {
                 throw new SQLException("Adding new categories to currently active class failed, no rows affected.");
             }
        	 System.out.println(" Class ID | Category ID | Category Name | Weight");
        	 System.out.println("-".repeat(128));
        	 System.out.format("%10d%10s     %10s  %10s\n", 
        			 active_class_id, categoryId, categoryName, myWeight); 
        	 rs.close();
	    }catch(SQLException sqlException){
			System.out.println("Failed to add a new category to currently active class");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlPreparedStatement != null)
                	sqlPreparedStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}
	
	/** Category and Assignment Management(context of the currently active class): 
	 *  show-categories
	 *  list the categories with their weights
	 */
	public static void showCategories() {
		Connection connection = null;
	    Statement sqlStatement = null;
	    //System.out.println(active_class_id);
		try {
			//Get connection
	    	connection = Database.getDatabaseConnection();
	    	//Query
	    	sqlStatement = connection.createStatement();
	    	String sql = "SELECT cc.*, c.name \r\n"
	    			 +"FROM class_category cc \r\n"
	    			 +"LEFT JOIN categories c \r\n"
	    			 +"ON cc.category_id = c.category_id \r\n"
	    			 +"WHERE cc.class_id =" + active_class_id+";";
	    	ResultSet rs = sqlStatement.executeQuery(sql); 
	    	if(!rs.next()) {
        		System.out.println("No categories in currently active class yet.");
        		return;
	    	};
	    	//Process
        	System.out.println("Class ID | Category ID | Category Name | Weight ");
        	System.out.println("-".repeat(128));
        	do{ 
        		int class_id = rs.getInt("class_id"); 
        		int category_id = rs.getInt("category_id"); 
        		String name = rs.getString("name");
        		int weight = rs.getInt("weight"); 
        		System.out.format("%10d%10d   %10s  %10d\n", 
           			 active_class_id, category_id, name, weight); 
        	}while(rs.next());
        	//Close result
       	 	rs.close();
		}
		catch(SQLException sqlException){
			System.out.println("Failed to list all categories in currently active class");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlStatement != null)
                    sqlStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}	
	
	/** Category and Assignment Management(context of the currently active class): 
	 *  add-assignment name category description points(add-assignment hw1 homework module1-3 80)
	 *  add a new assignment with points
	 */
	public static void addAssignmentsWithPoints(String assignmentName, String categoryName, String description, String points) {
		Connection connection = null;
		PreparedStatement sqlPreparedStatement = null;
	
		try {
	    	 connection = Database.getDatabaseConnection();
	    	 //get category_id using categoryName
	    	 String sql = "select category_id\r\n "
	    			 +"from categories\r\n"
	    			 +"where name= ?;";
	    	 sqlPreparedStatement = connection.prepareStatement(sql);
	         sqlPreparedStatement.setString(1, categoryName);
	        	
	    	 ResultSet rs = sqlPreparedStatement.executeQuery(); 
	    	 if(!rs.next()) {
	        		System.out.println("The currently active class doesn't have category " + categoryName);
	        		return;
	         };
	         int categoryId = rs.getInt("category_id");
             
	    	 sql = "INSERT INTO\r\n "
	    			 +"assignments(name, description, point_value, category_id, class_id)\r\n"
	    			 +"VALUES(?, ?, ?, ?, ?);";
	    	 sqlPreparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        	 sqlPreparedStatement.setString(1, assignmentName);
        	 sqlPreparedStatement.setString(2, description);
        	 sqlPreparedStatement.setString(3, points);
        	 sqlPreparedStatement.setInt(4, categoryId);
        	 sqlPreparedStatement.setInt(5, active_class_id);
        	 //System.out.println(sqlPreparedStatement);
        	 int impactedRows = sqlPreparedStatement.executeUpdate(); 
        	 if (impactedRows == 0) {
                 throw new SQLException("Adding new assignment to currently active class failed, no rows affected.");
             }
        	 ResultSet generatedKey = sqlPreparedStatement.getGeneratedKeys();
        	 int assignment_id = -1;
        	 if (generatedKey.next()) {// point ahead of the first index of the list
        		 assignment_id = generatedKey.getInt(1);//index of the value in the generated key list (SQL is a 1 index based language)
        	 }
        	 System.out.println("Assignment ID | Name | Description | Point Value | Category ID | Category Name | Class ID");
        	 System.out.println("-".repeat(128));
        	 System.out.format("%10d%10s   %10s   %10s   %10d   %10s   %10d\n", 
        			 assignment_id, assignmentName, description, points, categoryId, categoryName, active_class_id);
        	 rs.close();
	    }catch(SQLException sqlException){
			System.out.println("Failed to add a new assignment with points to currently active class");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlPreparedStatement != null)
                	sqlPreparedStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}
		
	/** Category and Assignment Management(context of the currently active class): 
	 *  show-assignments
	 *  list the assignments with their points value, grouped by category
	 */
	public static void showAssignments() {
		Connection connection = null;
	    Statement sqlStatement = null;
		try {
	    	connection = Database.getDatabaseConnection();
	    	sqlStatement = connection.createStatement();
	    	String sql = "SELECT a.*, c.name as category_name \r\n"
	    			 +"FROM assignments a \r\n"
	    			 +"LEFT JOIN categories c \r\n"
	    			 +"ON  a.category_id = c.category_id \r\n"
	    			 +"WHERE class_id =" + active_class_id +";";
	    	ResultSet rs = sqlStatement.executeQuery(sql); 
	    	if(!rs.next()) {
        		System.out.println("No assignments in currently active class yet.");
        		return;
	    	};
        	System.out.println("Assignment ID | Name | Description | Point Value | Category ID | Category Name | Class ID");
        	System.out.println("-".repeat(128));
        	do{ 
        		int assignment_id = rs.getInt("assignment_id"); 
        		String name = rs.getString("name"); 
        		String description = rs.getString("description"); 
        		int points = rs.getInt("point_value"); 
        		int category_id = rs.getInt("category_id"); 
        		String category_name = rs.getString("category_name"); 
        		int class_id = rs.getInt("class_id"); 
        		 System.out.format("%10d%10s   %10s   %10s   %10d   %10s   %10d\n", 
            			 assignment_id, name, description, points, category_id, category_name, active_class_id);
        	}while(rs.next());
       	 	rs.close();
		}
		catch(SQLException sqlException){
			System.out.println("Failed to list all assignments in currently active class");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlStatement != null)
                    sqlStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}
	
	/** Student Management(context of the currently active class): 
	 *  add-student username 
	 *  enrolls an already-existing student in the current class
	 *  if the specified student does not exist, report an error
	 */
	public static void enrollExistingStudent(String myUsername) {
		Connection connection = null;
		PreparedStatement sqlPreparedStatement = null;
	
		try {
	    	 connection = Database.getDatabaseConnection();
	    	 //If the specified student does not exist, report an error
	    	 String sql = "select * \r\n "
	    			 +"from students \r\n"
	    			 +"where username= ?;";
	    	 sqlPreparedStatement = connection.prepareStatement(sql);
	         sqlPreparedStatement.setString(1, myUsername);
	    	 ResultSet rs = sqlPreparedStatement.executeQuery(); 
	    	 if(!rs.next()) {
	        		System.out.println("Error: student " + myUsername + " doesn't exist.");
	        		return;
	         };
	         String student_id = rs.getString("student_id");
             
	    	 sql = "INSERT INTO\r\n "
	    			 +"class_enrollment\r\n"
	    			 +"VALUES(?, ?);";
	    	 sqlPreparedStatement = connection.prepareStatement(sql);
	    	 sqlPreparedStatement.setString(1, student_id);
        	 sqlPreparedStatement.setInt(2, active_class_id);
        	 int impactedRows = sqlPreparedStatement.executeUpdate(); 
        	 if (impactedRows == 0) {
                 throw new SQLException("Enrolling existing student to current class failed, no rows affected.");
             }
        	 System.out.println("Student ID | Class ID");
        	 System.out.println("-".repeat(128));
        	 System.out.format("%10s%10d\n", student_id, active_class_id);
        	 //expand the assignment_grade table to contain records of all enrolled students
        	 sql = "INSERT INTO assignment_grade(student_id, assignment_id)( \r\n "
	    			 +"SELECT t.* \r\n"
	    			 +"FROM ("
		        	 	+"SELECT student_id, assignment_id \r\n"
		        	 	+"FROM assignments a \r\n"
		        	 	+"JOIN class_enrollment e  \r\n"
		        	 	+"ON a.class_id = e.class_id \r\n"
		        	 	+"WHERE e.class_id="+active_class_id+"\r\n"
		        	 +") t \r\n"
		        	 +"WHERE NOT EXISTS ( \r\n"
		        	 	+"SELECT student_id, assignment_id \r\n"
		        	 	+"FROM assignment_grade a \r\n"
		        	 	+"WHERE t.student_id = a.student_id AND t.assignment_id = a.assignment_id  \r\n"
		        	 +") \r\n"
		           +");";
        	 //System.out.println(sql);
        	 sqlPreparedStatement = connection.prepareStatement(sql);
        	 impactedRows= sqlPreparedStatement.executeUpdate();
        	 //System.out.println("TEST: " + impactedRows + " row was added into the assignment_grade"); 
        	 if (impactedRows == 0) {
                 throw new SQLException("All enrolled students have been added into the grading book for all assignments.");
             }
        	 rs.close();
	    }catch(SQLException sqlException){
			System.out.println("Failed to enroll an existing student to current class");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlPreparedStatement != null)
                	sqlPreparedStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}
		
	/** Student Management(context of the currently active class): 
	 *  add-student username studentid last first
	 *  adds a student and enrolls them in the current class
	 *  if the student already exists, enroll them in the class
	 *  if the name provided does not match their stored name, 
	 *  update the name but print a warning that the name is being changed
	 */
	public static void addAndEnrollStudent(String myUsername, String studentId, String last, String first) {
		Connection connection = null;
		PreparedStatement sqlPreparedStatement = null;
	
		try {
	    	 connection = Database.getDatabaseConnection();
	    	 String sql = "select * \r\n "
	    			 +"from students \r\n"
	    			 +"where student_id = ?;";
	    	 sqlPreparedStatement = connection.prepareStatement(sql);
	         sqlPreparedStatement.setString(1, studentId);
	    	 ResultSet rs = sqlPreparedStatement.executeQuery(); 
	    	 if(rs.next()) {
	    		    String username = rs.getString("username");
	    		    String name = rs.getString("name");
	    		    //if the username provided does not match their stored username
	    		    if(!username.equals(myUsername)) {
	    		    	 sql = "update students \r\n "
	    		    			 +"set username = ? \r\n"
	    		    			 +"where student_id = ?;";
	    		    	 sqlPreparedStatement = connection.prepareStatement(sql);
	    		         sqlPreparedStatement.setString(1, myUsername);
	    		         sqlPreparedStatement.setString(2, studentId);
	    		    	 int impactedRows = sqlPreparedStatement.executeUpdate(); 
	    	        	 if (impactedRows == 0) {
	    	                 throw new SQLException("Updating the username of the existing student failed, no rows affected.");
	    	             }
	    		    }
	    		    //if the full name provided does not match their stored full name
	    		    if(!name.equals(first + " " + last)) {
	    		    	 sql = "update students \r\n "
	    		    			 +"set name = ? \r\n"
	    		    			 +"where student_id = ?;";
	    		    	 sqlPreparedStatement = connection.prepareStatement(sql);
	    		         sqlPreparedStatement.setString(1, first + " " + last);
	    		         sqlPreparedStatement.setString(2, studentId);
	    		    	 int impactedRows = sqlPreparedStatement.executeUpdate(); 
	    	        	 if (impactedRows == 0) {
	    	                 throw new SQLException("Updating the full name of the existing student failed, no rows affected.");
	    	             }
	    		    }
	        		//enrollExistingStudent(myUsername);
	         }else {
	        	 // add a new student
	        	 sql = "INSERT INTO \r\n "
		    			 +"students \r\n"
		    			 +"VALUES(?,?,?);";
		    	 sqlPreparedStatement = connection.prepareStatement(sql);
		    	 sqlPreparedStatement.setString(1, studentId);
		    	 sqlPreparedStatement.setString(2, myUsername);
		    	 sqlPreparedStatement.setString(3, first + " " + last);
		    	 int impactedRows = sqlPreparedStatement.executeUpdate(); 
	        	 if (impactedRows == 0) {
	                 throw new SQLException("Adding a new student failed, no rows affected.");
	             }
	         }
	    	 enrollExistingStudent(myUsername); 
        	 rs.close();
	    }catch(SQLException sqlException){
			System.out.println("Failed to add and enroll a student to current class");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlPreparedStatement != null)
                	sqlPreparedStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}
	
	/** Student Management(context of the currently active class): 
	 *  show-students
	 *  show all students enrolled in the current class
	 */
	public static void showStudents() {
		Connection connection = null;
	    Statement sqlStatement = null;
		try {
	    	connection = Database.getDatabaseConnection();
	    	sqlStatement = connection.createStatement();
	    	String sql = "SELECT  s.* \r\n"
	    			 +"FROM students s \r\n"
	    			 +"JOIN class_enrollment r \r\n"
	    			 +"ON s.student_id = r.student_id \r\n"
	    			 +"WHERE class_id = "+ active_class_id + ";\r\n";
	    	ResultSet rs = sqlStatement.executeQuery(sql); 
	    	if(!rs.next()) {
        		System.out.println("No students enrolled in the currently-active class.");
        		return;
	    	};
        	System.out.println("Student ID | Username | Name");
        	System.out.println("-".repeat(128));
        	do { 
        		int student_id = rs.getInt("student_id"); 
        		String username = rs.getString("username"); 
        		String name = rs.getString("name"); 
        		System.out.format("%10d%10s    %10s\n", student_id, username, name);
        	}while(rs.next());
       	 	rs.close();
		}
		catch(SQLException sqlException){
			System.out.println("Failed to show all students in the current class");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlStatement != null)
                    sqlStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}
		
	/** Student Management(context of the currently active class): 
	 *  show-students string
	 *  show all students with ‘string’ in their name or username(case-insensitive)
	 */
	public static void showStudents(String targetName) {
		Connection connection = null;
		PreparedStatement sqlPreparedStatement = null;
		try {
	    	connection = Database.getDatabaseConnection();
	    	String sql = "SELECT  * \r\n"
	    			 +"FROM students \r\n"
	    			 +"WHERE username LIKE ? OR name LIKE ?;"; //WHERE BINARY username LIKE ? OR BINARY name LIKE ?
	    	sqlPreparedStatement = connection.prepareStatement(sql);
	        sqlPreparedStatement.setString(1, '%'+targetName+'%');
	        sqlPreparedStatement.setString(2, '%'+targetName+'%');	
	    	ResultSet rs = sqlPreparedStatement.executeQuery(); 

	    	if(!rs.next()) {
        		System.out.println("No student's name or username contains \'" + targetName + "\'.");
        		return;
	    	};
        	System.out.println("Student ID | Username | Name");
        	System.out.println("-".repeat(128));
        	do{ 
        		int student_id = rs.getInt("student_id"); 
        		String username = rs.getString("username"); 
        		String name = rs.getString("name"); 
        		System.out.format("%10d%10s     %10s\n", student_id, username, name);
        	}while(rs.next()); 
       	 	rs.close();
		}
		catch(SQLException sqlException){
			System.out.println("Failed to show all students with \'" + targetName +  "\' in their name or username(case-insensitive).");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlPreparedStatement != null)
                    sqlPreparedStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}
		
	/** Student Management(context of the currently active class): 
	 *  grade assignmentname username grade
	 *  assign the 'grade' for student with 'username' for 'assignmentname'
	 *  if the student already has a grade for that assignment, replace it
	 *  if the number of points exceeds the number of points configured for the assignment, 
	 *  print a warning(showing the number of points configured)
	 */
	public static void gradeAssignment(String assignmentname, String username, String newGrade) {
		Connection connection = null;
		PreparedStatement sqlPreparedStatement = null;
	
		try {
	    	 connection = Database.getDatabaseConnection();
	    	//get grade using assignmentname and username
	    	 String sql = "SELECT s.student_id, assignment_id, point_value \r\n"
	    			 +"FROM class_enrollment e \r\n"
	    			 +"JOIN students s \r\n"
	    			 +"ON e.student_id=s.student_id \r\n"
	    			 +"JOIN assignments a \r\n"
	    			 +"ON e.class_id=a.class_id \r\n"
	    			 +"WHERE e.class_id = " + active_class_id  + " AND username = ? AND a.name = ?;";
	    	 sqlPreparedStatement = connection.prepareStatement(sql);
		     sqlPreparedStatement.setString(1, username);
		     sqlPreparedStatement.setString(2, assignmentname);	
		     ResultSet rs = sqlPreparedStatement.executeQuery();   
		     if(!rs.next()) {
		    	 System.out.println("The assignment is not assigned to the student");
		    	 return;
		     }
		     int student_id = rs.getInt("student_id"); 
     		 int assignment_id = rs.getInt("assignment_id"); 
     		 int point_value = rs.getInt("point_value"); 
     		 if(Double.parseDouble(newGrade) > point_value) {
     			 System.out.println("The grade exceeds the number of points which is " + point_value + " configured for " + assignmentname);
     			 return;
     		 }
     	
     		 sql = "SELECT * \r\n "
	    			 +"FROM assignment_grade \r\n "
	    			 +"WHERE student_id = " + student_id + " AND assignment_id = " + assignment_id +";";
     		 Statement sqlStatement = null;
     		 sqlStatement = connection.createStatement();
     		 ResultSet rs2 = sqlStatement.executeQuery(sql);
     		 if(!rs2.next()) {
        		System.out.println("Student was not enrolled in the currently-active class");
        		return;
	    	 };
     		 String oldGrade = rs2.getString("grade");  
     		 //System.out.println(oldGrade);//78.00
     		 if(oldGrade == null) {System.out.println("The grade is added");}
     		 else{
     			if(Double.parseDouble(oldGrade)==Double.parseDouble(newGrade)) {
        			 System.out.println("No grade is changed");
        			 return;
     			}else {System.out.println("The grade is replaced");}
     		 }

	 		sql = "UPDATE assignment_grade \r\n "
	    			 +"SET grade  = ? \r\n "
	    			 +"WHERE student_id = " + student_id + " AND assignment_id = " + assignment_id + ";";
	 		
	 		sqlPreparedStatement = connection.prepareStatement(sql);
		    sqlPreparedStatement.setString(1, newGrade);
		    int impactedRows = sqlPreparedStatement.executeUpdate(); 
        	if (impactedRows == 0) {
                 throw new SQLException("Replacing the grade failed, no rows affected.");
            };
	     	 
        	System.out.println("Student ID | Assignment ID | Grade");
        	System.out.println("-".repeat(128));
        	System.out.format("%10d  %10d%10s\n", student_id, assignment_id, newGrade);     
        	rs.close();
	    }catch(SQLException sqlException){
			System.out.println("Failed to assign the grade for a student's assignment");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlPreparedStatement != null)
                	sqlPreparedStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}
	
	/** Grade Reporting(context of the currently active class): 
	 *  student-grades username
	 *  show student's current grades with subtotals for each category and the overall grade in the class
	 */
	public static void showStudentGrades(String username) {
		Connection connection = null;
		PreparedStatement sqlPreparedStatement = null;
		try {
	    	connection = Database.getDatabaseConnection();
	    	
	    	String sql = "CREATE OR REPLACE VIEW t1 AS \r\n"
	    			 +"SELECT username,\r\n"
	    			 +"if(c.name is null, '', c.name)as category, \r\n"
	    			 +"if(a.name is null and c.name!= '' , 'Subtotal', if(c.name is null, 'Grand Total', a.name)) as assignment, \r\n"
	    			 +"sum(point_value) as total_points,  \r\n"
	    			 +"sum(if(grade is not null and (a.name not in ('Subtotal','Grand Total')), point_value,'')) as attempted_total_points, \r\n"
	    			 +"sum(grade) as received_points \r\n"
	    			 +"FROM assignments a \r\n"
	    			 +"LEFT JOIN categories c \r\n"
	    			 +"ON a.category_id = c.category_id \r\n"
	    			 +"LEFT JOIN assignment_grade g \r\n"
	    			 +"ON a.assignment_id= g.assignment_id \r\n"
	    			 +"LEFT JOIN students s \r\n"
	    			 +"ON g.student_id = s.student_id \r\n"
	    			 +"WHERE a.class_id = " + active_class_id + "  AND username =? \r\n"
	    			 +"GROUP BY  username, c.name, a.name WITH ROLLUP;";
	    	//System.out.println(sql);
	    	sqlPreparedStatement = connection.prepareStatement(sql);
	        sqlPreparedStatement.setString(1, username);
	    	sqlPreparedStatement.executeUpdate();
	    	//System.out.println("VIEW is created");
	    	
	    	sql = "SELECT \r\n"
	    			 +"category, assignment, total_points, received_points, received_percent, weight, total_grade,\r\n"
	    			 +"attempted_total_points, attempted_received_percent, \r\n"
	    			 +"if(assignment=\'Subtotal\' , round(attempted_total_points/attempted_grand_total_points,2)*100, \'\'), \r\n"
	    			 +"if(assignment=\'Subtotal\' or assignment=\'Grand Total\', round((received_points/attempted_total_points *round(attempted_total_points/attempted_grand_total_points,2)*100),2),\'\') \r\n"
	    			 +"FROM ("
		        	 	+"SELECT t1.*, \r\n"
		    			    +"if(assignment=\'Subtotal\', round(received_points/total_points, 2), \'\') as received_percent, \r\n"
		    			    +"if(assignment=\'Subtotal\', round(received_points/attempted_total_points, 2), \'\') as attempted_received_percent, \r\n"
		    			    + "if(assignment=\'Subtotal\' ,weight,\'\') as weight, \r\n"
		    			    + "if(assignment=\'Subtotal\', round(received_points/total_points *weight, 2), if(assignment=\'Grand Total\',round(received_points/total_points*100,2),\'\')) as total_grade, \r\n"
		    			    + "(select distinct attempted_total_points from t1 where assignment=\'Grand Total\') as attempted_grand_total_points, \r\n"
		    			    + "(select distinct total_points from t1 where assignment=\'Grand Total\') as grand_total_points \r\n"
		    			+"FROM t1 \r\n"
				        +"LEFT JOIN ( \r\n"
					        +"SELECT class_category.category_id, categories.name, class_category.weight  \r\n"
			        	 	+"FROM  class_category  \r\n"
			        	 	+"JOIN categories   \r\n"
			        	 	+"ON class_category.category_id = categories.category_id  \r\n"
			        	 	+"WHERE class_id = "+active_class_id+" \r\n"
				        +") t2 \r\n"
				        +"ON t1.category = t2.name \r\n"
		            +") t \r\n"
		            +"WHERE t.username is not null;";
	    	//System.out.println(sql);
	    	sqlPreparedStatement = connection.prepareStatement(sql);
	        //sqlPreparedStatement.setString(1, username);
	    	ResultSet rs = sqlPreparedStatement.executeQuery(); 
	    	if(!rs.next()) {
        		System.out.println("Currently no grades for student \'" + username + "\' in current class.");
        		return;
	    	};
        	System.out.println("Category | Assignment | Total | Received | Received | Weight | Total | Attempted    | Attempted        | Attempted | Attempted |");
        	System.out.println("         |            | Points| Points   | Percent  |        | Grade | Total Points | Received Percent | Weight    | Grade     |");
        	System.out.println("-".repeat(128));
        	do{ 
        		String category = rs.getString("category"); 
        		String assignment = rs.getString("assignment"); 
        		String total_points = rs.getString("total_points"); 
        		String received_points = rs.getString("received_points"); 
        		String received_percent = rs.getString("received_percent"); 
        		String weight = rs.getString("weight"); 
        		String total_grade = rs.getString("total_grade"); 
        		String attempted_total_points = rs.getString("attempted_total_points"); 
        		String attempted_received_percent = rs.getString("attempted_received_percent"); 
        		String attempted_weight = rs.getString("if(assignment='Subtotal' , round(attempted_total_points/attempted_grand_total_points,2)*100, '')"); 
        		String attempted_grade = rs.getString("if(assignment='Subtotal' or assignment='Grand Total', round((received_points/attempted_total_points *round(attempted_total_points/attempted_grand_total_points,2)*100),2),'')"); 
        		System.out.format("%10s%10s%10s%10s%10s%10s%10s%10s      %10s      %10s      %10s\n", 
        				category,assignment, total_points,received_points,received_percent,weight,total_grade,
        				attempted_total_points, attempted_received_percent, attempted_weight, attempted_grade);
        	}while(rs.next()); 
       	 	rs.close();
		}
		catch(SQLException sqlException){
			System.out.println("Failed to show student\'s current grades in current class");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlPreparedStatement != null)
                    sqlPreparedStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}
	
	/** Grade Reporting(context of the currently active class): 
	 *  gradebook
	 *  show the current class’s gradebook: students (username, student ID, and name), along with their total grades in the class
	 */
	public static void showClassGradebook() {
		Connection connection = null;
		Statement sqlStatement = null;
		try {
	    	connection = Database.getDatabaseConnection();
	    	sqlStatement = connection.createStatement();
	    	String sql = "CREATE OR REPLACE VIEW t1 AS \r\n"
	    			 +"SELECT username, s.student_id, s.name, \r\n"
	    			 +"if(c.name is null, '', c.name)as category, \r\n"
	    			 +"if(a.name is null and c.name!= '' , 'Subtotal', if(c.name is null, 'Grand Total', a.name)) as assignment, \r\n"
	    			 +"sum(point_value) as total_points,  \r\n"
	    			 +"sum(if(grade is not null and (a.name not in ('Subtotal','Grand Total')), point_value,'')) as attempted_total_points, \r\n"
	    			 +"sum(grade) as received_points \r\n"
	    			 +"FROM assignments a \r\n"
	    			 +"LEFT JOIN categories c \r\n"
	    			 +"ON a.category_id = c.category_id \r\n"
	    			 +"LEFT JOIN assignment_grade g \r\n"
	    			 +"ON a.assignment_id= g.assignment_id \r\n"
	    			 +"LEFT JOIN students s \r\n"
	    			 +"ON g.student_id = s.student_id \r\n"
	    			 +"WHERE a.class_id = " + active_class_id + "\r\n"
	    			 +"GROUP BY  username, s.student_id, s.name, c.name, a.name WITH ROLLUP;";
	    	//System.out.println(sql);
	    	sqlStatement.executeUpdate(sql);
	    	//System.out.println("VIEW is created");
	    	
	    	sql = "SELECT \r\n"
	    			 +"username, student_id, name, \r\n"
	    			 +"total_grade, \r\n"
	    			 +"convert_to_letter_grade(total_grade), \r\n"
	    			 +"if(assignment='Subtotal' or assignment='Grand Total', round((received_points/attempted_total_points *round(attempted_total_points/attempted_grand_total_points,2)*100),2),''), \r\n"
	    			 +"convert_to_letter_grade(if(assignment='Subtotal' or assignment='Grand Total', round((received_points/attempted_total_points *round(attempted_total_points/attempted_grand_total_points,2)*100),2),'')) \r\n"
	    			 +"FROM ("
		        	 	+"SELECT t1.*, \r\n"
		    			    +"if(assignment='Subtotal', round(received_points/total_points, 2), '') as received_percent, \r\n"
		    			    +"if(assignment='Subtotal', round(received_points/attempted_total_points, 2), '') as attempted_received_percent, \r\n"
		    			    + "if(assignment='Subtotal' ,weight,'') as weight, \r\n"
		    			    + "if(assignment='Subtotal', round(received_points/total_points *weight, 2), if(assignment='Grand Total',round(received_points/total_points*100,2),'')) as total_grade, \r\n"
		    			    + "(select distinct attempted_total_points from t1 where assignment='Grand Total'and username is not null) as attempted_grand_total_points, \r\n"
		    			    + "(select distinct total_points from t1 where assignment='Grand Total'and username is not null) as grand_total_points \r\n"
	    			    +"FROM t1 \r\n"
				        +"LEFT JOIN ( \r\n"
					        +"SELECT class_category.category_id, categories.name, class_category.weight  \r\n"
			        	 	+"FROM  class_category  \r\n"
			        	 	+"JOIN categories   \r\n"
			        	 	+"ON class_category.category_id = categories.category_id  \r\n"
			        	 	+"WHERE class_id = "+active_class_id+" \r\n"
				        +") t2 \r\n"
				        +"ON t1.category = t2.name \r\n"
		            +") t \r\n"
		            +"WHERE assignment='Grand Total' and name is not null \r\n"
		            +"ORDER BY t.student_id;";
	    	//System.out.println(sql);
	    	ResultSet rs = sqlStatement.executeQuery(sql); 
	    	if(!rs.next()) {
        		System.out.println("No grades added in current class.");
        		return;
	    	};
        	System.out.println("Username  | Student |      Name    |  Total  |  Total        | Attempted |  Attempted     |");
        	System.out.println("          | ID      |              |  Grade  |  Grade Letter | Grade     |  Grade Letter  |");
        	System.out.println("-".repeat(128));
        	do{ 
        		String username = rs.getString("username"); 
        		String studentID = rs.getString("student_id"); 
        		String name = rs.getString("name"); 
        		String total_grade = rs.getString("total_grade"); 
        		String total_grade_letter = rs.getString("convert_to_letter_grade(total_grade)"); 
        		String attempted_grade = rs.getString("if(assignment='Subtotal' or assignment='Grand Total', round((received_points/attempted_total_points *round(attempted_total_points/attempted_grand_total_points,2)*100),2),'')"); 
        		String attempted_grade_letter = rs.getString("convert_to_letter_grade(if(assignment=\'Subtotal\' or assignment=\'Grand Total\', round((received_points/attempted_total_points *round(attempted_total_points/attempted_grand_total_points,2)*100),2),\'\'))"); 
        		System.out.format("%10s%10s%15s%10s%12s%12s%10s\n", username, studentID, name, total_grade, total_grade_letter, attempted_grade, attempted_grade_letter);
        	}while(rs.next()); 
       	 	rs.close();
		}
		catch(SQLException sqlException){
			System.out.println("Failed to show all student\'s grades in current class");
	        System.out.println(sqlException.getMessage());
		}finally {
            try {
                if (sqlStatement != null)
                    sqlStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
	}
	
	/** PARSE ARGUMENTS **/
	public static List<String> parseArguments(String command) {
        List<String> commandArguments = new ArrayList<String>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(command);
        while (m.find()) commandArguments.add(m.group(1).replace("\"", ""));
        return commandArguments;
    }
	
	/** MAIN METHOD **/
	public static void main(String[] args) {
		 System.out.println("Welcome to the Grading Management System");
	     System.out.println("-".repeat(128));
	     Scanner scan = new Scanner(System.in);
	     String command = "";
	     do {
		     System.out.print("Command: ");
	         command = scan.nextLine();
	         List<String> commandArguments = parseArguments(command);
	         //System.out.println(commandArguments.size()); 
	         command = commandArguments.get(0);
	         if (command.equals("help")) {
	                System.out.println("-".repeat(38) + "Help" + "-".repeat(38));
	                System.out.println("test connection \n\ttests the database connection");

	                System.out.println("list-classes \n\tlist all classes with the # of students enrolled in each class");
	                System.out.println("new-class <course_number> <term> <section_number> <description>\n\tcreate a class with course_number, term, section_number, description");
	                System.out.println("select-class <course_number>\n\tif there's only one section of course_number in the latest term, active that section as currently-activate class, otherwise the activation failed");
	                System.out.println("select-class <course_number> <term>\n\tif there's only one section of course_number in the term, active that section as currently-activate class, otherwise the activation failed");
	                System.out.println("select-class <course_number> <term> <section_number>\n\tactive the section_number of course_number in the term as currently-activate class");
	                System.out.println("show-class \n\tshow the currently-active class");

	                System.out.println("add-category <category_name> <weight> \n\tin the context of currently-active class, add a new grading category with category_name and weight to the current class");
	                System.out.println("show-categories \n\tin the context of currently-active class, list all grading categories with their weights of the current class");
	                System.out.println("add-assignment <assignment_name> <category_name> <description> <points>\n\tin the context of currently-active class, add a new assignment_name of category_name with description and points to the current class");
	                System.out.println("show-assignments \n\tin the context of currently-active class, list all assignments of the current class with their points value, grouped by category");
	                
	                System.out.println("add-student <username> \n\tin the context of currently-active class, enroll an existing student to the current class");
	                System.out.println("add-student <username> <studentid> <last_name> <first_name> \n\tin the context of currently-active class, if the studentid doesn't exist, enroll this new student username to the current class with studentid, last_name and first_name\n\t"
	                		+ "if the studentid exists, update the username, last_name and first_name if they are different from the existing ones");
	                System.out.println("show-students \n\tin the context of currently-active class, show all students enrolled in the current class");
	                System.out.println("show-students <string> \n\tin the context of currently-active class, show all enrolled students with ‘string’ in their name or username(case-insensitive)");
	                
	                System.out.println("grade <assignment_name> <username> <grade> \n\tin the context of currently-active class, assign or update the grade of assignment_name for username");
	                System.out.println("student-grades <username> \n\tin the context of currently-active class, show username's current grades with subtotals for each category and the overall grade in the current class");
	                System.out.println("gradebook \n\tin the context of currently-active class, show the current class’s gradebook: students (username, student ID, and name), along with their total grades in the class");
	                
	                System.out.println("help \n\tlists help information");
	                System.out.println("quit \n\tExits the program");
	         } else if (command.equals("test") && commandArguments.get(1).equals("connection")) {
	                Database.testConnection();
	         } else if (command.equals("list-classes")) {
	        	 listAllClassesWithNumStudents();
	         } else if (command.equals("new-class")) {
	        	 createNewClass(commandArguments.get(1), commandArguments.get(2), commandArguments.get(3), commandArguments.get(4));
	         } else if (command.equals("select-class")) {
	        	 //System.out.println(commandArguments.get(2));  //if (commandArguments.get(2) == null) onlySectionInLatestTerm();
	        	 if(commandArguments.size()==2) activateClass(commandArguments.get(1));
	        	 if(commandArguments.size()==3) activateClass(commandArguments.get(1), commandArguments.get(2));
	        	 if(commandArguments.size()==4) activateClass(commandArguments.get(1), commandArguments.get(2), commandArguments.get(3));
	     	 } else if(command.equals("show-class")) {
	     		 showClass();
	         } else if (command.equals("add-category")){
	        	 addCategoriesWithWeights(commandArguments.get(1),commandArguments.get(2));
	         } else if (command.equals("show-categories")){
	        	 showCategories();
	         } else if (command.equals("add-assignment")) {
	        	 addAssignmentsWithPoints(commandArguments.get(1),commandArguments.get(2),commandArguments.get(3),commandArguments.get(4));
	         } else if (command.equals("show-assignments")) { 
	        	 showAssignments(); 
	         } else if(command.equals("add-student")) {
	        	 if(commandArguments.size()==2) enrollExistingStudent(commandArguments.get(1));
	        	 if(commandArguments.size()==5) addAndEnrollStudent(commandArguments.get(1),commandArguments.get(2),commandArguments.get(3),commandArguments.get(4));
	         } else if(command.equals("show-students")) {
	        	 if(commandArguments.size()==1) showStudents(); 
	        	 if(commandArguments.size()==2) showStudents(commandArguments.get(1)); 
	         } else if(command.equals("grade")) {
	        	 gradeAssignment(commandArguments.get(1),commandArguments.get(2),commandArguments.get(3));
	         } else if(command.equals("student-grades")) {
	        	 showStudentGrades(commandArguments.get(1));	
	         } else if(command.equals("gradebook")) {
	        	 showClassGradebook();	
	         } else if (!(command.equals("quit") || command.equals("exit"))) {
	             System.out.println("Command not found. Enter 'help' for list of commands");
	         }
	         System.out.println("-".repeat(128));
	     }while(!(command.equals("quit") || command.equals("exit")));
	     System.out.println("Bye!");
	     scan.close();
	 }
} 
