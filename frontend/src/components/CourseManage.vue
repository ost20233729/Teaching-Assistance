<script setup lang="ts">
import { computed, defineEmits, onMounted, ref, watch } from 'vue'
import {
  createCourse,
  deleteCourse,
  getTeacherCourses,
  updateCourseName,
  type TeacherCourse
} from '../api/courseManger'

type CourseFilter = 'all' | 'pending' | 'approved' | 'rejected'

interface CourseListViewState {
  searchKeyword: string
  activeFilter: CourseFilter
  currentPage: number
  pageSize: number
}

interface CourseCard extends TeacherCourse {
  imageUrl?: string
  isEditing?: boolean
  isSelected?: boolean
}

const emit = defineEmits(['course-selected'])

const coverImageUrl = 'https://res.cloudinary.com/dm3rouwgn/image/upload/t_media_lib_thumb/zuxomrowewwe5spaci7w'
const filterItems: Array<{ value: CourseFilter; label: string }> = [
  { value: 'all', label: '全部课程' },
  { value: 'pending', label: '待审核' },
  { value: 'approved', label: '已通过' },
  { value: 'rejected', label: '已驳回' }
]
const pageSizeOptions = [8, 12, 24]
const STORAGE_KEY = 'teacher-course-list-view-state'
const DEFAULT_FILTER: CourseFilter = 'all'
const DEFAULT_PAGE = 1
const DEFAULT_PAGE_SIZE = 8

const courses = ref<CourseCard[]>([])
const isDeleteMode = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const searchKeyword = ref('')
const activeFilter = ref<CourseFilter>(DEFAULT_FILTER)
const currentPage = ref(DEFAULT_PAGE)
const pageSize = ref(DEFAULT_PAGE_SIZE)
const totalCourses = ref(0)
const totalPages = ref(1)

const isCourseFilter = (value: unknown): value is CourseFilter => {
  return typeof value === 'string' && ['all', 'pending', 'approved', 'rejected'].includes(value)
}

const normalizePositiveInteger = (value: unknown, fallback: number) => {
  if (typeof value !== 'number' || !Number.isInteger(value) || value < 1) {
    return fallback
  }

  return value
}

const persistListState = () => {
  const state: CourseListViewState = {
    searchKeyword: searchKeyword.value,
    activeFilter: activeFilter.value,
    currentPage: currentPage.value,
    pageSize: pageSize.value
  }

  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state))
}

const restoreListState = () => {
  const storedState = sessionStorage.getItem(STORAGE_KEY)
  if (!storedState) {
    return
  }

  try {
    const parsedState = JSON.parse(storedState) as Partial<CourseListViewState>
    searchKeyword.value = typeof parsedState.searchKeyword === 'string' ? parsedState.searchKeyword : ''
    activeFilter.value = isCourseFilter(parsedState.activeFilter) ? parsedState.activeFilter : DEFAULT_FILTER
    currentPage.value = normalizePositiveInteger(parsedState.currentPage, DEFAULT_PAGE)
    pageSize.value = pageSizeOptions.includes(parsedState.pageSize as number)
      ? (parsedState.pageSize as number)
      : DEFAULT_PAGE_SIZE
  } catch (error) {
    console.warn('Failed to restore teacher course list view state, fallback to defaults.', error)
    sessionStorage.removeItem(STORAGE_KEY)
  }
}

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

const mapCourse = (course: TeacherCourse): CourseCard => ({
  ...course,
  reviewComment: course.reviewComment || '',
  reviewedAt: course.reviewedAt || null,
  imageUrl: coverImageUrl,
  isEditing: false,
  isSelected: false
})

const getStatusLabel = (status: string) => {
  switch (status) {
    case 'approved':
      return '已通过'
    case 'rejected':
      return '已驳回'
    case 'pending':
      return '待审核'
    default:
      return status
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

const fetchCourses = async (targetPage = currentPage.value) => {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await getTeacherCourses({
      keyword: searchKeyword.value.trim() || undefined,
      status: activeFilter.value === 'all' ? undefined : activeFilter.value,
      page: targetPage,
      pageSize: pageSize.value
    })

    if (response.total > 0 && targetPage > response.totalPages) {
      await fetchCourses(response.totalPages)
      return
    }

    courses.value = response.items.map(mapCourse)
    totalCourses.value = response.total
    currentPage.value = response.page
    pageSize.value = response.pageSize
    totalPages.value = response.totalPages
  } catch (error) {
    console.error('获取课程列表失败:', error)
    errorMessage.value = error instanceof Error ? error.message : '获取课程列表失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

watch([searchKeyword, activeFilter, currentPage, pageSize], persistListState)

onMounted(() => {
  restoreListState()
  void fetchCourses(currentPage.value)
})

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

const addNewCourse = async () => {
  try {
    const response = await createCourse('新课程')

    searchKeyword.value = ''
    activeFilter.value = 'pending'
    currentPage.value = DEFAULT_PAGE
    await fetchCourses(DEFAULT_PAGE)

    const createdCourse = courses.value.find((course) => course.id === response.id)
    if (createdCourse) {
      createdCourse.isEditing = true
    }
  } catch (error) {
    console.error('创建课程失败:', error)
    errorMessage.value = error instanceof Error ? error.message : '创建课程失败，请稍后重试'
  }
}

const editTitle = (course: CourseCard) => {
  if (!isDeleteMode.value) {
    course.isEditing = true
  } else {
    toggleCourseSelection(course)
  }
}

const finishEdit = async (course: CourseCard) => {
  const trimmedName = course.name.trim()
  if (!trimmedName) {
    errorMessage.value = '课程名称不能为空'
    return
  }

  try {
    const updatedCourse = await updateCourseName(course.id, trimmedName)
    course.name = updatedCourse.name
    course.isEditing = false
    await fetchCourses(currentPage.value)
  } catch (error) {
    console.error('更新课程名称失败:', error)
    errorMessage.value = error instanceof Error ? error.message : '更新课程名称失败，请稍后重试'
  }
}

const toggleDeleteMode = () => {
  isDeleteMode.value = !isDeleteMode.value

  if (!isDeleteMode.value) {
    courses.value.forEach((course) => {
      course.isSelected = false
    })
  }
}

const toggleCourseSelection = (course: CourseCard) => {
  if (isDeleteMode.value) {
    course.isSelected = !course.isSelected
  }
}

const deleteSelectedCourses = async () => {
  const selectedCourses = courses.value.filter((course) => course.isSelected)
  const deletePromises = selectedCourses.map((course) => deleteCourse(course.id))

  try {
    await Promise.all(deletePromises)
    isDeleteMode.value = false

    const shouldGoPreviousPage = selectedCourses.length === courses.value.length && currentPage.value > 1
    const nextPage = shouldGoPreviousPage ? currentPage.value - 1 : currentPage.value
    await fetchCourses(nextPage)
  } catch (error) {
    console.error('删除课程失败:', error)
    errorMessage.value = error instanceof Error ? error.message : '删除课程失败，请稍后重试'
  }
}

const handleCourseClick = (course: CourseCard) => {
  if (isDeleteMode.value) {
    toggleCourseSelection(course)
  } else {
    emit('course-selected', { id: course.id, name: course.name })
  }
}

const vFocus = {
  mounted(el: HTMLInputElement) {
    el.focus()
    el.select()
  }
}
</script>

<template>
  <div class="course-manage">
    <div class="course-header">
      <div>
        <h2 class="course-title">您已有的课程 {{ totalCourses }}</h2>
        <p class="results-meta">支持按课程名称搜索、按审核状态筛选，并按页浏览课程列表。</p>
      </div>
      <div class="course-actions">
        <button
          @click="toggleDeleteMode"
          class="delete-button"
          :class="{ active: isDeleteMode }"
        >
          {{ isDeleteMode ? '取消' : '删除' }}
        </button>
        <button
          v-if="isDeleteMode"
          @click="deleteSelectedCourses"
          class="confirm-delete-button"
        >
          确认删除
        </button>
      </div>
    </div>

    <div class="toolbar">
      <div class="search-group">
        <input
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="搜索课程名称"
          @keyup.enter="handleSearch"
        />
        <button class="toolbar-button primary" @click="handleSearch">搜索</button>
        <button class="toolbar-button" @click="resetFilters">重置</button>
      </div>
      <div class="page-size-group">
        <label for="teacher-course-page-size">每页</label>
        <select
          id="teacher-course-page-size"
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

    <div class="pagination-summary">
      <span>当前第 {{ currentPage }} / {{ totalPages }} 页</span>
      <span>共 {{ totalCourses }} 门课程</span>
    </div>

    <div v-if="loading" class="loading-state">
      正在加载课程，请稍候...
    </div>

    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>

    <div class="course-grid" v-if="!loading">
      <div class="course-card add-card" @click="addNewCourse">
        <div class="add-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
        </div>
      </div>

      <div
        v-for="course in courses"
        :key="course.id"
        class="course-card"
        :class="{ selected: course.isSelected }"
        @click="handleCourseClick(course)"
      >
        <div class="course-image">
          <img :src="course.imageUrl" alt="课程封面" />
          <div v-if="isDeleteMode" class="selection-indicator">
            <svg v-if="course.isSelected" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </div>
        </div>
        <div class="course-info">
          <h3 v-if="!course.isEditing" @click.stop="editTitle(course)">{{ course.name }}</h3>
          <input
            v-else
            v-model="course.name"
            type="text"
            class="title-input"
            @blur="finishEdit(course)"
            @keyup.enter="finishEdit(course)"
            @click.stop
            v-focus
          />
          <div class="course-status-row">
            <span class="status-badge" :class="`status-${course.status}`">{{ getStatusLabel(course.status) }}</span>
            <span v-if="course.reviewedAt" class="review-time">审核时间：{{ formatReviewTime(course.reviewedAt) }}</span>
          </div>
          <p v-if="course.reviewComment" class="review-comment">审核意见：{{ course.reviewComment }}</p>
          <p v-else-if="course.status === 'pending'" class="review-comment review-hint">当前课程正在等待管理员审核。</p>
        </div>
      </div>
    </div>

    <div v-if="!loading && courses.length === 0" class="empty-state">
      当前条件下没有课程，试试调整搜索词或筛选条件。
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
  </div>
</template>

<style scoped>
.course-manage {
  width: 100%;
  max-width: 100%;
  margin: 0 auto;
  padding: 20px;
}

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 18px;
}

.course-title {
  font-size: 24px;
  margin: 0;
  color: #333;
}

.results-meta {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.course-actions {
  display: flex;
  gap: 10px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 14px;
}

.search-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.search-input,
.page-size-select {
  border: 1px solid rgba(148, 163, 184, 0.36);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.92);
  color: #0f172a;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.search-input {
  width: 260px;
  max-width: 100%;
  padding: 10px 14px;
}

.search-input:focus,
.page-size-select:focus {
  border-color: rgba(99, 102, 241, 0.55);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.12);
}

.toolbar-button,
.delete-button,
.confirm-delete-button,
.filter-button,
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

.toolbar-button:hover,
.delete-button:hover,
.confirm-delete-button:hover,
.filter-button:hover,
.pagination-button:hover {
  transform: translateY(-1px);
}

.toolbar-button {
  min-height: 40px;
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(226, 232, 240, 0.9);
  color: #334155;
}

.toolbar-button.primary {
  background: linear-gradient(135deg, #818cf8, #6366f1);
  color: #fff;
  box-shadow: 0 12px 24px rgba(99, 102, 241, 0.2);
}

.page-size-group {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-size: 14px;
}

.page-size-select {
  padding: 8px 12px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.filter-button {
  min-height: 38px;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.92);
  color: #334155;
  box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.8);
}

.filter-button.active {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  color: #1d4ed8;
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.16);
}

.pagination-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 18px;
  color: #64748b;
  font-size: 14px;
}

.delete-button {
  min-height: 36px;
  padding: 6px 12px;
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
  border: 1px solid rgba(231, 76, 60, 0.2);
  border-radius: 6px;
  font-size: 14px;
}

.delete-button.active {
  background: #e74c3c;
  color: white;
}

.confirm-delete-button {
  min-height: 36px;
  padding: 6px 12px;
  background: #e74c3c;
  color: white;
  border-radius: 6px;
  font-size: 14px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  width: 100%;
}

.course-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  position: relative;
}

.course-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.course-card.selected {
  box-shadow: 0 0 0 2px #e74c3c, 0 8px 16px rgba(0, 0, 0, 0.15);
}

.course-image {
  width: 100%;
  height: 160px;
  overflow: hidden;
  position: relative;
}

.course-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.selection-indicator {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(231, 76, 60, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.course-info {
  padding: 16px;
  flex-grow: 1;
}

.course-info h3 {
  font-size: 18px;
  margin: 0;
  color: #333;
  text-align: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.course-info h3:hover {
  background: rgba(104, 112, 250, 0.1);
}

.title-input {
  width: 100%;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  text-align: center;
  border: none;
  border-bottom: 1px solid #6870fa;
  background: transparent;
  padding: 4px 8px;
  margin: 0;
  outline: none;
  box-shadow: 0 2px 8px rgba(104, 112, 250, 0.2);
  border-radius: 4px;
  transition: all 0.2s ease;
}

.title-input:focus {
  box-shadow: 0 4px 12px rgba(104, 112, 250, 0.3);
  background: rgba(255, 255, 255, 0.9);
}

.course-status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: center;
  margin-top: 14px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
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

.review-time {
  color: #64748b;
  font-size: 12px;
}

.review-comment {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: #475569;
  text-align: center;
}

.review-hint {
  color: #9a3412;
}

.add-card {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(240, 240, 250, 0.5);
  color: #6870fa;
  min-height: 226px;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.15);
}

.add-card:hover {
  background: rgba(240, 240, 250, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.25);
}

.add-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 20px;
  font-size: 16px;
  color: #6870fa;
}

.empty-state {
  color: #64748b;
}

.error-message {
  text-align: center;
  padding: 10px;
  margin-bottom: 20px;
  background-color: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
  border: 1px solid rgba(231, 76, 60, 0.2);
  border-radius: 6px;
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
  background: linear-gradient(135deg, #818cf8, #6366f1);
  color: #fff;
  box-shadow: 0 12px 24px rgba(99, 102, 241, 0.2);
}

.pagination-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

@media (max-width: 900px) {
  .course-header,
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .page-size-group {
    justify-content: flex-start;
  }
}
</style>
