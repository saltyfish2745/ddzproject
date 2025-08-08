<template>
  <div>
    <router-view/>
    <!-- 悬浮球和悬浮窗 -->
    <div 
      class="floating-ball"
      :class="{ 
        'active': isFloatingWindowVisible,
        'dragging': isDragging 
      }"
      :style="{
        left: ballPosition.x + 'px',
        top: ballPosition.y + 'px'
      }"
      @mousedown="handleMouseDown"
      @click="toggleFloatingWindow">
      <div class="ball-icon">
        <span v-if="isFloatingWindowVisible">×</span>
        <span v-else>?</span>
      </div>
    </div>
    
    <transition name="fade">
      <div 
        v-if="isFloatingWindowVisible" 
        class="floating-window"
        :class="{ 'show-on-left': showOnLeft }"
        :style="{
          left: showOnLeft ? 'auto' : (ballPosition.x + 70) + 'px',
          right: showOnLeft ? (windowSize.width - ballPosition.x + 20) + 'px' : 'auto',
          top: ballPosition.y + 'px'
        }">
        <div class="floating-window-content" v-html="htmlContent"></div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { getReadFile } from './api/backend'
import { ElMessage } from 'element-plus'

const isFloatingWindowVisible = ref(true) // 默认显示
const htmlContent = ref('')
const windowSize = ref({ width: window.innerWidth, height: window.innerHeight })
const ballPosition = ref({ x: 30, y: 30 }) // 左上角位置
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

// 计算悬浮窗是否应该显示在左边
const showOnLeft = computed(() => {
  return ballPosition.value.x > window.innerWidth / 2
})

const handleMouseDown = (e) => {
  isDragging.value = true
  dragOffset.value = {
    x: e.clientX - ballPosition.value.x,
    y: e.clientY - ballPosition.value.y
  }
}

const handleMouseMove = (e) => {
  if (!isDragging.value) return
  
  // 计算新位置
  let newX = e.clientX - dragOffset.value.x
  let newY = e.clientY - dragOffset.value.y
  
  // 限制在视窗范围内
  newX = Math.max(0, Math.min(window.innerWidth - 50, newX))
  newY = Math.max(0, Math.min(window.innerHeight - 50, newY))
  
  ballPosition.value = { x: newX, y: newY }
}

const handleMouseUp = () => {
  isDragging.value = false
}

// 更新窗口大小
const handleResize = () => {
  windowSize.value = {
    width: window.innerWidth,
    height: window.innerHeight
  }
  // 确保悬浮球在调整窗口大小后仍在视窗内
  ballPosition.value = {
    x: Math.min(ballPosition.value.x, window.innerWidth - 50),
    y: Math.min(ballPosition.value.y, window.innerHeight - 50)
  }
}

// 监听全局事件
onMounted(() => {
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  window.addEventListener('resize', handleResize)
})

const toggleFloatingWindow = () => {
  if (!isDragging.value) { // 只有在不拖动时才切换显示状态
    isFloatingWindowVisible.value = !isFloatingWindowVisible.value
  }
}

const fetchHtmlContent = async () => {
  // 通过后端获取免打包配置信息
  try {
    const res = await getReadFile()
    if (res.data.code === 1) {
      htmlContent.value = res.data.msg || ''
    } else {
      ElMessage.error('获取内容失败')
    }
  } catch (error) {
    ElMessage.error('网络错误')
  }
}

onMounted(() => {
  fetchHtmlContent()
})

// 清理事件监听器
onUnmounted(() => {
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  window.removeEventListener('resize', handleResize)
})
</script>

<style>
html, body {
  margin: 0;
  padding: 0;
  overflow: hidden;
  height: 100%;
  width: 100%;
}

#app {
  height: 100%;
  width: 100%;
}

/* 悬浮球样式 */
.floating-ball {
  position: fixed;
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  cursor: move;
  z-index: 1000;
  transition: all 0.3s ease;
  display: flex;
  justify-content: center;
  align-items: center;
  user-select: none;
}

.floating-ball:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
}

.floating-ball.active {
  background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%);
}

.floating-ball.dragging {
  transition: none;
  opacity: 0.9;
  transform: scale(1.1);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.4);
}

.ball-icon {
  color: white;
  font-size: 24px;
  font-weight: bold;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  pointer-events: none;
}

/* 悬浮窗样式 */
.floating-window {
  position: fixed;
  width: 300px;
  max-height: 400px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 15px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  z-index: 999;
  overflow: hidden;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
}

.floating-window-content {
  padding: 20px;
  overflow-y: auto;
  max-height: 360px;
  color: #34495e;
  line-height: 1.6;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* 滚动条样式 */
.floating-window-content::-webkit-scrollbar {
  width: 6px;
}

.floating-window-content::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

.floating-window-content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.floating-window-content::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}
</style>
