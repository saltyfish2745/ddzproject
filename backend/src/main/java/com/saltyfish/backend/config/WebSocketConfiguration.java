package com.saltyfish.backend.config;

import java.net.URI;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.util.UriComponentsBuilder;

import com.saltyfish.backend.constant.JwtClaimsConstant;
import com.saltyfish.backend.context.ApplicationContextUtil;
import com.saltyfish.backend.context.BaseContext;
import com.saltyfish.backend.properties.JwtProperties;
import com.saltyfish.backend.utils.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;

// websocket配置类
// 由于继承ServerEndpointConfig.Configurator则由WebSocket容器实例化，所以无法使用@Component注解和@Autowired注解
// 所以只能使用@Configuration注解，并实现ServerEndpointConfig.Configurator接口
@Slf4j
@Configuration
public class WebSocketConfiguration extends ServerEndpointConfig.Configurator {

    // 注册ServerEndpointExporter，用于扫描带有@ServerEndpoint注解的类
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    // 重写握手方法，用于校验Token并设置当前用户id到线程变量中
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // 从URL获取Token
        URI requestUri = request.getRequestURI();
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(requestUri).build().getQueryParams();
        List<String> tokens = queryParams.get("token");
        String token = (tokens != null && !tokens.isEmpty()) ? tokens.get(0) : null;
        // 获取jwt配置信息
        // 因为不能用spring自动注入，所以只能通过SpringContextUtil获取bean
        JwtProperties jwtProperties = (JwtProperties) ApplicationContextUtil.getBean("jwtProperties");
        // 校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("当前用户id：{}", userId);
            // jwt校验通过，设置当前用户id存到config中
            // 为什么不存线程变量？因为握手环节和onOpen环节不一定在同一个线程中，所以不能直接存到线程变量中
            sec.getUserProperties().put("userId", userId);
        } catch (RuntimeException ex) {
            log.error("JWT校验失败: {}", ex.getMessage());
            // 如果jwt校验失败，抛出异常，WebSocket容器会关闭连接
            throw new RuntimeException("JWT校验失败: " + ex.getMessage());
        }

        super.modifyHandshake(sec, request, response);
    }

}
