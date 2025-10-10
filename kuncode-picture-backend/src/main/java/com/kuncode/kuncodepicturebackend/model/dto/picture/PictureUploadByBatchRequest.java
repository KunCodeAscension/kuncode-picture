package com.kuncode.kuncodepicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureUploadByBatchRequest implements Serializable {

    private static final long serialVersionUID = 7176884705895203714L;

    /**
     * 关键词
     */
    private String searchText;

    /**
     * 默认爬取条数
     */
    private Integer count = 10;

    /**
     * 图片名称前缀
     */
    private String namePrefix;

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
    private List<String> tags;

}