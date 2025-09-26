package com.kuncode.kuncodepicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = -5792541976527203228L;

    private Long id;

    
    private String userName;

    
    private String userAvatar;

    
    private String userProfile;

    
    private String userRole;

}
