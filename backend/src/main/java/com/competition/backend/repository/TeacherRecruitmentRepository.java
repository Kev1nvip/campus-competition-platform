package com.competition.backend.repository;

import com.competition.backend.entity.TeacherRecruitment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherRecruitmentRepository
        extends JpaRepository<TeacherRecruitment, Long> {

    List<TeacherRecruitment> findByTeacherId(Long teacherId);

    List<TeacherRecruitment> findByStatus(String status);
}