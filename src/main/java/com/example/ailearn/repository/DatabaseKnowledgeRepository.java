package com.example.ailearn.repository;

import cn.hutool.core.util.StrUtil;
import com.example.ailearn.mapper.KnowledgeChunkMapper;
import com.example.ailearn.model.entity.KnowledgeChunkEntity;
import com.example.ailearn.rag.KnowledgeChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据库知识库 Repository
 *
 * 作用：
 * 1. 从 knowledge_chunk 表查询启用知识；
 * 2. 转换成 RAG 检索层使用的 KnowledgeChunk；
 * 3. 屏蔽数据库实体和业务模型之间的差异。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DatabaseKnowledgeRepository {

    private final KnowledgeChunkMapper knowledgeChunkMapper;

    /**
     * 查询启用中的知识片段
     */
    public List<KnowledgeChunk> listEnabledKnowledgeChunks() {
        List<KnowledgeChunkEntity> entityList = knowledgeChunkMapper.selectEnabledList();

        if (entityList == null || entityList.isEmpty()) {
            log.warn("数据库中暂无启用的知识片段");
            return new ArrayList<>();
        }

        List<KnowledgeChunk> chunks = entityList.stream()
                .map(this::convertToKnowledgeChunk)
                .collect(Collectors.toList());

        log.info("数据库知识片段加载完成，count={}", chunks.size());

        return chunks;
    }

    /**
     * 数据库实体转换为检索用 KnowledgeChunk
     */
    private KnowledgeChunk convertToKnowledgeChunk(KnowledgeChunkEntity entity) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(entity.getId());
        chunk.setTitle(entity.getTitle());
        chunk.setCategory(entity.getCategory());
        chunk.setSource(entity.getSource());
        chunk.setContent(entity.getContent());
        chunk.setPriority(entity.getPriority() == null ? 0 : entity.getPriority());
        chunk.setKeywords(parseKeywords(entity.getKeywords()));
        return chunk;
    }

    /**
     * 解析关键词
     *
     * 说明：
     * 当前阶段 keywords 使用英文逗号分隔。
     * 后续 Day 23 / Day 24 可以考虑改成 JSON 数组存储。
     */
    private List<String> parseKeywords(String keywords) {
        if (StrUtil.isBlank(keywords)) {
            return new ArrayList<>();
        }

        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }
}