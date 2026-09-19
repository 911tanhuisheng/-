package com.spingbootinit.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spingbootinit.common.result.Result;
import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.context.UserContext.IsAdminRoleString;
import com.spingbootinit.context.UserContext.UserContext;
import com.spingbootinit.model.entity.User;
import com.spingbootinit.service.UserService;
import com.spingbootinit.utils.MinioUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/file")
@Tag(name = "文件管理", description = "文件上传接口")
public class FileController {
    @Resource
    private MinioUtils minioUtils;
    @Resource
    private UserService userService;
    @Resource
    private IsAdminRoleString isAdminRole;

    @PostMapping("/upload")
    @Operation(summary = "文件上传", description = "上传用户头像")
    @ApiResponse(responseCode = "200", description = "上传成功")
    public Result<String> upload(@RequestParam("image") MultipartFile file) {

        // 从Token中获取用户ID
        Long currentUserId = UserContext.getCurrentUserId();

        // 上传文件
        String url = minioUtils.uploadImage(file);

        // 保存文件信息
        User user = new User();
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, currentUserId);
        user.setAvatar(url);
        userService.update(user, queryWrapper);
        return Result.success(url);
    }

    @PostMapping("/question-image")
    @Operation(summary = "上传图像题题图", description = "仅管理员可用，不会修改用户头像")
    public Result<String> uploadQuestionImage(@RequestParam("image") MultipartFile file) {
        Long currentUserId = UserContext.getCurrentUserId();
        User currentUser = currentUserId == null ? null : userService.getById(currentUserId);
        if (currentUser == null || !isAdminRole.isAdminRoleString(currentUser.getUserRole())) {
            throw new BusinessException(ResultCode.NO_AUTH_ERROR);
        }
        if (file == null || file.isEmpty() || file.getSize() > 10 * 1024 * 1024L) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "请上传不超过 10MB 的图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "文件必须是图片");
        }
        return Result.success(minioUtils.upload(file, "question-images/"));
    }

}
