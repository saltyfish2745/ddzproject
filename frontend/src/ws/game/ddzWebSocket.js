import { WS_URL } from '@/config/url';
import { useStore } from 'vuex';

/**
 * 斗地主游戏WebSocket封装
 * @param {Object} options 配置选项
 * @param {Function} options.onMatching 匹配状态回调
 * @param {Function} options.onConnected 连接成功回调
 * @param {Function} options.onError 错误处理回调
 * @returns {Object} WebSocket相关方法和状态
 */
export function useDdzWebSocket(options) {
  const store = useStore();
  const token = store.state.user.token;

  // 创建WebSocket连接
  const socket = new WebSocket(`${WS_URL}/ddz?token=${token}`);

  // 连接成功
  socket.onopen = () => {
    options.onConnected?.();
  };

  // 处理错误
  socket.onerror = (error) => {
    options.onError?.(error);
  };

  // 处理消息
  socket.onmessage = (message) => {
    try {
      const msg = JSON.parse(message.data);
      
      if (!msg.type) {
        console.error('WebSocket消息缺少type字段:', msg);
        return;
      }

      switch(msg.type) {
        case 'matching':
          if (typeof msg.currentCount !== 'number') {
            console.error('无效的匹配人数:', msg);
            return;
          }
          options.onMatching?.(msg.currentCount);
          break;
          
        case 'match_success':
          if (!Array.isArray(msg.players) || typeof msg.yourPlayerId !== 'number') {
            console.error('无效的匹配成功消息:', msg);
            return;
          }
          options.onMatchSuccess?.(msg.players, msg.yourPlayerId);
          break;
          
        case 'game':
          if (!msg.players || !Array.isArray(msg.players) || 
              typeof msg.currentPlayerId !== 'number' ||
              !msg.publicCards || !Array.isArray(msg.publicCards) ||
              typeof msg.gameMultiplier !== 'number') {
            console.error('无效的游戏状态消息:', msg);
            return;
          }
          options.onGame?.(msg);
          break;

        case 'gameOver':
          if (!msg.currentRoundType || msg.currentRoundType !== '结算阶段') {
            console.error('无效的游戏结束消息:', msg);
            return;
          }
          options.onGameOver?.(msg);
          break;
          
        default:
          console.warn('未知的WebSocket消息类型:', msg.type);
      }
    } catch (error) {
      console.error('WebSocket消息解析失败:', error, '原始消息:', message.data);
    }
  };

  // 关闭连接
  const close = () => {
    socket.close();
  };

  // 发送消息
  const send = (data) => {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(data));
    }
  };

  return {
    socket,
    send,
    close
  };
}
