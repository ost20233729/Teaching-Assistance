import axios from 'axios'
import { getToken, removeToken, setSessionToken, setToken } from './jwt'

const api = axios.create({
  baseURL: 'http://localhost:8080'
})

export interface RegisterResponse {
  success: boolean
  message?: string
}

export interface AvailabilityResponse {
  available: boolean
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  role: string
}

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

const parseUsernameFromToken = (token: string): string | null => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return typeof payload.sub === 'string' ? payload.sub : null
  } catch {
    return null
  }
}

export const login = async (
  username: string,
  password: string
): Promise<{ success: boolean; data?: LoginResponse; message?: string }> => {
  try {
    const response = await api.post<LoginResponse>('/api/v1/auth/teacher-sessions', {
      username,
      password
    })

    const loginData = response.data
    setToken(loginData.token)
    setSessionToken(loginData.token)
    localStorage.setItem('auth_username', loginData.username)
    sessionStorage.setItem('auth_username', loginData.username)
    localStorage.setItem('auth_role', loginData.role)
    sessionStorage.setItem('auth_role', loginData.role)

    return {
      success: true,
      data: loginData
    }
  } catch (error: unknown) {
    return {
      success: false,
      message: parseErrorMessage(error, '登录失败')
    }
  }
}

export const register = async (
  username: string,
  password: string,
  email: string,
  role: string = 'teacher'
): Promise<RegisterResponse> => {
  try {
    await api.post('/api/v1/auth/registrations', {
      username,
      password,
      email,
      role
    })

    return { success: true, message: '注册成功，请登录' }
  } catch (error: unknown) {
    return {
      success: false,
      message: parseErrorMessage(error, '注册请求失败')
    }
  }
}

export const checkUsernameAvailable = async (username: string): Promise<boolean> => {
  try {
    const response = await api.get<AvailabilityResponse>('/api/v1/auth/usernames/availability', {
      params: { username }
    })
    return response.data.available
  } catch (error: unknown) {
    console.error('检查用户名可用性失败', error)
    return false
  }
}

export const checkEmailAvailable = async (email: string): Promise<boolean> => {
  try {
    const response = await api.get<AvailabilityResponse>('/api/v1/auth/emails/availability', {
      params: { email }
    })
    return response.data.available
  } catch (error: unknown) {
    console.error('检查邮箱可用性失败', error)
    return false
  }
}

export const logout = (): { success: boolean; message: string } => {
  try {
    removeToken()
    localStorage.removeItem('auth_username')
    sessionStorage.removeItem('auth_username')
    localStorage.removeItem('auth_role')
    sessionStorage.removeItem('auth_role')
    localStorage.removeItem('user_info')
    sessionStorage.removeItem('user_info')
    localStorage.removeItem('user_id')
    sessionStorage.removeItem('user_id')
    localStorage.removeItem('userId')
    sessionStorage.removeItem('userId')
    return {
      success: true,
      message: '退出登录成功'
    }
  } catch (error) {
    console.error('退出登录失败', error)
    return {
      success: false,
      message: '退出登录失败'
    }
  }
}

export const checkAuthentication = (): boolean => {
  return !!getToken()
}

export const getCurrentUsername = (): string => {
  const storedUsername = localStorage.getItem('auth_username') || sessionStorage.getItem('auth_username')
  if (storedUsername) {
    return storedUsername
  }

  const token = getToken()
  if (!token) {
    return '教学用户'
  }

  return parseUsernameFromToken(token) || '教学用户'
}
