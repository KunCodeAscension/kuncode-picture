package com.kuncode.kuncodepicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuncode.kuncodepicturebackend.annotation.AuthCheck;
import com.kuncode.kuncodepicturebackend.common.BaseResponse;
import com.kuncode.kuncodepicturebackend.common.DeleteRequest;
import com.kuncode.kuncodepicturebackend.common.ResultUtils;
import com.kuncode.kuncodepicturebackend.constants.UserConstant;
import com.kuncode.kuncodepicturebackend.exception.BusinessException;
import com.kuncode.kuncodepicturebackend.exception.ErrorCode;
import com.kuncode.kuncodepicturebackend.exception.ThrowUtils;
import com.kuncode.kuncodepicturebackend.manager.auth.SpaceUserAuthManager;
import com.kuncode.kuncodepicturebackend.model.dto.space.SpaceAddRequest;
import com.kuncode.kuncodepicturebackend.model.dto.space.SpaceEditRequest;
import com.kuncode.kuncodepicturebackend.model.dto.space.SpaceQueryRequest;
import com.kuncode.kuncodepicturebackend.model.dto.space.SpaceUpdateRequest;
import com.kuncode.kuncodepicturebackend.model.entity.Space;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.model.enums.SpaceLevelEnum;
import com.kuncode.kuncodepicturebackend.model.vo.space.SpaceLevel;
import com.kuncode.kuncodepicturebackend.model.vo.space.SpaceVO;
import com.kuncode.kuncodepicturebackend.service.ISpaceService;
import com.kuncode.kuncodepicturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
@RequiredArgsConstructor
@Slf4j
public class SpaceController {

    final ISpaceService spaceService;

    final IUserService userService;

    final SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 添加用户空间
     * @param spaceAddRequest 要添加的空间信息
     * @param request HttpServletRequest
     * @return 空间ID
     */
    @PostMapping("/add")
    @AuthCheck
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest,HttpServletRequest request) throws InterruptedException {
        ThrowUtils.throwIf(spaceAddRequest == null,ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long res = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(res);
    }

    /**
     * 删除空间
     * @param deleteRequest 要删除的空间信息
     * @param request HttpServletRequest
     * @return 返回是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        spaceService.checkSpaceAuth(oldSpace, loginUser);
        boolean result = spaceService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 空间信息更新
     * @param spaceUpdateRequest 要更新的空间信息
     * @param request HttpServletRequest
     * @return 返回是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 数据校验
        spaceService.validSpace(space,false);
        long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(space.getId());
    }

    /**
     * 根据Id获取空间信息
     * @param id 空间Id
     * @param request HttpServletRequest
     * @return 返回空间信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(space);
    }

    /**
     * 根据Id获取空间脱敏信息
     * @param id 空间Id
     * @param request HttpServletRequest
     * @return 返回空间脱敏信息
     */
    @GetMapping("/get/vo")
    @AuthCheck
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间信息
     * @param spaceQueryRequest 分页查询条件
     * @param request HttpServletRequest
     * @return Page<Space>
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        long current = spaceQueryRequest.getPage();
        long size = spaceQueryRequest.getPageSize();

        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                SpaceQueryRequest.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 分页获取空间脱敏信息
     * @param spaceQueryRequest 分页查询条件
     * @param request HttpServletRequest
     * @return Page<SpaceVO>
     */
    @PostMapping("/list/page/vo")
    @AuthCheck
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        int page = spaceQueryRequest.getPage();
        int pageSize = spaceQueryRequest.getPageSize();
        // size 不得超过20
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 20, ErrorCode.PARAMS_ERROR);
        Page<Space> spacePage = spaceService.page(new Page<>(page, pageSize), SpaceQueryRequest.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request));
    }

    /**
     * 编辑图片
     * @param spaceEditRequest 要编辑的空间信息
     * @param request HttpServletRequest
     * @return 是否编辑成功
     */
    @PostMapping("/edit")
    @AuthCheck
    public BaseResponse<Long> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        space.setEditTime(new Date());
        // 信息自动填充
        spaceService.fillSpaceBySpaceLevel(space);
        // 信息校验
        spaceService.validSpace(space,false);
        User loginUser = userService.getLoginUser(request);
        long id = spaceEditRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        spaceService.checkSpaceAuth(oldSpace, loginUser);
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(space.getId());
    }

    /**
     * 返回空间权益列表
     * @return List<SpaceLevel>
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }

}
