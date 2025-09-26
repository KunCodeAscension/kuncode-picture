package com.kuncode.kuncodepicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuncode.kuncodepicturebackend.exception.BusinessException;
import com.kuncode.kuncodepicturebackend.exception.ErrorCode;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.mapper.UserMapper;
import com.kuncode.kuncodepicturebackend.model.enums.UserRoleEnum;
import com.kuncode.kuncodepicturebackend.model.vo.LoginUserVo;
import com.kuncode.kuncodepicturebackend.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

import static com.kuncode.kuncodepicturebackend.constants.UserConstant.USER_LOGIN_STATE;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author wyt
 * @since 2025-09-25
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Long UserRegister(String userAccount, String userPassword, String checkPassword) {
        // 校验两次输入的密码是否一致
        if(!checkPassword.equals(userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入密码不一致");
        }
        // 判断用户账号是否重复
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<User>().eq("userAccount", userAccount);
        Long count = this.baseMapper.selectCount(userQueryWrapper);
        if(count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号重复");
        }
        String encryptPassword = getEncryptPassword(userPassword);
        User user = User.builder()
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userName("默认用户名")
                .userRole(UserRoleEnum.USER.getValue())
                .build();
        boolean isSuccess = this.save(user);
        if(!isSuccess){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"注册失败，数据库出错");
        }
        return user.getId();
    }

    @Override
    public String getEncryptPassword(String userPassword) {

        final String SALT = "kuncode";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    @Override
    public LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //校验
        if(StrUtil.hasBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        String encryptPassword = getEncryptPassword(userPassword);
        QueryWrapper<User> queryWrapper =new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (user == null){
            log.info("user login failed,userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在或密码错误");
        }
        request.getSession().setAttribute(USER_LOGIN_STATE,user);
        return LoginUserVo.toLoginUserVo(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if(currentUser == null || currentUser.getId() == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long userId = currentUser.getId();
        currentUser = this.baseMapper.selectById(userId);
        if(currentUser == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        request.getSession().setAttribute(USER_LOGIN_STATE,currentUser);
        return currentUser;

    }

    @Override
    public Boolean userLogout(HttpServletRequest request) {
        Object currentUser = request.getSession().getAttribute(USER_LOGIN_STATE);
        if(currentUser == null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"未登录");
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return Boolean.TRUE;
    }
}
