<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="用户总数" :value="statistics.userCount">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="文章总数" :value="statistics.articleCount">
            <template #prefix>
              <el-icon><Document /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="评论总数" :value="statistics.commentCount">
            <template #prefix>
              <el-icon><ChatDotRound /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="学院数量" :value="statistics.collegeCount">
            <template #prefix>
              <el-icon><School /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>最新文章</span>
          </template>
          <el-table :data="recentArticles" stripe>
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="author.realName" label="作者" width="100" />
            <el-table-column prop="viewCount" label="浏览" width="80" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>待审核文章</span>
          </template>
          <el-table :data="pendingArticles" stripe>
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column prop="author.realName" label="作者" width="100" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button text size="small" type="success" @click="handleApprove(row.id, 1)">通过</el-button>
                <el-button text size="small" type="danger" @click="handleApprove(row.id, 2)">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getArticleList, approveArticle } from '@/api/article'
import { ElMessage } from 'element-plus'

const statistics = ref({
  userCount: 0,
  articleCount: 0,
  commentCount: 0,
  collegeCount: 5
})

const recentArticles = ref([])
const pendingArticles = ref([])

const fetchRecentArticles = async () => {
  try {
    const data = await getArticleList({
      current: 1,
      size: 5,
      isApproved: 1
    })
    recentArticles.value = data.records
    statistics.value.articleCount = data.total
  } catch (error) {
    console.error(error)
  }
}

const fetchPendingArticles = async () => {
  try {
    const data = await getArticleList({
      current: 1,
      size: 5,
      isApproved: 0
    })
    pendingArticles.value = data.records
  } catch (error) {
    console.error(error)
  }
}

const handleApprove = async (id, isApproved) => {
  try {
    await approveArticle(id, isApproved)
    ElMessage.success(isApproved === 1 ? '审核通过' : '审核拒绝')
    fetchPendingArticles()
    fetchRecentArticles()
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  fetchRecentArticles()
  fetchPendingArticles()
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}
</style>
