package com.kovan.controller;

import com.kovan.entity.Course;
import com.kovan.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

        @PostMapping()
        public ResponseEntity<Course> addCourse(@RequestBody Course course) {
        courseService.addCourse(course);
        return new ResponseEntity<>(course, HttpStatus.CREATED);
        }

        @GetMapping()
        public ResponseEntity<List<Course>> getAllCourses() {
            return new ResponseEntity<>(courseService.getAllCourses(), HttpStatus.OK);
        }

        @GetMapping("/{id}")
        public ResponseEntity<Course> getCourseById(@PathVariable int id) {
            Optional<Course> course = courseService.getCourseById(id);
            return course.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }

        @PutMapping("/{id}")
        public ResponseEntity<Course> updateCourse(@PathVariable int id, @RequestBody Course newCourse) {
            boolean updated = courseService.updateCourse(id, newCourse);
            if (updated) {
                return new ResponseEntity<>(newCourse, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteCourse(@PathVariable int id) {
            boolean deleted = courseService.deleteCourse(id);
            if (deleted) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        }
}

