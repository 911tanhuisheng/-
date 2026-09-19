package com.spingbootinit.utils;

import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtils {

    private final MinioProperties properties;

    public String uploadImage(MultipartFile file) {
        return upload(file, "images/");
    }

    public String uploadBytes(byte[] bytes, String contentType, String extension, String directory) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException("待上传图片为空");
        }
        validateConfiguration();
        String safeExtension = extension != null && extension.matches("\\.[a-zA-Z0-9]{1,8}")
                ? extension.toLowerCase() : ".png";
        String objectName = normalizeDirectory(directory) + generateFileName(safeExtension);
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(properties.getEndpoint())
                    .credentials(properties.getAccessKey(), properties.getSecretKey())
                    .build();
            ensureBucket(client);
            try (InputStream input = new ByteArrayInputStream(bytes)) {
                client.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucketName()).object(objectName)
                        .stream(input, bytes.length, -1)
                        .contentType(contentType == null ? "image/png" : contentType)
                        .build());
            }
            return publicObjectUrl(objectName);
        } catch (Exception e) {
            log.error("MinIO AI 生成图片上传失败, object={}", objectName, e);
            throw new BusinessException("AI 图片转存 MinIO 失败: " + e.getMessage());
        }
    }

    public String upload(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        validateConfiguration();
        String objectName = normalizeDirectory(directory) + generateFileName(extensionOf(file.getOriginalFilename()));
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(properties.getEndpoint())
                    .credentials(properties.getAccessKey(), properties.getSecretKey())
                    .build();
            ensureBucket(client);
            try (InputStream input = file.getInputStream()) {
                client.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucketName())
                        .object(objectName)
                        .stream(input, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            }
            return publicObjectUrl(objectName);
        } catch (Exception e) {
            log.error("MinIO 文件上传失败, object={}", objectName, e);
            throw new BusinessException("MinIO 文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 仅从当前 MinIO bucket 读取题图，避免判题服务成为任意 URL 下载器。
     */
    public byte[] downloadTrustedObject(String publicUrl, int maxBytes) {
        validateConfiguration();
        if (isBlank(publicUrl)) {
            throw new BusinessException("图像用例 URL 为空");
        }
        try {
            URI uri = URI.create(publicUrl.trim());
            String path = URLDecoder.decode(uri.getPath(), StandardCharsets.UTF_8);
            String bucketPrefix = "/" + properties.getBucketName() + "/";
            int bucketIndex = path.indexOf(bucketPrefix);
            if (bucketIndex < 0) {
                throw new BusinessException("图像用例必须来自本系统 MinIO bucket");
            }
            String objectName = path.substring(bucketIndex + bucketPrefix.length());
            if (objectName.isBlank() || objectName.contains("..")) {
                throw new BusinessException("图像用例对象路径无效");
            }
            MinioClient client = MinioClient.builder()
                    .endpoint(properties.getEndpoint())
                    .credentials(properties.getAccessKey(), properties.getSecretKey())
                    .build();
            try (InputStream input = client.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucketName()).object(objectName).build())) {
                byte[] bytes = input.readNBytes(maxBytes + 1);
                if (bytes.length > maxBytes) {
                    throw new BusinessException("单张题图不能超过 " + (maxBytes / 1024 / 1024) + "MB");
                }
                return bytes;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("MinIO 题图读取失败, url={}", publicUrl, e);
            throw new BusinessException("MinIO 题图读取失败: " + e.getMessage());
        }
    }

    public boolean isTrustedObjectUrl(String publicUrl) {
        if (isBlank(publicUrl) || isBlank(properties.getBucketName())) return false;
        try {
            URI uri = URI.create(publicUrl.trim());
            String path = URLDecoder.decode(uri.getPath(), StandardCharsets.UTF_8);
            String prefix = "/" + properties.getBucketName() + "/";
            int index = path.indexOf(prefix);
            if (index < 0 || path.substring(index + prefix.length()).isBlank()) return false;
            String endpointHost = URI.create(trimSlash(properties.getEndpoint())).getHost();
            String publicHost = isBlank(properties.getPublicUrl()) ? endpointHost
                    : URI.create(trimSlash(properties.getPublicUrl())).getHost();
            return uri.getHost() != null && (uri.getHost().equalsIgnoreCase(endpointHost)
                    || (publicHost != null && uri.getHost().equalsIgnoreCase(publicHost)));
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureBucket(MinioClient client) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.getBucketName()).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucketName()).build());
        }
        // 题图与头像需要被浏览器直接展示，仅开放对象读取，不开放列表和写入。
        String bucket = properties.getBucketName();
        String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\","
                + "\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],"
                + "\"Resource\":[\"arn:aws:s3:::" + bucket + "/*\"]}]}";
        client.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
    }

    private void validateConfiguration() {
        if (isBlank(properties.getEndpoint()) || isBlank(properties.getAccessKey())
                || isBlank(properties.getSecretKey()) || isBlank(properties.getBucketName())) {
            throw new BusinessException("MinIO 配置不完整，请检查 MINIO_ENDPOINT/ACCESS_KEY/SECRET_KEY/BUCKET");
        }
    }

    private String publicObjectUrl(String objectName) {
        String base = isBlank(properties.getPublicUrl())
                ? trimSlash(properties.getEndpoint()) + "/" + properties.getBucketName()
                : trimSlash(properties.getPublicUrl());
        return base + "/" + objectName;
    }

    private static String normalizeDirectory(String directory) {
        if (isBlank(directory)) return "";
        String normalized = directory.replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private static String extensionOf(String filename) {
        if (isBlank(filename) || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }

    private static String generateFileName(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return timestamp + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private static String trimSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
