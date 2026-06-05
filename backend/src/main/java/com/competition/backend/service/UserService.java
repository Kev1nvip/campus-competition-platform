package com.competition.backend.service;

import com.competition.backend.dto.UserUpdateDTO;
import com.competition.backend.vo.TeacherProfileVO;
import com.competition.backend.vo.UserInfoVO;

public interface UserService {

    /**
     * 获取当前登录用户的个人信息
     *
     * @param userId 当前登录用户ID（从Token解析）
     * @return 用户信息VO
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 更新当前登录用户的个人信息
     *
     * @param userId 当前登录用户ID（从Token解析）
     * @param dto    更新的字段
     */
    void updateUserInfo(Long userId, UserUpdateDTO dto);

    /**
     * 获取教师主页公开信息
     *
     * @param teacherId 教师用户ID
     * @return 教师主页VO
     */
    TeacherProfileVO getTeacherProfile(Long teacherId);
}