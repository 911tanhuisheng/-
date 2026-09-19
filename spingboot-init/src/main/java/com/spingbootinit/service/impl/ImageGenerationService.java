package com.spingbootinit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.config.ImageGenerationProperties;
import com.spingbootinit.model.dto.vision.ImageGenerateRequest;
import com.spingbootinit.model.vo.vision.ImageGenerationVO;
import com.spingbootinit.utils.MinioUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {
    private static final String DEFAULT_NEGATIVE = "text, watermark, logo, blurry, deformed, duplicate objects, cropped";
    private final ImageGenerationProperties properties;
    private final ObjectMapper objectMapper;
    private final MinioUtils minioUtils;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5)).build();

    public ImageGenerationVO generate(ImageGenerateRequest request) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "本地 AI 图像生成服务未启用");
        }
        String baseUrl = trimSlash(properties.getBaseUrl());
        long seed = request.getSeed() == null || request.getSeed() < 0
                ? ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE) : request.getSeed();
        long started = System.currentTimeMillis();
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("client_id", UUID.randomUUID().toString());
            body.set("prompt", workflow(request, seed));
            HttpResponse<String> queued = sendJson(baseUrl + "/prompt", body, 20);
            ensureSuccess(queued, "ComfyUI 拒绝了生成任务");
            String promptId = objectMapper.readTree(queued.body()).path("prompt_id").asText();
            if (promptId.isBlank()) throw new IllegalStateException("ComfyUI 未返回 prompt_id");

            JsonNode image = waitForImage(baseUrl, promptId);
            String filename = image.path("filename").asText();
            String subfolder = image.path("subfolder").asText();
            String type = image.path("type").asText("output");
            String query = "filename=" + encode(filename) + "&subfolder=" + encode(subfolder) + "&type=" + encode(type);
            HttpRequest imageRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/view?" + query))
                    .timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<byte[]> downloaded = httpClient.send(imageRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (downloaded.statusCode() / 100 != 2 || downloaded.body().length == 0) {
                throw new IllegalStateException("ComfyUI 图片下载失败 HTTP " + downloaded.statusCode());
            }
            String url = minioUtils.uploadBytes(downloaded.body(), "image/png", ".png", "question-images/ai/");
            return ImageGenerationVO.builder().imageUrl(url).promptId(promptId).seed(seed)
                    .generationMs(System.currentTimeMillis() - started).build();
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "AI 图像生成已中断");
        } catch (Exception e) {
            log.warn("ComfyUI 图像生成失败", e);
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(),
                    "AI 图像生成失败：" + reason + "。请确认 ComfyUI 已在 8188 端口启动且配置的模型文件存在");
        }
    }

    private JsonNode waitForImage(String baseUrl, String promptId) throws Exception {
        long deadline = System.currentTimeMillis() + properties.getTimeoutSeconds() * 1000L;
        while (System.currentTimeMillis() < deadline) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/history/" + encode(promptId)))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 == 2) {
                JsonNode task = objectMapper.readTree(response.body()).path(promptId);
                JsonNode status = task.path("status");
                if (status.path("completed").asBoolean(false)) {
                    JsonNode images = task.path("outputs").path("9").path("images");
                    if (images.isArray() && !images.isEmpty()) return images.get(0);
                    throw new IllegalStateException("ComfyUI 任务完成但没有输出图片");
                }
                if (status.path("status_str").asText().equals("error")) {
                    throw new IllegalStateException("ComfyUI 执行失败，请查看 ComfyUI 控制台");
                }
            }
            Thread.sleep(1000);
        }
        throw new IllegalStateException("ComfyUI 生成超时（" + properties.getTimeoutSeconds() + " 秒）");
    }

    private ObjectNode workflow(ImageGenerateRequest r, long seed) {
        ObjectNode root = objectMapper.createObjectNode();
        node(root, "4", "CheckpointLoaderSimple").with("inputs").put("ckpt_name", properties.getCheckpoint());
        ObjectNode positive = node(root, "6", "CLIPTextEncode").with("inputs");
        positive.put("text", r.getPrompt().trim() + ", realistic photo, clear separated objects, full objects in frame, good lighting");
        ref(positive, "clip", "4", 1);
        ObjectNode negative = node(root, "7", "CLIPTextEncode").with("inputs");
        negative.put("text", r.getNegativePrompt() == null || r.getNegativePrompt().isBlank() ? DEFAULT_NEGATIVE : r.getNegativePrompt());
        ref(negative, "clip", "4", 1);
        ObjectNode latent = node(root, "5", "EmptyLatentImage").with("inputs");
        latent.put("width", r.getWidth()).put("height", r.getHeight()).put("batch_size", 1);
        ObjectNode sampler = node(root, "3", "KSampler").with("inputs");
        sampler.put("seed", seed).put("steps", r.getSteps()).put("cfg", 7.0)
                .put("sampler_name", "euler").put("scheduler", "normal").put("denoise", 1.0);
        ref(sampler, "model", "4", 0); ref(sampler, "positive", "6", 0);
        ref(sampler, "negative", "7", 0); ref(sampler, "latent_image", "5", 0);
        ObjectNode decode = node(root, "8", "VAEDecode").with("inputs");
        ref(decode, "samples", "3", 0); ref(decode, "vae", "4", 2);
        ObjectNode save = node(root, "9", "SaveImage").with("inputs");
        save.put("filename_prefix", "mayue_oj"); ref(save, "images", "8", 0);
        return root;
    }

    private ObjectNode node(ObjectNode root, String id, String classType) {
        ObjectNode node = root.putObject(id); node.put("class_type", classType); node.putObject("inputs"); return node;
    }
    private void ref(ObjectNode inputs, String name, String node, int output) {
        inputs.putArray(name).add(node).add(output);
    }
    private HttpResponse<String> sendJson(String url, JsonNode body, int timeout) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(timeout))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private void ensureSuccess(HttpResponse<String> response, String message) {
        if (response.statusCode() / 100 != 2) {
            String body = response.body();
            throw new IllegalStateException(message + " HTTP " + response.statusCode() + (body == null ? "" : ": " + body.substring(0, Math.min(300, body.length()))));
        }
    }
    private static String trimSlash(String value) { return value == null ? "" : value.trim().replaceAll("/+$", ""); }
    private static String encode(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); }
}
