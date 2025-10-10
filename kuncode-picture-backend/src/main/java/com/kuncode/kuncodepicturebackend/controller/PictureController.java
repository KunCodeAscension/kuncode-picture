package com.kuncode.kuncodepicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kuncode.kuncodepicturebackend.annotation.AuthCheck;
import com.kuncode.kuncodepicturebackend.common.BaseResponse;
import com.kuncode.kuncodepicturebackend.common.DeleteRequest;
import com.kuncode.kuncodepicturebackend.common.ResultUtils;
import com.kuncode.kuncodepicturebackend.constants.UserConstant;
import com.kuncode.kuncodepicturebackend.exception.BusinessException;
import com.kuncode.kuncodepicturebackend.exception.ErrorCode;
import com.kuncode.kuncodepicturebackend.exception.ThrowUtils;
import com.kuncode.kuncodepicturebackend.model.dto.picture.*;
import com.kuncode.kuncodepicturebackend.model.entity.Picture;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.model.enums.PictureReviewStatusEnum;
import com.kuncode.kuncodepicturebackend.model.vo.PictureTagCategory;
import com.kuncode.kuncodepicturebackend.model.vo.PictureVO;
import com.kuncode.kuncodepicturebackend.service.IPictureService;
import com.kuncode.kuncodepicturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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

    final RedissonClient redissonClient;

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
        // 删除老图片
        pictureService.clearPictureFile(oldPicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
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
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        User loginUser = userService.getLoginUser(request);
        if(!picture.getReviewStatus().equals(PictureReviewStatusEnum.PASS.getValue()) && !userService.isAdmin(loginUser) && !picture.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
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
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
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
            long current = pictureQueryRequest.getPage();
            long size = pictureQueryRequest.getPageSize();
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
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
     * 分页获取图片脱敏信息
     * @param pictureQueryRequest 分页查询条件
     * @param request HttpServletRequest
     * @return Page<PictureVO>
     */
    @PostMapping("/list/page/vo/redisCache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageRedis(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        String queryCondition  = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String redisKey = String.format(PICTURE_LIST_VO_CACHE_KEY, hashKey);
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String cache = opsForValue.get(redisKey);
        if (cache != null) {
            Page<PictureVO> cachedPage = JSONUtil.toBean(cache, Page.class);
            return ResultUtils.success(cachedPage);
        }
        long current = pictureQueryRequest.getPage();
        long size = pictureQueryRequest.getPageSize();
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), PictureQueryRequest.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        int cacheExpireTime = 300 +  RandomUtil.randomInt(0, 300);
        opsForValue.set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 分页获取图片脱敏信息
     * @param pictureQueryRequest 分页查询条件
     * @param request HttpServletRequest
     * @return Page<PictureVO>
     */
    @PostMapping("/list/page/vo/caffeineCache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageCaffeine(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        String queryCondition  = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String caffeineKey = String.format(PICTURE_LIST_VO_CACHE_KEY, hashKey);
        String cache = LOCAL_CACHE.getIfPresent(caffeineKey);
        if (cache != null) {
            Page<PictureVO> cachedPage = JSONUtil.toBean(cache, Page.class);
            return ResultUtils.success(cachedPage);
        }
        long current = pictureQueryRequest.getPage();
        long size = pictureQueryRequest.getPageSize();
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), PictureQueryRequest.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        LOCAL_CACHE.put(caffeineKey, cacheValue);
        return ResultUtils.success(pictureVOPage);
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

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

}
