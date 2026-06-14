package com.competition.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rag_document")
public class RagDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文档名称，如蓝桥杯介绍
     */
    @Column(name = "doc_name", nullable = false, length = 128)
    private String docName;

    /**
     * 对应竞赛名称
     */
    @Column(name = "competition_name", nullable = false, length = 128)
    private String competitionName;

    /**
     * 分块序号，同一文档从0开始递增
     */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    /**
     * 分块后的原始文本内容
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 向量表示，维度1024，对应BGE-M3模型
     * 使用 columnDefinition 映射 pgvector 类型
     */
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    private String embedding;

    /**
     * 竞赛类别标签，如算法/数学/创新
     */
    @Column(name = "category", length = 32)
    private String category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}