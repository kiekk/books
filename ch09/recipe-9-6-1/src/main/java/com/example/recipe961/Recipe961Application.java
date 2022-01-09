package com.example.recipe961;

import com.example.recipe961.course.Course;
import com.example.recipe961.course.CourseDao;
import com.example.recipe961.course.hibernate.HibernateCourseDao;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.GregorianCalendar;

@SpringBootApplication
public class Recipe961Application {

    public static void main(String[] args) {
        CourseDao courseDao = new HibernateCourseDao();

        Course course = new Course();
        course.setTitle("Core Spring");
        course.setBeginDate(new GregorianCalendar(2007, 8, 1).getTime());
        course.setEndDate(new GregorianCalendar(2007, 9, 1).getTime());
        course.setFee(1000);

        System.out.println("\nCourse before persisting");
        System.out.println(course);

        courseDao.store(course);

        System.out.println("\nCourse after persisting");
        System.out.println(course);

        Long courseId = course.getId();
        Course courseFromDb = courseDao.findById(courseId);

        System.out.println("\nCourse fresh from database");
        System.out.println(courseFromDb);

        courseDao.delete(courseId);

        System.exit(0);
    }

}
