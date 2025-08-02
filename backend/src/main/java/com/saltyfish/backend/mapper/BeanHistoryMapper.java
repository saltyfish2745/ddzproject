package com.saltyfish.backend.mapper;

import java.time.LocalDate;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.saltyfish.backend.pojo.entity.BeanHistory;

@Mapper
public interface BeanHistoryMapper {

    // 插入数据
    @Insert("insert into bean_history(user_id, change_type, change_amount, current_bean, create_time) values(#{userId}, #{changeType}, #{changeAmount}, #{currentBean}, #{createTime})")
    public void insert(BeanHistory beanHistory);

    // 通过用户id、变更类型、创建时间查询bean历史记录，DATE(create_time) = #{today}是为了精确查询某一天的记录
    @Select("select * from bean_history where user_id = #{userId} and change_type = #{changeType} and DATE(create_time) = #{today}")
    public BeanHistory selectByUserIdAndChangeTypeAndCreateTime(Long userId, String changeType, LocalDate today);
}
