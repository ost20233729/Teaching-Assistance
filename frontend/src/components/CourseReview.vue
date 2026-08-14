<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, type AdminCourse, type User } from '@/api/admin'

type CourseFilter = 'pending' | 'approved' | 'rejected' | 'all'

interface CourseReviewViewState {
  searchKeyword: string
  activeFilter: CourseFilter
  currentPage: number
  pageSize: number
}

const STORAGE_KEY = 'admin-course-review-view-state'
const DEFAULT_FILTER: CourseFilter = 'pending'
const DEFAULT_PAGE = 1
const DEFAULT_PAGE_SIZE = 8

const loading = ref(false)
const searchKeyword = ref('')
const activeFilter = ref<CourseFilter>(DEFAULT_FILTER)
const actionCourseId = ref<number | null>(null)
const courses = ref<AdminCourse[]>([])
const teachers = ref<User[]>([])
const reviewCommentDrafts = ref<Record<number, string>>({})
const currentPage = ref(DEFAULT_PAGE)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const totalCourses = ref(0)
const totalPages = ref(1)

const filterItems: Array<{ value: CourseFilter; label: string }> = [
  { value: 'pending', label: '待审核' },
  { value: 'approved', label: '已通过' },
  { value: 'rejected', label: '已驳回' },
  { value: 'all', label: '全部课程' }
]
const pageSizeOptions = [6, 8, 12, 20]

const isCourseFilter = (value: unknown): value is CourseFilter => {
  return typeof value === 'string' && ['pending', 'approved', 'rejected', 'all'].includes(value)
}

const normalizePositiveInteger = (value: unknown, fallback: number) => {
  if (typeof value !== 'number' || !Number.isInteger(value) || value < 1) {
    return fallback
  }

  return value
}

const persistReviewState = () => {
  const state: CourseReviewViewState = {
    searchKeyword: searchKeyword.value,
    activeFilter: activeFilter.value,
    currentPage: currentPage.value,
    pageSize: pageSize.value
  }

  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

const restoreReviewState = () => {
  const storedState = sessionStorage.getItem(STORAGE_KEY)
  if (!storedState) {
    return
  }

  try {
    const parsedState = JSON.parse(storedState) as Partial<CourseReviewViewState>
    searchKeyword.value = typeof parsedState.searchKeyword === 'string' ? parsedState.searchKeyword : ''
    activeFilter.value = isCourseFilter(parsedState.activeFilter) ? parsedState.activeFilter : DEFAULT_FILTER
    currentPage.value = normalizePositiveInteger(parsedState.currentPage, DEFAULT_PAGE)
    pageSize.value = pageSizeOptions.includes(parsedState.pageSize as number)
      ? (parsedState.pageSize as number)
      : DEFAULT_PAGE_SIZE
  } catch (error) {
    console.warn('Failed to restore admin course review view state, fallback to defaults.', error)
    sessionStorage.removeItem(STORAGE_KEY)
  }
}

const teacherMap = computed(() => {
  return new Map(
    teachers.value
      .filter((teacher) => teacher.id !== undefined)
      .map((teacher) => [teacher.id as number, teacher])
  )
})

const visiblePages = computed(() => {
  const maxButtons = 5
  const total = totalPages.value

  if (total <= maxButtons) {
    return Array.from({ length: total }, (_, index) => index + 1)
  }

  const current = currentPage.value
  const half = Math.floor(maxButtons / 2)
  let start = Math.max(1, current - half)
  let end = Math.min(total, start + maxButtons - 1)

  if (end - start + 1 < maxButtons) {
    start = Math.max(1, end - maxButtons + 1)
  }

  return Array.from({ length: end - start + 1 }, (_, index) => start + index)
})

const getTeacherName = (teacherId: number) => {
  return teacherMap.value.get(teacherId)?.username || `教师 #${teacherId}`
}

const getTeacherEmail = (teacherId: number) => {
  return teacherMap.value.get(teacherId)?.email || '未找到教师邮箱'
}

const formatTime = (value?: string | Date | null) => {
  if (!value) {
    return '暂无时间'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '暂无时间'
  }

  return date.toLocaleString('zh-CN', { hour12: false })
}

const getStatusLabel = (status: string) => {
  switch (status) {
    case 'pending':
      return '待审核'
    case 'approved':
      return '已通过'
    case 'rejected':
      return '已驳回'
    default:
      return status
  }
}

const syncReviewCommentDrafts = (courseList: AdminCourse[]) => {
  reviewCommentDrafts.value = Object.fromEntries(
    courseList
      .filter((course) => course.id !== undefined)
      .map((course) => [course.id as number, course.reviewComment || ''])
  )
}

const getReviewCommentDraft = (course: AdminCourse) => {
  if (!course.id) {
    return ''
  }

  return reviewCommentDrafts.value[course.id] || ''
}

const fetchTeachers = async () => {
  if (teachers.value.length > 0) {
    return
  }

  teachers.value = await adminApi.getUserList()
}

const fetchCourses = async (targetPage = currentPage.value) => {
  loading.value = true

  try {
    await fetchTeachers()
    const response = await adminApi.getCourseList({
      keyword: searchKeyword.value.trim() || undefined,
      status: activeFilter.value === 'all' ? undefined : activeFilter.value,
      page: targetPage,
      pageSize: pageSize.value
    })

    if (response.total > 0 && targetPage > response.totalPages) {
      await fetchCourses(response.totalPages)
      return
    }

    courses.value = response.items
    totalCourses.value = response.total
    currentPage.value = response.page
    pageSize.value = response.pageSize
    totalPages.value = response.totalPages
    syncReviewCommentDrafts(courses.value)
  } catch (error) {
    const message = error instanceof Error ? error.message : '获取课程审核数据失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = DEFAULT_PAGE
  await fetchCourses(DEFAULT_PAGE)
}

const resetFilters = async () => {
  searchKeyword.value = ''
  activeFilter.value = DEFAULT_FILTER
  currentPage.value = DEFAULT_PAGE
  await fetchCourses(DEFAULT_PAGE)
}

const selectFilter = async (filter: CourseFilter) => {
  if (activeFilter.value === filter) {
    return
  }

  activeFilter.value = filter
  currentPage.value = DEFAULT_PAGE
  await fetchCourses(DEFAULT_PAGE)
}

const changePage = async (page: number) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) {
    return
  }

  currentPage.value = page
  await fetchCourses(page)
}

const changePageSize = async () => {
  currentPage.value = DEFAULT_PAGE
  await fetchCourses(DEFAULT_PAGE)
}

const updateStatus = async (course: AdminCourse, status: 'pending' | 'approved' | 'rejected') => {
  if (!course.id) {
    ElMessage.error('课程 ID 不存在，无法更新状态')
    return
  }

  const reviewComment = getReviewCommentDraft(course).trim()
  if (status === 'rejected' && !reviewComment) {
    ElMessage.warning('驳回课程时请填写审核意见')
    return
  }

  actionCourseId.value = course.id

  try {
    await adminApi.updateCourseReview(course.id, {
      status,
      reviewComment: status === 'pending' ? '' : reviewComment
    })

    if (status === 'approved') {
      ElMessage.success('课程已审核通过')
    } else if (status === 'rejected') {
      ElMessage.success('课程已驳回')
    } else {
      ElMessage.success('课程已重新设为待审核')
    }

    await fetchCourses(currentPage.value)
  } catch (error) {
    const message = error instanceof Error ? error.message : '更新课程状态失败'
    ElMessage.error(message)
  } finally {
    actionCourseId.value = null
  }
}

watch([searchKeyword, activeFilter, currentPage, pageSize], persistReviewState)

onMounted(() => {
  restoreReviewState()
  void fetchCourses(currentPage.value)
})
</script>

<template>
  <section class="panel-shell">
    <div class="panel-header">
      <div>
        <h2>课程审核</h2>
        <p>支持按课程、教师、审核意见搜索，并按状态筛选与分页处理审核任务。</p>
      </div>
      <div class="panel-actions">
        <input
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="搜索课程名称、教师姓名、教师邮箱、课程编号或审核意见"
          @keyup.enter="handleSearch"
        />
        <button class="refresh-button" @click="handleSearch">
          搜索
        </button>
        <button class="ghost-button" @click="resetFilters">
          重置
        </button>
      </div>
    </div>

    <div class="toolbar-row">
      <div class="filter-row">
        <button
          v-for="filter in filterItems"
          :key="filter.value"
          class="filter-button"
          :class="{ active: activeFilter === filter.value }"
          @click="selectFilter(filter.value)"
        >
          {{ filter.label }}
        </button>
      </div>
      <div class="page-size-group">
        <label for="admin-course-page-size">每页</label>
        <select
          id="admin-course-page-size"
          v-model.number="pageSize"
          class="page-size-select"
          @change="changePageSize"
        >
          <option v-for="option in pageSizeOptions" :key="option" :value="option">
            {{ option }}
          </option>
        </select>
        <span>条</span>
      </div>
    </div>

    <div class="summary-row">
      <span>当前第 {{ currentPage }} / {{ totalPages }} 页</span>
      <span>共 {{ totalCourses }} 门课程</span>
    </div>

    <div v-if="loading" class="empty-state">
      正在加载课程审核数据...
    </div>

    <div v-else-if="courses.length === 0" class="empty-state">
      当前筛选条件下没有课程记录。
    </div>

    <div v-else class="course-list">
      <article v-for="course in courses" :key="course.id" class="course-card">
        <div class="course-main">
          <div class="course-meta">
            <span>课程 #{{ course.id }}</span>
            <span>教师：{{ getTeacherName(course.teacherId) }}</span>
          </div>

          <h3 class="course-name">{{ course.name }}</h3>
          <p class="teacher-email">{{ getTeacherEmail(course.teacherId) }}</p>
          <label class="review-label" :for="`review-comment-${course.id}`">审核意见</label>
          <textarea
            :id="`review-comment-${course.id}`"
            v-model="reviewCommentDrafts[course.id || 0]"
            class="review-textarea"
            rows="3"
            :placeholder="course.status === 'approved'
              ? '可选填写通过意见或修改建议'
              : '驳回课程时请填写审核意见'"
          ></textarea>

          <div class="course-footer">
            <span class="status-badge" :class="`status-${course.status}`">
              {{ getStatusLabel(course.status) }}
            </span>
            <span class="time-text">更新时间：{{ formatTime(course.updatedAt || course.createdAt) }}</span>
            <span v-if="course.reviewedAt" class="time-text">审核时间：{{ formatTime(course.reviewedAt) }}</span>
          </div>
        </div>

        <div class="course-actions">
          <button
            v-if="course.status !== 'pending'"
            class="action-button pending-button"
            :disabled="actionCourseId === course.id"
            @click="updateStatus(course, 'pending')"
          >
            {{ actionCourseId === course.id ? '处理中...' : '设为待审核' }}
          </button>
          <button
            v-if="course.status !== 'approved'"
            class="action-button approve-button"
            :disabled="actionCourseId === course.id"
            @click="updateStatus(course, 'approved')"
          >
            {{ actionCourseId === course.id ? '处理中...' : '审核通过' }}
          </button>
          <button
            v-if="course.status !== 'rejected'"
            class="action-button reject-button"
            :disabled="actionCourseId === course.id"
            @click="updateStatus(course, 'rejected')"
          >
            {{ actionCourseId === course.id ? '处理中...' : '驳回课程' }}
          </button>
        </div>
      </article>
    </div>

    <div v-if="!loading && totalCourses > 0" class="pagination-bar">
      <button
        class="pagination-button"
        :disabled="currentPage === 1"
        @click="changePage(currentPage - 1)"
      >
        上一页
      </button>
      <button
        v-for="page in visiblePages"
        :key="page"
        class="pagination-button"
        :class="{ active: currentPage === page }"
        @click="changePage(page)"
      >
        {{ page }}
      </button>
      <button
        class="pagination-button"
        :disabled="currentPage === totalPages"
        @click="changePage(currentPage + 1)"
      >
        下一页
      </button>
    </div>
  </section>
</template>

<style scoped>
.panel-shell {
  margin: 0 20px 20px;
  padding: 24px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(255, 255, 255, 0.42);
  box-shadow: 0 20px 48px rgba(31, 38, 135, 0.12);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.panel-header h2 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #111827;
}

.panel-header p {
  margin: 0;
  color: #6b7280;
}

.panel-actions,
.toolbar-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.toolbar-row {
  justify-content: space-between;
  margin-bottom: 14px;
}

.search-input,
.page-size-select,
.review-textarea {
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.76);
  color: #0f172a;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.search-input {
  width: 360px;
  max-width: 100%;
  padding: 12px 14px;
  border-radius: 14px;
}

.search-input:focus,
.page-size-select:focus,
.review-textarea:focus {
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12);
}

.refresh-button,
.ghost-button,
.filter-button,
.action-button,
.pagination-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  cursor: pointer;
  line-height: 1.2;
  white-space: nowrap;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.refresh-button:hover,
.ghost-button:hover,
.filter-button:hover,
.action-button:hover,
.pagination-button:hover {
  transform: translateY(-1px);
}

.refresh-button {
  min-height: 44px;
  padding: 12px 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  color: #fff;
  box-shadow: 0 14px 28px rgba(59, 130, 246, 0.2);
}

.ghost-button {
  min-height: 44px;
  padding: 12px 16px;
  border-radius: 14px;
  background: rgba(226, 232, 240, 0.9);
  color: #334155;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-button {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.9);
  color: #334155;
  box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.8);
}

.filter-button.active {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  color: #1d4ed8;
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.16);
}

.page-size-group,
.summary-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-size: 14px;
}

.page-size-select {
  padding: 9px 12px;
  border-radius: 12px;
}

.summary-row {
  gap: 14px;
  margin-bottom: 18px;
  color: #64748b;
}

.course-list {
  display: grid;
  gap: 16px;
}

.course-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 20px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.88);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.course-main {
  flex: 1;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
  margin-bottom: 10px;
}

.course-name {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 22px;
}

.teacher-email {
  margin: 0 0 14px;
  color: #475569;
}

.review-label {
  display: block;
  margin-bottom: 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.review-textarea {
  width: 100%;
  min-height: 88px;
  padding: 12px 14px;
  margin-bottom: 14px;
  border-radius: 14px;
  resize: vertical;
}

.course-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.status-pending {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.status-approved {
  background: rgba(16, 185, 129, 0.14);
  color: #047857;
}

.status-rejected {
  background: rgba(239, 68, 68, 0.14);
  color: #b91c1c;
}

.time-text {
  color: #64748b;
  font-size: 13px;
}

.course-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 120px;
  align-items: stretch;
}

.action-button {
  min-height: 44px;
  padding: 12px 14px;
  border-radius: 14px;
  color: #fff;
  font-weight: 600;
}

.action-button:disabled,
.pagination-button:disabled {
  cursor: not-allowed;
  opacity: 0.75;
}

.approve-button {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  box-shadow: 0 12px 24px rgba(34, 197, 94, 0.18);
}

.pending-button {
  background: linear-gradient(135deg, #94a3b8, #64748b);
  box-shadow: 0 12px 24px rgba(100, 116, 139, 0.18);
}

.reject-button {
  background: linear-gradient(135deg, #fb7185, #ef4444);
  box-shadow: 0 12px 24px rgba(239, 68, 68, 0.18);
}

.empty-state {
  padding: 44px 20px;
  text-align: center;
  color: #64748b;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.75);
}

.pagination-bar {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-top: 24px;
}

.pagination-button {
  min-width: 42px;
  min-height: 40px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(241, 245, 249, 0.95);
  color: #334155;
}

.pagination-button.active {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  color: #fff;
  box-shadow: 0 14px 28px rgba(59, 130, 246, 0.2);
}

@media (max-width: 960px) {
  .panel-header,
  .panel-actions,
  .toolbar-row,
  .course-card {
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }

  .course-actions {
    min-width: auto;
    width: 100%;
  }

  .page-size-group {
    justify-content: flex-start;
  }
}
</style>
