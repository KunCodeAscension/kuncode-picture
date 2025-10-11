package com.kuncode.kuncodepicturebackend.model.dto.picture;

import lombok.Data;
import java.io.Serializable;

/**
 * 图片上传请求
 *
 * @author 程序员鱼皮 <a href="https://www.codefather.cn">编程导航原创项目</a>
 */
@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片 id（用于修改）
     */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 图片名称参数
     */
    private String fileName;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private String tags;

    /**
     * 空间ID
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}