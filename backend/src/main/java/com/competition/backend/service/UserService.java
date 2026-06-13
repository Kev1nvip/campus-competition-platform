package com.competition.backend.service;

import com.competition.backend.common.result.PageVO;
import com.competition.backend.dto.UserUpdateDTO;
import com.competition.backend.vo.TeacherProfileVO;
import com.competition.backend.vo.UserInfoVO;

public interface UserService {

    UserInfoVO getUserInfo(Long userId);

    void updateUserInfo(Long userId, UserUpdateDTO dto);

    TeacherProfileVO getTeacherProfile(Long teacherId);

    /**
     * 管理员分页查询用户列表
     */
    PageVO<UserInfoVO> listUsers(int page, int size, String keyword);
}