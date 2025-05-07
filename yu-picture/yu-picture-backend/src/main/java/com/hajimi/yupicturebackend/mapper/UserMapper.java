package com.hajimi.yupicturebackend.mapper;

import com.hajimi.yupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author kdc
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2025-04-12 14:46:37
* @Entity com.yupi.yupicturebackend.model.entity.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




