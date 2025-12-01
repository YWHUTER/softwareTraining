<template>
  <div class="privacy-page">
    <div class="page-header">
      <h1>隐私政策</h1>
      <p>我们重视并保护您的隐私</p>
    </div>
    
    <div class="content-section">
      <el-card class="policy-card">
        <el-timeline>
          <el-timeline-item timestamp="第一条" placement="top">
            <el-card class="policy-item">
              <h3>信息收集</h3>
              <p>
                我们仅收集为您提供服务所必需的信息，包括但不限于：
              </p>
              <ul>
                <li>注册信息：用户名、邮箱、真实姓名等</li>
                <li>使用信息：浏览记录、发布内容等</li>
                <li>设备信息：设备类型、操作系统等（用于优化服务）</li>
              </ul>
            </el-card>
          </el-timeline-item>
          
          <el-timeline-item timestamp="第二条" placement="top">
            <el-card class="policy-item">
              <h3>信息使用</h3>
              <p>
                我们收集的信息将用于以下目的：
              </p>
              <ul>
                <li>提供、维护和改进我们的服务</li>
                <li>处理您的请求和反馈</li>
                <li>向您发送服务相关通知</li>
                <li>保障系统安全和防止欺诈</li>
              </ul>
            </el-card>
          </el-timeline-item>
          
          <el-timeline-item timestamp="第三条" placement="top">
            <el-card class="policy-item">
              <h3>信息保护</h3>
              <p>
                我们采取多种安全措施保护您的个人信息：
              </p>
              <ul>
                <li>使用加密技术保护数据传输</li>
                <li>对敏感信息进行加密存储</li>
                <li>严格限制访问个人信息的员工范围</li>
                <li>定期进行安全审计和漏洞检测</li>
              </ul>
            </el-card>
          </el-timeline-item>
          
          <el-timeline-item timestamp="第四条" placement="top">
            <el-card class="policy-item">
              <h3>信息共享</h3>
              <p>
                我们不会向第三方出售、出租或交换您的个人信息，除非：
              </p>
              <ul>
                <li>获得您的明确同意</li>
                <li>法律法规要求</li>
                <li>保护我们或他人的权利、财产或安全所必需</li>
              </ul>
            </el-card>
          </el-timeline-item>
          
          <el-timeline-item timestamp="第五条" placement="top">
            <el-card class="policy-item">
              <h3>您的权利</h3>
              <p>
                您对自己的个人信息享有以下权利：
              </p>
              <ul>
                <li>访问和获取您的个人信息副本</li>
                <li>更正不准确或不完整的信息</li>
                <li>删除您的个人信息（在符合法规的情况下）</li>
                <li>撤回您之前给予的同意</li>
              </ul>
            </el-card>
          </el-timeline-item>
          
          <el-timeline-item timestamp="第六条" placement="top">
            <el-card class="policy-item">
              <h3>政策更新</h3>
              <p>
                我们可能会不时更新本隐私政策。如有重大变更，我们会通过系统通知或其他方式告知您。
                继续使用我们的服务即表示您同意更新后的隐私政策。
              </p>
              <p class="update-time">
                <strong>最后更新时间：</strong>2025年1月1日
              </p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </el-card>
      
      <el-card class="contact-tip">
        <el-icon :size="24" color="#409eff"><QuestionFilled /></el-icon>
        <div class="tip-content">
          <p>如果您对本隐私政策有任何疑问，请通过以下方式联系我们：</p>
          <p><strong>邮箱：</strong>privacy@campus-news.edu.cn</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { QuestionFilled } from '@element-plus/icons-vue'
</script>

<style scoped>
.privacy-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  color: white;
}

.page-header h1 {
  margin: 0 0 10px;
  font-size: 32px;
}

.page-header p {
  margin: 0;
  opacity: 0.9;
}

.policy-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.policy-item {
  border-radius: 8px;
}

.policy-item h3 {
  margin: 0 0 12px;
  color: #303133;
  font-size: 18px;
}

.policy-item p {
  color: #606266;
  line-height: 1.8;
  margin-bottom: 12px;
}

.policy-item ul {
  margin: 0;
  padding-left: 20px;
  color: #606266;
}

.policy-item li {
  line-height: 2;
}

.update-time {
  color: #909399;
  font-size: 14px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.contact-tip {
  border-radius: 12px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
}

.tip-content p {
  margin: 0 0 8px;
  color: #606266;
}

.tip-content p:last-child {
  margin-bottom: 0;
}
</style>
