<template>
  <div class="contact-page">
    <div class="page-header">
      <h1>联系方式</h1>
      <p>有任何问题或建议，欢迎与我们联系</p>
    </div>
    
    <div class="content-section">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-card class="contact-card">
            <template #header>
              <div class="card-header">
                <el-icon><Message /></el-icon>
                <span>联系信息</span>
              </div>
            </template>
            
            <div class="contact-item">
              <el-icon :size="24" color="#409eff"><Location /></el-icon>
              <div class="contact-info">
                <h4>地址</h4>
                <p>XX省XX市XX区XX路XX号 XX大学</p>
              </div>
            </div>
            
            <div class="contact-item">
              <el-icon :size="24" color="#67c23a"><Phone /></el-icon>
              <div class="contact-info">
                <h4>电话</h4>
                <p>0123-4567890</p>
              </div>
            </div>
            
            <div class="contact-item">
              <el-icon :size="24" color="#e6a23c"><Message /></el-icon>
              <div class="contact-info">
                <h4>邮箱</h4>
                <p>contact@campus-news.edu.cn</p>
              </div>
            </div>
            
            <div class="contact-item">
              <el-icon :size="24" color="#f56c6c"><Clock /></el-icon>
              <div class="contact-info">
                <h4>工作时间</h4>
                <p>周一至周五 8:00 - 17:00</p>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="12">
          <el-card class="contact-card">
            <template #header>
              <div class="card-header">
                <el-icon><EditPen /></el-icon>
                <span>在线留言</span>
              </div>
            </template>
            
            <el-form :model="form" label-position="top">
              <el-form-item label="您的姓名">
                <el-input v-model="form.name" placeholder="请输入姓名" />
              </el-form-item>
              <el-form-item label="联系邮箱">
                <el-input v-model="form.email" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item label="留言内容">
                <el-input 
                  v-model="form.message" 
                  type="textarea" 
                  :rows="4" 
                  placeholder="请输入您的留言内容"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitForm" style="width: 100%;">
                  提交留言
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Message, Location, Phone, Clock, EditPen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const form = ref({
  name: '',
  email: '',
  message: ''
})

const submitForm = () => {
  if (!form.value.name || !form.value.email || !form.value.message) {
    ElMessage.warning('请填写完整信息')
    return
  }
  ElMessage.success('留言提交成功，我们会尽快回复您！')
  form.value = { name: '', email: '', message: '' }
}
</script>

<style scoped>
.contact-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 40px 20px;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
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

.contact-card {
  border-radius: 12px;
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.contact-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.contact-item:last-child {
  border-bottom: none;
}

.contact-info h4 {
  margin: 0 0 4px;
  font-size: 15px;
  color: #303133;
}

.contact-info p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}
</style>
