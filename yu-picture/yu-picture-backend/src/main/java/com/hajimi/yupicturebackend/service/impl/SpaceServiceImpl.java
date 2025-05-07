package com.hajimi.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.hajimi.yupicturebackend.exception.BusinessException;
import com.hajimi.yupicturebackend.exception.ErrorCode;
import com.hajimi.yupicturebackend.exception.ThrowUtils;
import com.hajimi.yupicturebackend.model.entity.Space;
import com.hajimi.yupicturebackend.model.entity.User;
import com.hajimi.yupicturebackend.model.vo.SpaceVO;
import com.hajimi.yupicturebackend.model.vo.UserVO;
import com.hajimi.yupicturebackend.service.SpaceService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author kdc
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-03-11 08:30:02
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserServiceImpl userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 校验空间信息
     * @param space
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        //从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(spaceLevel);
        if(add){
            ThrowUtils.throwIf(spaceName == null, ErrorCode.PARAMS_ERROR, "空间名不能为空");
            ThrowUtils.throwIf(enumByValue == null, ErrorCode.PARAMS_ERROR, "空间等级不能为空");
        }
        if(spaceLevel!=null&&enumByValue==null){
            ThrowUtils.throwIf(enumByValue == null, ErrorCode.PARAMS_ERROR, "没有这个空间级别");
        }
        if(spaceName!=null&&spaceName.length()>30){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名过长");
        }

    }

    /**
     * 填充空间信息
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (enumByValue!=null){
            long maxSize = enumByValue.getMaxSize();
            if (space.getMaxSize()==null){
                space.setMaxSize(maxSize);
            }
            if (space.getMaxCount()==null){
                space.setMaxCount(enumByValue.getMaxCount());
            }
        }
    }

    /**
     * 添加空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest,User loginUser) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值导入space
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (space.getSpaceName()==null){
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel()==null){
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        fillSpaceBySpaceLevel(space);
        // 校验参数
        validSpace(space,true);
        //验证权限
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        //控制一个用户只能创建一个默认空间
        String lock = String.valueOf(userId).intern();
        synchronized (lock){
            Long executed = transactionTemplate.execute(status -> {
                //判断是否有默认空间
                boolean exist = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .exists();
                //如果有默认空间则抛出异常
                if (exist) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已经创建了默认空间");
                }
                //如果没有默认空间则创建一个默认空间
                boolean result = this.save(space);
                if (!result) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建空间失败");
                }
                return space.getId();
            });
            return executed;
        }
    }

    /**
     * 获取查询条件
     * @param spaceQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 判断条件
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjectUtils.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取空间
     * @param space
     * @param request
     * @return
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 获取空间分页
     * @param spacePage
     * @param request
     * @return
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }


}




