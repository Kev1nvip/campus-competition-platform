package com.competition.backend.service;

import com.competition.backend.common.result.PageVO;
import com.competition.backend.dto.UserUpdateDTO;
import com.competition.backend.vo.TeacherProfileVO;
import com.competition.backend.vo.UserInfoVO;

public interface UserService {

    UserInfoVO getUserInfo(Long userId);

    void updateUserInfo(Long userId, UserUpdateDTO dto);

    TeacherProfileVO getTeacherProfile(Long teacherId);

    PageVO<UserInfoVO> listUsers(int page, int size, String keyword);

    /**
     * 学生选老师时使用：分页查询所有教师，支持姓名/院系关键字搜索
     */
    PageVO<TeacherProfileVO> listTeachers(int page, int size, String keyword);

    /** 管理员禁用/启用用户 */
    void toggleUserStatus(Long userId, String status);
}