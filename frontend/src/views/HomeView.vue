<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { checkAuthentication, getCurrentUsername } from '@/api/auth'
import { TEACHER_NOTIFICATION_NAVIGATION_EVENT, type NotificationNavigationPayload } from '@/api/notifications'
import Header from '@/components/Header.vue'
import CourseManage from '@/components/CourseManage.vue'
import FunctionSelect from '@/components/FunctionSelect.vue'
import CourseInfo from '@/components/CourseInfo.vue'
import CourseDescription from '@/components/CourseDescription.vue'
import CourseOutline from '@/components/CourseOutline.vue'
import TeachingLecture from '@/components/TeachingLecture.vue'
import Courseware from '@/components/Courseware.vue'

type ModuleId = 'basic' | 'outline' | 'lecture' | 'courseware'
type TransientModuleStatus = 'generated_unsaved'

interface ModuleStatusChangePayload {
  moduleId: ModuleId
  status: TransientModuleStatus | 'completed'
}

const router = useRouter()
const username = ref('')
const isLoggedIn = ref(false)
const showFunctionSelect = ref(false)
const showCourseInfo = ref(false)
const showCourseDescription = ref(false)
const showCourseOutline = ref(false)
const showTeachingLecture = ref(false)
const showCourseware = ref(false)
const selectedCourseTitle = ref('')
const selectedCourseId = ref<number | undefined>(undefined)
const selectedModuleId = ref('')
const transientModuleStatuses = ref<Record<string, Partial<Record<ModuleId, TransientModuleStatus>>>>({})

const getCourseStatusKey = (courseId: number) => String(courseId)

const setTransientModuleStatus = (courseId: number, moduleId: ModuleId, status?: TransientModuleStatus) => {
  const courseKey = getCourseStatusKey(courseId)
  const nextCourseStatuses = {
    ...(transientModuleStatuses.value[courseKey] || {})
  }

  if (status) {
    nextCourseStatuses[moduleId] = status
    transientModuleStatuses.value = {
      ...transientModuleStatuses.value,
      [courseKey]: nextCourseStatuses
    }
    return
  }

  delete nextCourseStatuses[moduleId]

  if (Object.keys(nextCourseStatuses).length === 0) {
    const nextStatuses = { ...transientModuleStatuses.value }
    delete nextStatuses[courseKey]
    transientModuleStatuses.value = nextStatuses
    return
  }

  transientModuleStatuses.value = {
    ...transientModuleStatuses.value,
    [courseKey]: nextCourseStatuses
  }
}

const currentCourseTransientStatuses = computed<Partial<Record<ModuleId, TransientModuleStatus>>>(() => {
  if (selectedCourseId.value == null) {
    return {}
  }

  return transientModuleStatuses.value[getCourseStatusKey(selectedCourseId.value)] || {}
})

const restoreState = () => {
  const storedShowFunctionSelect = localStorage.getItem('showFunctionSelect')
  const storedShowCourseInfo = localStorage.getItem('showCourseInfo')
  const storedShowCourseDescription = localStorage.getItem('showCourseDescription')
  const storedShowCourseOutline = localStorage.getItem('showCourseOutline')
  const storedShowTeachingLecture = localStorage.getItem('showTeachingLecture')
  const storedShowCourseware = localStorage.getItem('showCourseware')
  const storedCourseTitle = localStorage.getItem('selectedCourseTitle')
  const storedCourseId = localStorage.getItem('selectedCourseId')
  const storedModuleId = localStorage.getItem('selectedModuleId')
  const fromLogin = sessionStorage.getItem('fromLogin') === 'true'

  if (fromLogin) {
    sessionStorage.removeItem('fromLogin')
    return
  }

  if (storedShowFunctionSelect === 'true' && storedCourseTitle) {
    showFunctionSelect.value = true
    selectedCourseTitle.value = storedCourseTitle
    if (storedCourseId) {
      selectedCourseId.value = parseInt(storedCourseId)
    }
  }

  if (storedShowCourseInfo === 'true') {
    showCourseInfo.value = true
  }

  if (storedShowCourseDescription === 'true' && storedModuleId) {
    showCourseDescription.value = true
    selectedModuleId.value = storedModuleId
  }

  if (storedShowCourseOutline === 'true') {
    showCourseOutline.value = true
  }

  if (storedShowTeachingLecture === 'true') {
    showTeachingLecture.value = true
  }

  if (storedShowCourseware === 'true') {
    showCourseware.value = true
  }
}

watch(
  [
    showFunctionSelect,
    showCourseInfo,
    showCourseDescription,
    showCourseOutline,
    showTeachingLecture,
    showCourseware,
    selectedCourseTitle,
    selectedCourseId,
    selectedModuleId
  ],
  () => {
    localStorage.setItem('showFunctionSelect', showFunctionSelect.value.toString())
    localStorage.setItem('showCourseInfo', showCourseInfo.value.toString())
    localStorage.setItem('showCourseDescription', showCourseDescription.value.toString())
    localStorage.setItem('showCourseOutline', showCourseOutline.value.toString())
    localStorage.setItem('showTeachingLecture', showTeachingLecture.value.toString())
    localStorage.setItem('showCourseware', showCourseware.value.toString())
    localStorage.setItem('selectedCourseTitle', selectedCourseTitle.value)
    if (selectedCourseId.value !== undefined) {
      localStorage.setItem('selectedCourseId', selectedCourseId.value.toString())
    }
    localStorage.setItem('selectedModuleId', selectedModuleId.value)
  }
)

const checkLoginStatus = () => {
  isLoggedIn.value = checkAuthentication()
  const isAdmin = (localStorage.getItem('isAdmin') || sessionStorage.getItem('isAdmin')) === 'true'

  if (!isLoggedIn.value) {
    router.push('/login')
    return
  }

  if (isAdmin) {
    router.push('/admin')
    return
  }

  username.value = getCurrentUsername()
}

const resetTeacherViews = () => {
  showFunctionSelect.value = false
  showCourseInfo.value = false
  showCourseDescription.value = false
  showCourseOutline.value = false
  showTeachingLecture.value = false
  showCourseware.value = false
  selectedModuleId.value = ''
}

onMounted(() => {
  checkLoginStatus()
  restoreState()
  window.addEventListener('login-state-changed', checkLoginStatus)
  window.addEventListener(TEACHER_NOTIFICATION_NAVIGATION_EVENT, handleNotificationNavigation as EventListener)
})

const openFunctionSelect = (course: { name: string; id: number }) => {
  resetTeacherViews()
  selectedCourseTitle.value = course.name
  selectedCourseId.value = course.id
  showFunctionSelect.value = true
}

const backToCourseManage = () => {
  resetTeacherViews()
  selectedCourseTitle.value = ''
  selectedCourseId.value = undefined
}

const showCourseInfoPanel = () => {
  showCourseInfo.value = true
  showCourseDescription.value = false
}

const hideCourseInfoPanel = () => {
  showCourseInfo.value = false
}

const showModule = (moduleId: string) => {
  if (selectedCourseId.value != null) {
    setTransientModuleStatus(selectedCourseId.value, moduleId as ModuleId)
  }

  selectedModuleId.value = moduleId
  showCourseDescription.value = moduleId === 'basic'
  showCourseOutline.value = moduleId === 'outline'
  showTeachingLecture.value = moduleId === 'lecture'
  showCourseware.value = moduleId === 'courseware'
  showCourseInfo.value = false
}

const backToFunctionSelect = () => {
  showCourseDescription.value = false
  showCourseOutline.value = false
  showTeachingLecture.value = false
  showCourseware.value = false
}

const handleModuleStatusChange = (payload: ModuleStatusChangePayload) => {
  if (selectedCourseId.value == null) {
    return
  }

  if (payload.status === 'completed') {
    setTransientModuleStatus(selectedCourseId.value, payload.moduleId)
    return
  }

  setTransientModuleStatus(selectedCourseId.value, payload.moduleId, payload.status)
}

const handleNotificationNavigation = (event: Event) => {
  const customEvent = event as CustomEvent<NotificationNavigationPayload>
  const payload = customEvent.detail
  if (!payload) {
    return
  }

  if (payload.type === 'open-course-list') {
    backToCourseManage()
    return
  }

  openFunctionSelect({
    id: payload.courseId,
    name: payload.courseName
  })
}

onBeforeUnmount(() => {
  window.removeEventListener('login-state-changed', checkLoginStatus)
  window.removeEventListener(TEACHER_NOTIFICATION_NAVIGATION_EVENT, handleNotificationNavigation as EventListener)
})
</script>

<template>
  <div class="home-container">
    <Header />

    <div v-if="isLoggedIn" class="content-area">
      <CourseManage
        v-if="!showFunctionSelect && !showCourseInfo && !showCourseDescription && !showCourseOutline && !showTeachingLecture && !showCourseware"
        @course-selected="openFunctionSelect"
      />
      <FunctionSelect
        v-else-if="showFunctionSelect && !showCourseInfo && !showCourseDescription && !showCourseOutline && !showTeachingLecture && !showCourseware"
        :courseTitle="selectedCourseTitle"
        :courseId="selectedCourseId"
        :transient-module-states="currentCourseTransientStatuses"
        @back="backToCourseManage"
        @show-course-info="showCourseInfoPanel"
        @show-module="showModule"
      />
      <CourseInfo
        v-else-if="showCourseInfo"
        @back="hideCourseInfoPanel"
      />
      <CourseDescription
        v-else-if="showCourseDescription"
        :courseId="selectedCourseId"
        @back="backToFunctionSelect"
        @module-status-change="handleModuleStatusChange"
      />
      <CourseOutline
        v-else-if="showCourseOutline"
        :courseId="selectedCourseId"
        @back="backToFunctionSelect"
        @module-status-change="handleModuleStatusChange"
      />
      <TeachingLecture
        v-else-if="showTeachingLecture"
        :courseId="selectedCourseId"
        @back="backToFunctionSelect"
        @module-status-change="handleModuleStatusChange"
      />
      <Courseware
        v-else-if="showCourseware"
        :courseId="selectedCourseId"
        @back="backToFunctionSelect"
        @module-status-change="handleModuleStatusChange"
      />
    </div>
  </div>
</template>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  position: relative;
}

.content-area {
  flex: 1;
  position: relative;
  margin-top: 50px;
}
</style>
