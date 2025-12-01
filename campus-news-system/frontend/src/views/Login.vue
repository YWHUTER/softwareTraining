<template>
  <div class="login-page">
    <!-- 校园俯视图背景 -->
    <div class="bg-image"></div>
    
    <!-- 半透明遮罩 -->
    <div class="bg-overlay"></div>

    <!-- 左上角校徽 -->
    <div class="logo-corner">
      <img src="@/assets/whut-logo.png" alt="武汉理工大学" class="school-logo" />
    </div>

    <!-- 主体内容区 -->
    <div class="main-content">
      <!-- 顶部标题区 -->
      <div class="header-section">
        <h1 class="site-title">校园新闻发布系统</h1>
        <p class="site-subtitle">您正在登录身份认证中心</p>
      </div>

      <!-- 登录卡片 -->
      <div class="login-box">
        <!-- Tab 切换（仅显示账号登录） -->
        <div class="login-tabs">
          <div class="tab-item active">
            <el-icon><User /></el-icon>
            <span>账号登录</span>
          </div>
        </div>

        <!-- 登录表单 -->
        <el-form :model="loginForm" :rules="rules" ref="formRef" @submit.prevent="handleLogin" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名/学号/工号"
              size="large"
              class="login-input"
              clearable
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              class="login-input"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              size="large" 
              :loading="loading" 
              @click="handleLogin" 
              class="login-btn"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 注册链接 -->
        <div class="register-row">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </div>

      <!-- 温馨提示 -->
      <div class="tips-box">
        <p>温馨提示：登录账号为学工号、校园卡号、手机号。测试账号：admin / admin123</p>
        <p>如需帮助，请联系管理员</p>
      </div>

      <!-- 浏览器建议 -->
      <div class="browser-tips">
        <span>建议浏览器：</span>
        <span>Chrome</span>
        <span>Firefox</span>
        <span>Edge</span>
      </div>

      <!-- 版权信息 -->
      <div class="copyright">
        <p>Copyright © 2025 校园新闻发布系统, All Rights Reserved.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const loginForm = ref({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login(loginForm.value)
        ElMessage.success('登录成功！欢迎回来')
        router.push('/')
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
/* 页面容器 */
.login-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

/* 校园背景 - 武汉理工大学 */
.bg-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('@/assets/login-bg.jpg') center center / cover no-repeat;
  z-index: 0;
}

/* 半透明蓝色遮罩 */
.bg-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(10, 60, 120, 0.35);
  z-index: 1;
}

/* 左上角校徽 */
.logo-corner {
  position: absolute;
  top: 30px;
  left: 30px;
  z-index: 10;
}

.school-logo {
  height: 150px;
  width: auto;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
}

/* 主体内容区 */
.main-content {
  position: relative;
  z-index: 2;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

/* 顶部标题区 */
.header-section {
  text-align: center;
  margin-bottom: 30px;
}

.site-title {
  margin: 0;
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 4px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.site-subtitle {
  margin: 12px 0 0;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  letter-spacing: 2px;
}

/* 登录卡片 */
.login-box {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

/* Tab 切换 */
.login-tabs {
  display: flex;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  font-size: 15px;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 3px solid transparent;
}

.tab-item.active {
  color: #409eff;
  background: #fff;
  border-bottom-color: #409eff;
  font-weight: 600;
}

.tab-item:hover {
  color: #409eff;
}

/* 登录表单 */
.login-form {
  padding: 30px 30px 20px;
}

.login-input :deep(.el-input__wrapper) {
  padding: 12px 15px;
  background: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: none;
  transition: all 0.3s;
}

.login-input :deep(.el-input__wrapper:hover) {
  border-color: #c0c4cc;
}

.login-input :deep(.el-input__wrapper.is-focus) {
  border-color: #409eff;
  background: #fff;
}

.login-input :deep(.el-input__prefix) {
  color: #909399;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 8px;
  border-radius: 4px;
  background: #409eff;
  border: none;
}

.login-btn:hover {
  background: #66b1ff;
}

/* 注册链接 */
.register-row {
  text-align: center;
  padding: 0 30px 25px;
  font-size: 14px;
  color: #909399;
}

.register-row a {
  color: #409eff;
  margin-left: 5px;
}

.register-row a:hover {
  text-decoration: underline;
}

/* 温馨提示 */
.tips-box {
  margin-top: 25px;
  text-align: center;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  line-height: 1.8;
  max-width: 500px;
}

.tips-box p {
  margin: 0;
}

/* 浏览器建议 */
.browser-tips {
  margin-top: 20px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.browser-tips span {
  margin: 0 8px;
}

.browser-tips span:first-child {
  margin-left: 0;
}

/* 版权信息 */
.copyright {
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.copyright p {
  margin: 0;
}

/* 响应式 */
@media (max-width: 480px) {
  .site-title {
    font-size: 26px;
    letter-spacing: 2px;
  }
  
  .site-subtitle {
    font-size: 14px;
  }
  
  .login-box {
    margin: 0 15px;
  }
  
  .login-form {
    padding: 25px 20px 15px;
  }
  
  .login-btn {
    letter-spacing: 4px;
  }
  
  .tips-box {
    padding: 0 15px;
    font-size: 12px;
  }
}
</style>
