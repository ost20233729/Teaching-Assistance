import axios from 'axios'
import { getAuthHeaders, setSessionToken, setToken } from './jwt'
import type { PagedResponse } from './pagination'

const API_BASE_URL = 'http://localhost:8080'

export interface AdminLoginDTO {
  username: string
  password: string
}

export interface User {
  id?: number
  username: string
  password?: string
  email: string
  avatarUrl?: string
  role?: string
  status?: string
  isDeleted?: number
  createdAt?: string | Date
  updatedAt?: string | Date
}

export interface LoginResponse {
  token: string
  user: User
}

export interface DashboardData {
  userStats: {
    totalUsers: number
    teacherCount: number
    adminCount: number
  }
  courseStats: {
    totalCourses: number
    pendingCount: number
    approvedCount: number
    rejectedCount: number
  }
  llmStats: LlmStatistics
}

export interface LlmCallLog {
  id: number
  userId: number
  username: string
  courseId?: number | null
  moduleType: string
  requestSummary: string
  status: string
  errorMessage?: string | null
  createdAt: string | Date
}

export interface LlmStatistics {
  totalCalls: number
  successCount: number
  failedCount: number
  successRate: number
  objectiveCount: number
  syllabusCount: number
  materialCount: number
  coursewareCount: number
  markdownCount: number
  recentCalls: LlmCallLog[]
}

export interface AdminCourse {
  id?: number
  teacherId: number
  name: string
  status: 'pending' | 'approved' | 'rejected' | string
  reviewComment?: string | null
  reviewedAt?: string | Date | null
  isDeleted?: number
  createdAt?: string | Date
  updatedAt?: string | Date
}

export interface CourseReviewPayload {
  status: 'pending' | 'approved' | 'rejected'
  reviewComment?: string
}

export interface AdminCourseQuery {
  keyword?: string
  status?: string
  page?: number
  pageSize?: number
}

export interface Restriction {
  id?: number
  userId: number
  functionName: string
  createdAt?: string | Date
}

export interface RestrictionOption {
  value: string
  label: string
  description: string
}

export const restrictionOptions: RestrictionOption[] = [
  {
    value: 'basic',
    label: '课程介绍与教学目标',
    description: '限制该教师使用课程介绍与教学目标模块。'
  },
  {
    value: 'outline',
    label: '课程大纲',
    description: '限制该教师查看和生成课程大纲。'
  },
  {
    value: 'lecture',
    label: '教学讲义',
    description: '限制该教师查看和生成教学讲义。'
  },
  {
    value: 'courseware',
    label: '教学课件提纲',
    description: '限制该教师使用教学课件提纲模块。'
  }
]

const parseErrorMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError(error)) {
    const responseData = error.response?.data
    if (typeof responseData === 'string' && responseData.trim()) {
      return responseData
    }

    const responseMessage = responseData?.message
    if (typeof responseMessage === 'string' && responseMessage.trim()) {
      return responseMessage
    }
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return fallback
}

const authConfig = () => ({
  headers: getAuthHeaders()
})

export const getDashboardData = async (): Promise<DashboardData> => {
  try {
    const response = await axios.get<DashboardData>(`${API_BASE_URL}/api/v1/admin/statistics/dashboard`, authConfig())
    return response.data
  } catch (error) {
    throw new Error(parseErrorMessage(error, '获取统计数据失败'))
  }
}

export const getLlmStatistics = async (): Promise<LlmStatistics> => {
  try {
    const response = await axios.get<LlmStatistics>(`${API_BASE_URL}/api/v1/admin/statistics/llm-calls`, authConfig())
    return response.data
  } catch (error) {
    throw new Error(parseErrorMessage(error, '获取 LLM 统计数据失败'))
  }
}

export const adminApi = {
  login: async (loginData: AdminLoginDTO): Promise<LoginResponse> => {
    try {
      const response = await axios.post<LoginResponse>(`${API_BASE_URL}/api/v1/auth/admin-sessions`, loginData)
      const { token, user } = response.data

      setToken(token)
      setSessionToken(token)
      localStorage.setItem('auth_username', user.username)
      sessionStorage.setItem('auth_username', user.username)
      localStorage.setItem('auth_role', 'admin')
      sessionStorage.setItem('auth_role', 'admin')

      if (user.id) {
        localStorage.setItem('user_id', user.id.toString())
        sessionStorage.setItem('user_id', user.id.toString())
      }

      localStorage.setItem('user_info', JSON.stringify(user))
      sessionStorage.setItem('user_info', JSON.stringify(user))
      return response.data
    } catch (error) {
      throw new Error(parseErrorMessage(error, '登录失败'))
    }
  },

  getUserList: async (): Promise<User[]> => {
    try {
      const response = await axios.get<User[]>(`${API_BASE_URL}/api/v1/admin/users`, authConfig())
      return response.data
    } catch (error) {
      throw new Error(parseErrorMessage(error, '获取用户列表失败'))
    }
  },

  addUser: async (userData: User): Promise<User> => {
    try {
      const response = await axios.post<User>(`${API_BASE_URL}/api/v1/admin/users`, userData, authConfig())
      return response.data
    } catch (error) {
      throw new Error(parseErrorMessage(error, '添加用户失败'))
    }
  },

  deleteUser: async (id: number): Promise<void> => {
    try {
      await axios.delete(`${API_BASE_URL}/api/v1/admin/users/${id}`, authConfig())
    } catch (error) {
      throw new Error(parseErrorMessage(error, '删除用户失败'))
    }
  },

  getUserInfo: async (id: number): Promise<User> => {
    try {
      const response = await axios.get<User>(`${API_BASE_URL}/api/v1/admin/users/${id}`, authConfig())
      return response.data
    } catch (error) {
      throw new Error(parseErrorMessage(error, '获取用户信息失败'))
    }
  },

  updateUser: async (id: number, payload: Partial<User>): Promise<User> => {
    try {
      const response = await axios.patch<User>(`${API_BASE_URL}/api/v1/admin/users/${id}`, payload, authConfig())
      return response.data
    } catch (error) {
      throw new Error(parseErrorMessage(error, '更新用户信息失败'))
    }
  },

  updateUsername: async (id: number, username: string): Promise<User> => {
    return adminApi.updateUser(id, { username })
  },

  updateEmail: async (id: number, email: string): Promise<User> => {
    return adminApi.updateUser(id, { email })
  },

  getCourseList: async (params: AdminCourseQuery = {}): Promise<PagedResponse<AdminCourse>> => {
    try {
      const response = await axios.get<PagedResponse<AdminCourse>>(`${API_BASE_URL}/api/v1/admin/courses`, {
        ...authConfig(),
        params
      })
      return response.data
    } catch (error) {
      throw new Error(parseErrorMessage(error, '获取课程列表失败'))
    }
  },

  getPendingCourses: async (): Promise<PagedResponse<AdminCourse>> => {
    return adminApi.getCourseList({ status: 'pending' })
  },

  updateCourseReview: async (id: number, payload: CourseReviewPayload): Promise<AdminCourse> => {
    try {
      const response = await axios.patch<AdminCourse>(
        `${API_BASE_URL}/api/v1/admin/courses/${id}`,
        payload,
        authConfig()
      )
      return response.data
    } catch (error) {
      throw new Error(parseErrorMessage(error, '更新课程审核失败'))
    }
  },

  approveCourse: async (id: number, reviewComment = ''): Promise<AdminCourse> => {
    return adminApi.updateCourseReview(id, { status: 'approved', reviewComment })
  },

  rejectCourse: async (id: number, reviewComment: string): Promise<AdminCourse> => {
    return adminApi.updateCourseReview(id, { status: 'rejected', reviewComment })
  },

  resetCourseToPending: async (id: number): Promise<AdminCourse> => {
    return adminApi.updateCourseReview(id, { status: 'pending' })
  },

  getUserRestrictions: async (userId: number): Promise<Restriction[]> => {
    try {
      const response = await axios.get<Restriction[]>(
        `${API_BASE_URL}/api/v1/admin/users/${userId}/restrictions`,
        authConfig()
      )
      return response.data
    } catch (error) {
      throw new Error(parseErrorMessage(error, '获取功能限制失败'))
    }
  },

  addRestriction: async (restriction: Restriction): Promise<Restriction> => {
    try {
      const response = await axios.post<Restriction>(
        `${API_BASE_URL}/api/v1/admin/users/${restriction.userId}/restrictions`,
        restriction,
        authConfig()
      )
      return response.data
    } catch (error) {
      throw new Error(parseErrorMessage(error, '添加功能限制失败'))
    }
  },

  removeRestriction: async (userId: number, id: number): Promise<void> => {
    try {
      await axios.delete(`${API_BASE_URL}/api/v1/admin/users/${userId}/restrictions/${id}`, authConfig())
    } catch (error) {
      throw new Error(parseErrorMessage(error, '移除功能限制失败'))
    }
  },

  getCurrentUser: (): User | null => {
    try {
      const userInfo = localStorage.getItem('user_info') || sessionStorage.getItem('user_info')
      return userInfo ? JSON.parse(userInfo) : null
    } catch (error) {
      console.error('[Admin] 获取当前用户信息失败:', error)
      return null
    }
  },

  getCurrentUserId: (): number | null => {
    const id = localStorage.getItem('user_id') || sessionStorage.getItem('user_id')
    return id ? parseInt(id, 10) : null
  }
}

export default adminApi
