<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import CourseReview from '@/components/CourseReview.vue'
import RestrictionManage from '@/components/RestrictionManage.vue'
import Statistics from '@/components/Statistics.vue'
import UserManage from '@/components/UserManage.vue'

type AdminSection = 'users' | 'courses' | 'restrictions'

const router = useRouter()
const isLoggedIn = ref(false)
const isAdmin = ref(false)
const activeSection = ref<AdminSection>('users')

const sections: Array<{ key: AdminSection; label: string; description: string }> = [
  {
    key: 'users',
    label: '用户管理',
    description: '维护教师账号信息'
  },
  {
    key: 'courses',
    label: '课程审核',
    description: '审批课程状态'
  },
  {
    key: 'restrictions',
    label: '功能限制',
    description: '按教师配置功能权限'
  }
]

const checkLoginStatus = () => {
  const token = sessionStorage.getItem('access_token') || localStorage.getItem('access_token')
  const adminFlag = sessionStorage.getItem('isAdmin') || localStorage.getItem('isAdmin')

  isLoggedIn.value = Boolean(token)
  isAdmin.value = adminFlag === 'true'

  if (!isLoggedIn.value) {
    router.push('/login')
    return
  }

  if (!isAdmin.value) {
    router.push('/')
  }
}

onMounted(() => {
  checkLoginStatus()
  window.addEventListener('login-state-changed', checkLoginStatus)
})

onBeforeUnmount(() => {
  window.removeEventListener('login-state-changed', checkLoginStatus)
})
</script>

<template>
  <div class="admin-container">
    <Header />

    <main v-if="isLoggedIn" class="content-area">
      <Statistics />

      <section class="switcher-shell">
        <div class="switcher-header">
          <div>
            <h2>管理员工作台</h2>
            <p>沿用当前管理界面的样式，将用户、课程和权限入口集中在一个页面中。</p>
          </div>
        </div>

        <div class="switcher-row">
          <button
            v-for="section in sections"
            :key="section.key"
            class="switcher-button"
            :class="{ active: activeSection === section.key }"
            @click="activeSection = section.key"
          >
            <strong>{{ section.label }}</strong>
            <span>{{ section.description }}</span>
          </button>
        </div>
      </section>

      <UserManage v-if="activeSection === 'users'" />
      <CourseReview v-else-if="activeSection === 'courses'" />
      <RestrictionManage v-else />
    </main>
  </div>
</template>

<style scoped>
.admin-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  position: relative;
}

.content-area {
  flex: 1;
  position: relative;
  margin-top: 50px;
  min-height: calc(100vh - 50px);
  padding-bottom: 24px;
}

.switcher-shell {
  margin: 0 20px 20px;
  padding: 24px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(255, 255, 255, 0.42);
  box-shadow: 0 20px 48px rgba(31, 38, 135, 0.12);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.switcher-header {
  margin-bottom: 18px;
}

.switcher-header h2 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #111827;
}

.switcher-header p {
  margin: 0;
  color: #6b7280;
}

.switcher-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.switcher-button {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 6px;
  min-height: 108px;
  padding: 18px 20px;
  text-align: left;
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.88);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.switcher-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(31, 38, 135, 0.08);
}

.switcher-button strong {
  display: block;
  margin-bottom: 0;
  color: #0f172a;
  font-size: 18px;
}

.switcher-button span {
  color: #64748b;
  font-size: 13px;
}

.switcher-button.active {
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.92), rgba(239, 246, 255, 0.94));
  border-color: rgba(96, 165, 250, 0.7);
  box-shadow: 0 14px 30px rgba(59, 130, 246, 0.14);
}

@media (max-width: 900px) {
  .switcher-row {
    grid-template-columns: 1fr;
  }
}
</style>
