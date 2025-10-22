package com.kuncode.kuncodepicturebackend.api.imagessearch;

import com.kuncode.kuncodepicturebackend.api.imagessearch.model.ImageSearchResult;
import com.kuncode.kuncodepicturebackend.api.imagessearch.sub.GetImageFirstUrlApi;
import com.kuncode.kuncodepicturebackend.api.imagessearch.sub.GetImageListApi;
import com.kuncode.kuncodepicturebackend.api.imagessearch.sub.GetImagePageUrlApi;

import java.util.List;

public class ImageSearchFacaed {

    /**
     * 以图搜图API
     * @param imgUrl 图片URL
     * @return List<ImageSearchResult>
     */
    public static List<ImageSearchResult> searchImage(String imgUrl){
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imgUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        return GetImageListApi.getImageList(imageFirstUrl);
    }

}
