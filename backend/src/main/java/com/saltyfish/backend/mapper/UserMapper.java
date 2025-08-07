package com.saltyfish.backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.github.pagehelper.Page;
import com.saltyfish.backend.pojo.entity.BeanHistory;
import com.saltyfish.backend.pojo.entity.User;
import com.saltyfish.backend.pojo.vo.BeanHistoryVO;

@Mapper
public interface UserMapper {

    // 根据账号查询用户信息
    @Select("SELECT * FROM user WHERE account = #{account}")
    public User selectByAccount(String account);

    // 插入用户信息
    @Insert("INSERT INTO user(username, account, password, bean_count, create_time, email) VALUES(#{username}, #{account}, #{password}, #{beanCount}, #{createTime}, #{email})")
    public void insert(User user);

    // 根据用户id查询用户信息
    @Select("SELECT * FROM user WHERE id = #{userId}")
    public User selectById(Long userId);

    // 给用户加豆
    @Update("UPDATE user SET bean_count = bean_count + (#{beanCount}) WHERE id = #{userId}")
    public void updatePlusBeanCount(Long userId, Long beanCount);

    // 根据用户id查询豆数量
    @Select("SELECT bean_count FROM user WHERE id = #{userId}")
    public Long selectBeanCountById(Long userId);

    // 更新用户信息
    @Update("UPDATE user SET username = #{username}, account = #{account}, password = #{password}, email = #{email} WHERE id = #{id}")
    public void updateById(User user);

    public List<BeanHistoryVO> pageBeanHistoryByUserId(Long userId, Integer page, Integer pageSize);

    // 根据ID更改用户名
    @Update("UPDATE user SET username = #{username} WHERE id = #{userId}")
    public void updateUsernameById(String username, Long userId);

}
