package com.kuncode.kuncodepicturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value ="space")
@Data
public class Space implements Serializable {

    @ApiModelProperty(value = "空间ID")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty(value = "空间名")
    private String spaceName;

    @ApiModelProperty(value = "空间级别")
    private Integer spaceLevel;

    @ApiModelProperty(value = "空间最大存储量")
    private Long maxSize;

    @ApiModelProperty(value = "空间存储图片最大数量")
    private Long maxCount;

    @ApiModelProperty(value = "空间已使用的量")
    private Long totalSize;

    @ApiModelProperty(value = "空间已存储的图片数量")
    private Long totalCount;

    @ApiModelProperty(value = "创建空间的用户ID")
    private Long userId;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "空间类型 0代表个人空间 1代表团队空间")
    private Integer spaceType;

    @ApiModelProperty(value = "编辑时间")
    private Date editTime;

    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    @ApiModelProperty(value = "是否删除")
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}