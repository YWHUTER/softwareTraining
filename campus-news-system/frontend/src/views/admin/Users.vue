<template>
  <div class="users-page">
    <el-card>
      <template #header>
        <div class="header">
          <span>用户管理</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户"
            style="width: 300px;"
            clearable
            @change="fetchUsers"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>
      
      <el-table :data="users" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="phone" label="手机" width="120" />
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role.id" size="small" style="margin-right: 5px;">
              {{ getRoleName(role.roleName) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学院" width="150">
          <template #default="{ row }">
            {{ row.college?.name || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button
              text
              size="small"
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchUsers"
        style="margin-top: 20px; justify-content: center;"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserList, updateUserStatus } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const users = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchUsers = async () => {
  loading.value = true
  try {
    const data = await getUserList({
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined
    })
    users.value = data.records
    total.value = data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleToggleStatus = async (user) => {
  // 状态判断：1 或 undefined/null 视为正常，0 视为禁用
  const currentStatus = user.status ?? 1
  const newStatus = currentStatus === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  
  try {
    await ElMessageBox.confirm(`确定要${action}用户 ${user.realName} 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const result = await updateUserStatus(user.id, newStatus)
    console.log('更新结果:', result)
    ElMessage.success(`${action}成功`)
    // 强制刷新列表
    await fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('更新状态失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const getRoleName = (roleName) => {
  const names = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  }
  return names[roleName] || roleName
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.users-page {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
