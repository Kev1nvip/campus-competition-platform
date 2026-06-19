package com.competition.backend.controller;

import com.competition.backend.common.constant.ErrorCode;
import com.competition.backend.common.constant.DocType;
import com.competition.backend.common.exception.BusinessException;
import com.competition.backend.common.result.PageVO;
import com.competition.backend.common.result.Result;
import com.competition.backend.dto.CompetitionSaveDTO;
import com.competition.backend.entity.Competition;
import com.competition.backend.entity.CompetitionDocument;
import com.competition.backend.repository.CompetitionDocumentRepository;
import com.competition.backend.repository.CompetitionRepository;
import com.competition.backend.service.CompetitionService;
import com.competition.backend.service.PdfChunkService;
import com.competition.backend.vo.CompetitionListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Tag(name = "竞赛模块")
@RestController
@RequestMapping("/api/v1/competitions")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerToken")
@Slf4j
public class CompetitionController {

    private final CompetitionService competitionService;
    private final CompetitionRepository competitionRepository;
    private final CompetitionDocumentRepository documentRepository;
    private final PdfChunkService pdfChunkService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Operation(summary = "发布竞赛")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Long> create(@Validated @RequestBody CompetitionSaveDTO saveDTO) {
        return Result.success(competitionService.createCompetition(saveDTO));
    }

    @Operation(summary = "竞赛列表")
    @GetMapping
    public Result<PageVO<CompetitionListVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return Result.success(competitionService.getCompetitionList(page, size, status, type, keyword));
    }

    @Operation(summary = "竞赛详情")
    @GetMapping("/{id}")
    public Result<Competition> detail(@PathVariable Long id) {
        return Result.success(competitionService.getCompetitionDetail(id));
    }

    @Operation(summary = "编辑竞赛")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody CompetitionSaveDTO saveDTO) {
        competitionService.updateCompetition(id, saveDTO);
        return Result.success();
    }

    @Operation(summary = "变更竞赛状态")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        competitionService.changeStatus(id, body.get("action"));
        return Result.success();
    }

    // ================================================
    // 竞赛文档管理
    // ================================================

    @Operation(summary = "上传竞赛文档（PDF）")
    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("docType") String docType) {
        // 校验竞赛存在
        competitionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "竞赛不存在"));

        // 校验文档类型
        try {
            DocType.valueOf(docType);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "无效的文档类型，可选: " +
                    Arrays.stream(DocType.values()).map(Enum::name).collect(Collectors.joining(", ")));
        }

        // 校验文件
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_NULL, "文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "仅支持 PDF 格式");
        }
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件大小不能超过 20MB");
        }

        try {
            // 保存文件：uploads/docs/{competitionId}/{docType}/{uuid}.pdf
            String datePath = "docs/" + id + "/" + docType;
            Path dir = Paths.get(uploadPath, datePath);
            Files.createDirectories(dir);

            String filename = UUID.randomUUID() + ".pdf";
            Path dest = dir.resolve(filename);
            file.transferTo(dest);

            String filePath = "/uploads/" + datePath + "/" + filename;

            // 写入数据库
            CompetitionDocument doc = CompetitionDocument.builder()
                    .competitionId(id)
                    .docType(docType)
                    .fileName(originalName)
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .build();
            CompetitionDocument saved = documentRepository.save(doc);

            // 异步解析 PDF → 章节分块 → 向量入库
            pdfChunkService.parseAndSave(file.getInputStream(), saved);

            Map<String, Object> result = new HashMap<>();
            result.put("id", saved.getId());
            result.put("filePath", filePath);
            result.put("fileName", originalName);
            result.put("docType", docType);
            return Result.success(result);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "文件保存失败");
        }
    }

    @Operation(summary = "获取竞赛文档列表")
    @GetMapping("/{id}/documents")
    public Result<List<Map<String, Object>>> listDocuments(@PathVariable Long id) {
        List<CompetitionDocument> docs = documentRepository.findByCompetitionId(id);
        List<Map<String, Object>> result = docs.stream().map(d -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("docType", d.getDocType());
            item.put("docTypeLabel", DocType.valueOf(d.getDocType()).getLabel());
            item.put("fileName", d.getFileName());
            item.put("filePath", d.getFilePath());
            item.put("fileSize", d.getFileSize());
            item.put("createdAt", d.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @Operation(summary = "下载竞赛文档")
    @GetMapping("/documents/{docId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long docId) {
        CompetitionDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));

        Path filePath = Paths.get(uploadPath).resolve(
                doc.getFilePath().replace("/uploads/", ""));
        if (!Files.exists(filePath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件已丢失");
        }

        Resource resource = new FileSystemResource(filePath);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }

    @Operation(summary = "删除竞赛文档")
    @DeleteMapping("/documents/{docId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> deleteDocument(@PathVariable Long docId) {
        CompetitionDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));

        // 删除向量库记录
        pdfChunkService.deleteByDocumentId(docId);

        // 删除物理文件
        Path filePath = Paths.get(uploadPath).resolve(
                doc.getFilePath().replace("/uploads/", ""));
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("删除文件失败, path={}", filePath);
        }

        documentRepository.delete(doc);
        return Result.success();
    }
}
