<template>
  <div class="publish-page">
    <el-card class="publish-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="24" color="#2196f3"><Edit /></el-icon>
            <h2>{{ isEdit ? '编辑文章' : '发布文章' }}</h2>
          </div>
          <div class="header-tip">
            <el-icon><Info /></el-icon>
            <span>请认真填写文章信息，确保内容真实准确</span>
          </div>
        </div>
      </template>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <!-- 基础信息区 -->
        <div class="form-section">
          <div class="section-title">
            <el-icon><Document /></el-icon>
            <span>基础信息</span>
          </div>
          
          <el-form-item label="文章标题" prop="title">
            <el-input 
              v-model="form.title" 
              placeholder="请输入一个吸引人的标题" 
              maxlength="200" 
              show-word-limit
              size="large"
              class="title-input"
            >
              <template #prefix>
                <el-icon><Tickets /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          
          <el-form-item label="文章摘要" prop="summary">
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="3"
              placeholder="简要描述文章内容，帮助读者快速了解"
              maxlength="500"
              show-word-limit
              class="summary-textarea"
            />
          </el-form-item>

          <div class="form-row">
            <el-form-item label="板块类型" prop="boardType" class="form-col">
              <el-select 
                v-model="form.boardType" 
                placeholder="选择发布板块" 
                size="large"
                style="width: 100%"
              >
                <el-option
                  v-if="userStore.isAdmin || userStore.isTeacher"
                  label="📢 官方新闻"
                  value="OFFICIAL"
                />
                <el-option label="🏫 全校新闻" value="CAMPUS" />
                <el-option
                  v-if="userStore.user?.collegeId"
                  label="🎓 学院新闻"
                  value="COLLEGE"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item 
              label="所属学院" 
              prop="collegeId" 
              v-if="form.boardType === 'COLLEGE'"
              class="form-col"
            >
              <el-select 
                v-model="form.collegeId" 
                placeholder="选择学院" 
                size="large"
                style="width: 100%" 
                disabled
              >
                <el-option
                  :label="userStore.user?.college?.name"
                  :value="userStore.user?.collegeId"
                />
              </el-select>
            </el-form-item>
          </div>
          
          <el-form-item label="封面图片" prop="coverImage">
            <el-input 
              v-model="form.coverImage" 
              placeholder="请输入图片URL（https://...）" 
              size="large"
            >
              <template #prefix>
                <el-icon><Picture /></el-icon>
              </template>
            </el-input>
            <div class="form-tip">
              <el-icon><Info /></el-icon>
              <span>建议尺寸：16:9，推荐使用高质量图片</span>
            </div>
          </el-form-item>
        </div>

        <!-- 内容编辑区 -->
        <div class="form-section">
          <div class="section-title">
            <el-icon><Edit /></el-icon>
            <span>文章内容</span>
          </div>
          
          <el-form-item prop="content">
            <div class="editor-wrapper">
              <QuillEditor
                ref="quillEditorRef"
                v-model:content="form.content"
                contentType="html"
                theme="snow"
                :options="editorOptions"
                class="custom-editor"
                @update:content="handleContentChange"
              />
            </div>
          </el-form-item>
        </div>
        
        <!-- 操作按钮 -->
        <el-form-item class="submit-section">
          <div class="button-group">
            <el-button 
              type="primary" 
              @click="handleSubmit" 
              :loading="loading"
              size="large"
              class="submit-btn"
            >
              <el-icon v-if="!loading"><Check /></el-icon>
              <span>{{ isEdit ? '保存修改' : '立即发布' }}</span>
            </el-button>
            <el-button 
              @click="$router.back()"
              size="large"
              class="cancel-btn"
            >
              <el-icon><Close /></el-icon>
              <span>取消</span>
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { createArticle, updateArticle, getArticleDetail } from '@/api/article'
import { ElMessage } from 'element-plus'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const quillEditorRef = ref(null)
const loading = ref(false)
const isEdit = ref(false)

const form = ref({
  title: '',
  summary: '',
  content: '',
  coverImage: '',
  boardType: '',
  collegeId: null,
  isPinned: 0
})

// 处理编辑器内容变化
const handleContentChange = (content) => {
  // 确保 content 是字符串类型的 HTML
  if (typeof content === 'string') {
    form.value.content = content
  }
}

const rules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入文章内容', trigger: 'blur' }],
  boardType: [{ required: true, message: '请选择板块类型', trigger: 'change' }]
}

// 富文本编辑器配置
const editorOptions = {
  modules: {
    toolbar: {
      container: [
        ['bold', 'italic', 'underline', 'strike'],
        ['blockquote', 'code-block'],
        [{ 'header': 1 }, { 'header': 2 }],
        [{ 'list': 'ordered'}, { 'list': 'bullet' }],
        [{ 'indent': '-1'}, { 'indent': '+1' }],
        [{ 'size': ['small', false, 'large', 'huge'] }],
        [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
        [{ 'color': [] }, { 'background': [] }],
        [{ 'align': [] }],
        ['link', 'image'],
        ['clean']
      ],
      handlers: {
        image: function() {
          const input = document.createElement('input')
          input.setAttribute('type', 'file')
          input.setAttribute('accept', 'image/*')
          input.click()
          
          input.onchange = () => {
            const file = input.files[0]
            if (file) {
              const reader = new FileReader()
              reader.onload = (e) => {
                const quill = this.quill
                const range = quill.getSelection(true)
                quill.insertEmbed(range.index, 'image', e.target.result)
                quill.setSelection(range.index + 1)
              }
              reader.readAsDataURL(file)
            }
          }
        }
      }
    }
  },
  placeholder: '在这里开始书写您的文章内容...'
}

watch(() => form.value.boardType, (newType) => {
  if (newType === 'COLLEGE') {
    form.value.collegeId = userStore.user?.collegeId
  } else {
    form.value.collegeId = null
  }
})

const handleSubmit = async () => {
  // 提交前确保获取最新的编辑器内容
  let htmlContent = form.value.content
  
  if (quillEditorRef.value) {
    // 尝试从编辑器实例获取内容
    const editor = quillEditorRef.value
    if (editor.getHTML) {
      htmlContent = editor.getHTML()
    } else if (editor.getQuill) {
      const quill = editor.getQuill()
      if (quill) {
        htmlContent = quill.root.innerHTML
      }
    }
  }
  
  // 调试日志
  console.log('提交的内容:', htmlContent)
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const submitData = {
          title: form.value.title,
          summary: form.value.summary,
          content: htmlContent,
          coverImage: form.value.coverImage,
          boardType: form.value.boardType,
          collegeId: form.value.collegeId,
          isPinned: form.value.isPinned
        }
        
        console.log('提交数据:', submitData)
        
        if (isEdit.value) {
          await updateArticle(route.query.id, submitData)
          ElMessage.success('更新成功！')
        } else {
          await createArticle(submitData)
          ElMessage.success('发布成功！文章正在审核中')
        }
        router.push('/')
      } catch (error) {
        console.error('提交失败:', error)
      } finally {
        loading.value = false
      }
    }
  })
}

const fetchArticle = async (id) => {
  try {
    const data = await getArticleDetail(id)
    form.value = {
      title: data.title,
      summary: data.summary,
      content: data.content,
      coverImage: data.coverImage,
      boardType: data.boardType,
      collegeId: data.collegeId,
      isPinned: data.isPinned
    }
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  if (route.query.id) {
    isEdit.value = true
    fetchArticle(route.query.id)
  }
})
</script>

<style scoped>
.publish-page {
  max-width: 1100px;
  margin: 0 auto;
}

.publish-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #2c3e50;
}

.header-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #e3f2fd;
  border-radius: 8px;
  color: #1976d2;
  font-size: 14px;
}

/* 表单区域 */
.form-section {
  margin-bottom: 40px;
  padding: 30px;
  background: #fafafa;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid #2196f3;
}

/* 表单项 */
.el-form-item {
  margin-bottom: 24px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #2c3e50;
  font-size: 15px;
  margin-bottom: 10px;
}

/* 标题输入框 */
.title-input :deep(.el-input__wrapper) {
  padding: 14px 16px;
  background: white;
  border: 2px solid transparent;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.title-input :deep(.el-input__wrapper:hover) {
  border-color: #e0e3e9;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.title-input :deep(.el-input__wrapper.is-focus) {
  border-color: #2196f3;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}

.title-input :deep(.el-input__inner) {
  font-size: 16px;
  font-weight: 500;
}

/* 摘要输入框 */
.summary-textarea :deep(.el-textarea__inner) {
  background: white;
  border: 2px solid transparent;
  border-radius: 8px;
  font-size: 15px;
  line-height: 1.6;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.summary-textarea :deep(.el-textarea__inner:hover) {
  border-color: #e0e3e9;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.summary-textarea :deep(.el-textarea__inner:focus) {
  border-color: #2196f3;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}

/* 表单行布局 */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.form-col {
  margin-bottom: 0;
}

/* 表单提示 */
.form-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

/* 富文本编辑器 */
.editor-wrapper {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid transparent;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.editor-wrapper:hover {
  border-color: #e0e3e9;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.editor-wrapper:focus-within {
  border-color: #2196f3;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}

.custom-editor {
  height: 500px;
}

.custom-editor :deep(.ql-toolbar) {
  background: #f8f9fa;
  border: none;
  border-bottom: 1px solid #e4e7ed;
  border-radius: 8px 8px 0 0;
  padding: 12px;
}

.custom-editor :deep(.ql-container) {
  border: none;
  font-size: 16px;
  line-height: 1.8;
}

.custom-editor :deep(.ql-editor) {
  min-height: 400px;
  padding: 20px;
}

.custom-editor :deep(.ql-editor.ql-blank::before) {
  color: #c0c4cc;
  font-style: normal;
}

/* 提交区域 */
.submit-section {
  margin-top: 40px;
  margin-bottom: 0;
  padding-top: 30px;
  border-top: 1px solid #e4e7ed;
}

.button-group {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.submit-btn,
.cancel-btn {
  min-width: 160px;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.submit-btn {
  background: linear-gradient(135deg, #2196f3 0%, #1976d2 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.4);
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(33, 150, 243, 0.5);
}

.cancel-btn {
  border: 2px solid #e4e7ed;
  background: white;
}

.cancel-btn:hover {
  border-color: #c0c4cc;
  background: #f8f9fa;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .publish-page {
    max-width: 100%;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-left h2 {
    font-size: 20px;
  }

  .form-section {
    padding: 20px;
    margin-bottom: 24px;
  }

  .form-row {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .form-col {
    margin-bottom: 24px;
  }

  .custom-editor {
    height: 400px;
  }

  .custom-editor :deep(.ql-editor) {
    min-height: 300px;
    padding: 16px;
  }

  .button-group {
    flex-direction: column;
    width: 100%;
  }

  .submit-btn,
  .cancel-btn {
    width: 100%;
  }
}

/* Select 样式优化 */
:deep(.el-select .el-input__wrapper) {
  background: white;
  border: 2px solid transparent;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

:deep(.el-select .el-input__wrapper:hover) {
  border-color: #e0e3e9;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

:deep(.el-select .el-input__wrapper.is-focus) {
  border-color: #2196f3;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}

/* Input 样式优化 */
:deep(.el-input__wrapper) {
  background: white;
  border: 2px solid transparent;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

:deep(.el-input__wrapper:hover) {
  border-color: #e0e3e9;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #2196f3;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}
</style>
