package com.saltyfish.backend.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.saltyfish.backend.constant.DatabaseConstant;
import com.saltyfish.backend.constant.GameConstant;
import com.saltyfish.backend.constant.MessageConstant;
import com.saltyfish.backend.context.BaseContext;
import com.saltyfish.backend.exception.AccountNotFoundException;
import com.saltyfish.backend.exception.BaseException;
import com.saltyfish.backend.mapper.BeanHistoryMapper;
import com.saltyfish.backend.mapper.UserMapper;
import com.saltyfish.backend.pojo.dto.UserDTO;
import com.saltyfish.backend.pojo.dto.UserLoginDTO;
import com.saltyfish.backend.pojo.entity.BeanHistory;
import com.saltyfish.backend.pojo.entity.User;
import com.saltyfish.backend.pojo.vo.BeanHistoryVO;
import com.saltyfish.backend.pojo.vo.UserInfo;
import com.saltyfish.backend.properties.EmailProperties;
import com.saltyfish.backend.result.PageResult;
import com.saltyfish.backend.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BeanHistoryMapper beanHistoryMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private EmailProperties emailProperties;

    // 登录操作
    @Override
    public User login(UserLoginDTO userLoginDTO) {
        String account = userLoginDTO.getAccount();
        String password = userLoginDTO.getPassword();
        // 根据账号查询数据库中的数据
        User user = userMapper.selectByAccount(account);
        // 判断账号是否存在
        if (user == null) {
            // 账号不存在抛出异常
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        // 判断密码是否正确
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(user.getPassword())) {
            // 密码错误抛出异常
            throw new AccountNotFoundException(MessageConstant.PASSWORD_ERROR);
        }
        // 登录成功
        return user;
    }

    // 注册操作
    @Override
    @Transactional
    public void register(UserDTO userDTO, String emailcode) {
        // 判断用户名账号或密码长度是否超过限制
        if (userDTO.getUsername().length() > DatabaseConstant.USERNAME_LENGTH
                || userDTO.getAccount().length() > DatabaseConstant.ACCOUNT_LENGTH
                || userDTO.getPassword().length() > DatabaseConstant.PASSWORD_LENGTH) {
            // 账号或密码长度超过限制抛出异常
            throw new BaseException(MessageConstant.UNKNOWN_ERROR);
        }
        // 判断账号是否已存在
        String account = userDTO.getAccount();
        User user1 = userMapper.selectByAccount(account);
        if (user1 != null) {
            // 账号已存在抛出异常
            throw new AccountNotFoundException(MessageConstant.ALREADY_EXISTS);
        }
        // 判断是否有邮箱
        if (userDTO.getEmail() != null) {
            // 判断验证码是否正确
            String emailcodeFromRedis = (String) redisTemplate.opsForValue().get(userDTO.getEmail());
            if (emailcodeFromRedis == null || !emailcodeFromRedis.equals(emailcode)) {
                // 验证码错误抛出异常
                throw new BaseException(MessageConstant.VERIFICATION_CODE_ERROR);
            }
        }
        User user = new User();
        // 复制dto到user对象
        BeanUtils.copyProperties(userDTO, user);
        // 设置时间和初始bean
        user.setCreateTime(LocalDateTime.now());
        user.setBeanCount(GameConstant.REGISTER_BEAN);
        // 加密密码
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        // 插入数据库
        userMapper.insert(user);
        // 获取ID
        User user2 = userMapper.selectByAccount(account);
        // 插入bean变更记录
        beanHistoryMapper.insert(BeanHistory.builder().userId(user2.getId())
                .changeType(DatabaseConstant.CHANGE_TYPE.REGISTER.toString()).changeAmount(GameConstant.REGISTER_BEAN)
                .currentBean(user2.getBeanCount()).createTime(LocalDateTime.now()).build());
    }

    // 检查账号是否存在// 存在返回true，不存在返回false
    @Override
    public boolean isAccountExist(String account) {
        User user = userMapper.selectByAccount(account);
        return user != null ? true : false;
    }

    // 用户密码找回
    @Override
    public void retrieve(UserDTO userDTO, String emailcode) {
        // 判断用户名账号或密码长度是否超过限制
        if (userDTO.getAccount().length() > DatabaseConstant.ACCOUNT_LENGTH
                || userDTO.getPassword().length() > DatabaseConstant.PASSWORD_LENGTH) {
            // 账号或密码长度超过限制抛出异常
            throw new BaseException(MessageConstant.UNKNOWN_ERROR);
        }
        // 根据账号查询数据库中的数据
        User user = userMapper.selectByAccount(userDTO.getAccount());
        // 判断账号是否存在
        if (user == null) {
            // 账号不存在抛出异常
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        // 判断邮箱是否正确
        if (user.getEmail() == null || !user.getEmail().equals(userDTO.getEmail())) {
            // 邮箱不正确抛出异常
            throw new BaseException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        // 判断是否有邮箱
        if (userDTO.getEmail() != null) {
            // 判断验证码是否正确
            String emailcodeFromRedis = (String) redisTemplate.opsForValue().get(userDTO.getEmail());
            if (emailcodeFromRedis == null || !emailcodeFromRedis.equals(emailcode)) {
                // 验证码错误抛出异常
                throw new BaseException(MessageConstant.VERIFICATION_CODE_ERROR);
            }
        }
        // 更新密码
        user.setPassword(DigestUtils.md5DigestAsHex(userDTO.getPassword().getBytes()));
        userMapper.updateById(user);
    }

    // 申请邮箱验证码操作
    @Override
    public void applyForEmailcode(String email) {
        // 定义邮箱格式的正则表达式
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        // 编译正则表达式
        Pattern pattern = Pattern.compile(emailRegex);
        // 创建Matcher对象
        Matcher matcher = pattern.matcher(email);
        // 验证邮箱格式
        if (!matcher.matches()) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        // 生成随机邮箱验证码,random生成的范围为0.0-1.0之间，转string取后8位
        String emailcode = Double.toString(Math.random()).substring(2, 8);
        // 发送邮件
        sendEmailcode(email, emailcode);
        // 先发送邮件，再保存到redis防止邮箱错误还存入redis
        // 30秒定时保存邮箱验证码到redis后续与注册接口进行验证
        redisTemplate.opsForValue().set(email, emailcode, DatabaseConstant.EMAILCODE_COUNTDOWN, TimeUnit.SECONDS);
    }

    // 用异步的方式发送邮件提高效率
    @Async
    public void sendEmailcode(String email, String emailcode) {
        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailProperties.getUsername());
            message.setTo(email);
            message.setSubject("DDZ邮箱验证码");
            message.setText("您的邮箱验证码为：" + emailcode);
            javaMailSender.send(message);
        } catch (MailException e) {
            log.error("发送邮件失败", e);
            throw new BaseException("发送邮件失败");
        }
    }

    // 签到操作
    @Override
    @Transactional
    public void clockIn() {
        // 通过线程变量获取当前用户ID
        Long userId = BaseContext.getCurrentId();
        LocalDate today = LocalDate.now();
        // 查询数据库中当天是否已签到
        BeanHistory beanHistory = beanHistoryMapper.selectByUserIdAndChangeTypeAndCreateTime(userId,
                DatabaseConstant.CHANGE_TYPE.CLOCK_IN.toString(), today);
        if (beanHistory != null) {
            // 已签到抛出异常
            throw new BaseException("已签到");
        }
        // 签到成功，更新数据库
        User user = userMapper.selectById(userId);
        userMapper.updatePlusBeanCount(userId, GameConstant.LOGIN_BEAN);
        beanHistoryMapper.insert(BeanHistory.builder().userId(userId)
                .changeType(DatabaseConstant.CHANGE_TYPE.CLOCK_IN.toString()).changeAmount(GameConstant.LOGIN_BEAN)
                .currentBean(user.getBeanCount() + GameConstant.LOGIN_BEAN).createTime(LocalDateTime.now()).build());
    }

    // 查看用户信息
    @Override
    public UserInfo viewUserInfo() {
        Long userId = BaseContext.getCurrentId();
        // 根据用户ID查询用户信息
        User user = userMapper.selectById(userId);
        // 复制user对象到UserInfo对象
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(user, userInfo);
        return userInfo;
    }

    // 分页查看用户豆币历史
    @Override
    public PageInfo<BeanHistoryVO> pageBeanHistory(int page, int pageSize) {
        Long userId = BaseContext.getCurrentId();
        PageHelper.startPage(page, pageSize);
        List<BeanHistoryVO> pageResult = userMapper.pageBeanHistoryByUserId(userId, page, pageSize);
        return new PageInfo<>(pageResult);
    }

}
