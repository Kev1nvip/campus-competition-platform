package com.competition.backend.service;

import com.competition.backend.common.result.PageVO;
import com.competition.backend.dto.IndividualSignupDTO;
import com.competition.backend.dto.SignupSubmitDTO;
import com.competition.backend.entity.IndividualSignup;
import com.competition.backend.entity.TeamSignup;

import java.util.Map;

public interface SignupService {
    // 个人赛
    Map<String, Object> signUpIndividual(IndividualSignupDTO dto);
    void submitIndividual(Long id, SignupSubmitDTO dto);
    PageVO<?> getMyIndividualSignups(int page, int size, String status);
    IndividualSignup getIndividualDetail(Long id);

    // 团队赛
    Map<String, Object> signUpTeam(Long teamId);
    void submitTeam(Long id);
    // 团队列表和详情建议根据实际关联表查询，此处暂略
}