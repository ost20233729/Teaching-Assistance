import axios from 'axios'
import { getAuthHeaders } from './jwt'

const API_BASE_URL = 'http://localhost:8080'

export interface NotificationItem {
  id: number
  userId: number
  title: string
  content: string
  type: string
  isRead: number
  createdAt?: string | Date
}

export type NotificationNavigationTarget =
  | { type: 'open-course'; courseName: string }
  | { type: 'open-course-list' }
  | { type: 'noop' }

export type NotificationNavigationPayload =
  | { type: 'open-course'; courseId: number; courseName: string }
  | { type: 'open-course-list' }

export const TEACHER_NOTIFICATION_NAVIGATION_EVENT = 'teacher-notification-navigation'

const authConfig = () => ({
  headers: getAuthHeaders()
})

const parseErrorMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError(error)) {
    const responseMessage = error.response?.data?.message
    if (typeof responseMessage === 'string' && responseMessage.trim()) {
      return responseMessage
    }
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return fallback
}

export const getNotifications = async (): Promise<NotificationItem[]> => {
  try {
    const response = await axios.get<NotificationItem[]>(`${API_BASE_URL}/api/v1/teacher/notifications`, authConfig())
    return response.data
  } catch (error) {
    throw new Error(parseErrorMessage(error, '获取通知失败'))
  }
}

export const markNotificationAsRead = async (notificationId: number): Promise<NotificationItem> => {
  try {
    const response = await axios.patch<NotificationItem>(
      `${API_BASE_URL}/api/v1/teacher/notifications/${notificationId}/read`,
      {},
      authConfig()
    )
    return response.data
  } catch (error) {
    throw new Error(parseErrorMessage(error, '更新通知状态失败'))
  }
}

export const markAllNotificationsAsRead = async (): Promise<void> => {
  try {
    await axios.patch(`${API_BASE_URL}/api/v1/teacher/notifications/read-all`, {}, authConfig())
  } catch (error) {
    throw new Error(parseErrorMessage(error, '批量更新通知状态失败'))
  }
}

export const extractCourseNameFromNotification = (content: string) => {
  const matched = content.match(/课程《(.+?)》/)
  return matched?.[1]?.trim() || ''
}

export const resolveNotificationNavigationTarget = (notification: NotificationItem): NotificationNavigationTarget => {
  switch (notification.type) {
    case 'course_approved':
    case 'course_rejected': {
      const courseName = extractCourseNameFromNotification(notification.content)
      return courseName
        ? { type: 'open-course', courseName }
        : { type: 'open-course-list' }
    }
    case 'restriction_added':
    case 'restriction_removed':
      return { type: 'open-course-list' }
    default:
      return { type: 'noop' }
  }
}

export const dispatchNotificationNavigation = (payload: NotificationNavigationPayload) => {
  window.dispatchEvent(new CustomEvent<NotificationNavigationPayload>(TEACHER_NOTIFICATION_NAVIGATION_EVENT, {
    detail: payload
  }))
}
