package com.kuncode.kuncodepicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kuncode.kuncodepicturebackend.annotation.AuthCheck;
import com.kuncode.kuncodepicturebackend.api.AliYunAiApi;
import com.kuncode.kuncodepicturebackend.api.aliyunAI.model.CreateOutPaintingTaskRequest;
import com.kuncode.kuncodepicturebackend.api.aliyunAI.model.CreateOutPaintingTaskResponse;
import com.kuncode.kuncodepicturebackend.api.aliyunAI.model.GetOutPaintingTaskResponse;
import com.kuncode.kuncodepicturebackend.api.imagessearch.ImageSearchFacaed;
import com.kuncode.kuncodepicturebackend.api.imagessearch.model.ImageSearchResult;
import com.kuncode.kuncodepicturebackend.common.BaseResponse;
import com.kuncode.kuncodepicturebackend.common.DeleteRequest;
import com.kuncode.kuncodepicturebackend.common.ResultUtils;
import com.kuncode.kuncodepicturebackend.constants.UserConstant;
import com.kuncode.kuncodepicturebackend.exception.BusinessException;
import com.kuncode.kuncodepicturebackend.exception.ErrorCode;
import com.kuncode.kuncodepicturebackend.exception.ThrowUtils;
import com.kuncode.kuncodepicturebackend.manager.auth.SpaceUserAuthManager;
import com.kuncode.kuncodepicturebackend.manager.auth.StpKit;
import com.kuncode.kuncodepicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.kuncode.kuncodepicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.kuncode.kuncodepicturebackend.model.dto.picture.*;
import com.kuncode.kuncodepicturebackend.model.entity.Picture;
import com.kuncode.kuncodepicturebackend.model.entity.Space;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.model.enums.PictureReviewStatusEnum;
import com.kuncode.kuncodepicturebackend.model.vo.picture.PictureTagCategory;
import com.kuncode.kuncodepicturebackend.model.vo.picture.PictureVO;
import com.kuncode.kuncodepicturebackend.service.IPictureService;
import com.kuncode.kuncodepicturebackend.service.ISpaceService;
import com.kuncode.kuncodepicturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.kuncode.kuncodepicturebackend.constants.RedisKeyConstant.PICTURE_LIST_VO_CACHE_KEY;

@RestController
@RequestMapping("/picture")
@RequiredArgsConstructor
@Slf4j
public class PictureController {

    final IUserService userService;

    final IPictureService pictureService;

    final StringRedisTemplate stringRedisTemplate;

    final ISpaceService spaceService;

    final TransactionTemplate transactionTemplate;

    final RedissonClient redissonClient;

    final AliYunAiApi aliYunAiApi;

    final SpaceUserAuthManager spaceUserAuthManager;

    final Cache<String,String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_100L)
            .expireAfterWrite(2,TimeUnit.MINUTES)
            .build();

    /**
     * 管理员上传图片
     * @param multipartFile 上传的文件
     * @param pictureUploadRequest 上传的文件信息
     * @param request HttpServletRequest
     * @return 返回图片信息
     */
    @PostMapping("/upload")
//    @AuthCheck
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过 URL 上传图片
     * @param pictureUploadRequest 上传的文件信息
     * @param request HttpServletRequest
     * @return 图片信息
     */
    @PostMapping("/upload/url")
//    @AuthCheck
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     * @param deleteRequest 要删除的图片信息
     * @param request HttpServletRequest
     * @return 返回是否删除成功
     */
    @PostMapping("/delete")
//    @AuthCheck
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        // 权限校验
//        pictureService.checkPictureAuth(loginUser, oldPicture);
        transactionTemplate.execute(status -> {
            boolean result = pictureService.removeById(oldPicture.getId());
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != 0L) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            // 删除老图片
            pictureService.clearPictureFile(oldPicture);
            return true;
        });
        return ResultUtils.success(true);
    }

    /**
     * 图片信息更新
     * @param pictureUpdateRequest 要更新的图片信息
     * @param request HttpServletRequest
     * @return 返回是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,HttpServletRequest request) {
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
        // 补充审核参数
        pictureService.fillReviewParams(picture, userService.getLoginUser(request));
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
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        Long spaceId = picture.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        Space space = null;
        if(spaceId != 0) {
//            pictureService.checkPictureAuth(loginUser, picture);
            boolean b = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!b, ErrorCode.NO_AUTH_ERROR,"无权查看");
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }else {
            if(!picture.getReviewStatus().equals(PictureReviewStatusEnum.PASS.getValue()) && !userService.isAdmin(loginUser) && !picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR,"图片不存在或在审核");
            }
        }
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, picture, loginUser);
//        if(space == null && picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            // 公共图库中图片 当前为图片创建人
//            permissionList.add(SpaceUserPermissionConstant.PICTURE_EDIT);
//            permissionList.add(SpaceUserPermissionConstant.PICTURE_DELETE);
//            permissionList.add(SpaceUserPermissionConstant.PICTURE_UPLOAD);
//        }
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        return ResultUtils.success(pictureVO);
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
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null,ErrorCode.PARAMS_ERROR);
        long current = pictureQueryRequest.getPage();
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 空间权限
        Long spaceId = pictureQueryRequest.getSpaceId();
        if(spaceId == null) {
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        }else {
            boolean b = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!b, ErrorCode.NO_AUTH_ERROR,"无权查看");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
//            if(!user.getId().equals(space.getUserId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间权限");
//            }
        }
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), PictureQueryRequest.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 分页获取图片脱敏信息
     * @param pictureQueryRequest 分页查询条件
     * @param request HttpServletRequest
     * @return Page<PictureVO>
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null,ErrorCode.PARAMS_ERROR);
        long current = pictureQueryRequest.getPage();
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 空间权限
        Long spaceId = pictureQueryRequest.getSpaceId();
        if(spaceId == null) {
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        }else {
            User user = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
            if(!user.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间权限");
            }
        }
        String queryCondition  = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format(PICTURE_LIST_VO_CACHE_KEY, hashKey);
        // 1，查询本地缓存 查询到直接返回
        String cache = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cache != null) {
            Page<PictureVO> cachedPage = JSONUtil.toBean(cache, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 2，查询不到本地缓存 查询Redis缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cache = opsForValue.get(cacheKey);
        if (cache != null) {
            // 存入本地缓存
            LOCAL_CACHE.put(cacheKey, cache);
            Page<PictureVO> cachedPage = JSONUtil.toBean(cache, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 3，都查询不到，查询数据库后写入到缓存
        Page<PictureVO> pictureVOPage;
        RLock lock = redissonClient.getLock(hashKey);
        lock.lock();
        try {
            // 再获取一边本地缓存 看缓存是否重构成功
            cache = LOCAL_CACHE.getIfPresent(cacheKey);
            if (cache != null) {
                Page<PictureVO> cachedPage = JSONUtil.toBean(cache, Page.class);
                return ResultUtils.success(cachedPage);
            }
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            Page<Picture> picturePage = pictureService.page(new Page<>(current, size), PictureQueryRequest.getQueryWrapper(pictureQueryRequest));
            pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
            String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
            LOCAL_CACHE.put(cacheKey, cacheValue);
            int cacheExpireTime = 120 +  RandomUtil.randomInt(0, 60);
            opsForValue.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        }catch (Exception e){
            log.error("获取图片数据出错",e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }finally {
            lock.unlock();
        }
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 编辑图片
     * @param pictureEditRequest 要编辑的图片信息
     * @param request HttpServletRequest
     * @return 是否编辑成功
     */
    @PostMapping("/edit")
//    @AuthCheck
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
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
//        // 权限校验
//        pictureService.checkPictureAuth(loginUser, oldPicture);
        // 图片已经是待审核状态并且不是第一次创建的时候 无法修改
        if(oldPicture.getReviewStatus().equals(PictureReviewStatusEnum.REVIEWING.getValue()) && StrUtil.isNotBlank(oldPicture.getReviewMessage())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"图片正在审核请勿修改");
        }
        // 填充审核参数
        pictureService.fillReviewParams(picture, loginUser);
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意","logo");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报","logo");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 图片审核接口
     * @param pictureReviewRequest 图片审核信息
     * @param request HttpServletRequest
     * @return 是否审核成功
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 通过关键词上传图片
     * @param pictureUploadByBatchRequest 上传的图片信息
     * @param request HttpServletRequest
     * @return 上传成功数量
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 以图识图
     * @param searchPictureByPictureRequest 要识别的图片信息
     * @return List<ImageSearchResult>
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchFacaed.searchImage(oldPicture.getUrl());
        return ResultUtils.success(resultList);
    }

    /**
     * 颜色搜图
     * @param searchPictureByColorRequest 颜色搜索信息
     * @param request HttpServletRequest
     * @return List<PictureVO>
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> result = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, HttpServletRequest request) {
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(pictureService.getById(pictureId)).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        User loginUser = userService.getLoginUser(request);
//        // 权限校验
//        pictureService.checkPictureAuth(loginUser,picture);
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        createOutPaintingTaskRequest.setInput(input);
        createOutPaintingTaskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        CreateOutPaintingTaskResponse outPaintingTask = aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
        return ResultUtils.success(outPaintingTask);
    }

    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }
}
