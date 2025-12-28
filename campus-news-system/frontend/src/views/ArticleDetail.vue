<template>
  <div class="article-detail-page" v-loading="loading" element-loading-text="加载中...">
    <!-- 返回导航栏 -->
    <div class="back-nav-bar" v-motion :initial="{ opacity: 0, y: -10 }" :enter="{ opacity: 1, y: 0 }">
      <el-button class="back-btn" @click="goBack" round>
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </el-button>
      <div class="nav-breadcrumb">
        <span class="breadcrumb-item" @click="goHome">首页</span>
        <el-icon class="breadcrumb-separator"><ArrowRight /></el-icon>
        <span class="breadcrumb-item" @click="goToBoard">{{ getBoardTypeName(article?.boardType) }}</span>
        <el-icon class="breadcrumb-separator"><ArrowRight /></el-icon>
        <span class="breadcrumb-current">文章详情</span>
      </div>
    </div>

    <el-row :gutter="24">
      <!-- 左侧主内容区 -->
      <el-col :xs="24" :sm="24" :md="17" :lg="17">
        <div class="main-content">
          <!-- 文章主体 -->
          <el-card v-if="article" class="article-card" :class="{ 'retro-newspaper-mode': isRetroMode }" shadow="never">
            <!-- 复古模式报头 -->
            <div v-if="isRetroMode" class="retro-header-banner">
              <div class="retro-top-meta">
                <span>{{ article.college?.name || 'CAMPUS NEWS' }}</span>
                <span>{{ formatDate(article.createdAt, 'YYYY年MM月DD日') }}</span>
              </div>
              <div class="retro-brand">CAMPUS CHRONICLE</div>
              <div class="retro-sub-meta">
                <span>VOL. {{ article.id }}</span>
                <span>ISSUE {{ new Date(article.createdAt).getMonth() + 1 }}</span>
                <span>PRICE: FREE</span>
              </div>
            </div>

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
            <el-avatar :size="48" class="author-avatar" :src="article.author?.avatar">
              {{ !article.author?.avatar ? article.author?.realName?.[0] : '' }}
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
        <!-- 第一行：基础操作按钮 -->
        <div class="actions-row">
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
          <el-dropdown trigger="click" @command="handleShare">
            <el-button size="large" class="action-btn" round>
              <el-icon><Share /></el-icon>
              <span>分享</span>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="wechat">
                  <svg class="share-icon" viewBox="0 0 24 24" width="18" height="18">
                    <path fill="#07C160" d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-6.656-6.088V8.89c-.135-.007-.269-.026-.407-.03zm-2.53 3.274c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982z"/>
                  </svg>
                  <span style="margin-left: 8px">微信</span>
                </el-dropdown-item>
                <el-dropdown-item command="weibo">
                  <svg class="share-icon" viewBox="0 0 24 24" width="18" height="18">
                    <path fill="#E6162D" d="M10.098 20.323c-3.977.391-7.414-1.406-7.672-4.02-.259-2.609 2.759-5.047 6.74-5.441 3.979-.394 7.413 1.404 7.671 4.018.259 2.6-2.759 5.049-6.739 5.443zM9.05 17.219c-.384.616-1.208.884-1.829.602-.612-.279-.793-.991-.406-1.593.379-.595 1.176-.861 1.793-.601.622.263.82.972.442 1.592zm1.27-1.627c-.141.237-.449.353-.689.253-.236-.09-.313-.361-.177-.586.138-.227.436-.346.672-.254.241.09.327.36.194.587zm.176-2.719c-1.893-.493-4.033.45-4.857 2.118-.836 1.704-.026 3.591 1.886 4.21 1.983.64 4.318-.341 5.132-2.179.8-1.793-.201-3.642-2.161-4.149zm7.563-1.224c-.346-.105-.579-.18-.401-.649.386-1.018.426-1.898.001-2.521-.797-1.17-2.982-1.105-5.452-.03 0 0-.782.345-.583-.279.387-1.205.328-2.21-.273-2.789-1.368-1.317-5.009.052-8.134 3.06C.672 11.857-.32 14.063-.32 15.899c0 3.514 4.516 5.652 8.931 5.652 5.787 0 9.635-3.359 9.635-6.026 0-1.613-1.361-2.525-2.157-2.876zm2.631-6.019c-.685-.756-1.7-1.103-2.691-.982-.305.039-.449-.17-.395-.45.055-.288.229-.456.547-.504 1.317-.178 2.674.292 3.55 1.26.878.972 1.18 2.283.869 3.504-.093.372-.344.453-.593.203-.239-.24-.329-.527-.246-.867.236-.967-.048-2.009-.841-2.764zm1.457-2.863c-1.34-1.478-3.324-2.142-5.307-1.894-.377.045-.602-.235-.536-.638.067-.399.341-.629.735-.681 2.41-.32 4.842.481 6.485 2.291 1.644 1.812 2.224 4.254 1.689 6.544-.103.439-.417.528-.696.206-.263-.297-.344-.621-.255-.996.433-1.86-.046-3.881-1.315-4.832z"/>
                  </svg>
                  <span style="margin-left: 8px">微博</span>
                </el-dropdown-item>
                <el-dropdown-item command="qq">
                  <svg class="share-icon" viewBox="0 0 24 24" width="18" height="18">
                    <path fill="#12B7F5" d="M21.395 15.035a39.548 39.548 0 0 0-.803-2.264l-1.079-2.695c.001-.032.014-.396.014-.396 0-4.636-3.023-8.39-6.756-8.39-.24 0-.48.015-.718.044C8.71 1.834 5.626 5.632 5.626 9.676c0 .24.015.48.044.717l-.941 2.352a39.69 39.69 0 0 0-.784 2.29c-.252.9-.39 1.655-.39 2.21 0 .592.148.996.469 1.262.339.28.834.345 1.377.345.932 0 2.107-.288 3.14-.598-.016.133-.03.265-.03.4 0 1.053.386 2.02 1.063 2.79-.48.225-.906.455-1.285.692-.823.517-1.31 1.074-1.31 1.77 0 .968 1.144 1.575 3.145 1.575 1.496 0 3.054-.357 4.063-.996.957.64 2.503.996 3.998.996 2.001 0 3.146-.607 3.146-1.575 0-.696-.488-1.253-1.31-1.77-.38-.237-.806-.467-1.285-.692.677-.77 1.063-1.737 1.063-2.79 0-.135-.014-.267-.03-.4 1.033.31 2.208.598 3.14.598.543 0 1.038-.065 1.377-.345.321-.266.469-.67.469-1.263 0-.554-.138-1.31-.39-2.21z"/>
                  </svg>
                  <span style="margin-left: 8px">QQ</span>
                </el-dropdown-item>
                <el-dropdown-item divided command="copy">
                  <el-icon><Link /></el-icon>
                  <span style="margin-left: 8px">复制链接</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button
            size="large"
            class="action-btn"
            :type="isRetroMode ? 'info' : 'default'"
            round
            @click="toggleRetroMode"
          >
            <el-icon><Reading /></el-icon>
            <span>{{ isRetroMode ? '退出复古' : '复古模式' }}</span>
          </el-button>
          <el-button
            size="large"
            class="action-btn"
            round
            @click="exportToPDF"
            :loading="exportLoading"
          >
            <el-icon><Download /></el-icon>
            <span>导出 PDF</span>
          </el-button>
        </div>
        
        <!-- 第二行：AI功能按钮 -->
        <div class="actions-row ai-actions-row">
          <el-button
            size="large"
            class="action-btn ai-btn"
            round
            @click="goToSummary"
          >
            <el-icon><Document /></el-icon>
            <span>文章摘要</span>
          </el-button>
          <el-button
            size="large"
            class="action-btn ai-btn"
            round
            @click="goToSentimentAnalysis"
          >
            <el-icon><Histogram /></el-icon>
            <span>情感分析</span>
          </el-button>
        </div>
      </div>
    </el-card>
    
    <!-- 微信分享弹窗 -->
    <el-dialog
      v-model="showWechatDialog"
      title="微信扫码分享"
      width="360px"
      center
      class="wechat-share-dialog"
    >
      <div class="wechat-share-content">
        <div class="qrcode-wrapper">
          <div ref="qrcodeRef" class="qrcode-container"></div>
        </div>
        <p class="share-tip">打开微信扫一扫，分享给好友</p>
        <p class="share-title">{{ article?.title }}</p>
      </div>
    </el-dialog>
    
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
        <el-avatar :size="40" class="comment-avatar" :src="userStore.user?.avatar">
          {{ !userStore.user?.avatar ? userStore.user?.realName?.[0] : '' }}
        </el-avatar>
        <div class="comment-input-wrapper">
          <div class="mention-container">
            <el-input
              ref="commentInputRef"
              v-model="commentContent"
              type="textarea"
              :rows="3"
              placeholder="发表你的看法... 输入 @ 可以提及用户"
              class="comment-textarea"
              @input="handleMentionInput"
              @keydown="handleMentionKeydown"
            />
            <!-- @提及用户下拉框 -->
            <div v-if="showMentionList" class="mention-dropdown">
              <div class="mention-header">选择要提及的用户</div>
              <div 
                v-for="(user, index) in mentionUsers" 
                :key="user.id" 
                class="mention-item"
                :class="{ 'is-active': mentionActiveIndex === index }"
                @click="selectMentionUser(user)"
                @mouseenter="mentionActiveIndex = index"
              >
                <el-avatar :size="28" :src="user.avatar">
                  {{ user.realName?.[0] }}
                </el-avatar>
                <div class="mention-info">
                  <span class="mention-name">{{ user.realName }}</span>
                  <span class="mention-username">@{{ user.username }}</span>
                </div>
              </div>
              <div v-if="mentionUsers.length === 0" class="mention-empty">
                未找到匹配的用户
              </div>
            </div>
          </div>
          <div class="comment-actions">
            <span class="comment-tip">💡 输入 @ 提及用户</span>
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
          <el-avatar :size="44" class="comment-avatar" :src="comment.user?.avatar">
            {{ !comment.user?.avatar ? comment.user?.realName?.[0] : '' }}
          </el-avatar>
          
          <div class="comment-content">
            <div class="comment-user-info">
              <span class="comment-user">{{ comment.user?.realName }}</span>
              <!-- AI 机器人标识 -->
              <el-tag 
                v-if="comment.user?.username === 'WHUTGPT'" 
                type="primary" 
                size="small" 
                effect="dark"
                class="ai-robot-tag"
              >
                🤖 AI
              </el-tag>
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
                <el-avatar :size="32" class="reply-avatar" :src="reply.user?.avatar">
                  {{ !reply.user?.avatar ? reply.user?.realName?.[0] : '' }}
                </el-avatar>
                <div class="reply-content">
                  <div class="reply-user-info">
                    <span class="reply-user">{{ reply.user?.realName }}</span>
                    <!-- AI 机器人标识 -->
                    <el-tag 
                      v-if="reply.user?.username === 'WHUTGPT'" 
                      type="primary" 
                      size="small" 
                      effect="dark"
                      class="ai-robot-tag"
                    >
                      🤖 AI
                    </el-tag>
                    <span v-if="reply.replyToUser" class="reply-to">
                      回复 <span class="reply-to-name">@{{ reply.replyToUser.realName }}</span>
                    </span>
                    <span class="reply-time">{{ formatTime(reply.createdAt) }}</span>
                  </div>
                  <div class="reply-text">{{ reply.content }}</div>
                  <div class="reply-footer">
                    <el-button text size="small" @click="handleReplyToReply(comment, reply)">
                      <el-icon><ChatDotRound /></el-icon>
                      回复
                    </el-button>
                    <el-button
                      v-if="userStore.user?.id === reply.userId"
                      text
                      size="small"
                      type="danger"
                      @click="handleDeleteComment(reply.id)"
                    >
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 回复输入框 -->
            <div v-if="replyTo?.id === comment.id" class="reply-form">
              <div class="mention-container">
                <el-input
                  v-model="replyContent"
                  type="textarea"
                  :rows="2"
                  :placeholder="replyPlaceholder"
                  class="reply-input"
                  @input="handleReplyMentionInput"
                  @keydown="handleReplyMentionKeydown"
                />
                <!-- 回复框@提及用户下拉框 -->
                <div v-if="showReplyMentionList" class="mention-dropdown">
                  <div class="mention-header">选择要提及的用户</div>
                  <div 
                    v-for="(user, index) in mentionUsers" 
                    :key="user.id" 
                    class="mention-item"
                    :class="{ 'is-active': mentionActiveIndex === index }"
                    @click="selectReplyMentionUser(user)"
                    @mouseenter="mentionActiveIndex = index"
                  >
                    <el-avatar :size="28" :src="user.avatar">
                      {{ user.realName?.[0] }}
                    </el-avatar>
                    <div class="mention-info">
                      <span class="mention-name">{{ user.realName }}</span>
                      <span class="mention-username">@{{ user.username }}</span>
                    </div>
                  </div>
                  <div v-if="mentionUsers.length === 0" class="mention-empty">
                    未找到匹配的用户
                  </div>
                </div>
              </div>
              <div class="reply-form-actions">
                <el-button size="small" @click="cancelReply">取消</el-button>
                <el-button type="primary" size="small" @click="submitReply" :loading="commentLoading">
                  回复
                </el-button>
              </div>
            </div>
          </div>
        </div>
        
        <el-empty v-if="comments.length === 0" description="暂无评论，快来抢沙发吧~" />
      </div>
    </el-card>
        </div>
      </el-col>
      
      <!-- 右侧侧边栏 -->
      <el-col :xs="24" :sm="24" :md="7" :lg="7">
        <div class="sidebar">
          <!-- 作者信息卡片 -->
          <el-card v-if="article" class="author-card" shadow="never">
            <div class="author-card-header">
              <el-avatar :size="64" class="author-card-avatar" :src="article.author?.avatar">
                {{ !article.author?.avatar ? article.author?.realName?.[0] : '' }}
              </el-avatar>
              <div class="author-card-info">
                <h4 class="author-card-name">{{ article.author?.realName }}</h4>
                <p class="author-card-desc">@{{ article.author?.username }}</p>
              </div>
            </div>
            
            <div class="author-stats">
              <div class="author-stat-item">
                <span class="stat-value">{{ authorStats.articleCount || 0 }}</span>
                <span class="stat-label">文章</span>
              </div>
              <div class="author-stat-item">
                <span class="stat-value">{{ authorStats.followerCount || 0 }}</span>
                <span class="stat-label">粉丝</span>
              </div>
              <div class="author-stat-item">
                <span class="stat-value">{{ authorStats.totalViews || 0 }}</span>
                <span class="stat-label">获赞</span>
              </div>
            </div>
            
            <el-button
              v-if="userStore.isLogin && article.author?.id !== userStore.user?.id"
              :type="isFollowingAuthor ? 'default' : 'primary'"
              class="follow-btn-large"
              @click="handleFollowAuthor"
            >
              <el-icon v-if="!isFollowingAuthor"><Plus /></el-icon>
              {{ isFollowingAuthor ? '已关注' : '关注作者' }}
            </el-button>
            
            <el-button
              v-if="userStore.isLogin && article.author?.id === userStore.user?.id"
              type="info"
              class="follow-btn-large"
              disabled
            >
              这是我自己
            </el-button>
          </el-card>
          
          <!-- 热门新闻榜 -->
          <el-card class="hot-news-card" shadow="never">
            <template #header>
              <div class="card-header">
                <el-icon color="#f56c6c"><Flame /></el-icon>
                <span>热门新闻榜</span>
              </div>
            </template>
            <div class="hot-news-list">
              <div 
                v-for="(news, index) in hotNewsList" 
                :key="news.id" 
                class="hot-news-item"
                @click="goToArticle(news.id)"
              >
                <span 
                  class="hot-rank" 
                  :class="{ 'rank-top': index < 3 }"
                >
                  {{ index + 1 }}
                </span>
                <div class="hot-news-info">
                  <p class="hot-news-title">{{ news.title }}</p>
                  <span class="hot-news-views">
                    <el-icon><View /></el-icon>
                    {{ formatViews(news.viewCount) }}
                  </span>
                </div>
              </div>
              <el-empty v-if="hotNewsList.length === 0" description="暂无数据" :image-size="60" />
            </div>
          </el-card>
          
          <!-- AI相似文章推荐 -->
          <RecommendationCard 
            v-if="article?.id"
            type="similar" 
            :article-id="article.id" 
            :count="5" 
            title="相似文章推荐"
            :show-refresh="true"
            class="similar-articles-card"
          />

          <!-- 作者其他文章 -->
          <el-card v-if="authorArticles.length > 0" class="author-articles-card" shadow="never">
            <template #header>
              <div class="card-header">
                <el-icon color="#409eff"><Document /></el-icon>
                <span>作者其他文章</span>
              </div>
            </template>
            <div class="author-articles-list">
              <div 
                v-for="item in authorArticles" 
                :key="item.id" 
                class="author-article-item"
                @click="goToArticle(item.id)"
              >
                <p class="author-article-title">{{ item.title }}</p>
                <div class="author-article-meta">
                  <span>{{ formatDate(item.createdAt) }}</span>
                  <span>
                    <el-icon><View /></el-icon>
                    {{ item.viewCount }}
                  </span>
                </div>
              </div>
            </div>
          </el-card>
          
          <!-- 快捷操作 -->
          <el-card class="quick-actions-card" shadow="never">
            <template #header>
              <div class="card-header">
                <el-icon color="#67c23a"><Operation /></el-icon>
                <span>快捷操作</span>
              </div>
            </template>
            <div class="quick-actions">
              <el-button 
                :type="article?.isLiked ? 'primary' : 'default'" 
                @click="handleLike"
                class="quick-action-btn"
              >
                <el-icon><Star /></el-icon>
                点赞 {{ article?.likeCount || 0 }}
              </el-button>
              <el-button 
                :type="article?.isFavorited ? 'warning' : 'default'" 
                @click="handleFavorite"
                class="quick-action-btn"
              >
                <el-icon><Collection /></el-icon>
                收藏
              </el-button>
              <el-button class="quick-action-btn" @click="scrollToComments">
                <el-icon><ChatDotRound /></el-icon>
                评论 {{ comments.length }}
              </el-button>
              <el-button class="quick-action-btn" @click="scrollToTop">
                <el-icon><Top /></el-icon>
                回顶部
              </el-button>
            </div>
          </el-card>
        </div>
      </el-col>
    </el-row>
    
    <!-- 返回顶部按钮 -->
    <BackToTop />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getArticleDetail, toggleLike, toggleFavorite, getArticleList } from '@/api/article'
import { getCommentList, createComment, deleteComment } from '@/api/comment'
import { toggleFollow, checkFollow, getFollowStats } from '@/api/follow'
import { searchUsers } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Link, Histogram, Document, Reading, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { htmlToText } from '@/utils/htmlParser'
import QRCode from 'qrcode'
import BackToTop from '@/components/BackToTop.vue'
import RecommendationCard from '@/components/RecommendationCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const commentLoading = ref(false)
const exportLoading = ref(false)
const article = ref(null)
const comments = ref([])
const commentContent = ref('')
const replyTo = ref(null)  // 当前回复的根评论
const replyToUser = ref(null)  // 当前回复的目标用户
const replyContent = ref('')  // 回复内容
const isFollowingAuthor = ref(false)
const isRetroMode = ref(false)

const toggleRetroMode = () => {
  isRetroMode.value = !isRetroMode.value
  if (isRetroMode.value) {
    ElMessage.success('已进入复古报纸模式')
  } else {
    ElMessage.success('已退出复古模式')
  }
}

// 分享相关
const showWechatDialog = ref(false)
const qrcodeRef = ref(null)

// 回复提示文字
const replyPlaceholder = computed(() => {
  if (replyToUser.value) {
    return `回复 @${replyToUser.value.realName}`
  }
  return '写下你的回复...'
})

// @ 提及用户相关
const commentInputRef = ref(null)
const showMentionList = ref(false)
const showReplyMentionList = ref(false)  // 回复框的提及列表
const mentionUsers = ref([])
const mentionActiveIndex = ref(0)
const mentionStartPos = ref(0)
const replyMentionStartPos = ref(0)  // 回复框的提及起始位置

// 侧边栏数据
const authorStats = ref({ articleCount: 0, followerCount: 0, totalViews: 0 })
const hotNewsList = ref([])
const authorArticles = ref([])

const fetchArticle = async () => {
  loading.value = true
  try {
    article.value = await getArticleDetail(route.params.id)
    // 检查是否已关注作者
    if (userStore.isLogin && article.value?.author?.id) {
      checkFollowStatus()
    }
    // 获取作者相关数据
    if (article.value?.author?.id) {
      fetchAuthorStats()
      fetchAuthorArticles()
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 获取作者统计数据
const fetchAuthorStats = async () => {
  try {
    const stats = await getFollowStats(article.value.author.id)
    authorStats.value = {
      articleCount: stats.articleCount || 0,
      followerCount: stats.followerCount || 0,
      totalViews: stats.totalLikes || 0
    }
  } catch (error) {
    console.error('获取作者统计失败:', error)
  }
}

// 获取热门新闻列表
const fetchHotNews = async () => {
  try {
    const result = await getArticleList({ 
      current: 1, 
      size: 5, 
      sortBy: 'views',
      sortOrder: 'desc'
    })
    // 确保只取前5条
    hotNewsList.value = (result.records || []).slice(0, 5)
  } catch (error) {
    console.error('获取热门新闻失败:', error)
  }
}

// 获取作者其他文章
const fetchAuthorArticles = async () => {
  try {
    const result = await getArticleList({
      current: 1,
      size: 5,
      authorId: article.value.author.id
    })
    // 过滤掉当前文章
    authorArticles.value = (result.records || []).filter(
      item => item.id !== article.value.id
    ).slice(0, 4)
  } catch (error) {
    console.error('获取作者文章失败:', error)
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

// AI分析功能
const goToSentimentAnalysis = () => {
  if (!article.value) return
  
  // 提取纯文本内容
  const content = htmlToText(article.value.content || '', true)
  
  // 存储到sessionStorage
  sessionStorage.setItem('aiAnalysisContent', JSON.stringify({
    title: article.value.title,
    content: content,
    articleId: article.value.id
  }))
  
  // 跳转到情感分析页面
  router.push('/ai-sentiment')
}

const goToSummary = () => {
  if (!article.value) return
  
  // 提取纯文本内容
  const content = htmlToText(article.value.content || '', true)
  
  // 存储到sessionStorage
  sessionStorage.setItem('aiAnalysisContent', JSON.stringify({
    title: article.value.title,
    content: content,
    articleId: article.value.id
  }))
  
  // 跳转到文章摘要页面
  router.push('/ai-summary')
}

// 分享功能
const handleShare = async (platform) => {
  const shareUrl = window.location.href
  const shareTitle = article.value?.title || '校园新闻'
  const shareSummary = article.value?.summary || ''
  
  switch (platform) {
    case 'wechat':
      // 显示微信二维码弹窗
      showWechatDialog.value = true
      // 等待DOM更新后生成二维码
      setTimeout(async () => {
        if (qrcodeRef.value) {
          qrcodeRef.value.innerHTML = ''
          try {
            const canvas = document.createElement('canvas')
            await QRCode.toCanvas(canvas, shareUrl, {
              width: 200,
              margin: 2,
              color: {
                dark: '#333333',
                light: '#ffffff'
              }
            })
            qrcodeRef.value.appendChild(canvas)
          } catch (err) {
            console.error('生成二维码失败:', err)
            ElMessage.error('生成二维码失败')
          }
        }
      }, 100)
      break
      
    case 'weibo':
      // 分享到微博
      const weiboUrl = `https://service.weibo.com/share/share.php?url=${encodeURIComponent(shareUrl)}&title=${encodeURIComponent(shareTitle)}&pic=`
      window.open(weiboUrl, '_blank', 'width=600,height=500')
      ElMessage.success('已打开微博分享窗口')
      break
      
    case 'qq':
      // 分享到QQ
      const qqUrl = `https://connect.qq.com/widget/shareqq/index.html?url=${encodeURIComponent(shareUrl)}&title=${encodeURIComponent(shareTitle)}&summary=${encodeURIComponent(shareSummary)}`
      window.open(qqUrl, '_blank', 'width=600,height=500')
      ElMessage.success('已打开QQ分享窗口')
      break
      
    case 'copy':
      // 复制链接
      try {
        await navigator.clipboard.writeText(shareUrl)
        ElMessage.success('链接已复制到剪贴板')
      } catch (err) {
        // 降级方案
        const textarea = document.createElement('textarea')
        textarea.value = shareUrl
        textarea.style.position = 'fixed'
        textarea.style.opacity = '0'
        document.body.appendChild(textarea)
        textarea.select()
        document.execCommand('copy')
        document.body.removeChild(textarea)
        ElMessage.success('链接已复制到剪贴板')
      }
      break
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

// 回复顶级评论
const handleReply = (comment) => {
  replyTo.value = comment
  replyToUser.value = comment.user
  replyContent.value = ''
}

// 回复二级评论（回复的回复）
const handleReplyToReply = (rootComment, reply) => {
  replyTo.value = rootComment  // 仍然挂在顶级评论下
  replyToUser.value = reply.user  // 回复的目标是这个回复的作者
  replyContent.value = ''
}

// 取消回复
const cancelReply = () => {
  replyTo.value = null
  replyToUser.value = null
  replyContent.value = ''
}

// 提交回复
const submitReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  
  commentLoading.value = true
  try {
    await createComment({
      articleId: article.value.id,
      content: replyContent.value,
      parentId: replyTo.value.id,
      rootId: replyTo.value.id,  // 所有回复都归属于顶级评论
      replyToUserId: replyToUser.value?.id
    })
    ElMessage.success('回复成功')
    replyContent.value = ''
    replyTo.value = null
    replyToUser.value = null
    fetchComments()
  } catch (error) {
    console.error(error)
    ElMessage.error('回复失败')
  } finally {
    commentLoading.value = false
  }
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

// @ 提及用户 - 处理输入
const handleMentionInput = (value) => {
  const text = value || commentContent.value
  const lastAtIndex = text.lastIndexOf('@')
  
  if (lastAtIndex !== -1) {
    const afterAt = text.slice(lastAtIndex + 1)
    // 检查 @ 后面是否有空格（表示已完成选择）
    if (!afterAt.includes(' ') && afterAt.length <= 20) {
      mentionStartPos.value = lastAtIndex
      searchMentionUsers(afterAt)
      showMentionList.value = true
      return
    }
  }
  
  showMentionList.value = false
}

// 搜索可提及的用户（从后端搜索所有用户）
const searchMentionUsers = async (keyword) => {
  try {
    // 调用后端 API 搜索用户
    const result = await searchUsers({
      current: 1,
      size: 8,
      keyword: keyword || ''
    })
    
    // 过滤掉当前用户
    const users = (result.records || []).filter(user => 
      user.id !== userStore.user?.id
    )
    
    mentionUsers.value = users.slice(0, 6)
    mentionActiveIndex.value = 0
  } catch (error) {
    console.error('搜索用户失败:', error)
    mentionUsers.value = []
  }
}

// 选择提及的用户
const selectMentionUser = (user) => {
  const text = commentContent.value
  const beforeMention = text.slice(0, mentionStartPos.value)
  const afterMention = text.slice(text.indexOf(' ', mentionStartPos.value) + 1) || ''
  
  commentContent.value = `${beforeMention}@${user.realName} ${afterMention}`
  showMentionList.value = false
  
  // 聚焦输入框
  commentInputRef.value?.focus()
}

// 键盘导航
const handleMentionKeydown = (e) => {
  if (!showMentionList.value) return
  
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    mentionActiveIndex.value = Math.min(mentionActiveIndex.value + 1, mentionUsers.value.length - 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    mentionActiveIndex.value = Math.max(mentionActiveIndex.value - 1, 0)
  } else if (e.key === 'Enter' && mentionUsers.value.length > 0) {
    e.preventDefault()
    selectMentionUser(mentionUsers.value[mentionActiveIndex.value])
  } else if (e.key === 'Escape') {
    showMentionList.value = false
  }
}

// 回复框 @ 提及 - 处理输入
const handleReplyMentionInput = (value) => {
  const text = value || replyContent.value
  const lastAtIndex = text.lastIndexOf('@')
  
  if (lastAtIndex !== -1) {
    const afterAt = text.slice(lastAtIndex + 1)
    if (!afterAt.includes(' ') && afterAt.length <= 20) {
      replyMentionStartPos.value = lastAtIndex
      searchMentionUsers(afterAt)
      showReplyMentionList.value = true
      return
    }
  }
  
  showReplyMentionList.value = false
}

// 回复框选择提及的用户
const selectReplyMentionUser = (user) => {
  const text = replyContent.value
  const beforeMention = text.slice(0, replyMentionStartPos.value)
  const afterAt = text.slice(replyMentionStartPos.value + 1)
  const spaceIndex = afterAt.indexOf(' ')
  const afterMention = spaceIndex !== -1 ? afterAt.slice(spaceIndex + 1) : ''
  
  replyContent.value = `${beforeMention}@${user.realName} ${afterMention}`
  showReplyMentionList.value = false
}

// 回复框键盘导航
const handleReplyMentionKeydown = (e) => {
  if (!showReplyMentionList.value) return
  
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    mentionActiveIndex.value = Math.min(mentionActiveIndex.value + 1, mentionUsers.value.length - 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    mentionActiveIndex.value = Math.max(mentionActiveIndex.value - 1, 0)
  } else if (e.key === 'Enter' && mentionUsers.value.length > 0) {
    e.preventDefault()
    selectReplyMentionUser(mentionUsers.value[mentionActiveIndex.value])
  } else if (e.key === 'Escape') {
    showReplyMentionList.value = false
  }
}

// 导出 PDF（使用浏览器打印功能）
const exportToPDF = () => {
  if (!article.value) return
  
  exportLoading.value = true
  
  // 创建打印窗口
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.error('请允许弹出窗口以导出 PDF')
    exportLoading.value = false
    return
  }
  
  // 构建打印内容
  const printContent = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <title>${article.value.title}</title>
      <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
          font-family: 'Microsoft YaHei', 'SimSun', sans-serif;
          padding: 40px;
          max-width: 800px;
          margin: 0 auto;
          color: #333;
          line-height: 1.8;
        }
        h1 {
          font-size: 26px;
          color: #2c3e50;
          margin-bottom: 20px;
          line-height: 1.4;
          border-bottom: 2px solid #667eea;
          padding-bottom: 15px;
        }
        .meta {
          color: #909399;
          font-size: 14px;
          margin-bottom: 25px;
          padding: 10px 0;
        }
        .meta span { margin-right: 20px; }
        .summary {
          color: #666;
          font-size: 15px;
          background: #f8f9fa;
          padding: 15px;
          border-radius: 8px;
          margin-bottom: 25px;
          border-left: 4px solid #667eea;
        }
        .content {
          font-size: 16px;
          line-height: 1.9;
        }
        .content img {
          max-width: 100%;
          height: auto;
          margin: 15px 0;
        }
        .content p { margin: 1em 0; }
        .content h2, .content h3 { margin: 1.5em 0 0.8em; color: #2c3e50; }
        .footer {
          margin-top: 40px;
          padding-top: 20px;
          border-top: 1px solid #eee;
          color: #999;
          font-size: 12px;
          text-align: center;
        }
        @media print {
          body { padding: 20px; }
          @page { margin: 1cm; }
        }
      </style>
    </head>
    <body>
      <h1>${article.value.title}</h1>
      <div class="meta">
        <span>📝 作者：${article.value.author?.realName || '未知'}</span>
        <span>📅 ${formatTime(article.value.createdAt)}</span>
        <span>👁️ ${article.value.viewCount} 次浏览</span>
      </div>
      ${article.value.summary ? `<div class="summary">${article.value.summary}</div>` : ''}
      <div class="content">${article.value.content}</div>
      <div class="footer">
        导出自 校园新闻发布系统 · ${new Date().toLocaleString('zh-CN')}
      </div>
      <script>
        window.onload = function() {
          window.print();
          window.onafterprint = function() { window.close(); }
        }
      <\/script>
    </body>
    </html>
  `
  
  printWindow.document.write(printContent)
  printWindow.document.close()
  
  ElMessage.success('已打开打印窗口，请选择"另存为 PDF"')
  exportLoading.value = false
}

// 返回上一页
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

// 返回首页
const goHome = () => {
  router.push('/')
}

// 返回对应板块
const goToBoard = () => {
  const boardType = article.value?.boardType
  if (boardType === 'MARKETPLACE') {
    router.push('/marketplace')
  } else if (boardType) {
    router.push(`/board/${boardType}`)
  } else {
    router.push('/')
  }
}

const getBoardTypeName = (type) => {
  const types = {
    OFFICIAL: '官方新闻',
    CAMPUS: '全校新闻',
    COLLEGE: '学院新闻',
    MARKETPLACE: '校园集市'
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

const formatDate = (time) => {
  return new Date(time).toLocaleDateString('zh-CN')
}

const formatViews = (views) => {
  if (views >= 10000) {
    return (views / 10000).toFixed(1) + 'w'
  }
  if (views >= 1000) {
    return (views / 1000).toFixed(1) + 'k'
  }
  return views
}

const goToArticle = (id) => {
  router.push(`/article/${id}`)
}

const scrollToComments = () => {
  document.querySelector('.comment-section')?.scrollIntoView({ behavior: 'smooth' })
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 监听路由变化，刷新页面数据
watch(() => route.params.id, (newId) => {
  if (newId) {
    fetchArticle()
    fetchComments()
  }
})

onMounted(() => {
  fetchArticle()
  fetchComments()
  fetchHotNews()
})
</script>

<style scoped>
.article-detail-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 返回导航栏 */
.back-nav-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 0;
  margin-bottom: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  color: #4b5563;
  font-weight: 500;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
  transform: translateX(-4px);
}

.nav-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #6b7280;
}

.breadcrumb-item {
  cursor: pointer;
  transition: color 0.2s;
}

.breadcrumb-item:hover {
  color: #667eea;
}

.breadcrumb-separator {
  font-size: 12px;
  color: #d1d5db;
}

.breadcrumb-current {
  color: #374151;
  font-weight: 500;
}

.main-content {
  margin-bottom: 24px;
}

:deep(.el-row) {
  align-items: flex-start !important;
}

:deep(.el-col) {
  display: flex;
  flex-direction: column;
}

/* 文章卡片 */
.article-card {
  border-radius: 16px;
  margin-bottom: 24px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  overflow: hidden;
  animation: fadeInUp 0.6s ease-out;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.author-section:hover .author-avatar {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.4);
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
.article-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  justify-content: center;
  align-items: center;
  margin-top: 32px;
  padding: 24px 20px;
  border-top: 1px solid #e4e7ed;
  background: rgba(250, 250, 250, 0.5);
  backdrop-filter: blur(10px);
  border-radius: 12px;
}

.actions-row {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.ai-actions-row {
  padding-top: 12px;
  border-top: 1px dashed #e4e7ed;
  width: 100%;
}

.action-btn {
  font-weight: 500;
}

/* AI分析按钮样式 */
.ai-btn {
  background: rgba(102, 126, 234, 0.1) !important;
  backdrop-filter: blur(10px) saturate(150%) !important;
  -webkit-backdrop-filter: blur(10px) saturate(150%) !important;
  border: 2px solid rgba(102, 126, 234, 0.2) !important;
  color: #667eea !important;
  font-weight: 600 !important;
  padding: 12px 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1),
              0 0 0 1px rgba(255, 255, 255, 0.5) inset !important;
}

.ai-btn:hover {
  background: rgba(102, 126, 234, 0.15) !important;
  border-color: rgba(102, 126, 234, 0.3) !important;
  color: #667eea !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2),
              0 0 0 1px rgba(255, 255, 255, 0.6) inset !important;
}

.ai-btn .el-icon {
  font-size: 18px !important;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 评论区 */
.comment-section {
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
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
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.comment-tip {
  font-size: 13px;
  color: #909399;
}

/* @ 提及用户 */
.mention-container {
  position: relative;
}

.mention-dropdown {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border: 1px solid #e4e7ed;
  margin-bottom: 8px;
  max-height: 250px;
  overflow-y: auto;
  z-index: 100;
  animation: mentionSlideUp 0.2s ease-out;
}

@keyframes mentionSlideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.mention-header {
  padding: 10px 12px;
  font-size: 12px;
  color: #909399;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.mention-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mention-item:hover,
.mention-item.is-active {
  background: #f5f7fa;
}

.mention-item.is-active {
  background: #ecf5ff;
}

.mention-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mention-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.mention-username {
  font-size: 12px;
  color: #909399;
}

.mention-empty {
  padding: 20px;
  text-align: center;
  color: #909399;
  font-size: 13px;
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

/* AI 机器人标签样式 */
.ai-robot-tag {
  margin-left: 6px;
  font-size: 11px;
  padding: 0 6px;
  height: 18px;
  line-height: 16px;
  border-radius: 9px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
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

/* 回复目标 */
.reply-to {
  font-size: 13px;
  color: #909399;
  margin-left: 8px;
}

.reply-to-name {
  color: #409eff;
  font-weight: 500;
}

/* 回复按钮区 */
.reply-footer {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.reply-footer .el-button {
  font-size: 12px;
}

/* 回复输入框 */
.reply-form {
  margin-top: 16px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.reply-input {
  margin-bottom: 12px;
}

.reply-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* ========== 侧边栏样式 ========== */
.sidebar {
  position: sticky;
  top: 80px;
  margin-top: 0;
}

/* 作者卡片 */
.author-card {
  border-radius: 16px;
  margin-bottom: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
}

.author-card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.author-card-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 24px;
  font-weight: 700;
  flex-shrink: 0;
}

.author-card-info {
  flex: 1;
  min-width: 0;
}

.author-card-name {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.author-card-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.author-stats {
  display: flex;
  justify-content: space-around;
  padding: 16px 0;
  margin-bottom: 16px;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.author-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.follow-btn-large {
  width: 100%;
  height: 40px;
  font-size: 15px;
  font-weight: 600;
}

/* 热门新闻榜 */
.hot-news-card {
  border-radius: 16px;
  margin-bottom: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.hot-news-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-news-item {
  display: flex;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.hot-news-item:hover {
  background: #f5f7fa;
}

.hot-rank {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 700;
  background: #e4e7ed;
  color: #909399;
  flex-shrink: 0;
}

.hot-rank.rank-top {
  background: linear-gradient(135deg, #f56c6c, #e6a23c);
  color: white;
}

.hot-news-info {
  flex: 1;
  min-width: 0;
}

.hot-news-title {
  margin: 0 0 6px;
  font-size: 14px;
  color: #2c3e50;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.hot-news-views {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

/* 作者其他文章 */
.author-articles-card {
  border-radius: 16px;
  margin-bottom: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
}

.author-articles-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.author-article-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}

.author-article-item:hover {
  background: #f5f7fa;
  border-left-color: #409eff;
}

.author-article-title {
  margin: 0 0 8px;
  font-size: 14px;
  color: #2c3e50;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.author-article-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.author-article-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 快捷操作 */
.quick-actions-card {
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 
              0 0 0 1px rgba(255, 255, 255, 0.4) inset;
}

.quick-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.quick-action-btn {
  width: 100%;
  font-size: 13px;
}

/* 分享下拉菜单图标 */
.share-icon {
  vertical-align: middle;
}

/* 微信分享弹窗 */
.wechat-share-content {
  text-align: center;
  padding: 20px 0;
}

.qrcode-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.qrcode-container {
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.qrcode-container canvas {
  display: block;
}

.share-tip {
  color: #909399;
  font-size: 14px;
  margin-bottom: 12px;
}

.share-title {
  color: #2c3e50;
  font-size: 15px;
  font-weight: 500;
  padding: 0 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .article-detail-page {
    padding: 0 12px;
  }
  
  .sidebar {
    position: static;
    margin-top: 24px;
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

/* ========== 复古报纸模式样式 ========== */
.retro-newspaper-mode {
  background-color: #f4f1ea !important;
  background-image: linear-gradient(0deg, transparent 24%, rgba(0, 0, 0, .05) 25%, rgba(0, 0, 0, .05) 26%, transparent 27%, transparent 74%, rgba(0, 0, 0, .05) 75%, rgba(0, 0, 0, .05) 76%, transparent 77%, transparent), linear-gradient(90deg, transparent 24%, rgba(0, 0, 0, .05) 25%, rgba(0, 0, 0, .05) 26%, transparent 27%, transparent 74%, rgba(0, 0, 0, .05) 75%, rgba(0, 0, 0, .05) 76%, transparent 77%, transparent) !important;
  background-size: 50px 50px !important;
  border: 1px solid #333 !important;
  box-shadow: 10px 10px 0 rgba(0,0,0,0.2) !important;
  color: #1a1a1a !important;
  font-family: "Times New Roman", "Songti SC", "SimSun", serif !important;
  padding: 0 !important;
  border-radius: 2px !important;
  backdrop-filter: none !important;
}

.retro-newspaper-mode .article-header {
  padding-top: 20px;
  text-align: center;
  border-bottom: 3px double #333;
}

.retro-newspaper-mode .article-title {
  font-family: "Impact", "Times New Roman", serif !important;
  font-size: 48px !important;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 10px !important;
  color: #000 !important;
  text-shadow: 2px 2px 0 rgba(0,0,0,0.1);
}

.retro-newspaper-mode .header-meta {
  display: none;
}

.retro-newspaper-mode .article-info {
  justify-content: center;
  border-top: 1px solid #333;
  border-bottom: 1px solid #333;
  padding: 10px 0;
  margin-top: 20px;
  font-style: italic;
  font-family: "Times New Roman", serif;
}

.retro-newspaper-mode .author-section {
  flex-direction: row;
}

.retro-newspaper-mode .author-avatar {
  filter: grayscale(100%);
  border: 1px solid #333;
}

.retro-newspaper-mode .article-cover {
  float: left;
  width: 50%;
  max-width: 500px;
  padding: 0;
  margin: 30px 30px 20px 40px;
  filter: grayscale(100%) contrast(1.2);
  position: relative;
  z-index: 1;
}

.retro-newspaper-mode .cover-image {
  width: 100%;
  height: auto;
  max-height: 400px;
  border: 4px solid #333;
  border-radius: 0;
  box-shadow: 8px 8px 0 rgba(0,0,0,0.15);
  object-fit: cover;
}

.retro-newspaper-mode .article-body {
  padding: 20px 40px;
  columns: auto !important; /* 取消多列布局以支持文字环绕 */
  text-align: justify;
  min-height: 400px; /* 确保内容区高度 */
}

@media (max-width: 768px) {
  .retro-newspaper-mode .article-cover {
    float: none;
    width: calc(100% - 80px);
    margin: 20px 40px;
    max-width: none;
  }
}

.retro-newspaper-mode .article-content {
  font-family: "Georgia", "Times New Roman", serif !important;
  font-size: 18px !important;
  line-height: 1.8 !important;
  color: #222 !important;
}

.retro-newspaper-mode .article-content :deep(p) {
  margin-bottom: 1.5em;
  text-indent: 0; /* 取消首行缩进，因为有图片环绕 */
}

.retro-newspaper-mode .article-content :deep(p:first-of-type)::first-letter {
  font-size: 4em;
  float: left;
  line-height: 0.8;
  padding-right: 0.1em;
  margin-top: 0.1em;
  font-family: "Times New Roman", serif;
  font-weight: bold;
  color: #000;
}

.retro-newspaper-mode .article-content :deep(img) {
  filter: grayscale(100%) contrast(1.1);
  border: 2px solid #333;
  border-radius: 0;
  box-shadow: none;
  max-width: 100%;
}

.retro-newspaper-mode .article-actions {
  clear: both; /* 确保操作按钮在浮动内容下方 */
  background: transparent;
  backdrop-filter: none;
  border-top: 3px double #333;
  border-radius: 0;
  margin: 40px 40px 20px;
  padding-top: 30px;
}

/* 复古头部Banner */
.retro-header-banner {
  padding: 20px 40px 0;
  text-align: center;
  border-bottom: 4px solid #333;
  background-color: #f4f1ea;
}

.retro-top-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  font-weight: bold;
  text-transform: uppercase;
  border-bottom: 1px solid #333;
  padding-bottom: 4px;
  margin-bottom: 10px;
}

.retro-brand {
  font-family: "Impact", "Arial Black", sans-serif;
  font-size: 64px;
  line-height: 1;
  margin: 10px 0;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.retro-sub-meta {
  display: flex;
  justify-content: space-between;
  border-top: 1px solid #333;
  border-bottom: 1px solid #333;
  padding: 6px 0;
  font-weight: bold;
  font-size: 14px;
  text-transform: uppercase;
}

/* 适配暗黑模式下的复古模式 */
.dark .retro-newspaper-mode {
  filter: brightness(0.9);
}
</style>
