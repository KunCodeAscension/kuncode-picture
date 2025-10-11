package com.kuncode.kuncodepicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kuncode.kuncodepicturebackend.model.dto.file.UploadPictureResult;
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
     * @param inputObject 图片文件 or url
     * @param pictureUploadRequest 上传图片的信息
     * @param loginUser 登录的用户
     * @return 返回图片脱敏信息
     */
    PictureVO uploadPicture(Object inputObject, PictureUploadRequest pictureUploadRequest, User loginUser);

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

    /**
     * 图片审核接口
     * @param pictureReviewRequest 图片审核信息
     * @param loginUser 登录的用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     * @param picture 图片类
     * @param user 登录用户
     */
    void fillReviewParams(Picture picture,User user);

    /**
     * 批量创建图片
     * @param pictureUploadByBatchRequest 批量创建的图片信息
     * @param loginUser 登录的管理员
     * @return 成功创建的图片数量
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest,User loginUser);

    /**
     * 删除图片
     * @param oldPicture 要删除的图片
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 图片权限校验
     * @param loginUser 登录的用户
     * @param picture 图片信息
     */
    void checkPictureAuth(User loginUser, Picture picture);

}
