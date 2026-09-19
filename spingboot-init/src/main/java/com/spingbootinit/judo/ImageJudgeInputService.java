package com.spingbootinit.judo;

import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.model.entity.Question;
import com.spingbootinit.utils.MinioUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** 将可信 MinIO 题图转为沙箱请求内的图片数据。 */
@Service
@RequiredArgsConstructor
public class ImageJudgeInputService {

    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_TOTAL_BYTES = 30 * 1024 * 1024;
    private final MinioUtils minioUtils;

    public boolean isImageQuestion(Question question) {
        return question != null && question.getQuestionType() != null
                && question.getQuestionType().toUpperCase().startsWith("IMAGE_");
    }

    public List<String> prepare(Question question, List<String> imageUrls, String language) {
        if (!isImageQuestion(question)) {
            return null;
        }
        if (!"python".equalsIgnoreCase(language)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(),
                    "图像类题目目前仅支持 Python");
        }
        List<String> encoded = new ArrayList<>();
        int total = 0;
        for (String url : imageUrls) {
            byte[] bytes = minioUtils.downloadTrustedObject(url, MAX_IMAGE_BYTES);
            total += bytes.length;
            if (total > MAX_TOTAL_BYTES) {
                throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "单次判题题图总大小不能超过 30MB");
            }
            encoded.add(Base64.getEncoder().encodeToString(bytes));
        }
        return encoded;
    }
}
