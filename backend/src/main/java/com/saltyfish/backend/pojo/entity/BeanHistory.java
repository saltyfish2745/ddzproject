package com.saltyfish.backend.pojo.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeanHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String changeType;
    private Long changeAmount;
    private Long currentBean;
    private LocalDateTime createTime;

}
