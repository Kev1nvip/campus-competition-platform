package com.competition.backend.repository;

import com.competition.backend.entity.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long>, JpaSpecificationExecutor<SysUser> {

    Optional<SysUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByStudentNo(String studentNo);

    long countByRole(String role);

    // 学生按学号/姓名搜索学生（邀请队友用）
    Page<SysUser> findByRoleAndStudentNoContaining(String role, String keyword, Pageable pageable);
    Page<SysUser> findByRoleAndRealNameContaining(String role, String keyword, Pageable pageable);
}