package com.kuncode.kuncodepicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuncode.kuncodepicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.kuncode.kuncodepicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.kuncode.kuncodepicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kuncode.kuncodepicturebackend.model.vo.spaceuser.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface ISpaceUserService extends IService<SpaceUser> {

    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest SpaceUserAddRequest
     * @return Long
     */
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员
     *
     * @param spaceUser SpaceUser
     * @param add   boolean    是否为创建时检验
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间成员包装类（单条）
     *
     * @param spaceUser SpaceUser
     * @param request HttpServletRequest
     * @return SpaceUserVO
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员包装类（列表）
     *
     * @param spaceUserList List<SpaceUser>
     * @return List<SpaceUserVO>
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}