package com.example.recipe981.course.hibernate;

import com.example.recipe981.course.Course;
import com.example.recipe981.course.CourseDao;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 하이버네이트는 예외가 발생하면 HibernateException 을 발생
// 일관된 예외 처리를 하려면 DataAccessException 으로 변환해야 하는데,
// @Repository 가 가능하게 해줍니다.
@Repository
public class HibernateCourseDao implements CourseDao {

    private final SessionFactory sessionFactory;

    public HibernateCourseDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Transactional
    public Course store(Course course) {
        Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(course);
        return course;
    }

    @Transactional
    public void delete(Long courseId) {
        Session session = sessionFactory.getCurrentSession();
        Course course = session.get(Course.class, courseId);
        session.delete(course);
    }

    @Transactional(readOnly = true)
    public Course findById(Long courseId) {
        Session session = sessionFactory.getCurrentSession();
        return session.get(Course.class, courseId);
    }

    @Transactional(readOnly = true)
    public List<Course> findAll() {
        Session session = sessionFactory.getCurrentSession();
        return session.createQuery("from Course", Course.class).list();
    }

}
