# 🎓 AI板块答辩要点速查

> 这份文档帮助你快速理解项目中的AI功能，应对答辩提问

---

## 📌 一、AI功能概览

项目中有**三大AI功能模块**：

| 模块 | 功能 | 核心文件 |
|------|------|----------|
| AI聊天助手 | 智能问答、系统导航 | `AIService.java` |
| AI Agent | 自动执行任务（搜索、发布等） | `NewsAgentService.java` |
| 智能推荐 | 个性化内容推荐 | `RecommendationService.java` |

---

## 📌 二、AI聊天助手

### 工作原理
```
用户提问 → 后端构造Prompt → 调用大模型API → 返回回答
```

### 核心代码位置
- `AIService.java` - AI服务核心
- `AiController.java` - API接口

### 常见问题

**Q: 用的什么AI模型？**
> A: 支持三种模型：
> - Kimi（月之暗面）- 默认
> - DeepSeek（深度求索）
> - 豆包（字节跳动）

**Q: 怎么调用AI的？**
> A: 通过HTTP请求调用大模型的API接口，发送用户问题，接收AI回复

**Q: 什么是流式输出？**
> A: 像ChatGPT一样，AI回复一个字一个字显示出来，而不是等全部生成完再显示。
> 技术实现：使用SSE（Server-Sent Events）服务器推送

**Q: AI怎么知道系统数据的？**
> A: 在发送给AI之前，后端会先查询数据库，把相关数据作为上下文一起发给AI

---

## 📌 三、AI Agent（重点！）

### 什么是Agent？
```
Agent = 大语言模型 + 工具 + 记忆
```
- **大语言模型**：理解用户意图
- **工具**：执行具体操作（搜索、发布、统计）
- **记忆**：记住对话历史，支持多轮对话

### 与普通AI聊天的区别
| 普通AI聊天 | AI Agent |
|-----------|----------|
| 只能回答问题 | 可以执行操作 |
| 不能操作数据库 | 可以搜索、发布、统计 |
| 无状态 | 有记忆，支持多轮对话 |

### 工作流程示例
```
用户说："帮我搜索校园活动的新闻"
    ↓
Agent理解意图：用户想搜索文章
    ↓
Agent选择工具：调用searchArticles方法
    ↓
执行工具：查询数据库，关键词="校园活动"
    ↓
整理结果：将搜索结果组织成自然语言返回
```

### 核心代码位置
- `NewsAgentService.java` - Agent服务
- `NewsAgentTools.java` - Agent可用的工具方法
- `AgentController.java` - API接口

### 常见问题

**Q: 用的什么框架？**
> A: LangChain4j，是Java版的LangChain框架，专门用于构建AI应用

**Q: Agent怎么知道调用哪个工具？**
> A: 大模型会分析用户意图，根据工具的描述（@Tool注解）自动选择合适的工具

**Q: 工具有哪些？**
> A: 主要工具包括：
> - searchArticles - 搜索文章
> - getHotArticles - 获取热门文章
> - getSystemStats - 系统统计
> - createArticleDraft - 创建文章草稿
> - likeArticle - 点赞文章
> - postComment - 发表评论

**Q: 什么是会话记忆？**
> A: Agent会记住之前的对话内容，比如用户说"继续搜索"，Agent知道之前搜的是什么

---

## 📌 四、智能推荐系统

### 架构设计
```
前端请求 → Java后端 → Python算法服务 → 返回推荐结果
                ↓（如果Python服务不可用）
              降级到数据库热门查询
```

### 推荐算法（Python端实现）
1. **协同过滤**：基于用户行为相似度（你喜欢的，相似用户也喜欢）
2. **内容推荐**：基于内容相似度（看了A文章，推荐相似的B文章）
3. **混合推荐**：结合以上两种算法

### 核心代码位置
- `RecommendationService.java` - 推荐服务
- `RecommendationController.java` - API接口
- `algorithm/` 目录 - Python算法服务

### 常见问题

**Q: 为什么用Python做推荐算法？**
> A: Python有丰富的机器学习库（scikit-learn、pandas），更适合做数据分析和推荐算法

**Q: Java和Python怎么通信的？**
> A: 通过HTTP请求。Java后端调用Python服务的REST API

**Q: 什么是服务降级？**
> A: 当Python推荐服务不可用时，系统不会崩溃，而是返回热门内容作为兜底方案

**Q: 推荐结果是实时计算的吗？**
> A: 是的，每次请求都会调用算法服务实时计算

---

## 📌 五、技术栈总结

| 技术 | 用途 |
|------|------|
| Spring Boot | Java后端框架 |
| LangChain4j | AI Agent框架 |
| Kimi/DeepSeek API | 大语言模型 |
| RestTemplate | HTTP客户端，调用外部API |
| SSE | 流式输出，实时推送 |
| Redis | 缓存对话历史 |
| Python Flask | 推荐算法服务 |

---

## 📌 六、API接口速查

### AI聊天
```
POST /api/ai/chat
请求：{"question": "你好", "model": "kimi"}
响应：{"answer": "你好！我是校园新闻助手..."}
```

### Agent执行任务
```
POST /api/ai/agent/execute
请求：{"message": "搜索校园活动", "userId": 1}
响应：{"result": "找到5篇相关文章...", "success": true}
```

### 个性化推荐
```
GET /api/recommendation/personalized?count=10
响应：[{文章1}, {文章2}, ...]
```

### 视频推荐
```
GET /api/recommendation/video/personalized?count=10
响应：[{"videoId": 1, "score": 0.95, "reason": "推荐理由"}]
```

---

## 📌 七、答辩加分点

1. **微服务架构**：Java后端 + Python算法服务，职责分离
2. **服务降级**：推荐服务不可用时自动降级，保证系统稳定
3. **流式输出**：使用SSE实现类ChatGPT的打字机效果
4. **Agent模式**：不只是聊天，还能自动执行任务
5. **多模型支持**：支持Kimi、DeepSeek、豆包多种AI模型

---

祝答辩顺利！🎉
