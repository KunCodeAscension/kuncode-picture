package com.kuncode.kuncodepicturebackend.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuncode.kuncodepicturebackend.exception.BusinessException;
import com.kuncode.kuncodepicturebackend.exception.ErrorCode;
import com.kuncode.kuncodepicturebackend.exception.ThrowUtils;
import com.kuncode.kuncodepicturebackend.manager.upload.FilePictureUpload;
import com.kuncode.kuncodepicturebackend.manager.upload.UrlPictureUpload;
import com.kuncode.kuncodepicturebackend.mapper.PictureMapper;
import com.kuncode.kuncodepicturebackend.model.dto.file.UploadPictureResult;
import com.kuncode.kuncodepicturebackend.model.dto.picture.*;
import com.kuncode.kuncodepicturebackend.model.entity.Picture;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.model.enums.PictureReviewStatusEnum;
import com.kuncode.kuncodepicturebackend.model.vo.PictureVO;
import com.kuncode.kuncodepicturebackend.model.vo.UserVO;
import com.kuncode.kuncodepicturebackend.service.IPictureService;
import com.kuncode.kuncodepicturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements IPictureService {

    final FilePictureUpload filePictureUpload;

    final UrlPictureUpload  urlPictureUpload;

    final IUserService userService;

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);

        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();

        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public PictureVO uploadPicture(Object inputObject, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }

        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            // 图片不存在
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR);
            // 当前图片不是自己的 当前角色也不是管理员
            if(!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 图片已经是待审核状态
            if(oldPicture.getReviewStatus().equals(PictureReviewStatusEnum.REVIEWING.getValue())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"图片正在审核请勿修改");
            }
        }
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult;
        if(inputObject instanceof MultipartFile) {
            uploadPictureResult = filePictureUpload.uploadPicture(inputObject, uploadPathPrefix);
        }else {
            uploadPictureResult = urlPictureUpload.uploadPicture(inputObject, uploadPathPrefix);
        }
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        String picName = uploadPictureResult.getPicName();
        if(pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getFileName())) {
            picName = pictureUploadRequest.getFileName();
        }
        if(pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getIntroduction())) {
            picture.setIntroduction(pictureUploadRequest.getIntroduction());
        }
        if(pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getCategory())) {
            picture.setCategory(pictureUploadRequest.getCategory());
        }
        if(pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getTags())) {
            picture.setTags(pictureUploadRequest.getTags());
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {

        PictureVO pictureVO = PictureVO.objToVo(picture);

        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = UserVO.toUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }

        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());

        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(UserVO.toUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {

            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR,"搜索词不能为空");
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30,ErrorCode.PARAMS_ERROR,"单次爬取数量不得超过30");
        if(StrUtil.isBlank(namePrefix)){
            namePrefix = searchText;
        }
        // https://cn.bing.com/images/async?q=%E9%A3%8E%E6%99%AF&first=1&count=10&mmasync=1 first 起始条数 count 要查询的条数
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败",e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }

            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            if (StrUtil.isNotBlank(namePrefix)) {
                pictureUploadRequest.setFileName(namePrefix + (uploadCount + 1));
            }
            pictureUploadRequest.setIntroduction(pictureUploadByBatchRequest.getIntroduction());
            pictureUploadRequest.setCategory(pictureUploadByBatchRequest.getCategory());
            pictureUploadRequest.setTags(JSONUtil.toJsonStr(pictureUploadByBatchRequest.getTags()));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

}











