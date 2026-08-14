<template>
  <div class="courseware-container">
    <div class="header">
      <button class="back-button" @click="$emit('back')">←</button>
      <h3 class="section-title">教学课件提纲</h3>
      <div class="header-right">
        <button class="ai-btn" @click="openPrompt" :disabled="isGenerating || isLoading || isSaving">
          <span class="ai-icon">✨</span>
          AI生成
        </button>
        <button class="btn btn-secondary" @click="showVersionHistory = true" :disabled="isGenerating || isLoading || isSaving">
          历史版本
        </button>
        <button class="btn btn-primary" @click="saveCoursewareContent" :disabled="isGenerating || isLoading || isSaving">
          保存
        </button>
      </div>
    </div>

    <div v-if="isLoading" class="status-message loading">
      <div class="spinner"></div>
      <span>{{ loadingMessage }}</span>
    </div>

    <div v-if="isGenerating" class="status-message generating">
      <div class="spinner"></div>
      <span>{{ generatingStatus }}</span>
    </div>

    <div v-if="isSaving" class="status-message saving">
      <div class="spinner"></div>
      <span>正在保存...</span>
    </div>

    <div v-if="showSuccessMessage" class="status-message success">
      <span class="success-icon">✓</span>
      <span>{{ successMessage }}</span>
    </div>

    <div v-if="error" class="status-message error">
      <span class="error-icon">❌</span>
      <span>{{ error }}</span>
    </div>

    <div class="section">
      <Markdown
        ref="markdownRef"
        v-model="coursewareContent"
        :initial-value="coursewareContent"
        :height="editorHeight"
        preview-style="tab"
        :editable="true"
      />
    </div>

    <Prompt
      :is-visible="showPrompt"
      title="AI生成教学课件提纲"
      description="请输入你希望生成的课件风格、页数重点或课堂活动要求"
      placeholder="例如：生成一份 12 页左右的教学课件提纲，突出案例互动和课堂提问设计"
      :templates="promptTemplates"
      :template-loading="promptTemplateLoading"
      :template-error="promptTemplateError"
      @close="showPrompt = false"
      @confirm="handlePromptConfirm"
    />

    <ContentVersionPanel
      :is-visible="showVersionHistory"
      :course-id="props.courseId"
      module="courseware"
      @close="showVersionHistory = false"
      @restored="handleVersionRestored"
    />
  </div>
</template>

<script setup lang="ts">
import { defineEmits, defineProps, onMounted, onUnmounted, ref } from 'vue'
import ContentVersionPanel from './ContentVersionPanel.vue'
import Markdown from './markdown.vue'
import Prompt from './Prompt.vue'
import {
  type ContentVersionRestoreResponse,
  generateCourseCourseware,
  getCourseCourseware,
  getPromptTemplates,
  saveCourseCourseware,
  type PromptTemplate
} from '../api/functions'

interface MarkdownInstance {
  setMarkdown: (content: string) => void
  [key: string]: any
}

const props = defineProps({
  courseId: {
    type: Number,
    required: false,
    default: undefined
  }
})

const emit = defineEmits<{
  (event: 'back'): void
  (event: 'module-status-change', payload: { moduleId: 'courseware'; status: 'generated_unsaved' | 'completed' }): void
}>()

const markdownRef = ref<MarkdownInstance | null>(null)
const showPrompt = ref(false)
const showVersionHistory = ref(false)
const promptTemplates = ref<PromptTemplate[]>([])
const promptTemplateLoading = ref(false)
const promptTemplateError = ref('')

const coursewareContent = ref('')
const error = ref('')
const isLoading = ref(false)
const loadingMessage = ref('正在加载教学课件提纲...')
const isGenerating = ref(false)
const generatingStatus = ref('正在生成教学课件提纲...')
const isSaving = ref(false)
const showSuccessMessage = ref(false)
const successMessage = ref('')
const editorHeight = ref('calc(100vh - 200px)')

const updateEditorHeight = () => {
  const calculatedHeight = Math.min(window.innerHeight - 200, 1800)
  editorHeight.value = `${calculatedHeight}px`
}

const applyContent = (content: string) => {
  coursewareContent.value = content

  if (markdownRef.value?.setMarkdown) {
    markdownRef.value.setMarkdown(content)
  }
}

const fetchCourseware = async () => {
  if (!props.courseId || Number.isNaN(props.courseId)) {
    error.value = '课程ID无效，无法获取教学课件提纲'
    return
  }

  isLoading.value = true
  loadingMessage.value = '正在加载教学课件提纲...'
  error.value = ''

  try {
    const response = await getCourseCourseware(props.courseId)
    applyContent(response?.content || '')
  } catch (fetchError) {
    console.error('获取教学课件提纲失败:', fetchError)
    error.value = '获取教学课件提纲失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
}

const fetchPromptTemplates = async () => {
  promptTemplateLoading.value = true
  promptTemplateError.value = ''

  try {
    promptTemplates.value = await getPromptTemplates('courseware')
  } catch (templateError) {
    console.error('获取教学课件提纲模板失败:', templateError)
    promptTemplateError.value = '模板加载失败，仍可手动输入提示词。'
  } finally {
    promptTemplateLoading.value = false
  }
}

const saveCoursewareContent = async () => {
  if (!props.courseId || Number.isNaN(props.courseId)) {
    window.alert('课程ID无效，无法保存教学课件提纲')
    return
  }

  isSaving.value = true
  error.value = ''

  try {
    await saveCourseCourseware(props.courseId, {
      content: coursewareContent.value
    })
    emit('module-status-change', { moduleId: 'courseware', status: 'completed' })
    showSuccessMessage.value = true
    successMessage.value = '保存成功！'
    window.setTimeout(() => {
      showSuccessMessage.value = false
    }, 3000)
  } catch (saveError) {
    console.error('保存教学课件提纲失败:', saveError)
    error.value = '保存教学课件提纲失败，请稍后重试'
  } finally {
    isSaving.value = false
  }
}

const handleVersionRestored = (response: ContentVersionRestoreResponse) => {
  applyContent(String(response.data.content || ''))
  emit('module-status-change', { moduleId: 'courseware', status: 'completed' })
  showSuccessMessage.value = true
  successMessage.value = '已恢复到所选历史版本'
  window.setTimeout(() => {
    showSuccessMessage.value = false
  }, 3000)
}

const handlePromptConfirm = async (content: string) => {
  if (!props.courseId || Number.isNaN(props.courseId)) {
    error.value = '课程ID无效，无法生成教学课件提纲'
    showPrompt.value = false
    return
  }

  if (!content.trim()) {
    window.alert('请输入有效的提示内容')
    return
  }

  showPrompt.value = false
  isGenerating.value = true
  generatingStatus.value = '正在生成教学课件提纲...'
  error.value = ''

  try {
    const response = await generateCourseCourseware(props.courseId, content)
    const generatedContent = response?.content?.trim() || ''

    if (!generatedContent) {
      error.value = '生成结果为空，请尝试提供更具体的提示词'
      return
    }

    applyContent(generatedContent)
    emit('module-status-change', { moduleId: 'courseware', status: 'generated_unsaved' })
    showSuccessMessage.value = true
    successMessage.value = '生成成功！'
    window.setTimeout(() => {
      showSuccessMessage.value = false
    }, 3000)
  } catch (generateError) {
    console.error('生成教学课件提纲失败:', generateError)
    error.value = '生成教学课件提纲失败，请稍后重试'
  } finally {
    isGenerating.value = false
  }
}

const openPrompt = () => {
  if (isGenerating.value || isLoading.value || isSaving.value) {
    return
  }
  showPrompt.value = true
}

onMounted(() => {
  updateEditorHeight()
  window.addEventListener('resize', updateEditorHeight)
  void fetchPromptTemplates()
  void fetchCourseware()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateEditorHeight)
})
</script>

<style scoped>
.courseware-container {
  max-width: 1500px;
  margin: 0 auto;
  padding: 20px;
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
  margin-left: auto;
}

.back-button {
  background-color: transparent;
  border: none;
  color: #2196f3;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
  transition: background-color 0.2s;
}

.back-button:hover {
  background-color: rgba(33, 150, 243, 0.1);
}

.section-title {
  font-size: 24px;
  font-weight: 500;
  color: #333;
  margin: 0;
  text-align: center;
  flex-grow: 1;
  min-width: 0;
}

.status-message {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.loading {
  background-color: rgba(33, 150, 243, 0.1);
}

.generating {
  background-color: rgba(255, 193, 7, 0.1);
}

.saving {
  background-color: rgba(76, 175, 80, 0.1);
}

.success {
  background-color: rgba(76, 175, 80, 0.2);
  animation: fadeOut 3s forwards;
}

.error {
  background-color: rgba(244, 67, 54, 0.1);
}

.success-icon {
  color: #4caf50;
  font-weight: bold;
  font-size: 18px;
}

.error-icon {
  color: #f44336;
  font-weight: bold;
  font-size: 18px;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 3px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  border-top-color: #2196f3;
  animation: spin 1s ease-in-out infinite;
}

@keyframes fadeOut {
  0% { opacity: 1; }
  70% { opacity: 1; }
  100% { opacity: 0; }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 18px;
  border-radius: 12px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.2;
  cursor: pointer;
  transition: all 0.3s;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.btn-primary {
  background-color: rgba(76, 175, 80, 0.7);
  color: white;
}

.btn-secondary {
  background-color: rgba(245, 245, 245, 0.7);
  color: #333;
}

.btn-secondary:hover {
  background-color: rgba(245, 245, 245, 0.85);
}

.btn-primary:hover {
  background-color: rgba(76, 175, 80, 0.85);
}

.btn:disabled,
.ai-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background-color: rgba(76, 175, 80, 0.7);
  color: white;
  border: none;
  border-radius: 12px;
  min-height: 40px;
  padding: 0 18px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.2;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.ai-btn:hover {
  background-color: rgba(76, 175, 80, 0.85);
}

.ai-icon {
  margin-right: 0;
  font-size: 14px;
}

.section {
  margin-bottom: 30px;
}
</style>
