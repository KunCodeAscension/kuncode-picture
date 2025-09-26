package com.kuncode.kuncodepicturebackend.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * <p>
 * 用户角色枚举类
 * </p>
 *
 * @author wyt
 * @since 2025-09-25
 */
@Getter
public enum UserRoleEnum {

    USER("用户","user"),

    ADMIN("管理员","admin");

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举类
     * @param value 枚举类 value 值
     * @return 返回枚举类
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.value.equals(value)) {
                return userRoleEnum;
            }
        }
        return null;
    }

}
