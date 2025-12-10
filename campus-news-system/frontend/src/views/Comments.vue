<template>
  <div class="comments-page">
    <div class="header">
      <div class="title-row">
        <h2>互动评论</h2>
        <el-tag type="success" effect="dark" round>{{ typeLabel }}</el-tag>
      </div>
      <div class="filters">
        <el-radio-group v-model="type" @change="handleTypeChange">
          <el-radio-button label="received">收到的评论</el-radio-button>
          <el-radio-button label="sent">我发出的评论</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          unlink-panels
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="handleDateChange"
          clearable
        />
      </div>
    </div>

    <el-card class="list-card" shadow="hover">
      <div class="list-header">
        <span>共 {{ total }} 条</span>
      </div>
      <el-empty v-if="!loading && comments.length === 0" description="暂无数据" />
      <el-table
        v-else
        :data="comments"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column label="评论内容" min-width="260">
          <template #default="scope">
            <div class="comment-content">{{ scope.row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column label="所属文章" min-width="200">
          <template #default="scope">
            <span class="link" @click="goArticle(scope.row.article?.id)">
              {{ scope.row.article?.title || '未知文章' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="对方" min-width="140">
          <template #default="scope">
            <div class="user-col">
              <el-avatar :size="28">{{ getUserName(scope.row.user).charAt(0) }}</el-avatar>
              <span>{{ getUserName(displayCounterparty(scope.row)) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="方向" width="100">
          <template #default="scope">
            <el-tag :type="type === 'received' ? 'success' : 'info'" effect="plain" round>
              {{ type === 'received' ? '收到' : '发出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" min-width="160" />
      </el-table>

      <div class="pager" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          layout="total, prev, pager, next, jumper"
          :total="total"
          @current-change="fetchComments"
          @size-change="fetchComments"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCommentHistory } from '@/api/comment'

const router = useRouter()

const loading = ref(false)
const comments = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const type = ref('received')
const dateRange = ref([])

const typeLabel = computed(() => (type.value === 'received' ? '收到的评论' : '我发出的评论'))

const fetchComments = async () => {
  loading.value = true
  try {
    const [start, end] = dateRange.value || []
    const data = await getCommentHistory({
      current: currentPage.value,
      size: pageSize.value,
      type: type.value,
      startDate: start,
      endDate: end,
      _t: Date.now()
    })
    comments.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleTypeChange = () => {
  currentPage.value = 1
  fetchComments()
}

const handleDateChange = () => {
  currentPage.value = 1
  fetchComments()
}

const goArticle = (id) => {
  if (!id) return
  router.push(`/article/${id}`)
}

const getUserName = (user) => {
  if (!user) return '未知用户'
  return user.realName || user.username || '未知用户'
}

const displayCounterparty = (row) => {
  return type.value === 'received' ? row.user : row.replyToUser || row.user
}

onMounted(() => {
  fetchComments()
})
</script>

<style scoped>
.comments-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filters {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.list-card {
  margin-top: 8px;
}
.list-header {
  margin-bottom: 12px;
  color: #666;
}
.comment-content {
  white-space: pre-wrap;
}
.link {
  color: #409eff;
  cursor: pointer;
}
.link:hover {
  text-decoration: underline;
}
.user-col {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>

