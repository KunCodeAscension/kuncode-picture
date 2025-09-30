package com.kuncode.kuncodepicturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuncode.kuncodepicturebackend.annotation.AuthCheck;
import com.kuncode.kuncodepicturebackend.common.BaseResponse;
import com.kuncode.kuncodepicturebackend.common.DeleteRequest;
import com.kuncode.kuncodepicturebackend.common.ResultUtils;
import com.kuncode.kuncodepicturebackend.constants.UserConstant;
import com.kuncode.kuncodepicturebackend.exception.BusinessException;
import com.kuncode.kuncodepicturebackend.exception.ErrorCode;
import com.kuncode.kuncodepicturebackend.exception.ThrowUtils;
import com.kuncode.kuncodepicturebackend.model.dto.picture.PictureEditRequest;
import com.kuncode.kuncodepicturebackend.model.dto.picture.PictureQueryRequest;
import com.kuncode.kuncodepicturebackend.model.dto.picture.PictureUpdateRequest;
import com.kuncode.kuncodepicturebackend.model.dto.picture.PictureUploadRequest;
import com.kuncode.kuncodepicturebackend.model.entity.Picture;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.model.vo.PictureVO;
import com.kuncode.kuncodepicturebackend.service.IPictureService;
import com.kuncode.kuncodepicturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@RestController
@RequestMapping("/picture")
@RequiredArgsConstructor
public class PictureController {

    final IUserService userService;

    final IPictureService pictureService;

    /**
     * 管理员上传图片
     * @param multipartFile 上传的文件
     * @param pictureUploadRequest 上传的文件信息
     * @param request HttpServletRequest HttpServletRequest
     * @return
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     * @param deleteRequest 要删除的图片信息
     * @param request HttpServletRequest
     * @return 返回是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();

        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 图片信息更新
     * @param pictureUpdateRequest 要更新的图片信息
     * @return 返回是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);

        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));

        pictureService.validPicture(picture);

        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据Id获取体图片信息
     * @param id 图片Id
     * @param request HttpServletRequest
     * @return 返回图片信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(picture);
    }

    /**
     * 根据Id获取图片脱敏信息
     * @param id 图片Id
     * @param request HttpServletRequest
     * @return 返回图片脱敏信息
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /**
     * 分页获取图片信息
     * @param pictureQueryRequest 分页查询条件
     * @return Page<Picture>
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getPage();
        long size = pictureQueryRequest.getPageSize();

        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                PictureQueryRequest.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片脱敏信息
     * @param pictureQueryRequest 分页查询条件
     * @param request HttpServletRequest
     * @return Page<PictureVO>
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getPage();
        long size = pictureQueryRequest.getPageSize();

        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                PictureQueryRequest.getQueryWrapper(pictureQueryRequest));

        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 编辑图片
     * @param pictureEditRequest 要编辑的图片信息
     * @param request HttpServletRequest
     * @return 是否编辑成功
     */
    @PostMapping("/edit")
    @AuthCheck
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);

        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));

        picture.setEditTime(new Date());

        pictureService.validPicture(picture);
        User loginUser = userService.getLoginUser(request);

        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


}
