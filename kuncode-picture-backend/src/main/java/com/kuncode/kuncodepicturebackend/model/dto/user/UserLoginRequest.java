package com.kuncode.kuncodepicturebackend.model.dto.user;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 2441246335401631186L;

    /**
     * 用户账号
     */
    @NotNull(message = "参数为空")
    @Size(min = 6,max = 16,message = "用户名长度必须在6-16之间）")
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$", message = "账号只能包含字母、数字、下划线和中文，不允许特殊字符")
    private String userAccount;

    /**
     * 用户密码
     */
    @NotNull(message = "参数为空")
    @Pattern(regexp = "^[A-Za-z0-9.!]+$", message = "密码只能包含大小写数字字母、.和!")
    @Size(min = 6, max = 255, message = "密码长度必须在6到255之间")
    private String userPassword;
}
