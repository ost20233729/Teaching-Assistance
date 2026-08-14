import { createRouter, createWebHistory } from 'vue-router'
import { checkAuthentication } from '@/api/auth'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomeView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/AuthForm.vue')
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (!to.matched.some(record => record.meta.requiresAuth)) {
    next()
    return
  }

  if (!checkAuthentication()) {
    next({ name: 'login' })
    return
  }

  const isAdmin = (localStorage.getItem('isAdmin') || sessionStorage.getItem('isAdmin')) === 'true'

  if (to.path === '/' && isAdmin) {
    next('/admin')
    return
  }

  if (to.matched.some(record => record.meta.requiresAdmin) && !isAdmin) {
    next('/')
    return
  }

  if (to.path === '/' && !isAdmin && from.path === '/login') {
    localStorage.removeItem('showFunctionSelect')
    localStorage.removeItem('showCourseInfo')
    localStorage.removeItem('showCourseDescription')
    localStorage.removeItem('showCourseOutline')
    localStorage.removeItem('showTeachingLecture')
    localStorage.removeItem('showCourseware')
    localStorage.removeItem('selectedCourseTitle')
    localStorage.removeItem('selectedCourseId')
    localStorage.removeItem('selectedModuleId')
  }

  next()
})

export default router
