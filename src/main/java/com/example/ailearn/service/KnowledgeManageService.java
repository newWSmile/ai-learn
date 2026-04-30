package com.example.ailearn.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.example.ailearn.mapper.KnowledgeChunkMapper;
import com.example.ailearn.model.dto.rp.EmbeddingReloadResult;
import com.example.ailearn.model.dto.rq.KnowledgeCreateRequest;
import com.example.ailearn.model.dto.rq.KnowledgeUpdateRequest;
import com.example.ailearn.model.entity.KnowledgeChunkEntity;
import com.example.ailearn.model.vo.KnowledgeChunkVO;
import com.example.ailearn.rag.EmbeddingKnowledgeRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库管理服务
 *
 * 作用：
 * 1. 新增知识；
 * 2. 修改知识；
 * 3. 查询知识；
 * 4. 启用 / 禁用知识；
 * 5. 触发知识向量刷新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeManageService {

    private static final int DEFAULT_LIMIT = 50;

    private static final int MAX_LIMIT = 200;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KnowledgeChunkMapper knowledgeChunkMapper;

    private final EmbeddingKnowledgeRetriever embeddingKnowledgeRetriever;

    /**
     * 查询知识列表
     */
    public List<KnowledgeChunkVO> list(String category, Boolean enabled, String keyword, Integer limit) {
        Integer enabledValue = enabled == null ? null : (enabled ? 1 : 0);
        int safeLimit = normalizeLimit(limit);

        List<KnowledgeChunkEntity> entityList = knowledgeChunkMapper.selectList(
                category,
                enabledValue,
                keyword,
                safeLimit
        );

        if (CollUtil.isEmpty(entityList)) {
            return Collections.emptyList();
        }

        return entityList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询知识详情
     */
    public KnowledgeChunkVO detail(String id) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("知识片段ID不能为空");
        }

        KnowledgeChunkEntity entity = knowledgeChunkMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("知识片段不存在：" + id);
        }

        return convertToVO(entity);
    }

    /**
     * 新增知识片段
     */
    public KnowledgeChunkVO create(KnowledgeCreateRequest request) {
        validateCreateRequest(request);

        String id = StrUtil.isBlank(request.getId())
                ? "kc_" + IdUtil.getSnowflakeNextIdStr()
                : request.getId();

        KnowledgeChunkEntity exist = knowledgeChunkMapper.selectById(id);
        if (exist != null) {
            throw new IllegalArgumentException("知识片段ID已存在：" + id);
        }

        String now = now();

        KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
        entity.setId(id);
        entity.setTitle(request.getTitle());
        entity.setCategory(request.getCategory());
        entity.setSource(request.getSource());
        entity.setContent(request.getContent());
        entity.setKeywords(joinKeywords(request.getKeywords()));
        entity.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        entity.setEnabled(request.getEnabled() == null || request.getEnabled() ? 1 : 0);
        entity.setGmtCreate(now);
        entity.setGmtModified(now);

        knowledgeChunkMapper.insert(entity);

        log.info("新增知识片段成功，id={}，title={}，enabled={}",
                entity.getId(), entity.getTitle(), entity.getEnabled());

        reloadEmbeddingSafely();

        return detail(id);
    }

    /**
     * 修改知识片段
     */
    public KnowledgeChunkVO update(KnowledgeUpdateRequest request) {
        validateUpdateRequest(request);

        KnowledgeChunkEntity exist = knowledgeChunkMapper.selectById(request.getId());
        if (exist == null) {
            throw new IllegalArgumentException("知识片段不存在：" + request.getId());
        }

        KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
        entity.setId(request.getId());
        entity.setTitle(request.getTitle());
        entity.setCategory(request.getCategory());
        entity.setSource(request.getSource());
        entity.setContent(request.getContent());
        entity.setKeywords(joinKeywords(request.getKeywords()));
        entity.setPriority(request.getPriority() == null ? 0 : request.getPriority());
        entity.setEnabled(request.getEnabled() == null || request.getEnabled() ? 1 : 0);
        entity.setGmtModified(now());

        knowledgeChunkMapper.updateById(entity);

        log.info("修改知识片段成功，id={}，title={}，enabled={}",
                entity.getId(), entity.getTitle(), entity.getEnabled());

        reloadEmbeddingSafely();

        return detail(request.getId());
    }

    /**
     * 启用知识片段
     */
    public KnowledgeChunkVO enable(String id) {
        updateEnabled(id, true);
        return detail(id);
    }

    /**
     * 禁用知识片段
     */
    public KnowledgeChunkVO disable(String id) {
        updateEnabled(id, false);
        return detail(id);
    }

    /**
     * 手动刷新知识向量
     */
    public EmbeddingReloadResult reload() {
        EmbeddingReloadResult result = embeddingKnowledgeRetriever.reload();

        log.info("手动刷新知识库Embedding完成，knowledgeCount={}，loadedCount={}，reusedCount={}，generatedCount={}，failedCount={}",
                result.getKnowledgeCount(),
                result.getLoadedCount(),
                result.getReusedCount(),
                result.getGeneratedCount(),
                result.getFailedCount());

        return result;
    }

    /**
     * 修改启用状态
     */
    private void updateEnabled(String id, boolean enabled) {
        if (StrUtil.isBlank(id)) {
            throw new IllegalArgumentException("知识片段ID不能为空");
        }

        KnowledgeChunkEntity exist = knowledgeChunkMapper.selectById(id);
        if (exist == null) {
            throw new IllegalArgumentException("知识片段不存在：" + id);
        }

        knowledgeChunkMapper.updateEnabled(id, enabled ? 1 : 0, now());

        log.info("修改知识片段启用状态成功，id={}，enabled={}", id, enabled);

        reloadEmbeddingSafely();
    }

    /**
     * 安全刷新 Embedding 向量
     */
    private void reloadEmbeddingSafely() {
        try {
            EmbeddingReloadResult result = embeddingKnowledgeRetriever.reload();

            log.info("知识库Embedding向量刷新成功，knowledgeCount={}，loadedCount={}，reusedCount={}，generatedCount={}，failedCount={}",
                    result.getKnowledgeCount(),
                    result.getLoadedCount(),
                    result.getReusedCount(),
                    result.getGeneratedCount(),
                    result.getFailedCount());
        } catch (Exception e) {
            log.error("知识库Embedding向量刷新失败", e);
        }
    }

    private void validateCreateRequest(KnowledgeCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }

        validateCommonFields(request.getTitle(), request.getCategory(), request.getContent());
    }

    private void validateUpdateRequest(KnowledgeUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }

        if (StrUtil.isBlank(request.getId())) {
            throw new IllegalArgumentException("知识片段ID不能为空");
        }

        validateCommonFields(request.getTitle(), request.getCategory(), request.getContent());
    }

    /**
     * 校验知识片段核心字段
     */
    private void validateCommonFields(String title, String category, String content) {
        if (StrUtil.isBlank(title)) {
            throw new IllegalArgumentException("知识标题不能为空");
        }

        if (StrUtil.isBlank(category)) {
            throw new IllegalArgumentException("知识分类不能为空");
        }

        if (StrUtil.isBlank(content)) {
            throw new IllegalArgumentException("知识正文不能为空");
        }
    }

    /**
     * 关键词列表转字符串
     *
     * 说明：
     * Day 23 先使用英文逗号分隔，后续可升级为 JSON 数组。
     */
    private String joinKeywords(List<String> keywords) {
        if (CollUtil.isEmpty(keywords)) {
            return "";
        }

        return keywords.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
    }

    /**
     * 数据库实体转接口返回 VO
     */
    private KnowledgeChunkVO convertToVO(KnowledgeChunkEntity entity) {
        KnowledgeChunkVO vo = new KnowledgeChunkVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setCategory(entity.getCategory());
        vo.setSource(entity.getSource());
        vo.setContent(entity.getContent());
        vo.setKeywords(splitKeywords(entity.getKeywords()));
        vo.setPriority(entity.getPriority());
        vo.setEnabled(entity.getEnabled() != null && entity.getEnabled() == 1);
        vo.setGmtCreate(entity.getGmtCreate());
        vo.setGmtModified(entity.getGmtModified());
        return vo;
    }

    /**
     * 关键词字符串转列表
     */
    private List<String> splitKeywords(String keywords) {
        if (StrUtil.isBlank(keywords)) {
            return Collections.emptyList();
        }

        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
