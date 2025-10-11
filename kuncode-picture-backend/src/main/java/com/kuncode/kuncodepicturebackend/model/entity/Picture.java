package com.kuncode.kuncodepicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.util.Date;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 图片
 * @TableName picture
 */
@TableName(value ="picture")
@Data
public class Picture implements Serializable {

    @ApiModelProperty(value = "id")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty(value = "图片访问路径")
    private String url;

    @ApiModelProperty(value = "图片名")
    private String name;

    @ApiModelProperty(value = "图片简介")
    private String introduction;

    @ApiModelProperty(value = "图片主题")
    private String category;

    @ApiModelProperty(value = "图片标签")
    private String tags;

    @ApiModelProperty(value = "图片尺寸")
    private Long picSize;

    @ApiModelProperty(value = "图片宽度")
    private Integer picWidth;

    @ApiModelProperty(value = "图片高度")
    private Integer picHeight;

    @ApiModelProperty(value = "图片宽高比")
    private Double picScale;

    @ApiModelProperty(value = "图片格式")
    private String picFormat;

    @ApiModelProperty(value = "用户Id")
    private Long userId;

    @ApiModelProperty(value = "缩略图")
    private String thumbnailUrl;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "编辑时间")
    private Date editTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    @ApiModelProperty(value = "审核状态")
    private Integer reviewStatus;

    @ApiModelProperty(value = "审核信息")
    private String reviewMessage;

    @ApiModelProperty(value = "审核通过人Id")
    private Long reviewerId;

    @ApiModelProperty(value = "审核时间")
    private Date reviewTime;

    @ApiModelProperty(value = "图片所属空间ID，如果为空说明在默认空间")
    private Long spaceId;

    @TableLogic
    @ApiModelProperty(value = "是否删除")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}