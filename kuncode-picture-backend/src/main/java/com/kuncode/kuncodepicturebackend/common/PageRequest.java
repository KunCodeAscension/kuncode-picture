package com.kuncode.kuncodepicturebackend.common;

import lombok.Data;

@Data
public class PageRequest {

    /**
     * 页码
     */
    private int page = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认顺序）
     */
    private String sortOrder = "descend";

}
