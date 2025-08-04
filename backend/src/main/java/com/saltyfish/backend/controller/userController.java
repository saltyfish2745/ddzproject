package com.saltyfish.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saltyfish.backend.constant.DatabaseConstant;
import com.saltyfish.backend.constant.JwtClaimsConstant;
import com.saltyfish.backend.constant.MessageConstant;
import com.saltyfish.backend.pojo.dto.UserDTO;
import com.saltyfish.backend.pojo.dto.UserLoginDTO;
import com.saltyfish.backend.pojo.entity.User;
import com.saltyfish.backend.pojo.vo.UserInfo;
import com.saltyfish.backend.pojo.vo.UserLoginVO;
import com.saltyfish.backend.properties.JwtProperties;
import com.saltyfish.backend.result.Result;
import com.saltyfish.backend.service.UserService;
import com.saltyfish.backend.utils.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RestController
@RequestMapping("")
@Tag(name = "用户接口")
public class userController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("/login")
    @Operation(summary = "用户登入", description = "用户登入接口")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("用户登入: " + userLoginDTO.toString());
        User user = userService.login(userLoginDTO);
        // 生成token
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getSecretKey(), jwtProperties.getExpiration(), claims);
        // 返回token
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .token(token).build();
        return Result.success(userLoginVO);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册接口")
    public Result<UserLoginVO> register(@RequestBody UserDTO userDTO,
            @RequestParam(required = false) String emailcode) {
        log.info("用户注册: " + userDTO.toString());
        // 注册用户
        userService.register(userDTO, emailcode);
        // 登入用户
        UserLoginDTO userLoginDTO = new UserLoginDTO();
        BeanUtils.copyProperties(userDTO, userLoginDTO);
        return login(userLoginDTO);

    }

    @GetMapping("/isAccountExist")
    @Operation(summary = "检查账号是否存在", description = "检查账号是否存在接口")
    public Result<Boolean> isAccountExist(@RequestParam String account) {
        log.info("检查账号是否存在: " + account);
        boolean exists = userService.isAccountExist(account);
        // 存在返回true，不存在返回false
        return Result.success(exists, exists ? MessageConstant.ACCOUNT_EXISTS : MessageConstant.ACCOUNT_NOT_FOUND);
    }

    @PostMapping("/retrieve")
    @Operation(summary = "用户密码找回", description = "用户密码找回接口")
    public Result retrieve(@RequestBody UserDTO userDTO, @RequestParam(required = false) String emailcode) {
        log.info("用户密码找回: " + userDTO.toString());
        // 密码找回
        userService.retrieve(userDTO, emailcode);
        // 返回成功信息
        return Result.success(MessageConstant.PASSWORD_RESET_SUCCESS);
    }

    @GetMapping("/applyForEmailcode")
    @Operation(summary = "申请邮箱验证码", description = "申请邮箱验证码接口")
    public Result applyForEmailcode(@RequestParam String email) {
        log.info("申请邮箱验证码: " + email);
        userService.applyForEmailcode(email);
        return Result.success(DatabaseConstant.EMAILCODE_COUNTDOWN);
    }

    @GetMapping("/clockIn")
    @Operation(summary = "用户签到", description = "用户签到接口")
    public Result clockIn() {
        log.info("用户签到");
        userService.clockIn();
        return Result.success("签到成功");
    }

    @GetMapping("/viewUserInfo")
    @Operation(summary = "查看用户信息", description = "查看用户信息接口")
    public Result<UserInfo> viewUserInfo() {
        log.info("查看用户信息");
        UserInfo userInfo = userService.viewUserInfo();
        return Result.success(userInfo);
    }
    

}
