package com.kuncode.kuncodepicturebackend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 3629102984553890425L;

    private Long id;
    
    private String userAccount;
    
    private String userName;
    
    private String userAvatar;

    private String userProfile;

    private String userRole;

    private Date createTime;

    private Date updateTime;

    public static UserVO toUserVO(User user) {
        if (user == null) {
            return null;
        }
        return BeanUtil.copyProperties(user, UserVO.class);
    }

    public static List<UserVO> toUserVOList(List<User> users) {
        if (CollectionUtil.isEmpty(users)) {
            return null;
        }
        return BeanUtil.copyToList(users, UserVO.class);
    }
}