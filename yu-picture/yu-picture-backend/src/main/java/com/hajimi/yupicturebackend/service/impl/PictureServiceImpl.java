package com.hajimi.yupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hajimi.yupicturebackend.exception.BusinessException;
import com.hajimi.yupicturebackend.exception.ErrorCode;
import com.hajimi.yupicturebackend.exception.ThrowUtils;
import com.hajimi.yupicturebackend.manager.upload.FilePictureUploadImpl;
import com.hajimi.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.hajimi.yupicturebackend.manager.upload.UrlUploadImpl;
import com.hajimi.yupicturebackend.mapper.PictureMapper;
import com.hajimi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.hajimi.yupicturebackend.model.entity.Picture;
import com.hajimi.yupicturebackend.model.entity.User;
import com.hajimi.yupicturebackend.model.vo.UserVO;
import com.hajimi.yupicturebackend.service.PictureService;
import com.hajimi.yupicturebackend.service.SpaceService;
import com.hajimi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.lang.model.util.Elements;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author kdc
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-02-28 19:26:13
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

/*
    @Resource
    FileManager fileManager;
*/

    @Resource
    UserService userService;
    @Resource
    FilePictureUploadImpl filePictureUploadImpl;
    @Resource
    UrlUploadImpl urlUploadImpl;
    @Resource
    SpaceService spaceService;



    /**
     * 上传图片
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验登录
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null){
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权操作");
        }
        PictureVO pictureVO = new PictureVO();
        // 校验图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        if (pictureId != null) {
            boolean exists = this.lambdaQuery().eq(Picture::getId, pictureId).exists();
            if (!exists) {
                ThrowUtils.throwIf(true, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }
            //只有管理员或图片所有者才能修改
            if (!userService.isAdmin(loginUser)&&!loginUser.getId().equals(pictureId)){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作");
            }
        }
        //上传图片
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        PictureUploadTemplate picUploadTemplate = filePictureUploadImpl;
        if (inputSource instanceof String){
            picUploadTemplate=urlUploadImpl;
        }
        // 上传图片
        UploadPictureResult uploadPictureResult = picUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        log.info(uploadPictureResult.toString());
        Picture picture=new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        String picName = uploadPictureResult.getPicName();
        //如果pictureUploadRequest含有picName，则覆盖默认值
        if (pictureUploadRequest!=null&&StrUtil.isNotBlank(pictureUploadRequest.getPicName())){
            picName=pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        if (pictureId != null){
            picture.setId(pictureId);
            picture.setUpdateTime(new Date());
        }
        //数据入库
        this.fillReviewParams(picture, loginUser);
        boolean save = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        return PictureVO.objToVo(picture);
    }

    /**
     * 获取查询包装类
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 通过picture获取VO
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 校验图片数据
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //检验参数
        ThrowUtils.throwIf(pictureReviewRequest == null||loginUser==null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        ThrowUtils.throwIf(reviewStatus==null||id==null,ErrorCode.PARAMS_ERROR);
        // 检验图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR);
        //查看是否重新审核
        ThrowUtils.throwIf(reviewStatusEnum.equals(oldPicture.getReviewStatus()),ErrorCode.OPERATION_ERROR,"请勿重复审核");
        //更新图片状态(新建一个对象减少update的消耗
        Picture picture = new Picture();
        picture.setId(id);
        picture.setReviewStatus(reviewStatus);
        picture.setReviewMessage(reviewMessage);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewTime(new Date());
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量上传图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest==null,ErrorCode.PARAMS_ERROR);
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count>30,ErrorCode.PARAMS_ERROR,"一次最多提取30张图片");
        //定义抓取url
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document = null;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("爬取图片失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"爬取图片失败");
        }

        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"没有找到图片");
        }
        Elements elements = document.select("img.mimg");
        //获取批量抓取图片前缀
        String prefix = pictureUploadByBatchRequest.getNamePrefix();
        //如果前缀为空，则使用搜索词作为前缀
        if (StrUtil.isEmpty(prefix)){
            prefix = searchText;
        }
        int uploadCount = 0;
        for (Element element : elements) {
            String fileUrl = element.attr("src");
            if (StrUtil.isEmpty(fileUrl)){
                log.info("图片地址为空,已跳过{}", fileUrl);
                continue;
            }
            int index = fileUrl.indexOf("?");
            if (index>-1){
                fileUrl = fileUrl.substring(0,index);
            }
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            //设置图片名称
            pictureUploadRequest.setPicName(prefix+(uploadCount+1));
            try {
                uploadPicture(fileUrl,pictureUploadRequest,loginUser);
                uploadCount++;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"图片上传失败");
            }
            if (uploadCount>=count){
                break;
            }


        }
        return uploadCount;

    }


}

