package com.kuncode.kuncodepicturebackend.model.vo.space;

import com.kuncode.kuncodepicturebackend.model.entity.Space;
import com.kuncode.kuncodepicturebackend.model.vo.user.UserVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class SpaceVO implements Serializable {
    
    private Long id;

    
    private String spaceName;

    
    private Integer spaceLevel;

    
    private Long maxSize;

    
    private Long maxCount;

    
    private Long totalSize;

    
    private Long totalCount;

    
    private Long userId;

    
    private Date createTime;

    
    private Date editTime;

    
    private Date updateTime;


    private Integer spaceType;


    private UserVO user;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();

    private static final long serialVersionUID = 1L;

    
    public static Space voToObj(SpaceVO spaceVO) {
        if (spaceVO == null) {
            return null;
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceVO, space);
        return space;
    }

    
    public static SpaceVO objToVo(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);
        return spaceVO;
    }
}