<template>
  <div class="article-detail" v-loading="loading" element-loading-text="加载中...">
    <!-- 文章主体 -->
    <el-card v-if="article" class="article-card" shadow="never">
      <!-- 文章头部 -->
      <div class="article-header">
        <div class="header-meta">
          <el-tag 
            :type="getBoardTypeTag(article.boardType)"
            size="large"
            effect="dark"
            class="board-tag"
          >
            {{ getBoardTypeName(article.boardType) }}
          </el-tag>
        </div>
        
        <h1 class="article-title">{{ article.title }}</h1>
        
        <div class="article-info">
          <div class="author-section">
            <el-avatar :size="48" class="author-avatar">
              {{ article.author?.realName?.[0] }}
            </el-avatar>
            <div class="author-info">
              <div class="author-name">{{ article.author?.realName }}</div>
              <div class="publish-info">
                <span v-if="article.college" class="college-name">
                  <el-icon><School /></el-icon>
                  {{ article.college?.name }}
                </span>
                <span class="publish-time">
                  <el-icon><Clock /></el-icon>
                  {{ formatTime(article.createdAt) }}
                </span>
              </div>
            </div>
            <!-- 关注按钮 -->
            <el-button
              v-if="userStore.isLogin && article.author?.id !== userStore.user?.id"
              :type="isFollowingAuthor ? 'default' : 'primary'"
              size="small"
              round
              @click="handleFollowAuthor"
              class="follow-author-btn"
            >
              {{ isFollowingAuthor ? '已关注' : '+ 关注' }}
            </el-button>
          </div>
          
          <div class="article-stats">
            <div class="stat-item">
              <el-icon :size="18"><View /></el-icon>
              <span>{{ article.viewCount }}</span>
            </div>
            <div class="stat-item">
              <el-icon :size="18"><ChatDotRound /></el-icon>
              <span>{{ article.commentCount }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <el-divider class="header-divider" />
      
      <!-- 封面图片 -->
      <div class="article-cover" v-if="article.coverImage">
        <img :src="article.coverImage" :alt="article.title" class="cover-image" />
      </div>
      
      <!-- 文章内容 -->
      <div class="article-body">
        <div class="article-content" v-html="article.content"></div>
      </div>
      
      <!-- 文章操作 -->
      <div class="article-actions">
        <el-button
          :type="article.isLiked ? 'primary' : 'default'"
          @click="handleLike"
          size="large"
          class="action-btn"
          round
        >
          <el-icon><StarFilled v-if="article.isLiked" /><Star v-else /></el-icon>
          <span>{{ article.isLiked ? '已点赞' : '点赞' }} ({{ article.likeCount }})</span>
        </el-button>
        <el-button
          :type="article.isFavorited ? 'warning' : 'default'"
          @click="handleFavorite"
          size="large"
          class="action-btn"
          round
        >
          <el-icon><Star /></el-icon>
          <span>{{ article.isFavorited ? '已收藏' : '收藏' }}</span>
        </el-button>
        <el-button
          size="large"
          class="action-btn"
          round
        >
          <el-icon><Share /></el-icon>
          <span>分享</span>
        </el-button>
      </div>
    </el-card>
    
    <!-- 评论区 -->
    <el-card class="comment-section" shadow="never">
      <template #header>
        <div class="comment-header">
          <div class="comment-title">
            <el-icon :size="20"><ChatDotRound /></el-icon>
            <span>全部评论 ({{ comments.length }})</span>
          </div>
        </div>
      </template>
      
      <!-- 发表评论 -->
      <div v-if="userStore.isLogin" class="comment-form">
        <el-avatar :size="40" class="comment-avatar">
          {{ userStore.user?.realName?.[0] }}
        </el-avatar>
        <div class="comment-input-wrapper">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            placeholder="发表你的看法..."
            class="comment-textarea"
          />
          <div class="comment-actions">
            <el-button 
              type="primary" 
              @click="handleComment" 
              :loading="commentLoading"
              round
            >
              发表评论
            </el-button>
          </div>
        </div>
      </div>
      
      <div v-else class="login-tip">
        <el-icon :size="40"><Lock /></el-icon>
        <p>请先 <router-link to="/login">登录</router-link> 后发表评论</p>
      </div>
      
      <el-divider v-if="comments.length > 0" />
      
      <!-- 评论列表 -->
      <div class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <el-avatar :size="44" class="comment-avatar">
            {{ comment.user?.realName?.[0] }}
          </el-avatar>
          
          <div class="comment-content">
            <div class="comment-user-info">
              <span class="comment-user">{{ comment.user?.realName }}</span>
              <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
            </div>
            
            <div class="comment-text">{{ comment.content }}</div>
            
            <div class="comment-footer">
              <el-button text size="small" @click="handleReply(comment)">
                <el-icon><ChatDotRound /></el-icon>
                回复
              </el-button>
              <el-button
                v-if="userStore.user?.id === comment.userId"
                text
                size="small"
                type="danger"
                @click="handleDeleteComment(comment.id)"
              >
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
            
            <!-- 回复列表 -->
            <div v-if="comment.replies?.length > 0" class="replies">
              <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                <el-avatar :size="32" class="reply-avatar">
                  {{ reply.user?.realName?.[0] }}
                </el-avatar>
                <div class="reply-content">
                  <div class="reply-user-info">
                    <span class="reply-user">{{ reply.user?.realName }}</span>
                    <span class="reply-time">{{ formatTime(reply.createdAt) }}</span>
                  </div>
                  <div class="reply-text">{{ reply.content }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <el-empty v-if="comments.length === 0" description="暂无评论，快来抢沙发吧~" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getArticleDetail, toggleLike, toggleFavorite } from '@/api/article'
import { getCommentList, createComment, deleteComment } from '@/api/comment'
import { toggleFollow, checkFollow } from '@/api/follow'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const commentLoading = ref(false)
const article = ref(null)
const comments = ref([])
const commentContent = ref('')
const replyTo = ref(null)
const isFollowingAuthor = ref(false)

const fetchArticle = async () => {
  loading.value = true
  try {
    article.value = await getArticleDetail(route.params.id)
    // 检查是否已关注作者
    if (userStore.isLogin && article.value?.author?.id) {
      checkFollowStatus()
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 检查是否关注作者
const checkFollowStatus = async () => {
  try {
    if (article.value?.author?.id && article.value.author.id !== userStore.user?.id) {
      isFollowingAuthor.value = await checkFollow(article.value.author.id)
    }
  } catch (error) {
    console.error('检查关注状态失败:', error)
  }
}

// 关注/取消关注作者
const handleFollowAuthor = async () => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const result = await toggleFollow(article.value.author.id)
    isFollowingAuthor.value = result.isFollowing
    ElMessage.success(result.message)
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const fetchComments = async () => {
  try {
    comments.value = await getCommentList(route.params.id)
  } catch (error) {
    console.error(error)
  }
}

const handleLike = async () => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await toggleLike(article.value.id)
    article.value.isLiked = !article.value.isLiked
    article.value.likeCount += article.value.isLiked ? 1 : -1
  } catch (error) {
    console.error(error)
  }
}

const handleFavorite = async () => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await toggleFavorite(article.value.id)
    article.value.isFavorited = !article.value.isFavorited
    ElMessage.success(article.value.isFavorited ? '收藏成功' : '取消收藏')
  } catch (error) {
    console.error(error)
  }
}

const handleComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commentLoading.value = true
  try {
    await createComment({
      articleId: route.params.id,
      content: commentContent.value,
      parentId: replyTo.value?.id
    })
    commentContent.value = ''
    replyTo.value = null
    ElMessage.success('评论成功')
    fetchComments()
  } catch (error) {
    console.error(error)
  } finally {
    commentLoading.value = false
  }
}

const handleReply = (comment) => {
  replyTo.value = comment
  commentContent.value = `回复 @${comment.user?.realName}: `
}

const handleDeleteComment = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteComment(id)
    ElMessage.success('删除成功')
    fetchComments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
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
  fetchArticle()
  fetchComments()
})
</script>

<style scoped>
.article-detail {
  max-width: 900px;
  margin: 0 auto;
}

/* 文章卡片 */
.article-card {
  border-radius: 16px;
  margin-bottom: 24px;
  border: none;
  overflow: hidden;
}

/* 文章头部 */
.article-header {
  padding: 40px 40px 20px;
}

.header-meta {
  margin-bottom: 20px;
}

.board-tag {
  border-radius: 8px;
  padding: 8px 16px;
  font-weight: 600;
  font-size: 14px;
}

.article-title {
  margin: 0 0 30px;
  font-size: 36px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1.4;
  letter-spacing: -0.5px;
}

.article-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.author-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.author-avatar {
  background: linear-gradient(135deg, #2196f3, #1976d2);
  color: white;
  font-weight: 700;
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.3);
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.author-name {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.publish-info {
  display: flex;
  align-items: center;
  gap: 16px;
  color: #909399;
  font-size: 14px;
}

.college-name,
.publish-time {
  display: flex;
  align-items: center;
  gap: 4px;
}

.article-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 15px;
  font-weight: 500;
}

.follow-author-btn {
  margin-left: 12px;
}

.header-divider {
  margin: 20px 0;
}

/* 封面图片 */
.article-cover {
  padding: 0 40px;
  margin-bottom: 24px;
}

.cover-image {
  width: 100%;
  max-height: 500px;
  object-fit: cover;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

/* 文章主体 */
.article-body {
  padding: 0 40px 40px;
}

.article-content {
  font-size: 17px;
  line-height: 1.9;
  color: #2c3e50;
  min-height: 300px;
}

.article-content :deep(p) {
  margin: 1.2em 0;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  margin: 1.5em 0 0.8em;
  font-weight: 700;
  color: #2c3e50;
}

.article-content :deep(img) {
  max-width: 100%;
  height: auto !important;
  display: block;
  border-radius: 8px;
  margin: 1.5em auto;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* Quill 编辑器插入的图片容器 */
.article-content :deep(.ql-align-center) {
  text-align: center;
}

.article-content :deep(.ql-align-right) {
  text-align: right;
}

.article-content :deep(p img),
.article-content :deep(span img) {
  max-width: 100%;
  height: auto !important;
  display: inline-block;
}

.article-content :deep(blockquote) {
  border-left: 4px solid #2196f3;
  padding-left: 20px;
  margin: 1.5em 0;
  color: #606266;
  font-style: italic;
  background: #f5f7fa;
  padding: 15px 20px;
  border-radius: 4px;
}

.article-content :deep(code) {
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  color: #e83e8c;
}

.article-content :deep(pre) {
  background: #2c3e50;
  color: #fff;
  padding: 20px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 1.5em 0;
}

/* 文章操作 */
.article-actions {
  display: flex;
  gap: 12px;
  padding: 30px 40px;
  border-top: 1px solid #e4e7ed;
  background: #fafafa;
}

.action-btn {
  font-weight: 600;
  padding: 12px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 评论区 */
.comment-section {
  border-radius: 16px;
  border: none;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comment-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
}

/* 评论表单 */
.comment-form {
  display: flex;
  gap: 16px;
  margin-bottom: 30px;
  padding: 24px;
  background: #f8f9fa;
  border-radius: 12px;
}

.comment-avatar {
  background: linear-gradient(135deg, #2196f3, #1976d2);
  color: white;
  font-weight: 700;
  flex-shrink: 0;
}

.comment-input-wrapper {
  flex: 1;
}

.comment-textarea :deep(.el-textarea__inner) {
  border-radius: 8px;
  border: 2px solid transparent;
  transition: all 0.3s ease;
  font-size: 15px;
  line-height: 1.6;
}

.comment-textarea :deep(.el-textarea__inner:focus) {
  border-color: #2196f3;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* 登录提示 */
.login-tip {
  text-align: center;
  padding: 60px 20px;
  background: #f8f9fa;
  border-radius: 12px;
  margin-bottom: 30px;
}

.login-tip .el-icon {
  color: #c0c4cc;
  margin-bottom: 16px;
}

.login-tip p {
  margin: 0;
  color: #606266;
  font-size: 15px;
}

.login-tip a {
  color: #2196f3;
  font-weight: 600;
  transition: color 0.3s ease;
}

.login-tip a:hover {
  color: #1976d2;
  text-decoration: underline;
}

/* 评论列表 */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.comment-item {
  display: flex;
  gap: 16px;
  padding: 24px;
  background: #fafafa;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.comment-item:hover {
  background: #f5f7fa;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-user-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.comment-user {
  font-weight: 600;
  color: #2c3e50;
  font-size: 15px;
}

.comment-time {
  color: #909399;
  font-size: 13px;
}

.comment-text {
  color: #606266;
  line-height: 1.7;
  font-size: 15px;
  margin-bottom: 12px;
  word-break: break-word;
}

.comment-footer {
  display: flex;
  gap: 16px;
}

.comment-footer .el-button {
  font-weight: 500;
}

/* 回复列表 */
.replies {
  margin-top: 20px;
  padding-left: 20px;
  border-left: 3px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.reply-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: white;
  border-radius: 8px;
}

.reply-avatar {
  background: linear-gradient(135deg, #4caf50, #45a049);
  color: white;
  font-weight: 600;
  flex-shrink: 0;
}

.reply-content {
  flex: 1;
  min-width: 0;
}

.reply-user-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 8px;
}

.reply-user {
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
}

.reply-time {
  color: #909399;
  font-size: 12px;
}

.reply-text {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .article-detail {
    max-width: 100%;
  }

  .article-header {
    padding: 24px 20px 16px;
  }

  .article-title {
    font-size: 26px;
    margin-bottom: 20px;
  }

  .article-info {
    flex-direction: column;
    align-items: flex-start;
  }

  .article-body {
    padding: 0 20px 24px;
  }

  .article-content {
    font-size: 16px;
  }

  .article-actions {
    padding: 20px;
    flex-wrap: wrap;
  }

  .action-btn {
    flex: 1;
    min-width: 120px;
  }

  .comment-form {
    flex-direction: column;
    padding: 16px;
  }

  .comment-item {
    padding: 16px;
  }

  .replies {
    padding-left: 12px;
  }
}
</style>
