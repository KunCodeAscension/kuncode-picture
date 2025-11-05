package com.kuncode.kuncodepicturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import com.kuncode.kuncodepicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.kuncode.kuncodepicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.kuncode.kuncodepicturebackend.manager.auth.model.SpaceUserRole;
import com.kuncode.kuncodepicturebackend.model.entity.Picture;
import com.kuncode.kuncodepicturebackend.model.entity.Space;
import com.kuncode.kuncodepicturebackend.model.entity.SpaceUser;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.model.enums.SpaceRoleEnum;
import com.kuncode.kuncodepicturebackend.model.enums.SpaceTypeEnum;
import com.kuncode.kuncodepicturebackend.service.ISpaceUserService;
import com.kuncode.kuncodepicturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SpaceUserAuthManager {

    private static final SpaceUserAuthConfig spaceUserAuthConfig;

    private final IUserService userService;

    private final ISpaceUserService spaceUserService;

    static{
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        spaceUserAuthConfig = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    public List<String> getPermissionsByRole(String spaceUserRoleName){
        if(spaceUserRoleName == null){
            return new ArrayList<>();
        }
        SpaceUserRole spaceUserRole = spaceUserAuthConfig.getRoles()
                .stream()
                .filter(role -> role.getKey().equals(spaceUserRoleName))
                .findFirst()
                .orElse(null);
        if(spaceUserRole == null){
            return new ArrayList<>();
        }
        return spaceUserRole.getPermissions();
    }


    /**
     * 获取权限列表
     */
    public List<String> getPermissionList(Space space, Picture picture, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        if (space == null && picture == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser) || picture.getUserId().equals(loginUser.getId())) {
                return ADMIN_PERMISSIONS;
            }
            return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }

}
