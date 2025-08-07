<template>
  <div class="page-bean-history-container">
    <div class="history-header">
      <h2>欢乐豆历史记录</h2>
      <el-button 
        class="close-button"
        circle
        @click="handleClose">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>
    
    <el-table
      :data="historyList"
      style="width: 100%"
      empty-text="暂无数据"
      class="history-table">
      <el-table-column
        prop="changeType"
        label="类型"
        width="120">
        <template #default="{row}">
          <el-tag :type="row.changeAmount > 0 ? 'success' : 'danger'">
            {{
              row.changeType === 'GAME' ? '游戏' : 
              row.changeType === 'REGISTER' ? '注册' :
              row.changeType === 'CLOCK_IN' ? '签到' : 
              row.changeType === 'OTHER' ? '其他' : ''
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="changeAmount"
        label="变动数量"
        width="120">
        <template #default="{row}">
          {{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}
        </template>
      </el-table-column>
      <el-table-column
        prop="currentBean"
        label="当前数量"/>
      <el-table-column
        prop="createTime"
        label="时间"/>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next, jumper"
      @current-change="handlePageChange"
      class="pagination-container"/>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'
import { getPageBeanHistory } from '../../api/user'

const router = useRouter()
const store = useStore()

const historyList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchHistory = async () => {
  try {
    const token = store.state.user.token
    if (!token) {
      router.push('/login')
      return
    }

    const res = await getPageBeanHistory(token, currentPage.value, pageSize.value)
    if (res.data.code === 1 && res.data.data) {
      historyList.value = res.data.data.list
      total.value = res.data.data.total
    }
  } catch (error) {
    console.error('获取历史记录失败:', error)
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchHistory()
}

const handleClose = () => {
  router.push('/')
}

onMounted(() => {
  fetchHistory()
})
</script>

<style scoped>
.page-bean-history-container {
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 70%;
  height: 70vh;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 0 0 20px 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  z-index: 100;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.history-header h2 {
  color: #34495e;
  margin: 0;
}

.close-button {
  background: transparent;
  border: none;
  font-size: 18px;
  color: #999;
  transition: all 0.3s;
}

.close-button:hover {
  color: #e74c3c;
  transform: rotate(90deg);
}

.history-table {
  flex: 1;
  margin-bottom: 20px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
