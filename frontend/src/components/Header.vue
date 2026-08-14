<template>
  <div>
    <header class="header-container">
      <el-row :gutter="0" type="flex" justify="space-between" align="middle">
      <!-- Logo -->
      <el-col :span="4">
        <div class="logo-container">
          <img src="https://res.cloudinary.com/dm3rouwgn/image/upload/t_media_lib_thumb/rfm1y1en2sqea4rd9ggy" alt="智教未来" class="logo" />
          <span class="logo-text">智教未来</span>
        </div>
      </el-col>
      
      <!-- 搜索框 -->
      <!-- <el-col :span="12" class="search-container">
        <el-input
          v-model="searchQuery"
          placeholder="请输入内容"
          class="search-input"
          :prefix-icon="Search"
          clearable
        />
      </el-col> -->
      
      <!-- 用户信息 -->
      <el-col :span="8">
        <div class="user-actions">
          <el-avatar :size="32" :icon="User" class="user-avatar" />
          <span class="username">{{ username }}</span>
          <template v-if="shouldShowNotifications">
            <el-divider direction="vertical" />
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
              <el-button text type="primary" :icon="Bell" @click="openNotificationDrawer" class="notification-btn">
                <span>通知</span>
              </el-button>
            </el-badge>
          </template>
          <el-divider direction="vertical" />
          <el-button text type="primary" :icon="SwitchButton" @click="handleLogout" class="logout-btn">
            <span>退出登录</span>
          </el-button>
        </div>
      </el-col>
      </el-row>
    </header>

    <el-drawer
      v-model="notificationDrawerVisible"
      title="站内通知"
      size="360px"
      :with-header="true"
    >
      <div class="drawer-header">
        <p>审核结果和功能限制变更会在这里提醒你。</p>
        <el-button
          v-if="unreadCount > 0"
          text
          type="primary"
          @click="handleMarkAllAsRead"
        >
          全部标记为已读
        </el-button>
      </div>

      <div v-if="notificationLoading" class="notification-state">
        正在加载通知...
      </div>

      <div v-else-if="notifications.length === 0" class="notification-state">
        当前暂无通知。
      </div>

      <div v-else class="notification-list">
        <article
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-card"
          :class="{ unread: notification.isRead !== 1 }"
          @click="handleNotificationClick(notification)"
        >
          <div class="notification-meta">
            <span class="notification-tag">{{ getNotificationTypeLabel(notification.type) }}</span>
            <span class="notification-time">{{ formatNotificationTime(notification.createdAt) }}</span>
          </div>
          <h4>{{ notification.title }}</h4>
          <p>{{ notification.content }}</p>
          <div class="notification-actions">
            <span v-if="notification.isRead === 1" class="read-state">已读</span>
            <el-button
              v-else
              text
              type="primary"
              :icon="Check"
              @click.stop="handleMarkAsRead(notification)"
            >
              标记已读
            </el-button>
          </div>
        </article>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { Bell, Check, Search, User, SwitchButton } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { logout } from '@/api/auth';
import { getTeacherCourses, type TeacherCourse } from '@/api/courseManger';
import {
  dispatchNotificationNavigation,
  getNotifications,
  markAllNotificationsAsRead,
  markNotificationAsRead,
  resolveNotificationNavigationTarget,
  type NotificationItem
} from '@/api/notifications';

const router = useRouter();
const searchQuery = ref('');
const username = ref('用户');
const userRole = ref('teacher');
const notificationDrawerVisible = ref(false);
const notificationLoading = ref(false);
const notifications = ref<NotificationItem[]>([]);
let notificationTimer: number | null = null;

const shouldShowNotifications = computed(() => userRole.value === 'teacher');
const unreadCount = computed(() => notifications.value.filter((item) => item.isRead !== 1).length);

// 解析JWT token获取用户名
const parseJwtToken = (token: string): any => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('解析token失败:', error);
    return { sub: '用户' };
  }
};

// 获取并设置用户名
const getUsernameFromToken = () => {
  try {
    // 优先从localStorage获取token，如果没有则从sessionStorage获取
    const token = localStorage.getItem('access_token') || sessionStorage.getItem('access_token');
    
    if (token) {
      const decoded = parseJwtToken(token);
      if (decoded && decoded.sub) {
        username.value = decoded.sub;
      }
    }
  } catch (error) {
    console.error('获取用户名失败:', error);
  }
};

const resolveUserRole = () => {
  const storedRole = localStorage.getItem('auth_role') || sessionStorage.getItem('auth_role');
  if (storedRole) {
    userRole.value = storedRole;
    return;
  }

  const adminFlag = localStorage.getItem('isAdmin') || sessionStorage.getItem('isAdmin');
  userRole.value = adminFlag === 'true' ? 'admin' : 'teacher';
};

const loadNotifications = async (silent = false) => {
  if (!shouldShowNotifications.value) {
    notifications.value = [];
    return;
  }

  if (!silent) {
    notificationLoading.value = true;
  }

  try {
    notifications.value = await getNotifications();
  } catch (error) {
    if (!silent) {
      ElMessage.error(error instanceof Error ? error.message : '获取通知失败');
    }
  } finally {
    if (!silent) {
      notificationLoading.value = false;
    }
  }
};

const openNotificationDrawer = async () => {
  notificationDrawerVisible.value = true;
  await loadNotifications();
};

const applyUpdatedNotification = (updatedNotification: NotificationItem) => {
  notifications.value = notifications.value.map((item) => (
    item.id === updatedNotification.id ? updatedNotification : item
  ));
};

const handleMarkAsRead = async (notification: NotificationItem) => {
  if (notification.isRead === 1) {
    return;
  }

  try {
    const updatedNotification = await markNotificationAsRead(notification.id);
    applyUpdatedNotification(updatedNotification);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新通知状态失败');
  }
};

const markNotificationAsReadSafely = async (notification: NotificationItem) => {
  if (notification.isRead === 1) {
    return;
  }

  try {
    const updatedNotification = await markNotificationAsRead(notification.id);
    applyUpdatedNotification(updatedNotification);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新通知状态失败');
  }
};

const findCourseByName = async (courseName: string): Promise<TeacherCourse | null> => {
  const normalizedCourseName = courseName.trim();
  if (!normalizedCourseName) {
    return null;
  }

  const response = await getTeacherCourses({
    keyword: normalizedCourseName,
    page: 1,
    pageSize: 50
  });

  if (response.items.length === 0) {
    return null;
  }

  const exactMatch = response.items.find((course) => course.name.trim() === normalizedCourseName);
  return exactMatch || response.items[0];
};

const handleNotificationClick = async (notification: NotificationItem) => {
  const target = resolveNotificationNavigationTarget(notification);
  await markNotificationAsReadSafely(notification);

  if (target.type === 'noop') {
    notificationDrawerVisible.value = false;
    return;
  }

  if (target.type === 'open-course-list') {
    if (notification.type === 'course_approved' || notification.type === 'course_rejected') {
      ElMessage.warning('未能定位到对应课程，请在课程列表中手动查找。');
    }

    dispatchNotificationNavigation({ type: 'open-course-list' });
    notificationDrawerVisible.value = false;
    return;
  }

  try {
    const matchedCourse = await findCourseByName(target.courseName);
    if (!matchedCourse) {
      ElMessage.warning('未能定位到对应课程，请在课程列表中手动查找。');
      dispatchNotificationNavigation({ type: 'open-course-list' });
      notificationDrawerVisible.value = false;
      return;
    }

    dispatchNotificationNavigation({
      type: 'open-course',
      courseId: matchedCourse.id,
      courseName: matchedCourse.name
    });
    notificationDrawerVisible.value = false;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '定位课程失败，请在课程列表中手动查找。');
    dispatchNotificationNavigation({ type: 'open-course-list' });
    notificationDrawerVisible.value = false;
  }
};

const handleMarkAllAsRead = async () => {
  try {
    await markAllNotificationsAsRead();
    notifications.value = notifications.value.map((item) => ({
      ...item,
      isRead: 1
    }));
    ElMessage.success('已将全部通知标记为已读');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量更新通知状态失败');
  }
};

const getNotificationTypeLabel = (type: string) => {
  switch (type) {
    case 'course_approved':
      return '审核通过';
    case 'course_rejected':
      return '审核驳回';
    case 'restriction_added':
      return '新增限制';
    case 'restriction_removed':
      return '解除限制';
    default:
      return '系统通知';
  }
};

const formatNotificationTime = (value?: string | Date) => {
  if (!value) {
    return '暂无时间';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '暂无时间';
  }

  return date.toLocaleString('zh-CN', { hour12: false });
};

// 组件挂载时获取用户名
onMounted(() => {
  getUsernameFromToken();
  resolveUserRole();
  if (shouldShowNotifications.value) {
    void loadNotifications(true);
    notificationTimer = window.setInterval(() => {
      void loadNotifications(true);
    }, 30000);
  }
});

onBeforeUnmount(() => {
  if (notificationTimer !== null) {
    window.clearInterval(notificationTimer);
    notificationTimer = null;
  }
});

// 退出登录功能
const handleLogout = () => {
  // 使用API进行登出
  const result = logout();
  
  // 清除所有本地存储的状态
  localStorage.removeItem('showFunctionSelect');
  localStorage.removeItem('showCourseInfo');
  localStorage.removeItem('showCourseDescription');
  localStorage.removeItem('showCourseOutline');
  localStorage.removeItem('showTeachingLecture');
  localStorage.removeItem('showCourseware');
  localStorage.removeItem('selectedCourseTitle');
  localStorage.removeItem('selectedCourseId');
  localStorage.removeItem('selectedModuleId');
  localStorage.removeItem('access_token');
  localStorage.removeItem('isAdmin');
  
  sessionStorage.removeItem('access_token');
  sessionStorage.removeItem('isAdmin');
  
  // 显示退出成功提示
  if (result.success) {
    ElMessage({
      message: result.message,
      type: 'success',
    });
  } else {
    ElMessage({
      message: result.message,
      type: 'error',
    });
  }
  
  // 跳转到登录页
  router.push('/login');
};
</script>

<style scoped>
.header-container {
  padding: 0 20px;
  height: 50px; /* 减小高度从60px到50px */
  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex; /* 添加flex布局 */
  align-items: center; /* 垂直居中 */
}

/* 添加发光边缘效果 */
.header-container::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0),
    rgba(255, 255, 255, 0.5),
    rgba(52, 152, 219, 0.3),
    rgba(255, 255, 255, 0.5),
    rgba(255, 255, 255, 0)
  );
  z-index: 1;
}

.logo-container {
  display: flex;
  align-items: center;
  height: 100%;
}

.logo {
  height: 28px; /* 稍微减小logo尺寸 */
  width: auto;
  filter: drop-shadow(0 0 3px rgba(255, 255, 255, 0.5));
}

.logo-text {
  margin-left: 8px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.search-container {
  display: flex;
  justify-content: center;
}

.search-input {
  max-width: 400px;
}

/* 修改搜索框样式使其融入背景 */
:deep(.el-input__wrapper) {
  border-radius: 20px;
  background-color: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  box-shadow: 0 2px 8px rgba(31, 38, 135, 0.1), 
              inset 0 1px 2px rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 2px 12px rgba(31, 38, 135, 0.15),
              inset 0 1px 2px rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 2px 12px rgba(52, 152, 219, 0.2),
              inset 0 1px 2px rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(52, 152, 219, 0.5);
  background-color: rgba(255, 255, 255, 0.25);
}

:deep(.el-input__inner) {
  color: rgba(0, 0, 0, 0.8);
}

:deep(.el-input__inner::placeholder) {
  color: rgba(0, 0, 0, 0.5);
}

.user-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
  gap: 0;
}

.user-avatar {
  background-color: rgba(52, 152, 219, 0.1);
  color: #3498db;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 2px 8px rgba(52, 152, 219, 0.2);
}

.username {
  margin: 0 12px 0 8px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.8);
}

:deep(.el-divider--vertical) {
  height: 1em;
  margin: 0 12px;
  border-color: rgba(255, 255, 255, 0.5);
}

/* 按钮样式 */
:deep(.el-button) {
  color: rgba(0, 0, 0, 0.7);
  transition: all 0.3s ease;
}

:deep(.el-button:hover) {
  color: rgba(52, 152, 219, 0.9);
  transform: translateY(-1px);
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.3);
}

/* 退出按钮样式增强 */
.logout-btn {
  position: relative;
  overflow: hidden;
}

.notification-badge {
  display: inline-flex;
  align-items: center;
}

.notification-btn {
  position: relative;
}

.logout-btn:hover {
  color: #e74c3c !important;
}

:deep(.logout-btn:hover .el-icon) {
  color: #e74c3c !important;
}

/* 确保el-row也垂直居中 */
:deep(.el-row) {
  height: 100%;
  width: 100%;
  align-items: center;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.drawer-header p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.notification-state {
  padding: 20px 0;
  color: #64748b;
  text-align: center;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-card {
  padding: 16px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(248, 250, 252, 0.88);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.notification-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.1);
}

.notification-card.unread {
  border-color: rgba(96, 165, 250, 0.55);
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.98), rgba(248, 250, 252, 0.96));
}

.notification-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.notification-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #1d4ed8;
  background: rgba(191, 219, 254, 0.48);
}

.notification-time {
  font-size: 12px;
  color: #94a3b8;
}

.notification-card h4 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #0f172a;
}

.notification-card p {
  margin: 0;
  color: #475569;
  line-height: 1.6;
}

.notification-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 12px;
}

.read-state {
  font-size: 12px;
  color: #10b981;
}
</style>
