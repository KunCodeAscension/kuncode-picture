package com.kuncode.kuncodepicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuncode.kuncodepicturebackend.exception.BusinessException;
import com.kuncode.kuncodepicturebackend.exception.ErrorCode;
import com.kuncode.kuncodepicturebackend.exception.ThrowUtils;
import com.kuncode.kuncodepicturebackend.manager.sharding.DynamicShardingManager;
import com.kuncode.kuncodepicturebackend.mapper.SpaceMapper;
import com.kuncode.kuncodepicturebackend.model.dto.space.SpaceAddRequest;
import com.kuncode.kuncodepicturebackend.model.entity.Space;
import com.kuncode.kuncodepicturebackend.model.entity.SpaceUser;
import com.kuncode.kuncodepicturebackend.model.entity.User;
import com.kuncode.kuncodepicturebackend.model.enums.SpaceLevelEnum;
import com.kuncode.kuncodepicturebackend.model.enums.SpaceRoleEnum;
import com.kuncode.kuncodepicturebackend.model.enums.SpaceTypeEnum;
import com.kuncode.kuncodepicturebackend.model.vo.space.SpaceVO;
import com.kuncode.kuncodepicturebackend.model.vo.user.UserVO;
import com.kuncode.kuncodepicturebackend.service.ISpaceService;
import com.kuncode.kuncodepicturebackend.service.ISpaceUserService;
import com.kuncode.kuncodepicturebackend.service.IUserService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.kuncode.kuncodepicturebackend.constants.RedisKeyConstant.SPACE_LEVEL_COMMON_ONLY_LOCK_KEY;

@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper,Space> implements ISpaceService {

    @Resource
    IUserService userService;

    @Resource
    RedissonClient redissonClient;

    @Resource
    DataSourceTransactionManager transactionManager;

    @Resource
    ISpaceUserService spaceUserService;

    @Lazy
    @Resource
    DynamicShardingManager dynamicShardingManager;

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);

        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
        }
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        if(spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间类别不存在");
        }
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {

        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = UserVO.toUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }

        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());

        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(UserVO.toUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) throws InterruptedException {
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        this.fillSpaceBySpaceLevel(space);
        this.validSpace(space, true);
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        RLock lock = redissonClient.getLock(String.format(SPACE_LEVEL_COMMON_ONLY_LOCK_KEY,userId.toString()));
        boolean b = lock.tryLock(0, 5, TimeUnit.SECONDS);
        if (!b) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"点击频繁");
        }
        Long newSpaceId;
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try{
            boolean exists = this.lambdaQuery()
                    .eq(Space::getUserId, userId)
                    .eq(Space::getSpaceType, space.getSpaceType())
                    .exists();
            ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户仅能有一个特定空间");
            boolean result = this.save(space);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建空间失败");
            newSpaceId = space.getId();
            if(SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()){
                SpaceUser spaceUser = new SpaceUser();
                spaceUser.setSpaceId(space.getId());
                spaceUser.setUserId(userId);
                spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                boolean save = spaceUserService.save(spaceUser);
                ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
            }
            if(SpaceLevelEnum.FLAGSHIP.getValue() == space.getSpaceLevel()){
                // 旗舰版空间创建单数的表存放图片
                dynamicShardingManager.createSpacePictureTable(space);
            }
            transactionManager.commit(status);
        }catch (Exception e){
            log.error("空间创建错误",e);
            transactionManager.commit(status);
            throw new BusinessException(ErrorCode.PARAMS_ERROR,e.getMessage());
        }finally {
            lock.unlock();
        }
        return newSpaceId;
    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }


}
