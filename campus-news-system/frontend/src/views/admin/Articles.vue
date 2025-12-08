<template>
  <div class="articles-page">
    <el-card>
      <template #header>
        <div class="header">
          <span>文章管理</span>
          <div class="filters">
            <el-select v-model="boardType" placeholder="板块类型" clearable @change="fetchArticles" style="width: 120px; margin-right: 10px;">
              <el-option label="官方新闻" value="OFFICIAL" />
              <el-option label="全校新闻" value="CAMPUS" />
              <el-option label="学院新闻" value="COLLEGE" />
            </el-select>
            <el-select v-model="isApproved" placeholder="审核状态" clearable @change="fetchArticles" style="width: 120px; margin-right: 10px;">
              <el-option label="待审核" :value="0" />
              <el-option label="已通过" :value="1" />
              <el-option label="未通过" :value="2" />
            </el-select>
            <el-input
              v-model="searchKeyword"
              placeholder="搜索文章"
              style="width: 200px;"
              clearable
              @change="fetchArticles"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>
      </template>
      
      <el-table :data="articles" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column label="板块" width="100">
          <template #default="{ row }">
            <el-tag :type="getBoardTypeTag(row.boardType)" size="small">
              {{ getBoardTypeName(row.boardType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="author.realName" label="作者" width="100" />
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column label="置顶" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isPinned ? 'danger' : 'info'" size="small">
              {{ row.isPinned ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核" width="100">
          <template #default="{ row }">
            <el-tag :type="getApproveTag(row.isApproved)" size="small">
              {{ getApproveName(row.isApproved) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="handleTogglePin(row)">
              {{ row.isPinned ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button text size="small" type="success" @click="handleApprove(row.id, 1)" v-if="row.isApproved !== 1">
              通过
            </el-button>
            <el-button text size="small" type="danger" @click="handleDelete(row.id)">
              删除
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
        @current-change="fetchArticles"
        style="margin-top: 20px; justify-content: center;"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getArticleList, deleteArticle, togglePinned, approveArticle } from '@/api/article'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const articles = ref([])
const searchKeyword = ref('')
const boardType = ref('')
const isApproved = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchArticles = async () => {
  loading.value = true
  try {
    const data = await getArticleList({
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      boardType: boardType.value || undefined,
      isApproved: isApproved.value
    })
    articles.value = data.records
    total.value = data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleTogglePin = async (article) => {
  try {
    await togglePinned(article.id, article.isPinned ? 0 : 1)
    ElMessage.success(article.isPinned ? '取消置顶成功' : '置顶成功')
    fetchArticles()
  } catch (error) {
    console.error(error)
  }
}

const handleApprove = async (id, status) => {
  try {
    await approveArticle(id, status)
    ElMessage.success('审核成功')
    fetchArticles()
  } catch (error) {
    console.error(error)
  }
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

const getBoardTypeName = (type) => {
  const types = {
    OFFICIAL: '官方',
    CAMPUS: '全校',
    COLLEGE: '学院'
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

const getApproveName = (status) => {
  const names = {
    0: '待审核',
    1: '已通过',
    2: '未通过'
  }
  return names[status] || ''
}

const getApproveTag = (status) => {
  const tags = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return tags[status] || ''
}

onMounted(() => {
  fetchArticles()
})
</script>

<style scoped>
.articles-page {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filters {
  display: flex;
  align-items: center;
}

/* 玻璃拟态卡片样式 */
.el-card {
  background: rgba(255, 255, 255, 0.35) !important;
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.5) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06), 
              0 0 0 1px rgba(255, 255, 255, 0.4) inset !important;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.el-card:hover {
  transform: translateY(-4px);
  background: rgba(255, 255, 255, 0.5) !important;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.12), 
              0 0 0 1px rgba(255, 255, 255, 0.6) inset !important;
  border-color: rgba(255, 255, 255, 0.8) !important;
}

/* 表格透明背景 */
:deep(.el-table),
:deep(.el-table__expanded-cell) {
  background-color: transparent !important;
  --el-table-tr-bg-color: transparent !important;
}

:deep(.el-table th.el-table__cell),
:deep(.el-table tr) {
  background-color: transparent !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(255, 255, 255, 0.15);
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td.el-table__cell) {
  background-color: rgba(255, 255, 255, 0.3) !important;
}
</style>
