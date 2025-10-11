package com.kuncode.kuncodepicturebackend.constants;

public interface RedisKeyConstant {

    String PICTURE_LIST_VO_CACHE_KEY = "KunCode_Picture:listPictureVOByPage:%s";

    /* LOCK */

    String SPACE_LEVEL_COMMON_ONLY_LOCK_KEY = "KunCode_Picture:spaceLevelCommonOnly:%s";

}
