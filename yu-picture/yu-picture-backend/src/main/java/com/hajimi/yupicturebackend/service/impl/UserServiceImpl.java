package com.hajimi.yupicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hajimi.yupicturebackend.model.entity.User;
import com.hajimi.yupicturebackend.service.UserService;
import com.hajimi.yupicturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author kdc
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-04-12 14:46:37
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




