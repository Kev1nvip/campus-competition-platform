package com.competition.backend.service;

import com.competition.backend.dto.LoginDTO;
import com.competition.backend.dto.RegisterDTO;
import com.competition.backend.vo.LoginVO;

public interface AuthService {
    /**
     * 用户注册
     */
    void register(RegisterDTO registerDTO);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);
}