package com.spingbootinit.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.spingbootinit.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

// java example

/**
 * 自动填充
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("开始插入填充...");
        try {
            this.strictInsertFill(metaObject, "createTime", Date::new,Date.class);
            this.strictUpdateFill(metaObject, "updateTime", Date::new,Date.class);
        } catch (Exception e) {
            throw new BusinessException("插入失败！！");
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("开始更新填充...");
        this.strictUpdateFill(metaObject, "updateTime", Date::new,Date.class);
    }
}