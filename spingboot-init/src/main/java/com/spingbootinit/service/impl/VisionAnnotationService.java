package com.spingbootinit.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.config.VisionAiProperties;
import com.spingbootinit.model.dto.vision.VisionAnnotateRequest;
import com.spingbootinit.model.vo.vision.VisionAnnotationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisionAnnotationService {

    private final VisionAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            // Uvicorn 默认使用 HTTP/1.1；禁止 JDK 客户端尝试 h2c 升级。
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public VisionAnnotationVO annotate(VisionAnnotateRequest request) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "YOLO 图像识别服务未启用");
        }
        String baseUrl = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().replaceAll("/+$", "");
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("imageUrl", request.getImageUrl());
            payload.put("confidence", request.getConfidence());
            payload.put("iou", request.getIou());
            payload.put("taskType", request.getTaskType());
            payload.put("modelKey", request.getModelKey());
            String jsonBody = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/detect"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("YOLO 自动标注返回错误, status={}, url={}, body={}",
                        response.statusCode(), request.getImageUrl(), response.body());
                throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(),
                        "YOLO 识别失败：" + extractErrorMessage(response.body(), String.valueOf(response.statusCode())));
            }
            VisionAnnotationVO result = objectMapper.readValue(response.body(), VisionAnnotationVO.class);
            if (result == null) {
                throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "YOLO 服务未返回有效识别结果");
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "YOLO 识别请求已中断");
        } catch (Exception e) {
            log.warn("YOLO 自动标注请求失败, url={}", request.getImageUrl(), e);
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(),
                    "YOLO 识别服务不可用：" + reason);
        }
    }

    private String extractErrorMessage(String responseBody, String fallback) {
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode detail = objectMapper.readTree(responseBody).get("detail");
                if (detail != null && detail.isTextual()) {
                    return detail.asText();
                }
                if (detail != null && detail.isArray() && !detail.isEmpty()) {
                    JsonNode message = detail.get(0).get("msg");
                    if (message != null && message.isTextual()) {
                        return message.asText();
                    }
                }
            } catch (Exception ignored) {
                // 非 JSON 错误体使用下方的纯文本回退。
            }
            return responseBody.length() > 300 ? responseBody.substring(0, 300) : responseBody;
        }
        return fallback == null || fallback.isBlank() ? "未知错误" : fallback;
    }
}
