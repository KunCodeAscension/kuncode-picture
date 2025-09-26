package com.kuncode.kuncodepicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserAddRequest implements Serializable {

    private static final long serialVersionUID = -576972363832672466L;

    private String userName;

    
    private String userAccount;

    
    private String userAvatar;

    
    private String userProfile;

    
    private String userRole;

}
