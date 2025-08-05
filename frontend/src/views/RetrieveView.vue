<template>
  <div class="retrieve-container">
    <div class="poker-cards">
      <div class="poker-card card1"></div>
      <div class="poker-card card2"></div>
      <div class="poker-card card3"></div>
    </div>
    <div class="retrieve-box">
      <el-card class="retrieve-card">
        <div class="retrieve-header">
          <h2 class="retrieve-title">找回密码</h2>
          <p class="retrieve-subtitle">重置您的登录密码</p>
        </div>
        <el-form 
          :model="retrieveForm" 
          :rules="rules" 
          ref="retrieveFormRef"
          class="retrieve-form">
          <el-form-item prop="account">
            <el-input 
              v-model="retrieveForm.account" 
              placeholder="请输入账号"
              prefix-icon="UserFilled"
              size="large" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input 
              v-model="retrieveForm.password" 
              type="password" 
              placeholder="请输入新密码"
              prefix-icon="Lock"
              size="large"
              show-password />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input 
              v-model="retrieveForm.confirmPassword" 
              type="password" 
              placeholder="请确认新密码"
              prefix-icon="Lock"
              size="large"
              show-password />
          </el-form-item>
          <el-form-item prop="email">
            <el-input 
              v-model="retrieveForm.email" 
              placeholder="请输入绑定的邮箱"
              prefix-icon="Message"
              size="large">
              <template #append>
                <el-button 
                  @click="handleGetCode" 
                  :disabled="!!countdown || !retrieveForm.email"
                  type="primary">
                  {{ countdown ? `${countdown}秒后重试` : '获取验证码' }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="emailcode">
            <el-input 
              v-model="retrieveForm.emailcode" 
              placeholder="请输入验证码"
              prefix-icon="Key"
              size="large" />
          </el-form-item>
          <el-form-item>
            <el-button 
              type="danger" 
              @click="handleRetrieve" 
              :loading="loading" 
              class="retrieve-btn"
              size="large">
              重置密码
            </el-button>
          </el-form-item>
          <div class="back-to-login">
            <el-button 
              @click="goLogin" 
              class="back-btn"
              text>
              返回登录
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
import { PostRetrieve, getApplyForEmailcode } from '../api/user'
import { UserFilled, Lock, Message, Key } from '@element-plus/icons-vue'

const router = useRouter()
const retrieveFormRef = ref()
const loading = ref(false)
const countdown = ref(0)

const retrieveForm = reactive({
  account: '',
  password: '',
  confirmPassword: '',
  email: '',
  emailcode: ''
})

const validatePass = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else {
    if (retrieveForm.confirmPassword !== '') {
      retrieveFormRef.value?.validateField('confirmPassword')
    }
    callback()
  }
}

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== retrieveForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 20, message: '长度在 4 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { validator: validatePass, trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validatePass2, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  emailcode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度应为6位', trigger: 'blur' }
  ]
}

const handleGetCode = async () => {
  try {
    const res = await getApplyForEmailcode(retrieveForm.email)
    if (res.data.code === 1) {
      ElMessage.success('验证码已发送')
      countdown.value = res.data.data
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    }
  } catch (error) {
    ElMessage.error('获取验证码失败')
  }
}

const handleRetrieve = () => {
  retrieveFormRef.value?.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      const { account, password, email, emailcode } = retrieveForm
      const res = await PostRetrieve(account, password, email, emailcode)
      if (res.data.code === 1) {
        ElMessage.success('密码重置成功')
        router.push('/login')
      } else {
        ElMessage.error(res.data.msg || '密码重置失败')
      }
    } catch (error) {
      ElMessage.error('密码重置失败')
    } finally {
      loading.value = false
    }
  })
}

const goLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.retrieve-container {
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

.retrieve-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 50%, rgba(231, 76, 60, 0.1) 0%, transparent 40%),
    radial-gradient(circle at 80% 50%, rgba(70, 130, 180, 0.1) 0%, transparent 40%),
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
  transform-style: preserve-3d;
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
  top: 20%;
  left: 20%;
  transform: rotate(-15deg) translateZ(20px);
  animation-delay: 0s;
}

.card2 {
  top: 40%;
  right: 25%;
  transform: rotate(10deg) translateZ(40px);
  animation-delay: 1s;
}

.card3 {
  bottom: 25%;
  left: 30%;
  transform: rotate(5deg) translateZ(60px);
  animation-delay: 0.5s;
}

@keyframes floatCard {
  0% {
    transform: translateY(0) rotate(var(--rotation, 0deg));
  }
  50% {
    transform: translateY(-15px) rotate(calc(var(--rotation, 0deg) + 2deg));
  }
  100% {
    transform: translateY(0) rotate(var(--rotation, 0deg));
  }
}

.retrieve-box {
  animation: fadeIn 0.8s ease-out;
  position: relative;
  z-index: 1;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.retrieve-card {
  width: 420px;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(70, 130, 180, 0.15);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(135, 206, 250, 0.2);
}

.retrieve-header {
  text-align: center;
  margin-bottom: 36px;
}

.retrieve-title {
  font-size: 32px;
  color: #e74c3c;
  margin-bottom: 8px;
  font-weight: 700;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.retrieve-subtitle {
  font-size: 18px;
  color: #34495e;
  margin: 0;
  font-weight: 500;
}

.retrieve-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.retrieve-form :deep(.el-input) {
  --el-input-hover-border: #e74c3c;
  --el-input-focus-border: #e74c3c;
}

.retrieve-form :deep(.el-input__wrapper) {
  padding: 8px 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.9);
}

.retrieve-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #e74c3c;
}

.retrieve-btn {
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

.retrieve-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(231, 76, 60, 0.4);
}

.retrieve-btn:active {
  transform: translateY(0);
}

.back-to-login {
  text-align: center;
  margin-top: 16px;
}

.back-btn {
  font-size: 15px;
  color: #34495e;
  font-weight: 500;
  transition: all 0.3s ease;
}

.back-btn:hover {
  opacity: 0.8;
  transform: translateY(-1px);
}
</style>