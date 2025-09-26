package com.kuncode.kuncodepicturebackend.service;

import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kuncode.kuncodepicturebackend.model.vo.LoginUserVo;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 用户 服务类
 * </p>
 *
 * @author wyt
 * @since 2025-09-25
 */
public interface IUserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount 账号
     * @param userPassword 密码
     * @param checkPassword 确认密码
     * @return 返回用户 id
     */
    Long UserRegister(String userAccount,String userPassword,String checkPassword);

    /**
     * 密码加盐
     * @param userPassword 加盐前密码
     * @return 加盐后密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param request HttpServletRequest
     * @return 返回用户脱敏后数据
     */
    LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录的用户
     * @param request HttpServletRequest
     * @return 返回用户信息
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     * @param request HttpServletRequest
     * @return 返回是否注销成功
     */
    Boolean userLogout(HttpServletRequest request);

}
