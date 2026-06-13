package com.competition.backend.controller;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.result.Result;
import com.competition.backend.dto.AwardAuditDTO;
import com.competition.backend.dto.CreateAwardDTO;
import com.competition.backend.service.AwardService;
import com.competition.backend.util.SecurityUtil;
import com.competition.backend.vo.AwardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Tag(name = "获奖模块")
@RestController
@RequestMapping("/api/v1/award")
@RequiredArgsConstructor
public class AwardController {

    private final AwardService awardService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Operation(summary = "提交获奖记录")
    @PostMapping
    public Result<Void> createAward(@Valid @RequestBody CreateAwardDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        awardService.createAward(userId, dto);
        return Result.success();
    }

    @Operation(summary = "上传获奖证书图片")
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadCertificate(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_NULL, "文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase()
                : ".jpg";
        if (!ext.matches("\\.(jpg|jpeg|png)")) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "仅支持 jpg、jpeg、png 格式");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件大小不能超过 5MB");
        }

        try {
            // 按日期分目录：uploads/certificates/2026/06/
            String datePath = "certificates/" + LocalDate.now().getYear() + "/" + String.format("%02d", LocalDate.now().getMonthValue());
            Path dir = Paths.get(uploadPath, datePath);
            Files.createDirectories(dir);

            String filename = UUID.randomUUID() + ext;
            Path dest = dir.resolve(filename);
            file.transferTo(dest);

            String url = "/uploads/" + datePath + "/" + filename;
            return Result.success(Map.of("url", url));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "文件保存失败");
        }
    }

    @Operation(summary = "管理员审核获奖记录")
    @PostMapping("/audit")
    public Result<Void> auditAward(@Valid @RequestBody AwardAuditDTO dto) {
        Long adminId = SecurityUtil.getCurrentUserId();
        awardService.auditAward(adminId, dto);
        return Result.success();
    }

    @Operation(summary = "按竞赛ID查询获奖列表（管理员用）")
    @GetMapping("/competition/{competitionId}")
    public Result<Page<AwardVO>> getAwardsByCompetition(
            @PathVariable Long competitionId,
            Pageable pageable) {
        Page<AwardVO> page = awardService.getAwardsByCompetition(competitionId, pageable);
        return Result.success(page);
    }

    @Operation(summary = "查询我的获奖记录")
    @GetMapping("/my")
    public Result<Page<AwardVO>> getMyAwards(Pageable pageable) {
        Long userId = SecurityUtil.getCurrentUserId();
        Page<AwardVO> page = awardService.getMyAwards(userId, pageable);
        return Result.success(page);
    }
}