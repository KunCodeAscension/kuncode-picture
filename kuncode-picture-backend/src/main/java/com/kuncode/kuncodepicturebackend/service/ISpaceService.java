package com.kuncode.kuncodepicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kuncode.kuncodepicturebackend.model.dto.space.SpaceAddRequest;
import com.kuncode.kuncodepicturebackend.model.entity.Space;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.model.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;

public interface ISpaceService extends IService<Space> {

    /**
     * 校验空间数据
     * @param space 空间信息
     * @param add 是否是添加
     */
    void validSpace(Space space, boolean add);

    /**
     * 空间默认数据填充
     * @param space 空间信息
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 空间信息脱敏
     * @param space 空间信息
     * @param request HttpServletRequest
     * @return 脱敏信息
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 分页空间信息脱敏
     * @param spacePage 空间分页信息
     * @param request HttpServletRequest
     * @return 分页空间脱敏信息
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 添加空间
     * @param spaceAddRequest 添加的空间信息
     * @param loginUser 登陆的用户
     * @return 空间ID
     */
    Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    void checkSpaceAuth(Space space, User loginUser);

}
