package com.kuncode.kuncodepicturebackend.model.vo.user;

import cn.hutool.core.bean.BeanUtil;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户登录脱敏数据
 * </p>
 *
 * @author wyt
 * @since 2025-09-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserVo implements Serializable {

    private static final long serialVersionUID = 103928114541947090L;

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "账号")
    private String userAccount;

    @ApiModelProperty(value = "用户昵称")
    private String userName;

    @ApiModelProperty(value = "用户头像")
    private String userAvatar;

    @ApiModelProperty(value = "用户简介")
    private String userProfile;

    @ApiModelProperty(value = "用户角色：user/admin")
    private String userRole;

    @ApiModelProperty(value = "编辑时间")
    private LocalDateTime editTime;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    public static LoginUserVo toLoginUserVo(User user) {
        if (user == null) {
            return null;
        }
        return BeanUtil.copyProperties(user, LoginUserVo.class);
    }
}
