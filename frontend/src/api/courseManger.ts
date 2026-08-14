import axios from 'axios'
import { getAuthHeaders } from './jwt'
import type { PagedResponse } from './pagination'

const API_URL = 'http://localhost:8080'

export interface TeacherCourse {
  id: number
  teacherId: number
  name: string
  status: string
  reviewComment?: string | null
  reviewedAt?: string | Date | null
  isDeleted?: number
  createdAt?: string | Date
  updatedAt?: string | Date
}

export interface TeacherCourseQuery {
  keyword?: string
  status?: string
  page?: number
  pageSize?: number
}

const parseFileName = (contentDisposition?: string, fallback = '课程成果.md') => {
  if (!contentDisposition) {
    return fallback
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch {
      return utf8Match[1]
    }
  }

  const filenameMatch = contentDisposition.match(/filename=\"?([^\";]+)\"?/i)
  return filenameMatch?.[1] || fallback
}

export const getTeacherCourses = async (params: TeacherCourseQuery = {}) => {
  const response = await axios.get<PagedResponse<TeacherCourse>>(`${API_URL}/api/v1/teacher/courses`, {
    headers: getAuthHeaders(),
    params
  })
  return response.data
}

export const getCourseDetail = async (courseId: number) => {
  const response = await axios.get(`${API_URL}/api/v1/teacher/courses/${courseId}`, {
    headers: getAuthHeaders()
  })
  return response.data
}

export const createCourse = async (courseName: string) => {
  const response = await axios.post(
    `${API_URL}/api/v1/teacher/courses`,
    { courseName },
    { headers: getAuthHeaders() }
  )
  return response.data as TeacherCourse
}

export const updateCourseName = async (courseId: number, courseName: string) => {
  const response = await axios.patch(
    `${API_URL}/api/v1/teacher/courses/${courseId}`,
    { courseName },
    { headers: getAuthHeaders() }
  )
  return response.data as TeacherCourse
}

export const deleteCourse = async (courseId: number) => {
  await axios.delete(`${API_URL}/api/v1/teacher/courses/${courseId}`, {
    headers: getAuthHeaders()
  })
}

export const downloadCourseMarkdownExport = async (courseId: number) => {
  try {
    const response = await axios.get(`${API_URL}/api/v1/teacher/courses/${courseId}/export/markdown`, {
      headers: getAuthHeaders(),
      responseType: 'blob'
    })

    const contentType = response.headers['content-type'] || 'text/markdown;charset=utf-8'
    const fileName = parseFileName(response.headers['content-disposition'], `course-${courseId}-export.md`)
    const blob = new Blob([response.data], { type: contentType })
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')

    link.href = objectUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(objectUrl)

    return fileName
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.data instanceof Blob) {
      const messageText = await error.response.data.text()

      try {
        const parsed = JSON.parse(messageText) as { message?: string }
        throw new Error(parsed.message || '课程成果导出失败')
      } catch {
        throw new Error(messageText || '课程成果导出失败')
      }
    }

    throw error
  }
}
