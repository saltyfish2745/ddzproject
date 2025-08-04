package com.saltyfish.backend.pojo.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeanHistoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String changeType;
    private Long changeAmount;
    private Long currentBean;

    // 注解用于格式化日期时间发给前端
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
