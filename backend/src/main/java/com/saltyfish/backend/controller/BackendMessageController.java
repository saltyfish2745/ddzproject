package com.saltyfish.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saltyfish.backend.properties.BackendProperties;
import com.saltyfish.backend.result.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/backend")
@Tag(name = "后端信息接口")
public class BackendMessageController {

    @Autowired
    private BackendProperties backendProperties;

    @GetMapping("/readFile")
    @Operation(summary = "读取动态文件内容")
    public Result readFile() {
        return Result.success(backendProperties.getMessage());
    }
}
