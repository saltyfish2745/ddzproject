package com.saltyfish.backend.service;

import com.saltyfish.backend.pojo.dto.UserDTO;
import com.saltyfish.backend.pojo.dto.UserLoginDTO;
import com.saltyfish.backend.pojo.entity.User;

public interface UserService {

    // 登入操作
    User login(UserLoginDTO userLoginDTO);

    // 注册操作
    void register(UserDTO userDTO, String emailcode);

    // 申请邮箱验证码操作
    void applyForEmailcode(String email);

    // 签到操作
    void clockIn();

    // 查看bean操作
    Long viewBean();

    // 检查账号是否存在// 存在返回true，不存在返回false
    boolean isAccountExist(String account);

}
