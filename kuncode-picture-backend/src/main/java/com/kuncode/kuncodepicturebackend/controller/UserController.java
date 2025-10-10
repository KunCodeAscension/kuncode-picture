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
import com.kuncode.kuncodepicturebackend.model.dto.user.*;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.model.vo.LoginUserVo;
import com.kuncode.kuncodepicturebackend.model.vo.UserVO;
import com.kuncode.kuncodepicturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@ResponseBody
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    final IUserService userService;

    final Long SUPER_ADMIN_ID = 1971464186297221121L;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 注册信息
     * @return 返回用户 Id
     */
    @PostMapping("register")
    public BaseResponse<Long> userRegister(@Valid @RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = userService.UserRegister(userRegisterRequest.getUserAccount(),userRegisterRequest.getUserPassword(),userRegisterRequest.getCheckPassword());
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录
     * @param userLoginRequest 登录信息
     * @param request HttpServletRequest
     * @return 返回用户脱敏信息
     */
    @PostMapping("login")
    public BaseResponse<LoginUserVo> userLogin(@Valid @RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVo loginUserVo = userService.userLogin(userLoginRequest.getUserAccount(), userLoginRequest.getUserPassword(), request);
        return ResultUtils.success(loginUserVo);
    }

    /**
     * 获取登录用户信息
     * @param request HttpServletRequest
     * @return 返回用户脱敏信息
     */
    @GetMapping("get/login")
    public BaseResponse<LoginUserVo> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(LoginUserVo.toLoginUserVo(user));
    }

    /**
     * 用户注销
     * @param request HttpServletRequest
     * @return 返回是否注销成功
     */
    @PostMapping("logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        Boolean success = userService.userLogout(request);
        return ResultUtils.success(success);
    }

    /**
     * 创建用户
     * @param userAddRequest 添加用户的信息
     * @return 用户 Id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 获取用户信息
     * @param id 用户id
     * @return 返回用户信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 获取用户脱敏信息
     * @param id 用户id
     * @return 返回用户脱敏信息
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(UserVO.toUserVO(user));
    }

    /**
     * 删除用户信息
     * @param deleteRequest 要删除的用户信息
     * @return 返回是否删除
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest,HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        Long userId = deleteRequest.getId();
        if(user.getId().equals(userId)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"禁止删除自己");
        }
        if(userId.equals(SUPER_ADMIN_ID)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"禁止删除超级管理员");
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户i西南西
     * @param userUpdateRequest 要更新的用户信息
     * @return 返回是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页查询用户信息
     * @param userQueryRequest 分页信息
     * @return 返回分页数据
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long page = userQueryRequest.getPage();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(page, pageSize),
                UserQueryRequest.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(page, pageSize, userPage.getTotal());
        List<UserVO> userVOList = UserVO.toUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

}
