package com.hajimi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itkd.picture.model.dto.space.SpaceAddRequest;
import com.itkd.picture.model.dto.space.SpaceQueryRequest;
import com.itkd.picture.model.entity.Space;
import com.itkd.picture.model.entity.User;
import com.itkd.picture.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author kdc
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-03-11 08:30:02
*/
public interface SpaceService extends IService<Space> {

    void validSpace(Space space, boolean add);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    Page<SpaceVO> getSpaceVOPage(Page<Space> SpacePage, HttpServletRequest request);

    public void fillSpaceBySpaceLevel(Space space);

    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

}
