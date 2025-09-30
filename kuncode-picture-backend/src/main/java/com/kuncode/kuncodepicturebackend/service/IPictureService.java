package com.kuncode.kuncodepicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kuncode.kuncodepicturebackend.model.dto.picture.*;
import com.kuncode.kuncodepicturebackend.model.entity.Picture;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

public interface IPictureService extends IService<Picture> {

    /**
     * 校验图片
     * @param picture 图片信息
     */
    void validPicture(Picture picture);

    /**
     * 上传图片
     * @param multipartFile 图片文件
     * @param pictureUploadRequest 上传图片的信息
     * @param loginUser 登录的用户
     * @return 返回图片脱敏信息
     */
    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 获取图片包装类（单条）
     * @param picture 图片信息
     * @param request HttpServletRequest
     * @return 返回图片脱敏信息
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片包装类（分页）
     * @param picturePage 分页查询信息
     * @param request HttpServletRequest
     * @return 返回分页图片脱敏信息
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);
}
