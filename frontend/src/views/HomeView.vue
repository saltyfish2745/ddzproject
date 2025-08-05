<template>
  <div class="home-container">
    <PageBeanHistoryView v-if="$route.path === '/pageBeanHistory'" />
    <div class="user-info">
      <div 
        class="avatar"
        @click="handleAvatarClick"
        title="点击更改头像">
        {{ computedAvatar }}
      </div>
      <div class="username">{{ userInfo.username }}</div>
    </div>
    <div class="top-left-buttons">
      <el-button 
        type="warning" 
        class="logout-button"
        round
        size="large"
        @click="handleLogout">
        退出登录
      </el-button>
    </div>
    <div class="top-right-buttons">
      <el-button 
        type="danger" 
        class="history-button"
        round
        size="large"
        @click="handleHistory">
        查询历史记录
      </el-button>
      <el-button 
        type="success" 
        class="clock-in-button"
        round
        size="large"
        @click="handleClockIn">
        每日签到
      </el-button>
    </div>
    
    <div class="match-button-container">
      <el-button 
        type="danger" 
        class="match-button"
        round
        size="large"
        @click="handleMatch">
        开始匹配
      </el-button>
    </div>

    <div class="bean-count">
      <el-tooltip
        content="欢乐豆数量"
        placement="top"
        :show-after="500">
        <div class="bean-display">
          <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23FFD700'%3E%3Cpath d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z'/%3E%3Cpath d='M12 6c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm0 10c-2.21 0-4-1.79-4-4s1.79-4 4-4 4 1.79 4 4-1.79 4-4 4z'/%3E%3C/svg%3E" 
               alt="豆" 
               class="bean-icon" />
          {{ userInfo.beanCount }}
        </div>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getViewUserInfo, getClockIn } from '../api/user'
import PageBeanHistoryView from './HomeViewChildren/PageBeanHistoryView.vue'

const router = useRouter()
const store = useStore()
const userInfo = ref({
  username: '',
  beanCount: 0
})

const avatarBgColor = ref('')

const generateRandomColor = () => {
  // 使用HSL颜色模型，固定饱和度和亮度，只随机色相
  const hue = Math.floor(Math.random() * 360)
  return `hsl(${hue}, 70%, 60%)`
}

const computedAvatar = computed(() => {
  if (!userInfo.value.username) return ''
  const firstChar = userInfo.value.username.charAt(0)
  avatarBgColor.value = generateRandomColor()
  return /[a-zA-Z]/.test(firstChar) ? firstChar.toUpperCase() : firstChar
})

const fetchUserInfo = async () => {
  try {
    const token = store.state.user.token
    if (!token) {
      ElMessage.error('未登录或登录已过期')
      router.push('/login')
      return
    }

    const res = await getViewUserInfo(token)
    if (res.data.code === 1 && res.data.data) {
      userInfo.value = res.data.data
    } else {
      ElMessage.error('获取用户信息失败')
    }
  } catch (error) {
    ElMessage.error('网络错误')
  }
}

const handleMatch = () => {
  router.push('/ddz')
}

const handleHistory = () => {
  router.push('/pageBeanHistory')
}

const handleClockIn = async () => {
  try {
    const token = store.state.user.token
    if (!token) {
      ElMessage.error('请先登录')
      router.push('/login')
      return
    }

    const res = await getClockIn(token)
    if (res.data.code === 1) {
      ElMessage.success(res.data.msg || '签到成功')
      await fetchUserInfo() // 更新用户信息
    } else {
      ElMessage.error(res.data.msg || '签到失败')
    }
  } catch (error) {
    ElMessage.error('网络错误')
  }
}

const handleLogout = () => {
  store.commit('user/clearToken')
  router.push('/login')
  ElMessage.success('已退出登录')
}

const handleAvatarClick = () => {
  ElMessage.info({
    message: '头像更改功能开发中...<br/>等作者学会minIO',
    type: 'info',
    dangerouslyUseHTMLString: true // 设置为true表示使用HTML字符串
  })
}

onMounted(() => {
  fetchUserInfo()
  avatarBgColor.value = generateRandomColor()
})
</script>

<style scoped>
.home-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: linear-gradient(135deg, #ffffff 0%, #e8f4ff 100%);
  overflow: hidden;
  perspective: 1000px;
  position: relative;
  color: #34495e;
}

.home-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 20%, rgba(135, 206, 250, 0.1) 0%, transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(70, 130, 180, 0.1) 0%, transparent 40%),
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 80 80' width='80' height='80'%3E%3Cpath fill='%234682b4' fill-opacity='0.03' d='M14 16H9v-2h5V9.87a4 4 0 1 1 2 0V14h5v2h-5v15.95A10 10 0 0 0 23.66 27l-3.46-2 8.2-2.2-2.9 5a12 12 0 0 1-21 0l-2.89-5 8.2 2.2-3.47 2A10 10 0 0 0 14 31.95V16zm40 40h-5v-2h5v-4.13a4 4 0 1 1 2 0V54h5v2h-5v15.95A10 10 0 0 0 63.66 67l-3.47-2 8.2-2.2-2.89 5a12 12 0 0 1-21 0l-2.89-5 8.2 2.2-3.47 2A10 10 0 0 0 54 71.95V56zm-39 6a2 2 0 1 1 0-4 2 2 0 0 1 0 4zm40-40a2 2 0 1 1 0-4 2 2 0 0 1 0 4zM15 8a2 2 0 1 0 0-4 2 2 0 0 0 0 4zm40 40a2 2 0 1 0 0-4 2 2 0 0 0 0 4z'%3E%3C/path%3E%3C/svg%3E");
  opacity: 0.8;
}

.user-info {
  margin-top: 15vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.top-left-buttons {
  position: fixed;
  top: 40px;
  left: 40px;
  display: flex;
  flex-direction: row;
  gap: 15px;
  z-index: 10;
}

.top-right-buttons {
  position: fixed;
  top: 40px;
  right: 40px;
  display: flex;
  flex-direction: row;
  gap: 15px;
  z-index: 10;
}

.history-button {
  width: 120px;
  height: 50px;
  border-radius: 12px !important;
  font-size: 16px !important;
  font-weight: bold !important;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%) !important;
  border: none !important;
  animation: pulseButton 2s infinite;
  color: white;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.clock-in-button {
  width: 120px;
  height: 50px;
  border-radius: 12px !important;
  font-size: 16px !important;
  font-weight: bold !important;
  background: linear-gradient(135deg, #2ecc71 0%, #27ae60 100%) !important;
  border: none !important;
  animation: pulseButton 2s infinite;
  color: white;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.history-button:hover,
.clock-in-button:hover,
.logout-button:hover {
  transform: scale(1.05) translateY(-2px);
}

.history-button:active,
.clock-in-button:active,
.logout-button:active {
  transform: scale(0.95) translateY(0);
}

.logout-button {
  width: 120px;
  height: 50px;
  border-radius: 12px !important;
  font-size: 16px !important;
  font-weight: bold !important;
  background: linear-gradient(135deg, #e67e22 0%, #d35400 100%) !important;
  border: none !important;
  animation: pulseButton 2s infinite;
  color: white;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.avatar {
  width: 100px;
  height: 100px;
  background: v-bind(avatarBgColor);
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
}

.avatar:hover {
  transform: scale(1.05);
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

.username {
  font-size: 24px;
  font-weight: bold;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
  font-family: '微软雅黑', 'Microsoft YaHei', sans-serif;
  color: #34495e;
}

.match-button-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.match-button {
  width: 160px;
  height: 160px;
  border-radius: 50% !important;
  font-size: 24px !important;
  font-weight: bold !important;
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%) !important;
  border: none !important;
  animation: pulseButton 2s infinite;
  color: white;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

@keyframes pulseButton {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}

.match-button:hover {
  transform: scale(1.05) translateY(-2px);
}

.match-button:active {
  transform: scale(0.95) translateY(0);
}

.bean-count {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 12px 24px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(135, 206, 250, 0.2);
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

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>