<template>
  <div class="register-page">
    <!-- 校园背景 -->
    <div class="bg-image"></div>
    <!-- 半透明遮罩 -->
    <div class="bg-overlay"></div>

    <!-- 左上角校徽 -->
    <div class="logo-corner">
      <img src="@/assets/whut-logo.png" alt="武汉理工大学" class="school-logo" />
    </div>

    <!-- 主体内容区 -->
    <div class="main-content">
      <el-card class="register-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="50" class="header-icon"><Reading /></el-icon>
          <h2>加入我们</h2>
          <p>创建您的账号，开启精彩校园生活</p>
        </div>
      </template>
      
      <el-form :model="registerForm" :rules="rules" ref="formRef" label-width="90px" label-position="top">
        <div class="form-row">
          <el-form-item label="用户名" prop="username">
            <el-input 
              v-model="registerForm.username" 
              placeholder="字母、数字或下划线" 
              size="large"
              prefix-icon="User"
            />
          </el-form-item>
          
          <el-form-item label="密码" prop="password">
            <el-input 
              v-model="registerForm.password" 
              type="password" 
              placeholder="至少6位密码" 
              size="large"
              prefix-icon="Lock"
              show-password 
            />
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item label="真实姓名" prop="realName">
            <el-input 
              v-model="registerForm.realName" 
              placeholder="请输入真实姓名" 
              size="large"
            />
          </el-form-item>
          
          <el-form-item label="学号/工号" prop="studentId">
            <el-input 
              v-model="registerForm.studentId" 
              placeholder="请输入学号或工号" 
              size="large"
            />
          </el-form-item>
        </div>

        <el-form-item label="邮箱" prop="email">
          <el-input 
            v-model="registerForm.email" 
            placeholder="name@example.com" 
            size="large"
            prefix-icon="Message"
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input 
            v-model="registerForm.phone" 
            placeholder="11位手机号码" 
            size="large"
            prefix-icon="Phone"
          />
        </el-form-item>

        <div class="form-row">
          <el-form-item label="身份角色" prop="roleId">
            <el-select 
              v-model="registerForm.roleId" 
              placeholder="选择您的身份" 
              size="large"
              style="width: 100%"
            >
              <el-option label="🎓 学生" :value="3" />
              <el-option label="👨‍🏫 教师" :value="2" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="所属学院" prop="collegeId">
            <el-select 
              v-model="registerForm.collegeId" 
              placeholder="选择您的学院" 
              size="large"
              style="width: 100%"
            >
              <el-option
                v-for="college in colleges"
                :key="college.id"
                :label="college.name"
                :value="college.id"
              />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item style="margin-top: 30px;">
          <el-button 
            type="primary" 
            :loading="loading" 
            @click="handleRegister" 
            size="large"
            class="register-button"
          >
            <span v-if="!loading">立即注册</span>
            <span v-else>注册中...</span>
          </el-button>
        </el-form-item>
        
        <div class="footer-links">
          <span class="hint-text">已有账号？</span>
          <router-link to="/login" class="login-link">立即登录</router-link>
        </div>
      </el-form>
    </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCollegeList } from '@/api/college'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const colleges = ref([])

const registerForm = ref({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  roleId: null,
  collegeId: null,
  studentId: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  collegeId: [{ required: true, message: '请选择学院', trigger: 'change' }]
}

const fetchColleges = async () => {
  try {
    colleges.value = await getCollegeList()
  } catch (error) {
    console.error(error)
  }
}

const handleRegister = async () => {
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.register(registerForm.value)
        ElMessage.success('注册成功')
        router.push('/')
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  fetchColleges()
})
</script>

<style scoped>
/* 页面容器 */
.register-page {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

/* 校园背景 */
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
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.register-card {
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(30px) saturate(150%);
  -webkit-backdrop-filter: blur(30px) saturate(150%);
  border-radius: 24px;
  padding: 0;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.2), 
              0 0 0 1px rgba(255, 255, 255, 0.5) inset,
              0 2px 4px rgba(255, 255, 255, 0.8) inset;
  width: 100%;
  max-width: 750px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  animation: slideUp 0.8s ease-out 0.2s both;
  overflow: hidden;
}

.register-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 5px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 24px 24px 0 0;
  z-index: 1;
}

.register-card:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 12px 36px rgba(102, 126, 234, 0.5);
}

.card-header {
  text-align: center;
  padding: 30px 0 20px;
  position: relative;
}

.header-icon {
  color: #2196f3;
  margin-bottom: 15px;
  filter: drop-shadow(0 2px 8px rgba(33, 150, 243, 0.3));
}

.card-header h2 {
  margin: 15px 0 10px;
  font-size: 30px;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.card-header p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

/* 表单布局 */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.el-form-item {
  margin-bottom: 20px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
}

.register-card :deep(.el-input__wrapper) {
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05), 
              0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  transition: all 0.3s ease;
}

.register-card :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08), 
              0 0 0 1px rgba(255, 255, 255, 0.8) inset;
}

.register-card :deep(.el-input__wrapper.is-focus) {
  border-color: white;
  background: rgba(255, 255, 255, 0.6);
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.3), 
              0 8px 24px rgba(0, 0, 0, 0.1),
              0 0 0 1px white inset;
}

.register-card :deep(.el-input__inner) {
  color: #2c3e50;
  font-weight: 500;
}

.register-card :deep(.el-input__inner::placeholder) {
  color: rgba(44, 62, 80, 0.5);
}

.register-card :deep(.el-input__prefix) {
  color: #667eea;
}

.register-card :deep(.el-form-item__label) {
  color: #2c3e50;
  font-weight: 600;
}

.register-button {
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

.register-button::before {
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

.register-button:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 12px 36px rgba(79, 172, 254, 0.5);
}

.register-button:hover::before {
  width: 300px;
  height: 300px;
}

/* 底部链接 */
.footer-links {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.hint-text {
  color: #909399;
  font-size: 14px;
  margin-right: 8px;
}

.login-link {
  color: #667eea;
  font-weight: 600;
  font-size: 15px;
  transition: all 0.3s ease;
}

.login-link:hover {
  color: #764ba2;
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .register-container {
    padding: 20px 10px;
  }

  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .card-header h2 {
    font-size: 26px;
  }

  :deep(.el-form-item__label) {
    font-size: 14px;
  }
}
</style>
