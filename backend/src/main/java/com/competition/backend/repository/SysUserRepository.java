package com.competition.backend.repository;

import com.competition.backend.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    /**
     * 根据用户名查询用户
     */
    Optional<SysUser> findByUsername(String username);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查学号是否存在
     */
    boolean existsByStudentNo(String studentNo);
    // 统计某个角色的用户数量
    long countByRole(String role);
}