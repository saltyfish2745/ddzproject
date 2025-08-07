package com.saltyfish.backend.ws.game;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.saltyfish.backend.constant.GameConstant;
import com.saltyfish.backend.context.ApplicationContextUtil;
import com.saltyfish.backend.mapper.BeanHistoryMapper;
import com.saltyfish.backend.mapper.UserMapper;
import com.saltyfish.backend.pojo.entity.BeanHistory;

import jakarta.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class GameRoom {
    // 初始化牌
    private static List<Card> staticCards = new ArrayList<>();
    // 静态代码块
    // 特点：随着类的加载而在加载的，而且只执行一次。
    static {
        // 准备牌
        String[] color = { "红桃", "黑桃", "方块", "梅花" };
        String[] num = { "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2" };
        int i = 4, index = 0;
        for (String n : num) {
            for (String c : color) {
                staticCards.add(new Card(index, c + n, i / 4));
                i++;
                index++;
            }
        }
        staticCards.add(new Card(index, "小王", 99));
        staticCards.add(new Card(index + 1, "大王", 100));
    }
    private static final String CURRENT_ROUND_TYPE_1 = "叫牌阶段";
    private static final String CURRENT_ROUND_TYPE_2 = "出牌阶段";
    private static final String CURRENT_ROUND_TYPE_3 = "结算阶段";

    private static final Long BASIC_BEAN = 1000L;// 基础豆子数

    private static final int PLAYER_TURNTIME_COUNTDOWN = 20; // 玩家每回合的计时器，单位秒

    // 因为websock不归spring管理，所以需要用ApplicationContextUtil来注入mapper
    private UserMapper userMapper = ApplicationContextUtil.getBean(UserMapper.class);
    private BeanHistoryMapper beanHistoryMapper = ApplicationContextUtil.getBean(BeanHistoryMapper.class);

    private final String roomId = UUID.randomUUID().toString();// 房间id

    private final List<Session> players = new CopyOnWriteArrayList<>();// 玩家session列表
    private final List<Session> absentPlayers = new CopyOnWriteArrayList<>();// 离线玩家session列表
    private List<Card> publicCards = new ArrayList<>();// 公共3张牌
    private List<String> publicCardsString = new ArrayList<>();// 公共3张牌的字符串表示
    private final Map<Session, List<Card>> hands = new ConcurrentHashMap<>();// 每个玩家的手牌
    private Map<Session, Integer> playersId = new ConcurrentHashMap<>();// 每个玩家的id
    private Map<Session, Long> usersId = new ConcurrentHashMap<>();// 每个玩家的数据库id
    private PlayerOperationInfoVO previousPlayerOperationInfoVO; // 上个玩家的操作信息为发送客户端所需的对象
    private PlayerOperationInfo previousNotPassedPlayerOperationInfo; // 上个未pass的玩家的操作信息
    private Session currentPlayer;// 当前玩家session
    private String currentRoundType = CURRENT_ROUND_TYPE_1;// 当前轮次类型
    private Integer landlordPlayerId;// 地主玩家id
    private Integer gameMultiplier = 1;// 游戏倍数

    // 构造方法
    public GameRoom(List<Session> players) {
        players.forEach(this.players::add);
    }

    // 初始房间
    public void initialize() {
        players.forEach(p -> {
            playersId.put(p, (Integer) p.getUserProperties().get("playerId"));
            usersId.put(p, (Long) p.getUserProperties().get("userId"));
        });
        // 打乱牌
        List<Card> cards = new ArrayList<>(staticCards);
        Collections.shuffle(cards);
        // 公共3张牌
        publicCards.add(cards.remove(0));
        publicCards.add(cards.remove(0));
        publicCards.add(cards.remove(0));
        publicCards.forEach(publicCard -> {
            publicCardsString.add(publicCard.getCardname());
        });
        // 给每个玩家发牌
        for (Session player : players) {
            List<Card> hand = new ArrayList<>();
            for (int i = 0; i < 17; i++) {
                hand.add(cards.remove(0));
            }
            hand.sort(Comparator.comparingInt(Card::getIndex));// 手牌排序根据牌的index
            hands.put(player, hand);// 放入hands中
        }
        // 设置第一个玩家为当前玩家
        currentPlayer = players.get(0);
        // 通知所有玩家游戏开始
        proceedToNextPlayer();
    }

    public void handleDisconnect(Session session) {
        // 取消当前定时任务
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel(false);
        }
        currentRoundType = CURRENT_ROUND_TYPE_3;
        Integer losePlayerId = playersId.get(session);
        Long loseUserId = usersId.get(session);
        usersId.remove(session);
        players.remove(session);

        Long bean = BASIC_BEAN * gameMultiplier;
        // 给loser用户扣除豆子
        userMapper.updatePlusBeanCount(loseUserId, -(bean * 2));
        Long currentBean = userMapper.selectBeanCountById(loseUserId);
        // 记录豆子变动
        beanHistoryMapper.insert(
                new BeanHistory(loseUserId, GameConstant.CHANGE_TYPE_GAME, -(bean * 2), currentBean,
                        LocalDateTime.now()));
        // 给winer用户加豆子发通知
        usersId.forEach((player, userId) -> {
            userMapper.updatePlusBeanCount(userId, bean);
            Long currentBean1 = userMapper.selectBeanCountById(userId);
            // 记录豆子变动
            beanHistoryMapper.insert(new BeanHistory(
                    userId, GameConstant.CHANGE_TYPE_GAME, bean, currentBean1,
                    LocalDateTime.now()));
            // 给winer用户发通知
            JSONObject msg = new JSONObject();
            msg.put("type", "gameOver");
            msg.put("currentRoundType", currentRoundType);
            msg.put("message", "玩家退出");
            JSONArray playersJSON = new JSONArray();
            JSONObject loserJsonObject = new JSONObject();
            loserJsonObject.put("playerId", losePlayerId);
            loserJsonObject.put("bean", -(bean * 2));
            playersJSON.add(loserJsonObject);
            usersId.forEach((p, winerId) -> {
                JSONObject winerJsonObject = new JSONObject();
                winerJsonObject.put("playerId", (int) p.getUserProperties().get("playerId"));
                winerJsonObject.put("bean", bean);
                playersJSON.add(winerJsonObject);
            });
            msg.put("players", playersJSON);
            try {
                player.getBasicRemote().sendText(msg.toJSONString());
            } catch (IOException e) {
            }
        });
        GameManager.removeGameRoom(roomId);
    }

    public void gameOverButNotCalled() {
        GameManager.removeGameRoom(roomId);
        // 取消当前定时任务
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel(false);
        }
        currentRoundType = CURRENT_ROUND_TYPE_3;
        JSONObject msg = new JSONObject();
        msg.put("type", "gameOver");
        msg.put("currentRoundType", currentRoundType);
        msg.put("message", "无人当地主");

        players.forEach(player -> {
            try {
                player.getBasicRemote().sendText(msg.toJSONString());
                player.close();
            } catch (IOException e) {
            }
        });

    }

    // 通知全部玩家游戏信息
    private void sendMesToAllPlayers() {
        players.forEach(p -> {
            JSONObject msg = new JSONObject();
            JSONArray players = new JSONArray();
            hands.forEach((player, hand) -> {
                JSONObject playerMsg = new JSONObject();
                playerMsg.put("playerId", player.getUserProperties().get("playerId"));// 放入玩家id
                // 判断是否是被通知玩家
                if (player.getUserProperties().get("playerId") == p.getUserProperties().get("playerId")) {
                    playerMsg.put("cards", Card.getAllValuesByMember(hand, Card.CARD_FILED_CARDNAME));// 是被通知玩家放入被通知玩家的手牌具体信息
                } else {
                    playerMsg.put("cards", hand.size());// 不是被通知玩家放入手牌数量
                }
                players.add(playerMsg);
            });
            msg.put("players", players);
            msg.put("type", "game");
            msg.put("currentRoundType", currentRoundType);
            msg.put("publicCards", publicCardsString); // 公共3张牌
            msg.put("currentPlayerId", currentPlayer.getUserProperties().get("playerId"));// 当前操作玩家的id
            msg.put("playerTurnTimeCountdown", PLAYER_TURNTIME_COUNTDOWN);// 玩家每回合的计时器，单位秒
            msg.put("previousPlayerOperationInfoVO", previousPlayerOperationInfoVO); // 上一个玩家操作信息
            msg.put("previousNotPassedPlayerOperationInfoPlayerId",
                    previousNotPassedPlayerOperationInfo != null ? previousNotPassedPlayerOperationInfo.getPlayerId()
                            : null); // 上个未pass的玩家的操作信息
            msg.put("landlordPlayerId", landlordPlayerId); // 地主玩家id
            msg.put("gameMultiplier", gameMultiplier); // 游戏倍率
            msg.put("basicBean", BASIC_BEAN); // 基础豆子数
            p.getAsyncRemote().sendText(msg.toJSONString());
        });

    }

    private ScheduledFuture<?> timerTask;
    private ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor();
    private int remainingSeconds = PLAYER_TURNTIME_COUNTDOWN;

    // 添加定时任务方法
    private void startTurnTimer() {
        remainingSeconds = PLAYER_TURNTIME_COUNTDOWN;
        timerTask = timerExecutor.scheduleAtFixedRate(() -> {
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                // 倒计时结束处理
                log.info("倒计时结束处理");
                handleTimeout();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    // 倒计时结束处理
    private void handleTimeout() {
        // 判断当前轮次类型
        if (currentRoundType.equals(CURRENT_ROUND_TYPE_1)) {
            // 叫牌阶段超时，自动pass不叫地主
            autoPass();
        } else if (currentRoundType.equals(CURRENT_ROUND_TYPE_2)) {
            // 出牌阶段超时，判断上个未pass玩家是否位当前回合玩家，如果是则自动出最小牌，否则pass
            if ((int) currentPlayer.getUserProperties().get("playerId") == previousNotPassedPlayerOperationInfo
                    .getPlayerId()) {
                autoPlaySmallestCard();
            } else {
                autoPass();
            }
        }
    }

    // 自动pass
    private void autoPass() {
        JSONObject operation = new JSONObject();
        operation.put("call", 0);
        operation.put("cards", null);
        handlePlayerOperation(currentPlayer, operation);
    }

    // 自动出一张最小牌
    private void autoPlaySmallestCard() {
        JSONObject operation = new JSONObject();
        operation.put("call", 1);
        List<Integer> cards = new ArrayList<>();
        cards.add(0);
        operation.put("cards", cards);
        handlePlayerOperation(currentPlayer, operation);
    }

    // 叫地主逻辑器
    private GameCallLandlordRuler gameCallLandlordRuler = new GameCallLandlordRuler();
    // 出牌逻辑器
    private GamePlayCardRuler gamePlayCardRuler = new GamePlayCardRuler();

    // 处理玩家操作信息
    // 加锁保证线程安全,防止玩家操作和超时操作同时进行
    public synchronized void handlePlayerOperation(Session session, JSONObject operation) {
        // 判断发来信息的玩家是否是当前玩家
        if (session != currentPlayer)
            return;
        // 接受玩家操作信息
        PlayerOperationInfoDTO playerOperationInfoDTO = JSON.toJavaObject(operation, PlayerOperationInfoDTO.class);// 转换为对象
        playerOperationInfoDTO.setPlayerId((int) session.getUserProperties().get("playerId"));// 设置玩家id因为前端json中没有playerId

        // 判断当前轮次类型
        if (currentRoundType.equals(CURRENT_ROUND_TYPE_1)) {
            log.info("叫牌阶段");
            // 叫牌阶段
            // 处理叫地主逻辑
            gameCallLandlordRuler.handlePlayerCall(this, playerOperationInfoDTO);
            previousPlayerOperationInfoVO = new PlayerOperationInfoVO();
            previousPlayerOperationInfoVO.setCall(playerOperationInfoDTO.getCall());
            previousPlayerOperationInfoVO.setPlayerId(playerOperationInfoDTO.getPlayerId());
            // 判断是否有地主
            if (landlordPlayerId != null) {
                log.info("地主玩家id为：" + landlordPlayerId);
                // 有地主了，进入出牌阶段
                currentRoundType = CURRENT_ROUND_TYPE_2;
                previousNotPassedPlayerOperationInfo = new PlayerOperationInfo();
                previousNotPassedPlayerOperationInfo.setPlayerId(landlordPlayerId);
                // 为地主加三张牌
                List<Card> landlordCards = new ArrayList<>(hands.get(players.get(landlordPlayerId)));
                landlordCards.addAll(publicCards);
                landlordCards.sort(Comparator.comparingInt(Card::getIndex));// 手牌排序根据牌的index
                hands.put(players.get(landlordPlayerId), landlordCards);// 放入hands中
                gameCallLandlordRuler = null;// 清空叫地主逻辑器
            }
        } else if (currentRoundType.equals(CURRENT_ROUND_TYPE_2)) {
            log.info("出牌阶段");
            // 出牌阶段
            // 处理出牌逻辑,如果无效出牌直接return不反应

            // 判断当前玩家pass了但是上个未pass玩家是不是当前玩家.如果是不让pass
            if (playerOperationInfoDTO.getCall() == false
                    && previousNotPassedPlayerOperationInfo.getPlayerId() == playerOperationInfoDTO.getPlayerId())
                return;
            // 如果出牌
            if (playerOperationInfoDTO.getCall() == true) {
                // 送个handlePlayerPlayCard方法处理出牌逻辑
                if (!gamePlayCardRuler.handlePlayerPlayCard(this, playerOperationInfoDTO)) {
                    return;
                }
                // 出完判断手牌空没
                if (hands.get(currentPlayer).size() == 0) {
                    sendMesToAllPlayers();
                    // 结算阶段
                    gameOver();
                    return;
                }
            } else {
                previousPlayerOperationInfoVO = new PlayerOperationInfoVO();
                previousPlayerOperationInfoVO.setCall(playerOperationInfoDTO.getCall());
                previousPlayerOperationInfoVO.setPlayerId(playerOperationInfoDTO.getPlayerId());
                setPreviousPlayerOperationInfoVO(previousPlayerOperationInfoVO);
            }

            // 设置下一位玩家为当前玩家
            setCurrentPlayer(getNextPlayerSession(currentPlayer, getPlayers()));
        }

        // 取消当前定时任务
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel(false);
        }
        log.info("玩家：{},call:{},出牌组合:{},出牌:{}", previousPlayerOperationInfoVO.getPlayerId(),
                previousPlayerOperationInfoVO.getCall(), previousPlayerOperationInfoVO.getCardCombo(),
                previousPlayerOperationInfoVO.getCards());
        // 继续下一轮并启动新定时器
        proceedToNextPlayer();
    }

    private void gameOver() {
        GameManager.removeGameRoom(roomId);
        // 取消当前定时任务
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel(false);
        }
        currentRoundType = CURRENT_ROUND_TYPE_3;

        Integer WinnerId = (int) currentPlayer.getUserProperties().get("playerId");
        Long WinnerUserId = usersId.get(currentPlayer);

        Long bean = BASIC_BEAN * gameMultiplier;

        if (WinnerId == landlordPlayerId) {
            // 地主获胜
            // 给地主加豆子
            userMapper.updatePlusBeanCount(WinnerUserId, bean * 2);
            Long currentBean = userMapper.selectBeanCountById(WinnerUserId);
            beanHistoryMapper.insert(new BeanHistory(WinnerUserId, GameConstant.CHANGE_TYPE_GAME, bean * 2, currentBean,
                    LocalDateTime.now()));
            playersId.remove(currentPlayer);
            usersId.remove(currentPlayer);
            JSONArray playersJSON = new JSONArray();
            JSONObject winnerJsonObject = new JSONObject();
            winnerJsonObject.put("playerId", (int) currentPlayer.getUserProperties().get("playerId"));
            winnerJsonObject.put("bean", bean * 2);
            playersJSON.add(winnerJsonObject);
            // 给农民扣除豆子
            usersId.forEach((player, userId) -> {
                userMapper.updatePlusBeanCount(userId, -(bean));
                Long currentBean1 = userMapper.selectBeanCountById(userId);
                // 记录豆子变动
                beanHistoryMapper.insert(new BeanHistory(userId, GameConstant.CHANGE_TYPE_GAME, -(bean), currentBean1,
                        LocalDateTime.now()));
                JSONObject loserJsonObject = new JSONObject();
                loserJsonObject.put("playerId", (int) player.getUserProperties().get("playerId"));
                loserJsonObject.put("bean", -(bean));
                playersJSON.add(loserJsonObject);
            });
            JSONObject msg = new JSONObject();
            msg.put("type", "gameOver");
            msg.put("currentRoundType", currentRoundType);
            msg.put("message", "地主获胜");
            msg.put("players", playersJSON);
            players.forEach(p -> {
                try {
                    p.getBasicRemote().sendText(msg.toJSONString());
                    p.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else {
            // 农民获胜
            Integer loserPlayerId = landlordPlayerId;
            Long loserUserId = usersId.get(players.get(landlordPlayerId));
            Session loserSession = players.get(landlordPlayerId);
            // 给地主扣除豆子
            userMapper.updatePlusBeanCount(loserUserId, -(bean * 2));
            Long currentBean = userMapper.selectBeanCountById(loserUserId);
            beanHistoryMapper.insert(new BeanHistory(
                    loserUserId, GameConstant.CHANGE_TYPE_GAME, -(bean * 2), currentBean,
                    LocalDateTime.now()));

            playersId.remove(loserSession);
            usersId.remove(loserSession);

            JSONArray playersJSON = new JSONArray();
            JSONObject loserJsonObject = new JSONObject();
            loserJsonObject.put("playerId", loserPlayerId);
            loserJsonObject.put("bean", -(bean * 2));
            playersJSON.add(loserJsonObject);
            // 给农民加豆子
            usersId.forEach((player, userId) -> {
                userMapper.updatePlusBeanCount(userId, bean);
                Long currentBean1 = userMapper.selectBeanCountById(userId);
                // 记录豆子变动
                beanHistoryMapper.insert(new BeanHistory(userId, GameConstant.CHANGE_TYPE_GAME, bean, currentBean1,
                        LocalDateTime.now()));
                JSONObject winnerJsonObject = new JSONObject();
                winnerJsonObject.put("playerId", (int) player.getUserProperties().get("playerId"));
                winnerJsonObject.put("bean", bean);
                playersJSON.add(winnerJsonObject);
            });
            JSONObject msg = new JSONObject();
            msg.put("type", "gameOver");
            msg.put("currentRoundType", currentRoundType);
            msg.put("message", "农民获胜");
            msg.put("players", playersJSON);
            players.forEach(p -> {
                try {
                    p.getBasicRemote().sendText(msg.toJSONString());
                    p.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        }
    }

    // 获取下一位玩家session
    private Session getNextPlayerSession(Session currentPlayer, List<Session> players) {
        int currentIndex = players.indexOf(currentPlayer);// 当前玩家的索引
        int nextIndex = currentIndex + 1;// 下一位玩家的索引
        if (currentIndex == players.size() - 1) {
            // 如果当前玩家是最后一位玩家，则从下一位玩家索引设置0
            nextIndex = 0;
        }
        // 另外这里的bannedCallers最多只能一名玩家，因为有两名则在外层判断了不会进来
        return players.get(nextIndex);
    }

    private void proceedToNextPlayer() {
        // 通知所有对局信息
        sendMesToAllPlayers();
        // 启动新定时器并设置玩家没有超时
        startTurnTimer();
    }
}

// 叫地主逻辑器
@Slf4j
class GameCallLandlordRuler {
    private final List<Session> bannedCallers = new CopyOnWriteArrayList<>();
    private Session theFirestCaller;
    private Session thePreviousCaller;

    // 处理玩家叫地主逻辑,如果有地主出现,设置地主玩家id和当前玩家为地主session,否则设置下一位玩家为当前玩家供下一轮操作
    public void handlePlayerCall(GameRoom gameRoom, PlayerOperationInfoDTO playerOperationInfoDTO) {
        // 获得当前操作玩家的session
        Session player = gameRoom.getPlayers().get(playerOperationInfoDTO.getPlayerId());
        log.info("处理玩家叫地主逻辑");
        log.info("玩家id为：" + (int) player.getUserProperties().get("playerId"));
        log.info("叫牌状态为：" + playerOperationInfoDTO.getCall());
        // 判断是否叫牌
        if (playerOperationInfoDTO.getCall() == true) {
            // 叫牌逻辑...
            gameRoom.setGameMultiplier(gameRoom.getGameMultiplier() * 2);// 有玩家叫地主,游戏倍率翻倍
            // 如果叫牌玩家有两位，则当前玩家为地主
            if (bannedCallers.size() == gameRoom.getPlayers().size() - 1) {
                gameRoom.setLandlordPlayerId((int) player.getUserProperties().get("playerId"));// 设置地主玩家id
                return;
            }

            // 如果第一个叫地主玩家为自己着，则设置地主玩家id
            if (theFirestCaller == player) {
                gameRoom.setLandlordPlayerId((int) player.getUserProperties().get("playerId"));// 设置地主玩家id
                return;
            }
            // 设置第一个叫地主的玩家
            if (theFirestCaller == null) {
                theFirestCaller = player;
            }
            // 设置上个叫牌的玩家为当前玩家为下一位操作玩家进行判断
            thePreviousCaller = player;
            // 设置下一位玩家为当前玩家
            gameRoom.setCurrentPlayer(getNextPlayerSession(player, gameRoom.getPlayers(), bannedCallers));
        } else {
            bannedCallers.add(player);
            // 当全部玩家不叫地主，则游戏结束
            if (bannedCallers.size() >= gameRoom.getPlayers().size()) {
                log.info("游戏结束");
                gameRoom.gameOverButNotCalled();
                return;
            }
            // 当第一个叫牌玩家为当前玩家且不叫地主，设置上个叫牌玩家为地主和当前玩家为地主玩家
            if (theFirestCaller == player) {
                gameRoom.setLandlordPlayerId((int) thePreviousCaller.getUserProperties().get("playerId"));// 设置地主玩家id
                gameRoom.setCurrentPlayer(thePreviousCaller);
                return;
            }
            // 判断是否有两位玩家不叫地主
            if (bannedCallers.size() == gameRoom.getPlayers().size() - 1) {
                // 有两位玩家不叫地主,判断是否有一位玩家叫了地主,是则设置地主玩家id,否则设置下一位玩家为当前玩家
                if (thePreviousCaller != null) {
                    gameRoom.setLandlordPlayerId((int) thePreviousCaller.getUserProperties().get("playerId"));// 设置地主玩家id
                    gameRoom.setCurrentPlayer(thePreviousCaller);
                    return;
                }
            }
            // 设置下一位玩家为当前玩家
            gameRoom.setCurrentPlayer(getNextPlayerSession(player, gameRoom.getPlayers(), bannedCallers));
        }
    }

    // 获取下一位玩家session通过bannedCallers判断是否被禁止叫牌
    private Session getNextPlayerSession(Session currentPlayer, List<Session> players, List<Session> bannedCallers) {
        int currentIndex = players.indexOf(currentPlayer);// 当前玩家的索引
        int nextIndex = currentIndex + 1;// 下一位玩家的索引
        if (currentIndex == players.size() - 1) {
            // 如果当前玩家是最后一位玩家，则从下一位玩家索引设置0
            nextIndex = 0;
        }
        if (bannedCallers.size() > 0 && (int) bannedCallers.get(0).getUserProperties().get("playerId") == nextIndex) {
            // 如果下一位玩家是被禁止叫牌的玩家，则从将索引+1
            nextIndex++;
        }
        // 另外这里的bannedCallers最多只能一名玩家，因为有两名则在外层判断了不会进来
        return players.get(nextIndex);
    }
}

// 出牌逻辑器
@Slf4j
class GamePlayCardRuler {
    // 处理玩家出牌逻辑
    public boolean handlePlayerPlayCard(GameRoom gameRoom, PlayerOperationInfoDTO operation) {
        Session player = gameRoom.getPlayers().get(operation.getPlayerId());
        List<Card> hand = gameRoom.getHands().get(player);

        // 1. 转换下标为实际卡牌
        List<Card> playedCards = convertIndexesToCards(operation.getCards(), hand);
        if (playedCards == null) {
            log.warn("无效的牌索引");
            return false;
        }

        // 2. 验证基本规则
        if (!validateBasicRules(playedCards, hand)) {
            log.warn("包含无效或重复的卡牌");
            return false;
        }

        // 3. 分析牌型
        CardCombo currentCombo = analyzeCardCombo(playedCards);
        if (currentCombo == null) {
            log.warn("不合法的牌型");
            return false;
        }

        // 4. 与上家比较
        PlayerOperationInfo lastValid = gameRoom.getPreviousNotPassedPlayerOperationInfo();
        // 如果上家是自己，则上家无效
        if (lastValid != null && lastValid.getPlayerId() == operation.getPlayerId()) {
            lastValid = null;
        }
        if (lastValid != null && !currentCombo.canBeat(lastValid.getCardCombo())) {
            log.warn("不能压过上家的牌");
            return false;
        }

        // 5. 更新游戏状态
        updateGameState(gameRoom, player, playedCards, currentCombo);
        return true;
    }

    // 将下标转换为实际卡牌（带校验）
    private List<Card> convertIndexesToCards(List<Integer> indexes, List<Card> hand) {
        List<Card> cards = new ArrayList<>();
        try {
            for (Integer index : indexes) {
                if (index < 0 || index >= hand.size())
                    return null;
                cards.add(hand.get(index));
            }
            return cards;
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    // 基本规则校验
    private boolean validateBasicRules(List<Card> playedCards, List<Card> hand) {
        // 检查是否包含重复卡牌
        Set<Card> cardSet = new HashSet<>(playedCards);
        if (cardSet.size() != playedCards.size())
            return false;

        // 检查是否全部属于当前手牌
        return new HashSet<>(hand).containsAll(cardSet);
    }

    // 牌型分析
    private CardCombo analyzeCardCombo(List<Card> cards) {
        cards.sort(Comparator.comparingInt(Card::getCardvalue));

        // 判断特殊牌型
        if (isRocket(cards))
            return new CardCombo(ComboType.ROCKET, 100);
        if (isBomb(cards))
            return new CardCombo(ComboType.BOMB, cards.get(0).getCardvalue());

        // 四带牌型判断（在炸弹判断之后）
        CardCombo quadrupletCombo = checkQuadrupletWithCards(cards);
        if (quadrupletCombo != null) {
            return quadrupletCombo;
        }

        CardCombo tripletWithSingle = checkTripletWithSingle(cards);
        if (tripletWithSingle != null)
            return tripletWithSingle;

        CardCombo tripletWithPair = checkTripletWithPair(cards);
        if (tripletWithPair != null)
            return tripletWithPair;

        CardCombo tripletCombo = checkTripletStraight(cards);
        if (tripletCombo != null) {
            return tripletCombo;
        }

        // 连对判断（需要优先于单顺判断）
        PairStraightType pairStraight = checkPairStraight(cards);
        if (pairStraight != null) {
            return new CardCombo(pairStraight.comboType, cards.get(0).getCardvalue());
        }

        // 单顺判断
        StraightType straight = checkStraight(cards);
        if (straight != null) {
            return new CardCombo(straight.comboType, cards.get(0).getCardvalue());
        }

        // 常规牌型判断
        switch (cards.size()) {
            case 1:
                return new CardCombo(ComboType.SINGLE, cards.get(0).getCardvalue());
            case 2:
                return isPair(cards) ? new CardCombo(ComboType.PAIR, cards.get(0).getCardvalue()) : null;
            case 3:
                return isTriplet(cards) ? new CardCombo(ComboType.TRIPLET, cards.get(0).getCardvalue()) : null;
            case 4:
                if (isTripletWithSingle(cards)) {
                    return new CardCombo(ComboType.TRIPLET_WITH_SINGLE, getTripletValue(cards));
                }
            case 5:
                if (isTripletWithPair(cards)) {
                    return new CardCombo(ComboType.TRIPLET_WITH_PAIR, getTripletValue(cards));
                }
                break;
            // 可继续扩展更多牌型...
        }
        return null;
    }

    // 四带牌型验证
    private CardCombo checkQuadrupletWithCards(List<Card> cards) {
        // 基础校验
        if (cards.size() != 6 && cards.size() != 8)
            return null;

        // 查找四张组
        Map<Integer, List<Card>> valueMap = cards.stream()
                .collect(Collectors.groupingBy(Card::getCardvalue));

        // 获取所有可能四张组
        List<Integer> quadrupletValues = valueMap.entrySet().stream()
                .filter(e -> e.getValue().size() == 4)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (quadrupletValues.isEmpty())
            return null;

        // 分离四张组和带牌
        List<Card> quadruplet = valueMap.get(quadrupletValues.get(0));
        List<Card> supplements = cards.stream()
                .filter(c -> c.getCardvalue() != quadrupletValues.get(0))
                .collect(Collectors.toList());

        // 带牌验证
        if (cards.size() == 6) {
            if (supplements.size() == 2 &&
                    supplements.get(0).getCardvalue() != supplements.get(1).getCardvalue()) {
                return new CardCombo(ComboType.QUADRUPLET_WITH_SINGLE2, quadruplet.get(0).getCardvalue());
            }
        } else if (cards.size() == 8) {
            Map<Integer, Long> suppCount = supplements.stream()
                    .collect(Collectors.groupingBy(Card::getCardvalue, Collectors.counting()));
            if (suppCount.values().stream().allMatch(c -> c == 2) && suppCount.size() == 2) {
                return new CardCombo(ComboType.QUADRUPLET_WITH_PAIR2, quadruplet.get(0).getCardvalue());
            }
        }
        return null;
    }

    // 三顺带单验证（例如：333444+76）
    private CardCombo checkTripletWithSingle(List<Card> cards) {
        int totalGroups = getPossibleTripletGroups(cards.size(), true);
        if (totalGroups < 2 || totalGroups > 5)
            return null;

        // 分离三顺和单牌
        TripletStructure structure = extractTripletStructure(cards, totalGroups, true);
        if (structure != null && isValidSingleSupplement(structure)) {
            return new CardCombo(
                    getTripletWithSingleType(totalGroups),
                    structure.tripletStartValue);
        }
        return null;
    }

    // 三顺带对验证（例如：333444+6688）
    private CardCombo checkTripletWithPair(List<Card> cards) {
        int totalGroups = getPossibleTripletGroups(cards.size(), false);
        if (totalGroups < 2 || totalGroups > 4)
            return null;

        // 分离三顺和对子
        TripletStructure structure = extractTripletStructure(cards, totalGroups, false);
        if (structure != null && isValidPairSupplement(structure)) {
            return new CardCombo(
                    getTripletWithPairType(totalGroups),
                    structure.tripletStartValue);
        }
        return null;
    }

    // 核心数据结构
    private static class TripletStructure {
        List<Card> triplets;
        List<Card> supplements;
        int tripletStartValue;
    }

    // 牌型映射方法
    private ComboType getTripletWithSingleType(int groupCount) {
        return switch (groupCount) {
            case 2 -> ComboType.TRIPLET_STRAIGHT2_WITH_SINGLE2;
            case 3 -> ComboType.TRIPLET_STRAIGHT3_WITH_SINGLE3;
            case 4 -> ComboType.TRIPLET_STRAIGHT4_WITH_SINGLE4;
            case 5 -> ComboType.TRIPLET_STRAIGHT5_WITH_SINGLE5;
            default -> null;
        };
    }

    private ComboType getTripletWithPairType(int groupCount) {
        return switch (groupCount) {
            case 2 -> ComboType.TRIPLET_STRAIGHT2_WITH_PAIR2;
            case 3 -> ComboType.TRIPLET_STRAIGHT3_WITH_PAIR3;
            case 4 -> ComboType.TRIPLET_STRAIGHT4_WITH_PAIR4;
            default -> null;
        };
    }

    // 添加缺失的方法实现
    private int getPossibleTripletGroups(int totalSize, boolean isSingle) {
        // 三顺带单牌数公式：3N + N = 4N → N = totalSize/4
        // 三顺带对牌数公式：3N + 2N = 5N → N = totalSize/5
        return isSingle ? totalSize / 4 : totalSize / 5;
    }

    // 三顺结构分离核心逻辑
    private TripletStructure extractTripletStructure(List<Card> cards, int groupCount, boolean isSingle) {
        Map<Integer, List<Card>> valueMap = cards.stream()
                .collect(Collectors.groupingBy(Card::getCardvalue));

        // 找出所有可能的三张组
        List<Map.Entry<Integer, List<Card>>> triplets = valueMap.entrySet().stream()
                .filter(e -> e.getValue().size() >= 3)
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        // 查找连续的三张组
        List<Integer> sequence = new ArrayList<>();
        for (int i = 0; i < triplets.size() - groupCount + 1; i++) {
            List<Integer> currentSequence = triplets.subList(i, i + groupCount).stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            if (isConsecutive(currentSequence)) {
                sequence = currentSequence;
                break;
            }
        }

        if (sequence.isEmpty())
            return null;

        // 构建结构体
        TripletStructure structure = new TripletStructure();
        structure.triplets = sequence.stream()
                .flatMap(v -> valueMap.get(v).stream().limit(3))
                .collect(Collectors.toList());
        structure.tripletStartValue = sequence.get(0);

        // 收集剩余牌
        List<Card> remaining = new ArrayList<>(cards);
        remaining.removeAll(structure.triplets);
        structure.supplements = remaining;

        return structure;
    }

    // 连续数值校验
    private boolean isConsecutive(List<Integer> values) {
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) - values.get(i - 1) != 1) {
                return false;
            }
        }
        return true;
    }

    // 补充校验方法
    private boolean isValidSingleSupplement(TripletStructure structure) {
        // 带牌数量应等于组数且全部为单张
        return structure.supplements.size() == structure.triplets.size() / 3;
    }

    private boolean isValidPairSupplement(TripletStructure structure) {
        // 验证带牌全部成对
        Map<Integer, Long> countMap = structure.supplements.stream()
                .collect(Collectors.groupingBy(Card::getCardvalue, Collectors.counting()));
        return countMap.values().stream().allMatch(c -> c == 2);
    }

    // 三顺验证方法
    private CardCombo checkTripletStraight(List<Card> cards) {
        // 牌数必须为3的倍数且至少6张（2组）
        if (cards.size() < 6 || cards.size() % 3 != 0)
            return null;
        int groupCount = cards.size() / 3;
        if (groupCount < 2 || groupCount > 6)
            return null;

        // 分组统计
        Map<Integer, Long> countMap = cards.stream()
                .collect(Collectors.groupingBy(Card::getCardvalue, Collectors.counting()));

        // 验证所有牌都是3张且数值连续
        List<Integer> orderedValues = countMap.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        // 检查每个值都是3张且连续
        if (orderedValues.stream().allMatch(v -> countMap.get(v) == 3) &&
                isConsecutiveSequence(orderedValues) &&
                !containsInvalidStraightCards(orderedValues)) {
            ComboType comboType = getTripletStraightType(groupCount);
            return comboType != null ? new CardCombo(comboType, orderedValues.get(0)) : null;
        }
        return null;
    }

    // 三顺类型映射
    private ComboType getTripletStraightType(int groupCount) {
        return switch (groupCount) {
            case 2 -> ComboType.TRIPLET_STRAIGHT2; // 2组：333444
            case 3 -> ComboType.TRIPLET_STRAIGHT3; // 3组：333444555
            case 4 -> ComboType.TRIPLET_STRAIGHT4; // 4组
            case 5 -> ComboType.TRIPLET_STRAIGHT5; // 5组
            case 6 -> ComboType.TRIPLET_STRAIGHT6; // 6组
            default -> null;
        };
    }

    // 顺子验证逻辑
    private StraightType checkStraight(List<Card> cards) {
        int size = cards.size();
        if (size < 5 || size > 13)
            return null;

        // 去重后排序
        List<Integer> distinctValues = cards.stream()
                .map(Card::getCardvalue)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // 检测是否连续
        if (distinctValues.size() == size &&
                isConsecutiveSequence(distinctValues) &&
                !containsInvalidStraightCards(distinctValues)) {
            return new StraightType(getStraightComboType(size), distinctValues.get(0));
        }
        return null;
    }

    // 连对验证逻辑
    private PairStraightType checkPairStraight(List<Card> cards) {
        int pairCount = cards.size() / 2;
        if (pairCount < 3 || pairCount > 10 || cards.size() % 2 != 0)
            return null;

        // 分组统计
        Map<Integer, Long> countMap = cards.stream()
                .collect(Collectors.groupingBy(Card::getCardvalue, Collectors.counting()));

        // 验证全部成对且连续
        List<Integer> orderedValues = countMap.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        if (orderedValues.stream().allMatch(v -> countMap.get(v) == 2) &&
                isConsecutiveSequence(orderedValues) &&
                !containsInvalidStraightCards(orderedValues)) {
            return new PairStraightType(getPairStraightComboType(pairCount), orderedValues.get(0));
        }
        return null;
    }

    // 连续数值检测
    private boolean isConsecutiveSequence(List<Integer> values) {
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) - values.get(i - 1) != 1) {
                return false;
            }
        }
        return true;
    }

    // 禁牌检测（不能含有2或王）
    private boolean containsInvalidStraightCards(List<Integer> values) {
        return values.stream().anyMatch(v -> v >= 13 || v == 99 || v == 100);
    }

    // 顺子类型匹配
    private ComboType getStraightComboType(int length) {
        return switch (length) {
            case 5 -> ComboType.STRAIGHT5;
            case 6 -> ComboType.STRAIGHT6;
            case 7 -> ComboType.STRAIGHT7;
            case 8 -> ComboType.STRAIGHT8;
            case 9 -> ComboType.STRAIGHT9;
            case 10 -> ComboType.STRAIGHT10;
            case 11 -> ComboType.STRAIGHT11;
            case 12 -> ComboType.STRAIGHT12;
            case 13 -> ComboType.STRAIGHT13;
            default -> null;
        };
    }

    // 连对类型匹配
    private ComboType getPairStraightComboType(int pairCount) {
        return switch (pairCount) {
            case 3 -> ComboType.PAIR_STRAIGHT3;
            case 4 -> ComboType.PAIR_STRAIGHT4;
            case 5 -> ComboType.PAIR_STRAIGHT5;
            case 6 -> ComboType.PAIR_STRAIGHT6;
            case 7 -> ComboType.PAIR_STRAIGHT7;
            case 8 -> ComboType.PAIR_STRAIGHT8;
            case 9 -> ComboType.PAIR_STRAIGHT9;
            case 10 -> ComboType.PAIR_STRAIGHT10;
            default -> null;
        };
    }

    // 辅助类型
    private static class StraightType {
        final ComboType comboType;

        StraightType(ComboType comboType, int startValue) {
            this.comboType = comboType;
        }
    }

    private static class PairStraightType {
        final ComboType comboType;

        PairStraightType(ComboType comboType, int startValue) {
            this.comboType = comboType;
        }
    }

    // 更新游戏状态
    private void updateGameState(GameRoom gameRoom, Session player, List<Card> playedCards, CardCombo combo) {
        // 移除已出的牌
        List<Card> newHand = gameRoom.getHands().get(player).stream()
                .filter(c -> !playedCards.contains(c))
                .collect(Collectors.toList());
        gameRoom.getHands().put(player, newHand);

        // 记录操作信息
        PlayerOperationInfo info = new PlayerOperationInfo();
        info.setPlayerId((int) player.getUserProperties().get("playerId"));
        info.setCall(true);
        info.setCards(playedCards);
        info.setCardCombo(combo);

        gameRoom.setPreviousNotPassedPlayerOperationInfo(info);
        // 记录上个玩家操作信息VO
        PlayerOperationInfoVO vo = new PlayerOperationInfoVO();
        vo.setPlayerId(info.getPlayerId());
        vo.setCall(info.getCall());
        List<String> cards = new ArrayList<>();
        for (Card card : info.getCards()) {
            cards.add(card.getCardname());
        }
        vo.setCards(cards);
        vo.setCardCombo(info.getCardCombo());
        gameRoom.setPreviousPlayerOperationInfoVO(vo);

        // 处理炸弹翻倍
        if (combo.getType() == ComboType.BOMB || combo.getType() == ComboType.ROCKET) {
            gameRoom.setGameMultiplier(gameRoom.getGameMultiplier() * 2);
        }

    }

    // 牌型判断辅助方法
    private boolean isRocket(List<Card> cards) {
        return cards.size() == 2 &&
                cards.get(0).getCardvalue() == 99 &&
                cards.get(1).getCardvalue() == 100;
    }

    private boolean isBomb(List<Card> cards) {
        return cards.size() == 4 &&
                cards.stream().allMatch(c -> c.getCardvalue() == cards.get(0).getCardvalue());
    }

    private boolean isPair(List<Card> cards) {
        return cards.size() == 2 &&
                cards.get(0).getCardvalue() == cards.get(1).getCardvalue();
    }

    private boolean isTriplet(List<Card> cards) {
        return cards.size() == 3 &&
                cards.stream().allMatch(c -> c.getCardvalue() == cards.get(0).getCardvalue());
    }

    private boolean isTripletWithSingle(List<Card> cards) {
        Map<Integer, Long> countMap = cards.stream()
                .collect(Collectors.groupingBy(Card::getCardvalue, Collectors.counting()));
        return countMap.containsValue(3L) && countMap.containsValue(1L);
    }

    private boolean isTripletWithPair(List<Card> cards) {
        if (cards.size() != 5) {
            return false;
        }
        Map<Integer, Long> countMap = cards.stream()
                .collect(Collectors.groupingBy(Card::getCardvalue, Collectors.counting()));

        // 需要满足两个条件：
        // 1. 包含一组3张和一组2张
        // 2. 没有其他牌型混合
        return countMap.values().contains(3L) &&
                countMap.values().contains(2L) &&
                countMap.size() == 2;
    }

    private int getTripletValue(List<Card> cards) {
        Map<Integer, Long> countMap = cards.stream()
                .collect(Collectors.groupingBy(Card::getCardvalue, Collectors.counting()));
        return countMap.entrySet().stream()
                .filter(e -> e.getValue() == 3)
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(-1);
    }
}

// 牌型定义
enum ComboType {
    SINGLE, // 单张
    PAIR, // 对子
    TRIPLET, // 三张
    TRIPLET_WITH_SINGLE, // 三带一
    TRIPLET_WITH_PAIR, // 三带一对
    STRAIGHT5, // 顺子5张
    STRAIGHT6, // 顺子6张
    STRAIGHT7, // 顺子7张
    STRAIGHT8, // 顺子8张
    STRAIGHT9, // 顺子9张
    STRAIGHT10, // 顺子10张
    STRAIGHT11, // 顺子11张
    STRAIGHT12, // 顺子12张
    STRAIGHT13, // 顺子13张
    PAIR_STRAIGHT3, // 连对顺子3对
    PAIR_STRAIGHT4, // 连对顺子4对
    PAIR_STRAIGHT5, // 连对顺子5对
    PAIR_STRAIGHT6, // 连对顺子6对
    PAIR_STRAIGHT7, // 连对顺子7对
    PAIR_STRAIGHT8, // 连对顺子8对
    PAIR_STRAIGHT9, // 连对顺子9对
    PAIR_STRAIGHT10, // 连对顺子10对
    TRIPLET_STRAIGHT2, // 三顺2组
    TRIPLET_STRAIGHT3, // 三顺3组
    TRIPLET_STRAIGHT4, // 三顺4组
    TRIPLET_STRAIGHT5, // 三顺5组
    TRIPLET_STRAIGHT6, // 三顺6组
    TRIPLET_STRAIGHT2_WITH_SINGLE2, // 三顺2组带单//例如:33344476
    TRIPLET_STRAIGHT3_WITH_SINGLE3, // 三顺3组带单//例如:444555666498
    TRIPLET_STRAIGHT4_WITH_SINGLE4, // 三顺4组带单//例如:3334445556663489
    TRIPLET_STRAIGHT5_WITH_SINGLE5, // 三顺5组带单
    TRIPLET_STRAIGHT2_WITH_PAIR2, // 三顺2组带对//例如:3334446688
    TRIPLET_STRAIGHT3_WITH_PAIR3, // 三顺3组带对//例如:333444555778899
    TRIPLET_STRAIGHT4_WITH_PAIR4, // 三顺4组带对
    QUADRUPLET_WITH_SINGLE2, // 四带二//例如:444479
    QUADRUPLET_WITH_PAIR2, // 四带二对//例如:44446699
    BOMB, // 炸弹
    ROCKET // 王炸
}

// 牌型组合
@Data
class CardCombo {
    private final ComboType type;
    private final int value; // 比较用值

    public boolean canBeat(CardCombo other) {
        if (this.type == ComboType.ROCKET)
            return true;
        if (this.type == ComboType.BOMB) {
            return other.type != ComboType.BOMB || this.value > other.value;
        }
        return this.type == other.type && this.value > other.value;
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
// 玩家操作信息
class PlayerOperationInfoDTO {
    private int playerId; // 玩家id
    // 上个玩家的操作判断// 如果是是叫牌阶段，true为叫牌，false为不叫牌// 如果是出牌阶段，true为出牌，false为不出牌
    private Boolean call;
    private List<Integer> cards;// 上个玩家出牌的牌
    private CardCombo cardCombo;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
// 玩家操作信息
class PlayerOperationInfoVO {
    private int playerId; // 玩家id
    // 上个玩家的操作判断// 如果是是叫牌阶段，true为叫牌，false为不叫牌// 如果是出牌阶段，true为出牌，false为不出牌
    private Boolean call;
    private List<String> cards;// 上个玩家出牌的牌
    private CardCombo cardCombo;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
// 玩家操作信息
class PlayerOperationInfo {
    private int playerId; // 玩家id
    // 上个玩家的操作判断// 如果是是叫牌阶段，true为叫牌，false为不叫牌// 如果是出牌阶段，true为出牌，false为不出牌
    private Boolean call;
    private List<Card> cards;// 上个玩家出牌的牌
    private CardCombo cardCombo;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
// 牌类
class Card {

    private Integer index;// 牌的排序位置
    private String cardname;// 牌的具体内容
    private int cardvalue;// 牌的数值大小

    public static final String CARD_FILED_CARDNAME = "cardname";
    public static final String CARD_FILED_CARDVALUE = "cardvalue";

    public static Object[] getAllValuesByMember(List<Card> cards, String memberName) {
        Object[] values = new Object[cards.size()];
        try {
            // 使用反射获取成员的getter方法
            Method method = Card.class
                    .getMethod("get" + memberName.substring(0, 1).toUpperCase() + memberName.substring(1));
            for (int i = 0; i < cards.size(); i++) {
                // 调用getter方法获取成员值
                values[i] = method.invoke(cards.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return values;
    }

}
