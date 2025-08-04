package com.saltyfish.backend.service;

import com.saltyfish.backend.pojo.dto.UserDTO;
import com.saltyfish.backend.pojo.dto.UserLoginDTO;
import com.saltyfish.backend.pojo.entity.User;
import com.saltyfish.backend.pojo.vo.UserInfo;
import com.saltyfish.backend.result.PageResult;

public interface UserService {

    // 登入操作
    User login(UserLoginDTO userLoginDTO);

    // 注册操作
    void register(UserDTO userDTO, String emailcode);

    // 申请邮箱验证码操作
    void applyForEmailcode(String email);

    // 签到操作
    void clockIn();

    // 检查账号是否存在// 存在返回true，不存在返回false
    boolean isAccountExist(String account);

    // 用户密码找回
    void retrieve(UserDTO userDTO, String emailcode);

    // 查看用户信息
    UserInfo viewUserInfo();

    // 分页查看用户豆币历史
    PageResult pageBeanHistory(Integer page, Integer pageSize);

}
