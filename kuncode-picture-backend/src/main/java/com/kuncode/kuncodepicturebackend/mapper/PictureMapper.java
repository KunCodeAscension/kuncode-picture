package com.kuncode.kuncodepicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuncode.kuncodepicturebackend.model.entity.Picture;
import org.apache.ibatis.annotations.Param;


public interface PictureMapper extends BaseMapper<Picture> {

    void updateByIdExcludeSpaceId(@Param("picture") Picture picture);

}




