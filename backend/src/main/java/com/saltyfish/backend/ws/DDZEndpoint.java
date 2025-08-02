package com.saltyfish.backend.ws;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.saltyfish.backend.config.WebSocketConfiguration;
import com.saltyfish.backend.context.ApplicationContextUtil;
import com.saltyfish.backend.context.BaseContext;
import com.saltyfish.backend.mapper.UserMapper;
import com.saltyfish.backend.pojo.entity.User;
import com.saltyfish.backend.ws.game.GameManager;

import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;

@ServerEndpoint(value = "/user/ddz", configurator = WebSocketConfiguration.class)
@Component
@Slf4j
public class DDZEndpoint {

    // 因为websock不归spring管理，所以需要用ApplicationContextUtil来注入mapper
    private UserMapper userMapper = ApplicationContextUtil.getBean(UserMapper.class);

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // 保存当前用户的id
        Long userId = BaseContext.getCurrentId();
        // 根据userId查询用户信息
        User user = userMapper.selectById(userId);
        // 保存用户信息到session属性中
        session.getUserProperties().put("userId", userId);
        session.getUserProperties().put("username", user.getUsername());
        session.getUserProperties().put("beanCount", user.getBeanCount());
        log.info("User {} 连接成功.", user.getUsername());

        GameManager.joinMatching(session); // 加入匹配池
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        if (session.getUserProperties().containsKey("roomId")) {// 判断是否在游戏中
            // 处理游戏消息
            JSONObject json = JSONObject.parseObject(message);
            GameManager.handleMessage(session, json);
        }

    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        log.info("User {} 断开连接.", session.getUserProperties().get("username"));
        GameManager.handleDisconnect(session); // 处理断开连接事件
    }
}
