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
  animation: fadeInLeft 0.8s ease-out;
}

@keyframes fadeInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.school-logo {
  height: 80px;
  width: auto;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.3));
  transition: transform 0.3s ease;
}

.school-logo:hover {
  transform: scale(1.05);
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
  animation: fadeInDown 0.8s ease-out;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.site-title {
  font-size: 42px;
  font-weight: 800;
  margin: 0 0 12px;
  color: white;
  text-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
  letter-spacing: -1px;
  animation: fadeInDown 0.8s ease-out;
}

.site-subtitle {
  margin: 12px 0 0;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  letter-spacing: 2px;
}

/* 登录卡片 */
.login-box {
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(30px) saturate(150%);
  -webkit-backdrop-filter: blur(30px) saturate(150%);
  border-radius: 24px;
  padding: 50px 45px;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.2), 
              0 0 0 1px rgba(255, 255, 255, 0.5) inset,
              0 2px 4px rgba(255, 255, 255, 0.8) inset;
  width: 100%;
  max-width: 480px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  animation: slideUp 0.8s ease-out 0.2s both;
  position: relative;
  overflow: hidden;
}


/* Tab 切换 - 隐藏样式，保留结构 */
.login-tabs {
  display: none;
}

/* 登录表单 */
.login-form {
  padding: 40px 0 20px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-input :deep(.el-input__wrapper) {
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05), 
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  transition: all 0.3s ease;
}

.login-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08), 
              0 0 0 1px rgba(255, 255, 255, 0.8) inset;
}

.login-input :deep(.el-input__wrapper.is-focus) {
  border-color: white;
  background: rgba(255, 255, 255, 0.6);
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.3), 
              0 8px 24px rgba(0, 0, 0, 0.1),
              0 0 0 1px white inset;
}

.login-input :deep(.el-input__prefix) {
  color: #667eea;
  font-size: 18px;
}

.login-input :deep(.el-input__inner) {
  color: #2c3e50;
  font-weight: 500;
}

.login-input :deep(.el-input__inner::placeholder) {
  color: rgba(44, 62, 80, 0.5);
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  font-size: 17px;
  font-weight: 700;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border: none;
  box-shadow: 0 8px 24px rgba(79, 172, 254, 0.4);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: width 0.6s ease, height 0.6s ease;
}

.login-btn:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 12px 36px rgba(79, 172, 254, 0.5);
}

.login-btn:hover::before {
  width: 300px;
  height: 300px;
}

/* 注册链接 */
.register-row {
  text-align: center;
  padding: 0 0 30px;
  font-size: 15px;
  color: #606266;
}

.register-row a {
  color: #1890ff;
  margin-left: 6px;
  font-weight: 700;
  transition: all 0.3s ease;
}

.register-row a:hover {
  color: #096dd9;
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
