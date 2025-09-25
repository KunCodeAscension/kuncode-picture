package com.kuncode.kuncodepicturebackend.service.impl;

import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.mapper.UserMapper;
import com.kuncode.kuncodepicturebackend.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author wyt
 * @since 2025-09-25
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}
