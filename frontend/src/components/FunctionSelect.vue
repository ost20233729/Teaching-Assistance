<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import axios from 'axios'
import { downloadCourseMarkdownExport, getCourseDetail, updateCourseName } from '../api/courseManger'
import { getCurrentRestrictions } from '../api/functions'

interface Props {
  courseTitle?: string
  courseId?: number
  transientModuleStates?: Partial<Record<ModuleId, TransientModuleStatus>>
}

type ModuleId = 'basic' | 'outline' | 'lecture' | 'courseware'
type TransientModuleStatus = 'generated_unsaved'
type ModuleStatus = 'not_started' | 'generated_unsaved' | 'completed' | 'restricted' | 'pending_review' | 'rejected'

interface ModuleConfig {
  id: ModuleId
  title: string
  icon: string
  status: ModuleStatus
  disabled?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits(['back', 'show-course-info', 'show-module', 'update:courseTitle'])

const courseTitle = ref(props.courseTitle || '课程标题')
const isEditingTitle = ref(false)
const isExporting = ref(false)
const courseStatus = ref('')
const reviewComment = ref('')
const reviewedAt = ref<string | Date | null>(null)
const createEmptySavedModuleStates = (): Record<ModuleId, boolean> => ({
  basic: false,
  outline: false,
  lecture: false,
  courseware: false
})
const savedModuleStates = ref<Record<ModuleId, boolean>>(createEmptySavedModuleStates())

const modules = ref<ModuleConfig[]>([
  { id: 'basic', title: '课程介绍与教学目标', icon: 'info', status: 'not_started', disabled: false },
  { id: 'outline', title: '课程大纲', icon: 'list', status: 'not_started', disabled: false },
  { id: 'lecture', title: '教学讲义', icon: 'book', status: 'not_started', disabled: false },
  { id: 'courseware', title: '教学课件提纲', icon: 'presentation', status: 'not_started', disabled: false }
])

watch(() => props.courseTitle, (newTitle) => {
  if (newTitle) {
    courseTitle.value = newTitle
  }
})

const enabledModuleCount = computed(() => modules.value.filter((module) => !module.disabled).length)

const completedModuleCount = computed(() => {
  return modules.value.filter((module) => !module.disabled && savedModuleStates.value[module.id]).length
})

const courseMetaText = computed(() => {
  switch (courseStatus.value) {
    case 'approved':
      return '课程已审核通过'
    case 'rejected':
      return '课程审核未通过，请先查看审核意见'
    case 'pending':
      return '课程正在等待管理员审核，暂时不能进入生成模块'
    default:
      return '课程未完成全部内容也可以导出当前成果'
  }
})

const reviewSummaryText = computed(() => {
  if (reviewComment.value) {
    return reviewComment.value
  }

  if (courseStatus.value === 'approved') {
    return '课程已审核通过，可以继续完成内容生成与导出。'
  }

  if (courseStatus.value === 'pending') {
    return '课程提交后需等待管理员审核通过，才能继续使用生成类功能。'
  }

  if (courseStatus.value === 'rejected') {
    return '课程已被驳回，请根据审核意见修改后重新提交。'
  }

  return ''
})

const resolveModuleStatus = (moduleId: ModuleId, disabled: boolean): ModuleStatus => {
  if (courseStatus.value === 'pending') {
    return 'pending_review'
  }

  if (courseStatus.value === 'rejected') {
    return 'rejected'
  }

  if (disabled) {
    return 'restricted'
  }

  if (props.transientModuleStates?.[moduleId] === 'generated_unsaved') {
    return 'generated_unsaved'
  }

  if (savedModuleStates.value[moduleId]) {
    return 'completed'
  }

  return 'not_started'
}

const syncModuleStatuses = () => {
  modules.value = modules.value.map((module) => ({
    ...module,
    status: resolveModuleStatus(module.id, Boolean(module.disabled))
  }))
}

const loadRestrictions = async () => {
  try {
    const restrictions = await getCurrentRestrictions()
    const restrictedSet = new Set(restrictions.map((item) => item.functionName))

    modules.value = modules.value.map((module) => {
      const disabled = restrictedSet.has(module.id)
      return {
        ...module,
        disabled,
        status: module.status
      }
    })
    syncModuleStatuses()
  } catch (error) {
    console.error('获取功能限制失败:', error)
  }
}

const loadCourseDetail = async () => {
  savedModuleStates.value = createEmptySavedModuleStates()
  courseStatus.value = ''
  reviewComment.value = ''
  reviewedAt.value = null
  syncModuleStatuses()

  if (!props.courseId) {
    return
  }

  try {
    const courseDetail = await getCourseDetail(props.courseId)
    courseStatus.value = courseDetail?.course?.status || ''
    reviewComment.value = courseDetail?.course?.reviewComment || ''
    reviewedAt.value = courseDetail?.course?.reviewedAt || null
    const objective = courseDetail?.objective
    const syllabus = courseDetail?.syllabus
    const material = courseDetail?.material
    const courseware = courseDetail?.courseware
    const nextSavedModuleStates = createEmptySavedModuleStates()

    if ((objective?.courseContent && objective.courseContent.trim()) ||
        (objective?.teachingTarget && objective.teachingTarget.trim())) {
      nextSavedModuleStates.basic = true
    }

    if (syllabus?.content && syllabus.content.trim()) {
      nextSavedModuleStates.outline = true
    }

    if (material?.content && material.content.trim()) {
      nextSavedModuleStates.lecture = true
    }

    if (courseware?.content && courseware.content.trim()) {
      nextSavedModuleStates.courseware = true
    }

    savedModuleStates.value = nextSavedModuleStates
    syncModuleStatuses()
  } catch (error) {
    console.error('获取课程详情失败:', error)
  }
}

const handleExport = async () => {
  if (!props.courseId || isExporting.value) {
    return
  }

  const completedCount = completedModuleCount.value
  const requiredCount = enabledModuleCount.value

  if (completedCount === 0) {
    const shouldContinue = window.confirm('当前课程还没有可导出的内容，将导出课程模板框架，是否继续？')
    if (!shouldContinue) {
      return
    }
  } else if (completedCount < requiredCount) {
    const shouldContinue = window.confirm('当前课程尚未完成全部模块，将导出已完成内容和未完成提示，是否继续？')
    if (!shouldContinue) {
      return
    }
  }

  isExporting.value = true

  try {
    const fileName = await downloadCourseMarkdownExport(props.courseId)
    window.alert(`导出成功：${fileName}`)
  } catch (error) {
    console.error('导出课程成果失败:', error)

    if (axios.isAxiosError(error)) {
      const message = typeof error.response?.data?.message === 'string'
        ? error.response.data.message
        : error.message
      window.alert(message || '课程成果导出失败，请稍后重试')
      return
    }

    if (error instanceof Error) {
      window.alert(error.message)
      return
    }

    window.alert('课程成果导出失败，请稍后重试')
  } finally {
    isExporting.value = false
  }
}

watch(() => props.courseId, () => {
  void loadRestrictions()
  void loadCourseDetail()
})

watch(
  () => props.transientModuleStates,
  () => {
    syncModuleStatuses()
  },
  { deep: true }
)

onMounted(async () => {
  await loadRestrictions()
  await loadCourseDetail()
})

const toggleTitleEdit = async () => {
  if (!isEditingTitle.value) {
    isEditingTitle.value = true
    return
  }

  if (props.courseId === undefined) {
    window.alert('当前课程 ID 无效，无法修改课程名称')
    return
  }

  const trimmedTitle = courseTitle.value.trim()
  if (!trimmedTitle) {
    window.alert('课程名称不能为空')
    return
  }

  try {
    const result = await updateCourseName(props.courseId, trimmedTitle)
    courseTitle.value = result.name
    emit('update:courseTitle', result.name)
    isEditingTitle.value = false
  } catch (error) {
    console.error('更新课程名称失败:', error)
    if (props.courseTitle) {
      courseTitle.value = props.courseTitle
    }

    const message = error instanceof Error ? error.message : '修改课程名称失败，请稍后重试'
    window.alert(message)
  }
}

const goBack = () => {
  emit('back')
}

const showModule = (moduleId: ModuleId) => {
  const target = modules.value.find((item) => item.id === moduleId)
  if (!target) {
    return
  }

  switch (target.status) {
    case 'restricted':
      window.alert('当前教师被限制使用该模块，请联系管理员解除限制。')
      return
    case 'pending_review':
      window.alert('当前课程待管理员审核通过后才能进入该模块。')
      return
    case 'rejected':
      window.alert(reviewComment.value
        ? `当前课程已被驳回：${reviewComment.value}`
        : '当前课程已被驳回，请先查看审核意见。')
      return
    default:
      emit('show-module', moduleId)
  }
}

const formatReviewTime = (value?: string | Date | null) => {
  if (!value) {
    return ''
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }

  return date.toLocaleString('zh-CN', { hour12: false })
}

const getStatusIcon = (status: ModuleStatus) => {
  switch (status) {
    case 'generated_unsaved':
      return '≈'
    case 'completed':
      return '✓'
    case 'restricted':
      return '!'
    case 'pending_review':
      return '…'
    case 'rejected':
      return '×'
    default:
      return '○'
  }
}

const getStatusLabel = (status: ModuleStatus) => {
  switch (status) {
    case 'generated_unsaved':
      return '已生成未保存'
    case 'completed':
      return '已完成'
    case 'restricted':
      return '受限'
    case 'pending_review':
      return '待审核'
    case 'rejected':
      return '已驳回'
    default:
      return '未开始'
  }
}

const getStatusHint = (status: ModuleStatus) => {
  switch (status) {
    case 'generated_unsaved':
      return '本次会话已生成新内容，但还没有保存。'
    case 'completed':
      return '当前模块已有已保存内容。'
    case 'restricted':
      return '当前教师被限制使用该模块。'
    case 'pending_review':
      return '课程待管理员审核通过后可进入。'
    case 'rejected':
      return '课程已被驳回，请先查看审核意见。'
    default:
      return '当前模块还没有已保存内容。'
  }
}

const isModuleBlocked = (status: ModuleStatus) => (
  status === 'restricted' || status === 'pending_review' || status === 'rejected'
)

const getModuleIcon = (iconName: string) => {
  switch (iconName) {
    case 'info':
      return 'i'
    case 'list':
      return '≣'
    case 'book':
      return '▣'
    case 'presentation':
      return '▶'
    default:
      return ''
  }
}
</script>

<template>
  <div class="function-select-container">
    <div class="title-container">
      <button class="back-button" @click="goBack">←</button>
      <div class="title-main">
        <div v-if="isEditingTitle" class="editing-title">
          <input
            v-model="courseTitle"
            type="text"
            class="title-input"
            @keyup.enter="toggleTitleEdit"
          />
        </div>
        <h1 v-else class="course-title">{{ courseTitle }}</h1>
        <p class="course-meta">
          {{ courseMetaText }}
        </p>
        <div v-if="reviewSummaryText" class="review-summary" :class="`review-${courseStatus || 'default'}`">
          <strong class="review-summary-title">审核意见</strong>
          <span>{{ reviewSummaryText }}</span>
          <small v-if="reviewedAt" class="review-time">审核时间：{{ formatReviewTime(reviewedAt) }}</small>
        </div>
      </div>
      <div class="title-actions">
        <button class="export-button" :disabled="isExporting" @click="handleExport">
          {{ isExporting ? '导出中...' : '导出成果' }}
        </button>
        <button class="edit-button" @click="toggleTitleEdit">
          {{ isEditingTitle ? '保存课程名称' : '编辑课程名称' }}
        </button>
      </div>
    </div>

    <div class="modules-container">
      <template v-for="(module, index) in modules" :key="module.id">
        <div class="module-card" :class="{ blocked: isModuleBlocked(module.status) }" @click="showModule(module.id)">
          <div class="status-icon" :class="`status-${module.status}`">
            {{ getStatusIcon(module.status) }}
          </div>
          <div class="module-icon">
            <div class="icon-circle">
              {{ getModuleIcon(module.icon) }}
            </div>
          </div>
          <div class="module-title">{{ module.title }}</div>
          <div class="module-status-badge" :class="`badge-${module.status}`">{{ getStatusLabel(module.status) }}</div>
          <div class="module-hint" :class="`hint-${module.status}`">{{ getStatusHint(module.status) }}</div>
        </div>

        <div v-if="index < modules.length - 1" class="arrow">→</div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.function-select-container {
  padding: 20px;
  width: 100%;
}

.title-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 40px;
  position: relative;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
  gap: 20px;
  padding: 0 60px;
}

.title-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
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
  position: absolute;
  left: 10px;
  flex-shrink: 0;
}

.course-title {
  font-size: 28px;
  font-weight: bold;
  margin: 0 15px;
  color: #333;
}

.course-meta {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 14px;
  text-align: center;
}

.review-summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 14px;
  padding: 12px 16px;
  border-radius: 14px;
  max-width: 560px;
  text-align: left;
}

.review-summary-title {
  font-size: 13px;
}

.review-approved {
  background: rgba(16, 185, 129, 0.12);
  color: #065f46;
}

.review-rejected {
  background: rgba(239, 68, 68, 0.12);
  color: #991b1b;
}

.review-pending {
  background: rgba(245, 158, 11, 0.12);
  color: #9a3412;
}

.review-default {
  background: rgba(148, 163, 184, 0.12);
  color: #334155;
}

.review-time {
  font-size: 12px;
  opacity: 0.85;
}

.editing-title {
  margin: 0 10px;
}

.title-input {
  font-size: 28px;
  font-weight: bold;
  border: none;
  border-bottom: 2px solid #2196f3;
  padding: 5px 10px;
  border-radius: 4px;
  width: 400px;
  background-color: rgba(33, 150, 243, 0.05);
  outline: none;
}

.edit-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 15px;
  background-color: rgba(33, 150, 243, 0.2);
  color: rgba(33, 150, 243, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.2;
}

.title-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.export-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 16px;
  background-color: rgba(76, 175, 80, 0.18);
  color: #2e7d32;
  border: 1px solid rgba(76, 175, 80, 0.25);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.2;
}

.export-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modules-container {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 30px;
  padding: 20px;
  width: 100%;
  max-width: 1400px;
  margin: 30px auto;
  flex-wrap: nowrap;
  overflow-x: auto;
}

.module-card {
  width: 220px;
  min-height: 250px;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  background: rgba(255, 255, 255, 0.25);
  box-shadow:
    0 4px 10px rgba(0, 0, 0, 0.05),
    0 10px 30px rgba(31, 38, 135, 0.1);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  padding: 25px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.module-card:hover {
  transform: translateY(-8px);
}

.module-card.blocked {
  opacity: 0.72;
  cursor: not-allowed;
}

.module-card.blocked:hover {
  transform: none;
}

.status-icon {
  position: absolute;
  top: 15px;
  right: 15px;
  font-size: 24px;
  font-weight: 700;
}

.status-not_started {
  color: #9e9e9e;
}

.status-generated_unsaved {
  color: #2563eb;
}

.status-pending_review {
  color: #ff9800;
}

.status-completed {
  color: #4caf50;
}

.status-restricted {
  color: #f44336;
}

.status-rejected {
  color: #dc2626;
}

.module-icon {
  margin-bottom: 14px;
}

.icon-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: rgba(227, 242, 253, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2196f3;
  font-size: 40px;
  font-weight: 700;
  box-shadow: 0 4px 20px rgba(33, 150, 243, 0.2);
}

.module-title {
  font-size: 22px;
  font-weight: 600;
  margin-top: 12px;
  text-align: center;
}

.module-status-badge {
  margin-top: 14px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.badge-not_started {
  background: rgba(148, 163, 184, 0.16);
  color: #475569;
}

.badge-generated_unsaved {
  background: rgba(59, 130, 246, 0.16);
  color: #1d4ed8;
}

.badge-completed {
  background: rgba(34, 197, 94, 0.16);
  color: #15803d;
}

.badge-restricted {
  background: rgba(239, 68, 68, 0.16);
  color: #b91c1c;
}

.badge-pending_review {
  background: rgba(245, 158, 11, 0.16);
  color: #b45309;
}

.badge-rejected {
  background: rgba(220, 38, 38, 0.16);
  color: #991b1b;
}

.module-hint {
  margin-top: 10px;
  font-size: 13px;
  text-align: center;
  line-height: 1.5;
}

.hint-not_started {
  color: #64748b;
}

.hint-generated_unsaved {
  color: #1d4ed8;
}

.hint-completed {
  color: #15803d;
}

.hint-restricted {
  color: #ef4444;
}

.hint-pending_review {
  color: #b45309;
}

.hint-rejected {
  color: #991b1b;
}

.arrow {
  margin: 0 25px;
  font-size: 48px;
  color: #2196f3;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.8;
}

@media (max-width: 768px) {
  .title-container {
    flex-direction: column;
    padding: 0 20px;
  }

  .title-actions {
    width: 100%;
    justify-content: center;
  }

  .module-card {
    min-width: 180px;
  }

  .arrow {
    margin: 0 15px;
    font-size: 36px;
  }
}
</style>
