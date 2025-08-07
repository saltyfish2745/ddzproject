<template>
  <div class="ddz-container">
    <!-- 匹配动画 -->
    <div class="matching-overlay" v-if="gameState.isMatching">
      <div class="matching-circle">
        <div class="matching-glow"></div>
        <div class="matching-count">{{ gameState.matchingCount }}/3</div>
        <div class="matching-text">正在匹配</div>
      </div>
    </div>

    <!-- 游戏结算弹窗 -->
    <div class="game-over-overlay" v-if="gameState.gameOver">
      <div class="game-over-content">
        <h2 class="game-over-title">游戏结束</h2>
        <div class="game-over-message">{{ gameState.gameOverMessage }}</div>
        
        <!-- 显示玩家结算信息 -->
        <div class="settlement-players" v-if="gameState.settlementPlayers && gameState.settlementPlayers.length > 0">
          <div v-for="player in gameState.settlementPlayers" :key="player.playerId" class="settlement-player">
            <div class="settlement-avatar"
                :class="{ 
                  'landlord-avatar': player.playerId === gameState.landlordPlayerId && gameState.gameOverMessage !== '玩家退出',
                  'landlord-winner': player.playerId === gameState.landlordPlayerId && player.bean > 0 && gameState.gameOverMessage !== '玩家退出',
                  'landlord-loser': player.playerId === gameState.landlordPlayerId && player.bean < 0 && gameState.gameOverMessage !== '玩家退出',
                  'farmer-winner': player.playerId !== gameState.landlordPlayerId && player.bean > 0 && gameState.gameOverMessage !== '玩家退出',
                  'farmer-loser': player.playerId !== gameState.landlordPlayerId && player.bean < 0 && gameState.gameOverMessage !== '玩家退出',
                  'quit-player': gameState.gameOverMessage === '玩家退出' && player.bean < 0,
                  'compensation-player': gameState.gameOverMessage === '玩家退出' && player.bean > 0
                }"
                :style="{ backgroundColor: getAvatarColor(getPlayerName(player.playerId)) }">
              {{ getAvatarText(getPlayerName(player.playerId)) }}
              <div class="landlord-badge" v-if="player.playerId === gameState.landlordPlayerId && gameState.gameOverMessage !== '玩家退出'">地主</div>
              <div class="farmer-badge" v-if="player.playerId !== gameState.landlordPlayerId && gameState.gameOverMessage !== '玩家退出'">农民</div>
              <div class="quit-badge" v-if="gameState.gameOverMessage === '玩家退出' && player.bean < 0">退出</div>
              <div class="compensation-badge" v-if="gameState.gameOverMessage === '玩家退出' && player.bean > 0">补偿</div>
            </div>
            <div class="settlement-username">{{ getPlayerName(player.playerId) }}</div>
            <div class="settlement-bean" :class="{ 'gain': player.bean > 0, 'loss': player.bean < 0 }">
              {{ player.bean > 0 ? '+' : ''}}{{ player.bean }}
            </div>
          </div>
        </div>

        <el-button type="primary" @click="returnToLobby" class="return-button">
          返回大厅
        </el-button>
      </div>
    </div>

    <!-- 游戏倍率显示 -->
    <div class="multiplier-display" v-if="gameState.currentRoundType">
      <el-tag type="warning" size="large">基础：{{ gameState.basicBean }}&nbsp;&nbsp;&nbsp;&nbsp;倍率: {{ gameState.gameMultiplier }}x</el-tag>
    </div>
    
    <!-- 游戏阶段提示 -->
    <div class="round-type-display" v-if="gameState.currentRoundType">
      <el-tag :type="gameState.currentPlayerId === gameState.yourPlayerId ? 'success' : 'info'">
        {{ gameState.currentRoundType }} - 
        {{ gameState.currentPlayerId === gameState.yourPlayerId ? '您的回合' : `${getPlayerName(gameState.currentPlayerId)}的回合` }}
      </el-tag>
    </div>

    <!-- 游戏桌面 -->
    <div class="game-table">
      <!-- 公共牌区域 -->
      <div class="public-cards">
        <div 
          v-for="(card, index) in gameState.publicCards" 
          :key="index" 
          class="public-card"
          :style="{ 'z-index': index }">
          <img :src="getCardImage(card)" :alt="card" class="card-image">
        </div>
      </div>
      
      <!-- 倒计时显示 (中间) -->
      <div 
        class="countdown-display center-countdown" 
        v-if="gameState.currentPlayerId === gameState.yourPlayerId && gameState.playerTurnTimeCountdown > 0">
        {{ gameState.playerTurnTimeCountdown }}
      </div>

      <!-- 上家操作显示 -->
      <div class="operation-info prev-operation" v-if="gameState.prevOperationInfo && gameState.previousRoundType && gameState.prevPlayerId !== gameState.currentPlayerId">
        <div v-if="gameState.previousRoundType === '叫牌阶段'" class="operation-text">
          {{ gameState.prevOperationInfo.call === false ? '不叫' : '叫地主' }}
        </div>
        <div v-else-if="gameState.previousRoundType === '出牌阶段'">
          <template v-if="gameState.prevOperationInfo.cards && gameState.prevOperationInfo.cards.length > 0">
            <div class="operation-cards">
              <img 
                v-for="(card, index) in gameState.prevOperationInfo.cards" 
                :key="index"
                :src="getCardImage(card)"
                :alt="card"
                class="operation-card">
            </div>
          </template>
          <span v-else class="operation-text">不出</span>
        </div>
      </div>

      <!-- 本家操作显示 -->
      <div class="operation-info your-operation" v-if="gameState.yourOperationInfo && gameState.previousRoundType && gameState.yourPlayerId !== gameState.currentPlayerId">
        <div v-if="gameState.previousRoundType === '叫牌阶段'" class="operation-text">
          {{ gameState.yourOperationInfo.call === false ? '不叫' : '叫地主' }}
        </div>
        <div v-else-if="gameState.previousRoundType === '出牌阶段'">
          <template v-if="gameState.yourOperationInfo.cards && gameState.yourOperationInfo.cards.length > 0">
            <div class="operation-cards">
              <img 
                v-for="(card, index) in gameState.yourOperationInfo.cards" 
                :key="index"
                :src="getCardImage(card)"
                :alt="card"
                class="operation-card">
            </div>
          </template>
          <span v-else class="operation-text">不出</span>
        </div>
      </div>

      <!-- 下家操作显示 -->
      <div class="operation-info next-operation" v-if="gameState.nextOperationInfo && gameState.previousRoundType && gameState.nextPlayer.playerId !== gameState.currentPlayerId">
        <div v-if="gameState.previousRoundType === '叫牌阶段'" class="operation-text">
          {{ gameState.nextOperationInfo.call === false ? '不叫' : '叫地主' }}
        </div>
        <div v-else-if="gameState.previousRoundType === '出牌阶段'">
          <template v-if="gameState.nextOperationInfo.cards && gameState.nextOperationInfo.cards.length > 0">
            <div class="operation-cards">
              <img 
                v-for="(card, index) in gameState.nextOperationInfo.cards" 
                :key="index"
                :src="getCardImage(card)"
                :alt="card"
                class="operation-card">
            </div>
          </template>
          <span v-else class="operation-text">不出</span>
        </div>
      </div>
    </div>
    <!-- 你的操作区域 -->
    <div class="your-operation-area" v-if="gameState.currentPlayerId === gameState.yourPlayerId && gameState.currentPlayerId !== null">
      <!-- 叫牌阶段按钮 -->
      <div v-if="gameState.currentRoundType === '叫牌阶段'" class="call-buttons">
        <el-button type="success" @click="send({call: 1})">叫地主</el-button>
        <el-button type="danger" @click="send({call: 0})">不叫</el-button>
      </div>
      
      <!-- 出牌阶段按钮 -->
      <div v-if="gameState.currentRoundType === '出牌阶段'" class="play-buttons">
        <el-button type="primary" @click="playCards">出牌</el-button>
        <el-button 
          type="info" 
          @click="passPlay"
          v-if="(gameState.previousNotPassedPlayerOperationInfoPlayerId !== gameState.yourPlayerId) || gameState.previousNotPassedPlayerOperationInfoPlayerId !== gameState.currentPlayerId">
          不出
        </el-button>
      </div>
    </div>
    <!-- 本家卡牌区域 -->
    <div class="your-player-cards">
      <template v-if="gameState.yourPlayer &amp;&amp; Array.isArray(gameState.yourPlayer.cards)">
        <div 
          v-for="(card, index) in gameState.yourPlayer.cards" 
          :key="index" 
          class="your-player-card"
          :class="{ 'selected-card': selectedCards.includes(index) }"
          :style="{ 'z-index': index, 'transform': selectedCards.includes(index) ? 'translateY(-20px)' : '' }"
          @click="toggleCardSelection(index)">
          <img :src="getCardImage(card)" :alt="card" class="card-image">
        </div>
      </template>
    </div>
    <!-- 玩家区域 -->
      <!-- 当前玩家 (下方) -->
      <div class="player-area your-player" v-if="gameState.yourPlayer">
        <div 
          class="avatar"
          :class="{ 'landlord-avatar': gameState.landlordPlayerId === gameState.yourPlayerId }"
          :style="{ backgroundColor: getAvatarColor(gameState.yourPlayer.username) }">
          {{ getAvatarText(gameState.yourPlayer.username) }}
          <div class="landlord-badge" v-if="gameState.landlordPlayerId === gameState.yourPlayerId">地主</div>
          <div class="farmer-badge" v-if="gameState.currentRoundType === '出牌阶段' && gameState.landlordPlayerId !== gameState.yourPlayerId">农民</div>
        </div>
        <div class="username">{{ gameState.yourPlayer.username }}</div>
        <div class="bean-display">
          <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23FFD700'%3E%3Cpath d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z'/%3E%3Cpath d='M12 6c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm0 10c-2.21 0-4-1.79-4-4s1.79-4 4-4 4 1.79 4 4-1.79 4-4 4z'/%3E%3C/svg%3E" 
               alt="豆" 
               class="bean-icon">
          {{ gameState.yourPlayer.beanCount }}
        </div>
      </div>

      <!-- 上家 (左方) -->
      <div class="player-area prev-player" v-if="gameState.prevPlayer">
        <div class="prev-player-cards">
          <div class="opponent-cards">
            <img 
              v-if="typeof gameState.prevPlayer.cards === 'number'" 
              :src="getCardImage('卡牌背面')" 
              alt="卡牌背面" 
              class="card-back">
            <div class="card-count" v-if="typeof gameState.prevPlayer.cards === 'number'">
              {{ gameState.prevPlayer.cards }}
            </div>
          </div>
        </div>
        <div 
          class="avatar"
          :class="{ 'landlord-avatar': gameState.landlordPlayerId === gameState.prevPlayer.playerId }"
          :style="{ backgroundColor: getAvatarColor(gameState.prevPlayer.username) }">
          {{ getAvatarText(gameState.prevPlayer.username) }}
          <div class="landlord-badge" v-if="gameState.landlordPlayerId === gameState.prevPlayer.playerId">地主</div>
          <div class="farmer-badge" v-if="gameState.currentRoundType === '出牌阶段' && gameState.landlordPlayerId !== gameState.prevPlayer.playerId">农民</div>
        </div>
        <div class="username">{{ gameState.prevPlayer.username }}</div>
        <div class="bean-display">
          <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23FFD700'%3E%3Cpath d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z'/%3E%3Cpath d='M12 6c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm0 10c-2.21 0-4-1.79-4-4s1.79-4 4-4 4 1.79 4 4-1.79 4-4 4z'/%3E%3C/svg%3E" 
               alt="豆" 
               class="bean-icon">
          {{ gameState.prevPlayer.beanCount }}
        </div>
              <!-- 倒计时显示 (上家) -->
        <div 
         class="countdown-display prev-countdown" 
         v-if="gameState.currentPlayerId === gameState.prevPlayer?.playerId && gameState.playerTurnTimeCountdown > 0">
         {{ gameState.playerTurnTimeCountdown }}
       </div>
      </div>

      <!-- 下家 (右方) -->
      <div class="player-area next-player" v-if="gameState.nextPlayer">
        <div class="next-player-cards">
          <div class="opponent-cards">
            <img 
              v-if="typeof gameState.nextPlayer.cards === 'number'" 
              :src="getCardImage('卡牌背面')" 
              alt="卡牌背面" 
              class="card-back">
            <div class="card-count" v-if="typeof gameState.nextPlayer.cards === 'number'">
              {{ gameState.nextPlayer.cards }}
            </div>
          </div>
        </div>
        <div 
          class="avatar"
          :class="{ 'landlord-avatar': gameState.landlordPlayerId === gameState.nextPlayer.playerId }"
          :style="{ backgroundColor: getAvatarColor(gameState.nextPlayer.username) }">
          {{ getAvatarText(gameState.nextPlayer.username) }}
          <div class="landlord-badge" v-if="gameState.landlordPlayerId === gameState.nextPlayer.playerId">地主</div>
          <div class="farmer-badge" v-if="gameState.currentRoundType === '出牌阶段' && gameState.landlordPlayerId !== gameState.nextPlayer.playerId">农民</div>
        </div>
        <div class="username">{{ gameState.nextPlayer.username }}</div>
        <div class="bean-display">
          <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23FFD700'%3E%3Cpath d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z'/%3E%3Cpath d='M12 6c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm0 10c-2.21 0-4-1.79-4-4s1.79-4 4-4 4 1.79 4 4-1.79 4-4 4z'/%3E%3C/svg%3E" 
               alt="豆" 
               class="bean-icon">
          {{ gameState.nextPlayer.beanCount }}
        </div>
              <!-- 倒计时显示 (下家) -->
        <div 
          class="countdown-display next-countdown" 
         v-if="gameState.currentPlayerId === gameState.nextPlayer?.playerId && gameState.playerTurnTimeCountdown > 0">
         {{ gameState.playerTurnTimeCountdown }}
        </div>
      </div>
  </div>
</template>

<script>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useStore } from 'vuex';
import { useDdzWebSocket } from '@/ws/game/ddzWebSocket';
import { useRouter } from 'vue-router';

export default {
  setup() {
    const store = useStore();
    const router = useRouter();
    const token = computed(() => store.state.user.token);
    const selectedCards = ref([]);
    
    // 使用单一数据源管理玩家信息
    const gameState = ref({
      isMatching: false,
      matchingCount: 0,
      players: [],
      yourPlayerId: null,
      yourPlayer: null,
      prevPlayer: null,
      nextPlayer: null,
      avatarBgColor: '',
      publicCards: [],
      gameMultiplier: 1,
      basicBean: 0,
      currentRoundType: '',
      previousRoundType: '叫牌阶段',
      roundStageCounter: 0,
      currentPlayerId: null,
      playerTurnTimeCountdown: 0,
      previousPlayerOperationInfoVO: null,
      previousNotPassedPlayerOperationInfoPlayerId: null,
      landlordPlayerId: null,
      yourOperationInfo: null,
      prevOperationInfo: null,
      nextOperationInfo: null,
      gameOver: false,
      gameOverMessage: '',
      settlementPlayers: []
    });

    
    // 生成随机颜色
    const generateRandomColor = () => {
      const hue = Math.floor(Math.random() * 360);
      return `hsl(${hue}, 70%, 60%)`;
    };

    // 更新玩家信息
    const updatePlayers = (players, yourPlayerId) => {
      // 确保players是有效数组
      if (!Array.isArray(players) || players.length !== 3) {
        console.error('无效的玩家列表:', players);
        return;
      }

      // 确保yourPlayerId有效
      if (typeof yourPlayerId !== 'number' || yourPlayerId < 0 || yourPlayerId > 2) {
        console.error('无效的玩家ID:', yourPlayerId);
        return;
      }

      // 更新玩家列表，保留已有的username和beanCount
      gameState.value.players = players.map(p => {
        // 查找已有玩家信息
        const existingPlayer = gameState.value.players.find(ep => ep.playerId === p.playerId);
        
        return {
          playerId: p.playerId,
          username: p.username || (existingPlayer?.username || `玩家${p.playerId + 1}`),
          beanCount: typeof p.beanCount === 'number' ? p.beanCount : (existingPlayer?.beanCount || 0),
          cards: p.cards
        };
      });

      gameState.value.yourPlayerId = yourPlayerId;
      gameState.value.yourPlayer = gameState.value.players.find(p => p.playerId === yourPlayerId);
      
      // 计算上家和下家ID
      const nextId = (yourPlayerId + 1) % 3;
      const prevId = (yourPlayerId + 2) % 3;
      
      gameState.value.nextPlayer = gameState.value.players.find(p => p.playerId === nextId);
      gameState.value.prevPlayer = gameState.value.players.find(p => p.playerId === prevId);
      
      // 生成头像背景色
      gameState.value.avatarBgColor = generateRandomColor();
      
      console.log('更新后的游戏状态:', JSON.parse(JSON.stringify(gameState.value)));
    };

    // 返回大厅
    const returnToLobby = () => {
      router.push('/');
    };

    // WebSocket连接
    const { socket, send, close } = useDdzWebSocket({
      onMatching: (count) => {
        gameState.value.isMatching = true;
        gameState.value.matchingCount = count;
        console.log('匹配中, 当前人数:', count);
      },
      onMatchSuccess: (players, yourPlayerId) => {
        try {
          console.log('收到匹配成功消息:', { players, yourPlayerId });
          
          if (!players || !Array.isArray(players) || players.length !== 3) {
            throw new Error('无效的玩家列表');
          }
          
          if (typeof yourPlayerId !== 'number' || yourPlayerId < 0 || yourPlayerId > 2) {
            throw new Error('无效的玩家ID');
          }
          
          updatePlayers(players, yourPlayerId);
          gameState.value.isMatching = false;
          
          console.log('玩家匹配成功处理完成');
        } catch (error) {
          console.error('处理匹配成功消息时出错:', error);
          gameState.value.isMatching = false;
        }
      },
      onGame: (gameData) => {
        try {
          console.log('收到游戏状态更新:', gameData);
          if (!gameData || 
              !gameData.players || 
              !Array.isArray(gameData.players) || 
              gameData.players.length !== 3 ||
              typeof gameState.value.yourPlayerId !== 'number') {
            console.error('无效的游戏数据:', gameData);
            return;
          }
          
          updatePlayers(gameData.players, gameState.value.yourPlayerId);
          gameState.value.publicCards = gameData.publicCards || [];
          gameState.value.gameMultiplier = gameData.gameMultiplier || 1;
          gameState.value.basicBean = gameData.basicBean || 0;
          
          // 处理previousRoundType更新
          if (gameData.currentRoundType === '出牌阶段') {
            gameState.value.roundStageCounter += 1;
            if (gameState.value.roundStageCounter === 2) {
              gameState.value.previousRoundType = '出牌阶段';
            }
          } 
          
          gameState.value.currentRoundType = gameData.currentRoundType || '';
          gameState.value.currentPlayerId = gameData.currentPlayerId;
          gameState.value.playerTurnTimeCountdown = gameData.playerTurnTimeCountdown || 0;
          gameState.value.previousNotPassedPlayerOperationInfoPlayerId = gameData.previousNotPassedPlayerOperationInfoPlayerId || null;

          // 启动倒计时
          startCountdown();
          
          // 更新操作信息
          if (gameData.previousPlayerOperationInfoVO) {
            const opInfo = { ...gameData.previousPlayerOperationInfoVO };
            console.log('收到操作信息:', opInfo);
            
            // 保存最新的操作信息
            gameState.value.previousPlayerOperationInfoVO = opInfo;
            
            // 根据playerId更新对应的操作信息
            if (opInfo.playerId === gameState.value.yourPlayerId) {
              gameState.value.yourOperationInfo = opInfo;
            } else if (opInfo.playerId === gameState.value.prevPlayer?.playerId) {
              gameState.value.prevOperationInfo = opInfo;
            } else if (opInfo.playerId === gameState.value.nextPlayer?.playerId) {
              gameState.value.nextOperationInfo = opInfo;
            }
            
            console.log('更新后的操作信息:', {
              current: gameState.value.previousPlayerOperationInfoVO,
              your: gameState.value.yourOperationInfo,
              prev: gameState.value.prevOperationInfo,
              next: gameState.value.nextOperationInfo,
              roundType: gameState.value.previousRoundType
            });
          }
          
          gameState.value.landlordPlayerId = gameData.landlordPlayerId;
        } catch (error) {
          console.error('处理游戏状态更新时出错:', error);
        }
      },
      onConnected: () => {
        console.log('WebSocket连接成功');
      },
      onError: (error) => {
        console.error('WebSocket连接错误:', error);
      },
      onGameOver: (data) => {
        console.log('游戏结束:', data);
        gameState.value.gameOver = true;
        gameState.value.gameOverMessage = data.message;
        gameState.value.settlementPlayers = data.players || [];
        gameState.value.currentRoundType = data.currentRoundType;
      }
    });

    // 组件挂载时已自动连接
    onMounted(() => {
      console.log('组件挂载, 开始连接WebSocket');
    });

    // 倒计时定时器
    let countdownTimer = null;
    
    // 启动倒计时
    const startCountdown = () => {
      if (countdownTimer) clearInterval(countdownTimer);
      
      countdownTimer = setInterval(() => {
        if (gameState.value.playerTurnTimeCountdown > 0) {
          gameState.value.playerTurnTimeCountdown -= 1;
        } else {
          clearInterval(countdownTimer);
        }
      }, 1000);
    };
    
    // 组件卸载时断开连接
    onUnmounted(() => {
      console.log('组件卸载, 断开WebSocket连接');
      if (countdownTimer) clearInterval(countdownTimer);
      close();
    });
    
    // 获取卡牌图片路径
    const getCardImage = (cardName) => {
      if (!cardName) return '';
      return require(`@/assets/cards/${cardName}.png`);
    };

    // 获取玩家名称
    const getPlayerName = (playerId) => {
      const player = gameState.value.players.find(p => p.playerId === playerId);
      return player?.username || `玩家${playerId + 1}`;
    };

    const toggleCardSelection = (index) => {
      const idx = selectedCards.value.indexOf(index);
      if (idx === -1) {
        selectedCards.value.push(index);
      } else {
        selectedCards.value.splice(idx, 1);
      }
    };

    const playCards = () => {
      if (selectedCards.value.length === 0) {
        return;
      }
      send({
        call: 1,
        cards: [...selectedCards.value].sort((a, b) => a - b)
      });
      selectedCards.value = [];
    };

    const passPlay = () => {
      send({ call: 0 });
      selectedCards.value = [];
    };

    return {
      token,
      gameState,
      send,
      getPlayerName,
      selectedCards,
      toggleCardSelection,
      playCards,
      passPlay,
      getAvatarText: (username) => {
        if (!username) return '';
        const firstChar = username.charAt(0);
        return /[a-zA-Z]/.test(firstChar) ? firstChar.toUpperCase() : firstChar;
      },
      getAvatarColor: (username) => {
        if (!username) return '#3498db'; // 默认蓝色
        let hash = 0;
        for (let i = 0; i < username.length; i++) {
          hash = username.charCodeAt(i) + ((hash << 5) - hash);
        }
        const h = Math.abs(hash) % 360;
        return `hsl(${h}, 70%, 60%)`;
      },
      returnToLobby,
      getCardImage
    };
  },
  watch: {
    '$store.state.user.matchingStatus': {
      handler(newVal) {
        this.gameState.isMatching = newVal.isMatching;
        this.gameState.matchingCount = newVal.count;
      },
      immediate: true
    }
  },
  methods: {
    getAvatarText(username) {
      if (!username) return '';
      const firstChar = username.charAt(0);
      return /[a-zA-Z]/.test(firstChar) ? firstChar.toUpperCase() : firstChar;
    },
    getAvatarColor(username) {
      if (!username) return '#3498db'; // 默认蓝色
      // 基于用户名的稳定哈希算法生成颜色
      let hash = 0;
      for (let i = 0; i < username.length; i++) {
        hash = username.charCodeAt(i) + ((hash << 5) - hash);
      }
      const h = Math.abs(hash) % 360;
      return `hsl(${h}, 70%, 60%)`;
    }
  }
}
</script>

<style scoped>
/* 倒计时显示样式 */
.countdown-display {
  position: absolute;
  font-size: 24px;
  font-weight: bold;
  color: #ff5722;
  background: rgba(255, 255, 255, 0.8);
  padding: 8px 16px;
  border-radius: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.center-countdown {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.prev-countdown {
  bottom: -60px;
  left: 50%;
  transform: translateX(-50%);
}

.next-countdown {
  bottom: -60px;
  right: 50%;
  transform: translateX(50%);
}

.ddz-container {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #ffffff 0%, #e8f4ff 100%);
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  perspective: 1000px;
}

.ddz-container::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 20%, rgba(135, 206, 250, 0.1) 0%, transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(70, 130, 180, 0.1) 0%, transparent 40%),
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80' width='80' height='80'%3E%3Cpath fill='%234682b4' fill-opacity='0.03' d='M14 16H9v-2h5V9.87a4 4 0 1 1 2 0V14h5v2h-5v15.95A10 10 0 0 0 23.66 27l-3.46-2 8.2-2.2-2.9 5a12 12 0 0 1-21 0l-2.89-5 8.2 2.2-3.47 2A10 10 0 0 0 14 31.95V16zm40 40h-5v-2h5v-4.13a4 4 0 1 1 2 0V54h5v2h-5v15.95A10 10 0 0 0 63.66 67l-3.47-2 8.2-2.2-2.89 5a12 12 0 0 1-21 0l-2.89-5 8.2 2.2-3.47 2A10 10 0 0 0 54 71.95V56zm-39 6a2 2 0 1 1 0-4 2 2 0 0 1 0 4zm40-40a2 2 0 1 1 0-4 2 2 0 0 1 0 4zM15 8a2 2 0 1 0 0-4 2 2 0 0 0 0 4zm40 40a2 2 0 1 0 0-4 2 2 0 0 0 0 4z'%3E%3C/path%3E%3C/svg%3E");
  opacity: 0.8;
}

.game-table {
  width: 75%;
  height: 65%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 30px;
  position: absolute;
  top:13%;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.2);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.3);
  display: flex;
  justify-content: center;
  align-items: center;
}

@keyframes pulseAvatar {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes gradient {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.back-button {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 100;
  width: 100px;
  height: 40px;
  border-radius: 20px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  color: white;
  border: none;
  font-weight: bold;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.back-button:active {
  transform: translateY(0);
}

/* ===== 绝对定位的玩家区域样式 ===== */
.player-area {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 5;
}

.your-player {
  bottom: 20px;
  left: 10%;
  transform: translateX(-50%);
}

.prev-player {
  left: 20px;
  top: 40%;
  transform: translateY(-50%);
}

.next-player {
  right: 20px;
  top: 40%;
  transform: translateY(-50%);
}



.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 48px;
  color: white;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  margin-bottom: 16px;
  animation: pulseAvatar 2s infinite;
  user-select: none;
  font-family: '微软雅黑', 'Microsoft YaHei', sans-serif;
  border: 8px solid white;
  cursor: pointer;
  transition: transform 0.3s ease, background 0.5s ease;
  position: relative;
}

.landlord-avatar {
  box-shadow: 0 0 15px 5px rgba(255, 215, 0, 0.7);
}

.landlord-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #ff5722;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: bold;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.avatar:hover {
  transform: scale(1.05);
}

.username {
  font-size: 24px;
  font-weight: bold;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
  font-family: '微软雅黑', 'Microsoft YaHei', sans-serif;
  color: #34495e;
  margin-bottom: 8px;
  text-align: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bean-display {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: bold;
  color: #FFD700;
}

.bean-icon {
  width: 24px;
  height: 24px;
  animation: spin 10s linear infinite;
}

/* 游戏倍率显示 */
.multiplier-display {
  position: absolute;
  top: 20px;
  left: 50%;
  z-index: 100;
  justify-content: center;
  transform: translateX(-50%);
}

/* 游戏阶段提示 */
.round-type-display {
  position: absolute;
  top: 60px;
  left: 50%;
  z-index: 100;
  justify-content: center;
  transform: translateX(-50%);
  
}

/* 卡牌基础样式 */
.card {
  position: relative;
  width: 80px;
  height: 120px;
  margin: 0 -20px;
  transition: transform 0.3s ease;
}


.card-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.operation-card {
  width: 80px;
  height: 120px;
  margin: 0 -10px;
  object-fit: contain;
}

/* 操作信息显示 */
.operation-info {
  position: absolute;
  background: rgba(255, 255, 255, 0.95); /* 增加背景透明度 */
  padding: 15px; /* 增加内边距 */
  border-radius: 15px; /* 增加圆角 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); /* 增强阴影效果 */
  z-index: 10;
  display: flex;
  justify-content: center;
  align-items: center;
  min-width: 120px; /* 设置最小宽度 */
  min-height: 60px; /* 设置最小高度 */
}

.your-operation {
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
}

.prev-operation {
  left: 20px;
  top: 50%;
  transform: translateY(-50%);
}

.next-operation {
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
}

.operation-text {
  font-size: 24px; /* 增加字体大小 */
  font-weight: bold;
  color: #333;
  padding: 4px 12px; /* 添加内边距 */
  border-radius: 6px; /* 添加圆角 */
  background-color: rgba(255, 255, 255, 0.9); /* 添加背景色 */
}

.operation-cards {
  display: flex;
  gap: 5px;
}

/* 公共牌区域 */
.public-card {
  position: relative;
  width: 60px;
  height: 90px;
  margin: 0 -20px;
  transition: transform 0.3s ease;
}
.public-cards {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  z-index: 5;
}
/* 本家基础样式 */
.your-player-card {
  position: relative;
  width: 80px; 
  height: 120px; 
  margin: 0 -25px; /* 调整重叠间距 */
  transition: transform 0.3s ease;
  cursor: pointer;
}

.your-player-card:hover {
  transform: translateY(-10px) !important;
}

.selected-card {
  transform: translateY(-20px) !important;
  filter: drop-shadow(0 10px 5px rgba(0, 0, 0, 0.3));
}

/* 本家卡牌区域 */
.your-operation-area {
  position: absolute;
  bottom: 180px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.your-operation-area .call-buttons,
.your-operation-area .play-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.your-player-cards {
  position: absolute;
  bottom: 35px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  z-index: 100;
}

/* 对手卡牌区域 */
.opponent-cards {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.card-back {
  width: 80px;
  height: 120px;
  object-fit: contain;
  filter: drop-shadow(0 3px 6px rgba(0, 0, 0, 0.4)); /* 增强阴影效果 */
}

.card-count {
  position: absolute;
  top: -20px;
  color: black;
  font-weight: bold;
  text-shadow: 0 0 4px white;
  font-size: 21px;
}

/* 游戏结算样式 */
.game-over-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.game-over-content {
  width: 70%;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.3);
  text-align: center;
}

.game-over-title {
  font-size: 32px;
  color: #2c3e50;
  margin-bottom: 20px;
}

.game-over-message {
  font-size: 24px;
  color: #34495e;
  margin-bottom: 30px;
}

.settlement-players {
  display: flex;
  justify-content: space-around;
  margin: 30px 0;
}

.settlement-player {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.settlement-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 36px;
  color: white;
  margin-bottom: 10px;
  border: 4px solid white;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  position: relative;
}

.settlement-avatar.landlord-avatar {
  border: 8px solid white;
}

.settlement-avatar.landlord-winner {
  box-shadow: 0 0 15px 5px rgba(255, 215, 0, 0.7);
}

.settlement-avatar.landlord-loser {
  box-shadow: 0 0 15px 5px rgba(255, 255, 255, 0.7);
}

.settlement-avatar.farmer-winner {
  box-shadow: 0 0 15px 5px rgba(82, 196, 26, 0.7);
}

.settlement-avatar.farmer-loser {
  box-shadow: 0 0 15px 5px rgba(255, 255, 255, 0.7);
}

.settlement-avatar.quit-player {
  box-shadow: 0 0 15px 5px rgba(255, 0, 0, 0.7);
}

.settlement-avatar.compensation-player {
  box-shadow: 0 0 15px 5px rgba(82, 196, 26, 0.7);
}

.farmer-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #52c41a;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: bold;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.quit-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #f5222d;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: bold;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.compensation-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #52c41a;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: bold;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.settlement-username {
  font-size: 18px;
  color: #2c3e50;
  margin: 10px 0;
  font-weight: bold;
}

.settlement-bean {
  font-size: 24px;
  font-weight: bold;
  padding: 5px 15px;
  border-radius: 20px;
  margin-top: 5px;
}

.settlement-bean.gain {
  color: #27ae60;
  background: rgba(39, 174, 96, 0.1);
}

.settlement-bean.loss {
  color: #c0392b;
  background: rgba(192, 57, 43, 0.1);
}

.return-button {
  margin-top: 30px;
  padding: 12px 30px;
  font-size: 18px;
}

/* 匹配动画样式 */
.matching-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  backdrop-filter: blur(5px);
}

.matching-circle {
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  box-shadow: 0 0 30px rgba(52, 152, 219, 0.3);
  animation: pulse 2s infinite ease-in-out;
}

.matching-circle::before {
  content: '';
  position: absolute;
  width: 210px;
  height: 210px;
  border-radius: 50%;
  background: linear-gradient(135deg, 
    #3498db 0%, 
    #9b59b6 50%, 
    #e74c3c 100%);
  background-size: 400% 400%;
  animation: rotate 3s linear infinite, gradient 6s ease infinite;
  z-index: -1;
  filter: blur(10px);
  opacity: 0.8;
}

.matching-glow {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: conic-gradient(
    from 0deg,
    transparent,
    rgba(52, 152, 219, 0.8) 45deg,
    rgba(52, 152, 219, 0.3) 90deg,
    transparent 180deg
  );
  animation: rotate 2s linear infinite;
}

.matching-count {
  font-size: 48px;
  font-weight: bold;
  color: #2c3e50;
  z-index: 1;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 8px;
}

.matching-text {
  font-size: 24px;
  color: #34495e;
  z-index: 1;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    box-shadow: 0 0 30px rgba(52, 152, 219, 0.3);
  }
  50% {
    transform: scale(1.02);
    box-shadow: 0 0 50px rgba(52, 152, 219, 0.5);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 0 30px rgba(52, 152, 219, 0.3);
  }
}

</style>
