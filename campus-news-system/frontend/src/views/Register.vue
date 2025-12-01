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
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.register-card {
  width: 100%;
  max-width: 600px;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.card-header {
  text-align: center;
  padding-bottom: 15px;
}

.header-icon {
  color: #2196f3;
  margin-bottom: 15px;
  filter: drop-shadow(0 2px 8px rgba(33, 150, 243, 0.3));
}

.card-header h2 {
  margin: 0 0 10px;
  color: #2c3e50;
  font-size: 32px;
  font-weight: 700;
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

:deep(.el-input__wrapper) {
  padding: 12px 16px;
  background: #f8f9fa;
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  background: #fff;
  border-color: #e0e3e9;
}

:deep(.el-input__wrapper.is-focus) {
  background: #fff;
  border-color: #2196f3;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}

.register-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
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
  color: #2196f3;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s ease;
}

.login-link:hover {
  color: #1976d2;
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
