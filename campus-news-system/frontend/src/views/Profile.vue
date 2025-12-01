<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <div class="user-card">
            <el-avatar :size="80">{{ userStore.user?.realName?.[0] }}</el-avatar>
            <h3>{{ userStore.user?.realName }}</h3>
            <p>@{{ userStore.user?.username }}</p>
            <el-tag v-for="role in userStore.user?.roles" :key="role.id">
              {{ getRoleName(role.roleName) }}
            </el-tag>
          </div>
        </el-card>
        
        <el-card style="margin-top: 20px;">
          <div class="user-info">
            <div class="info-item">
              <span class="label">邮箱：</span>
              <span>{{ userStore.user?.email }}</span>
            </div>
            <div class="info-item">
              <span class="label">手机：</span>
              <span>{{ userStore.user?.phone }}</span>
            </div>
            <div class="info-item" v-if="userStore.user?.college">
              <span class="label">学院：</span>
              <span>{{ userStore.user?.college?.name }}</span>
            </div>
            <div class="info-item" v-if="userStore.user?.studentId">
              <span class="label">学号/工号：</span>
              <span>{{ userStore.user?.studentId }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="18">
        <el-card>
          <template #header>
            <span>我的文章</span>
          </template>
          
          <div v-loading="loading">
            <el-empty v-if="articles.length === 0 && !loading" description="暂无文章" />
            
            <el-table :data="articles" stripe>
              <el-table-column prop="title" label="标题" />
              <el-table-column prop="boardType" label="板块" width="120">
                <template #default="{ row }">
                  <el-tag :type="getBoardTypeTag(row.boardType)" size="small">
                    {{ getBoardTypeName(row.boardType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="viewCount" label="浏览" width="80" />
              <el-table-column prop="likeCount" label="点赞" width="80" />
              <el-table-column prop="commentCount" label="评论" width="80" />
              <el-table-column prop="createdAt" label="发布时间" width="160">
                <template #default="{ row }">
                  {{ formatTime(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button text size="small" @click="handleEdit(row)">编辑</el-button>
                  <el-button text size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            
            <el-pagination
              v-if="total > 0"
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              layout="total, prev, pager, next"
              @current-change="fetchArticles"
              style="margin-top: 20px; justify-content: center;"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getArticleList, deleteArticle } from '@/api/article'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const articles = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchArticles = async () => {
  loading.value = true
  try {
    const data = await getArticleList({
      current: currentPage.value,
      size: pageSize.value,
      authorId: userStore.user?.id
    })
    articles.value = data.records
    total.value = data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleEdit = (article) => {
  router.push({
    path: '/publish',
    query: { id: article.id }
  })
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇文章吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteArticle(id)
    ElMessage.success('删除成功')
    fetchArticles()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
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

const getBoardTypeName = (type) => {
  const types = {
    OFFICIAL: '官方新闻',
    CAMPUS: '全校新闻',
    COLLEGE: '学院新闻'
  }
  return types[type] || type
}

const getBoardTypeTag = (type) => {
  const tags = {
    OFFICIAL: 'danger',
    CAMPUS: 'primary',
    COLLEGE: 'success'
  }
  return tags[type] || ''
}

const formatTime = (time) => {
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchArticles()
})
</script>

<style scoped>
.user-card {
  text-align: center;
}

.user-card h3 {
  margin: 15px 0 5px;
}

.user-card p {
  color: #909399;
  margin: 5px 0 15px;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
}

.info-item .label {
  color: #909399;
  width: 80px;
  flex-shrink: 0;
}
</style>
