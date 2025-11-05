package com.kuncode.kuncodepicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceAddByAdminRequest implements Serializable {

    private String spaceName;


    private Long userId;


    private Integer spaceLevel;


    private Integer spaceType;


    private static final long serialVersionUID = 1L;
}
