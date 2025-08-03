<template>
  <div class="login-container">
    <div class="poker-cards">
      <div class="poker-card card1"></div>
      <div class="poker-card card2"></div>
      <div class="poker-card card3"></div>
    </div>
    <div class="login-box">
      <el-card class="login-card">
        <div class="login-header">
          <h2 class="login-title">欢迎来到斗地主</h2>
          <p class="login-subtitle">准备好成为负债累累了吗</p>
        </div>
        <el-form 
          :model="loginForm" 
          :rules="rules" 
          ref="loginFormRef"
          class="login-form">
          <el-form-item prop="account">
            <el-input 
              v-model="loginForm.account" 
              placeholder="输入您的玩家账号"
              prefix-icon="User"
              size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="输入您的密码"
              prefix-icon="Lock"
              size="large"
              show-password />
          </el-form-item>
          <el-form-item>
            <el-button 
              type="danger" 
              @click="handleLogin" 
              :loading="loading" 
              class="login-btn"
              size="large">
              登入
            </el-button>
          </el-form-item>
          <div class="login-actions">
            <el-button 
              type="warning" 
              @click="goRegister" 
              class="action-btn"
              text>
              注册新玩家
            </el-button>
            <el-button 
              @click="goRetrieve" 
              class="action-btn"
              text>
              找回密码
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { postLogin } from '../api/user'

const router = useRouter()
const store = useStore()
const loginForm = reactive({
  account: '',
  password: ''
})
const loading = ref(false)
const loginFormRef = ref()

const rules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleLogin = () => {
  loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await postLogin(loginForm.account, loginForm.password)
      if (res.data.code === 1) {
        store.commit('user/setToken', res.data.data.token)
        router.push('/')
      } else {
        ElMessage.error(res.data.msg || '登录失败')
      }
    } catch (e) {
      ElMessage.error('网络错误或服务器异常')
    } finally {
      loading.value = false
    }
  })
}

const goRegister = () => {
  router.push('/register')
}
const goRetrieve = () => {
  router.push('/retrieve')
}
</script>

<style scoped>
.login-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #ffffff 0%, #e8f4ff 100%);
  overflow: hidden;
  perspective: 1000px;
  position: relative;
}

.login-container::before {
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

.poker-cards {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.poker-card {
  position: absolute;
  width: 150px;
  height: 210px;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  animation: floatCard 3s ease-in-out infinite;
  background: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 48px;
  color: #e74c3c;
  border: 8px solid white;
  overflow: hidden;
}

.poker-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    linear-gradient(45deg, transparent 48%, #e74c3c 48%, #e74c3c 52%, transparent 52%),
    linear-gradient(-45deg, transparent 48%, #e74c3c 48%, #e74c3c 52%, transparent 52%);
  opacity: 0.1;
}

.card1 {
  top: 10%;
  left: 15%;
  transform: rotate(-15deg);
  animation-delay: 0s;
  background: white;
}

.card2 {
  top: 20%;
  right: 20%;
  transform: rotate(10deg);
  animation-delay: 0.5s;
  background: white;
}

.card3 {
  bottom: 15%;
  left: 25%;
  transform: rotate(-5deg);
  animation-delay: 1s;
  background: white;
}

@keyframes floatCard {
  0% {
    transform: translateY(0) rotate(var(--rotation, 0deg));
    box-shadow: 0 5px 15px rgba(0,0,0,0.3);
  }
  50% {
    transform: translateY(-20px) rotate(calc(var(--rotation, 0deg) + 3deg));
    box-shadow: 0 25px 15px rgba(0,0,0,0.2);
  }
  100% {
    transform: translateY(0) rotate(var(--rotation, 0deg));
    box-shadow: 0 5px 15px rgba(0,0,0,0.3);
  }
}

.login-box {
  animation: fadeIn 0.8s ease-out;
  position: relative;
  z-index: 1;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px) rotateX(10deg);
  }
  to {
    opacity: 1;
    transform: translateY(0) rotateX(0);
  }
}

.login-card {
  width: 420px;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(70, 130, 180, 0.15);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(135, 206, 250, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-title {
  font-size: 32px;
  color: #e74c3c;
  margin-bottom: 8px;
  font-weight: 700;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.login-subtitle {
  font-size: 18px;
  color: #34495e;
  margin: 0;
  font-weight: 500;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-form :deep(.el-input) {
  --el-input-hover-border: #e74c3c;
  --el-input-focus-border: #e74c3c;
}

.login-form :deep(.el-input__wrapper) {
  padding: 8px 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.9);
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #e74c3c;
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
  border: none;
  margin-top: 12px;
  transition: all 0.3s ease;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(231, 76, 60, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.login-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
  padding: 0 8px;
}

.action-btn {
  font-size: 15px;
  padding: 4px 0;
  transition: all 0.3s ease;
  color: #34495e;
  font-weight: 500;
}

.action-btn:hover {
  opacity: 0.8;
  transform: translateY(-1px);
}

:deep(.el-button--text) {
  color: #34495e;
}

:deep(.el-button--text.el-button--warning) {
  color: #f39c12;
}
</style>
