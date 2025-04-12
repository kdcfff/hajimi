package com.hajimi.yupicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hajimi.yupicturebackend.exception.BusinessException;
import com.hajimi.yupicturebackend.exception.ErrorCode;
import com.hajimi.yupicturebackend.exception.ThrowUtils;
import com.hajimi.yupicturebackend.model.entity.User;
import com.hajimi.yupicturebackend.service.UserService;
import com.hajimi.yupicturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
* @author kdc
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-04-12 14:46:37
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 查询账户是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密密码
        String encryptPassword = encryptPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserName("无名");
        user.setUserPassword(encryptPassword);
        boolean saveResult = this.save(user);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        return user.getId();
    }

    /**
     * 加密密码
     * @return
     */
    private String encryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "kdc";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }
}




