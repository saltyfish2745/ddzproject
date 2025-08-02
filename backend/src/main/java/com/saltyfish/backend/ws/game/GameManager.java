package com.saltyfish.backend.ws.game;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GameManager {
    // 匹配池（使用线程安全集合）
    private static final Set<Session> matchingPool = ConcurrentHashMap.newKeySet();
    // 房间集合
    private static final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    public static void removeGameRoom(String roomId) {
        rooms.remove(roomId);
    }

    // 加入匹配池
    public static synchronized void joinMatching(Session session) {
        matchingPool.add(session);
        if (matchingPool.size() >= 3) {
            // 创建玩家List和房间
            List<Session> players = new ArrayList<>(matchingPool).subList(0, 3);
            GameRoom room = new GameRoom(players);
            // 加入房间集合
            rooms.put(room.getRoomId(), room);
            // 从匹配池移除玩家
            matchingPool.removeAll(players);
            // 设置玩家ID用于前端分辨玩家身份，因为username允许重名
            for (int i = 0; i < players.size(); i++) {
                players.get(i).getUserProperties().put("playerId", i);
            }
            // 通知玩家匹配成功
            players.forEach(player -> {
                try {
                    // 发生匹配成功消息并为玩家session设置房间ID
                    if (player.isOpen()) {
                        player.getUserProperties().put("roomId", room.getRoomId());
                        // 生成匹配成功JSON消息
                        String msg = buildMatchSuccessMsg(room, (Integer) player.getUserProperties().get("playerId"));
                        player.getBasicRemote().sendText(msg);
                    } else {
                        // 玩家已断开连接，移除房间断开连接的玩家
                        rooms.remove(room.getRoomId());
                        players.forEach(p -> {
                            if (p.isOpen()) {
                                try {
                                    p.close(new CloseReason(
                                            CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                                            "检测到队友离线"));
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                }
            });
            // 房间初始化
            room.initialize();
        }
    }

    // 生成匹配成功JSON消息
    private static String buildMatchSuccessMsg(GameRoom room, Integer playerId) {
        JSONObject msg = new JSONObject();
        msg.put("type", "match_success");
        msg.put("yourPlayerId", playerId);
        JSONArray players = new JSONArray();
        room.getPlayers().forEach(p -> {
            JSONObject playerInfo = new JSONObject();
            playerInfo.put("username", p.getUserProperties().get("username"));
            playerInfo.put("beanCount", p.getUserProperties().get("beanCount"));
            playerInfo.put("playerId", p.getUserProperties().get("playerId"));
            players.add(playerInfo);
        });
        msg.put("players", players);
        return msg.toJSONString();
        /*
         * 示例匹配成功JSON消息
         * {
         * "type": "match_success",
         * "yourPlayerId": 0,
         * "players": [
         * {"username": "玩家1", "beanCount": 1000},
         * {"username": "玩家2", "beanCount": 2000},
         * {"username": "玩家3", "beanCount": 3000}
         * ]
         * }
         */
    }

    // 处理断连
    public static void handleDisconnect(Session session) {
        // 通过session的房间Id判断玩家是否在房间中
        if (session.getUserProperties().containsKey("roomId")) {
            try{
                rooms.get(session.getUserProperties().get("roomId")).handleDisconnect(session);
            } catch (Exception e) {
            }
        } else {
            // 不在房间中，从匹配池移除玩家
            matchingPool.remove(session);
        }
    }

    public static void handleMessage(Session session, JSONObject json) {
        rooms.get(session.getUserProperties().get("roomId")).handlePlayerOperation(session, json);
    }

}
