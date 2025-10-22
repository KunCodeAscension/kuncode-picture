package com.kuncode.kuncodepicturebackend.service.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuncode.kuncodepicturebackend.common.ResultUtils;
import com.kuncode.kuncodepicturebackend.exception.BusinessException;
import com.kuncode.kuncodepicturebackend.exception.ErrorCode;
import com.kuncode.kuncodepicturebackend.exception.ThrowUtils;
import com.kuncode.kuncodepicturebackend.manager.CosManager;
import com.kuncode.kuncodepicturebackend.manager.upload.FilePictureUpload;
import com.kuncode.kuncodepicturebackend.manager.upload.UrlPictureUpload;
import com.kuncode.kuncodepicturebackend.mapper.PictureMapper;
import com.kuncode.kuncodepicturebackend.model.dto.file.UploadPictureResult;
import com.kuncode.kuncodepicturebackend.model.dto.picture.*;
import com.kuncode.kuncodepicturebackend.model.entity.Picture;
import com.kuncode.kuncodepicturebackend.model.entity.Space;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.model.enums.PictureReviewStatusEnum;
import com.kuncode.kuncodepicturebackend.model.vo.PictureVO;
import com.kuncode.kuncodepicturebackend.model.vo.UserVO;
import com.kuncode.kuncodepicturebackend.service.IPictureService;
import com.kuncode.kuncodepicturebackend.service.ISpaceService;
import com.kuncode.kuncodepicturebackend.service.IUserService;
import com.kuncode.kuncodepicturebackend.utils.ColorSimilarUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements IPictureService {

    final FilePictureUpload filePictureUpload;

    final UrlPictureUpload  urlPictureUpload;

    final CosManager cosManager;

    final IUserService userService;

    final ISpaceService spaceService;

    final TransactionTemplate transactionTemplate;

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
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = pictureUploadRequest.getId();
        Long spaceId = pictureUploadRequest.getSpaceId();
        if(spaceId != null){
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            if(!loginUser.getId().equals(space.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权访问");
            }
            if(space.getTotalSize() >= space.getMaxSize()){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"空间大小不足");
            }
            if(space.getTotalCount() >= space.getMaxCount()){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"空间条数不足");
            }
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
            if(spaceId == null){
                if(oldPicture.getSpaceId() != null){
                    spaceId = oldPicture.getSpaceId();
                }
            }else {
                // 必须和之前的一致
                if(ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间ID不一致");
                }
            }
        }
        String uploadPathPrefix = spaceId != null ? String.format("public/%s", spaceId) : String.format("space/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = inputObject instanceof MultipartFile ? filePictureUpload.uploadPicture(inputObject, uploadPathPrefix) : urlPictureUpload.uploadPicture(inputObject, uploadPathPrefix);
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        String picName = uploadPictureResult.getPicName();
        if(StrUtil.isNotBlank(pictureUploadRequest.getFileName())) {
            picName = pictureUploadRequest.getFileName();
        }
        if(StrUtil.isNotBlank(pictureUploadRequest.getIntroduction())) {
            picture.setIntroduction(pictureUploadRequest.getIntroduction());
        }
        if(StrUtil.isNotBlank(pictureUploadRequest.getCategory())) {
            picture.setCategory(pictureUploadRequest.getCategory());
        }
        if(StrUtil.isNotBlank(pictureUploadRequest.getTags())) {
            picture.setTags(pictureUploadRequest.getTags());
        }
        picture.setName(picName);
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        Long finalSpaceId = spaceId;
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
            transactionTemplate.execute(status -> {
                Picture oldPicture = this.getById(pictureId);
                boolean saveOrUpdate = this.saveOrUpdate(picture);
                ThrowUtils.throwIf(!saveOrUpdate, ErrorCode.OPERATION_ERROR, "图片上传失败");
                if(finalSpaceId != null){
                    long sizeDiff = picture.getPicSize() - oldPicture.getPicSize();
                    boolean update = spaceService.lambdaUpdate()
                            .eq(Space::getId, finalSpaceId)
                            .setSql("totalSize = totalSize + " + sizeDiff)
                            .update();
                    ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
                }
                // 清理资源
                this.clearPictureFile(oldPicture);
                return picture;
            });
            return PictureVO.objToVo(picture);
        }else {
            transactionTemplate.execute(status -> {
                boolean saveOrUpdate = this.saveOrUpdate(picture);
                ThrowUtils.throwIf(!saveOrUpdate, ErrorCode.OPERATION_ERROR, "图片上传失败");
                if(finalSpaceId != null){
                    String sql = String.format(
                            "totalSize = totalSize + %d, totalCount = totalCount + 1",
                            picture.getPicSize() // 传入新图片大小
                    );
                    boolean update = spaceService.lambdaUpdate()
                            .eq(Space::getId, finalSpaceId)
                            .setSql(sql)
                            .update();
                    ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间更新失败");
                }
                return picture;
            });
            return PictureVO.objToVo(picture);
        }
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

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        cosManager.deleteObject(oldPicture.getUrl());
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String color,User loginUser) {
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null,ErrorCode.PARAMS_ERROR,"空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无该空间访问权限");
        }
        List<Picture> pictures = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        Color targetColor = Color.decode(color);
        return pictures.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String colorStr = picture.getPicColor();
                    if (StrUtil.isBlank(colorStr)) {
                        return Double.MAX_VALUE;
                    }
                    Color haxColor = Color.decode(colorStr);
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, haxColor);
                }))
                .limit(12)
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

}











