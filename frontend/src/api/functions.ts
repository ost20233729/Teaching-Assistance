import axios from 'axios'
import { getAuthHeaders } from './jwt'

const API_URL = 'http://localhost:8080'

const authConfig = () => ({
  headers: getAuthHeaders()
})

export type PromptTemplateModule = 'objective' | 'syllabus' | 'material' | 'courseware'

export type ContentVersionModule = 'objective' | 'syllabus' | 'material' | 'courseware'

export interface PromptTemplate {
  id: string
  module: PromptTemplateModule
  category: string
  name: string
  description: string
  prompt: string
}

export interface ContentVersionRecord {
  id: number
  courseId: number
  moduleType: ContentVersionModule
  preview: string
  content: string
  createdBy: number
  createdAt: string | Date
}

export interface ContentVersionRestoreResponse {
  moduleType: ContentVersionModule
  data: Record<string, any>
}

export interface Restriction {
  id?: number
  userId: number
  functionName: string
  createdAt?: string | Date
}

export interface CoursewarePayload {
  courseId?: number
  content?: string
  createdAt?: string | Date
  updatedAt?: string | Date
}

export const getCourseObjective = async (courseId: number) => {
  const response = await axios.get(`${API_URL}/api/v1/teacher/courses/${courseId}/objective`, authConfig())
  return response.data
}

export const getPromptTemplates = async (module?: PromptTemplateModule): Promise<PromptTemplate[]> => {
  const response = await axios.get(`${API_URL}/api/v1/teacher/prompt-templates`, {
    ...authConfig(),
    params: module ? { module } : undefined
  })
  return response.data
}

export const generateCourseObjective = async (courseId: number, prompt: string) => {
  const response = await axios.post(
    `${API_URL}/api/v1/teacher/courses/${courseId}/objective-generations`,
    { prompt },
    authConfig()
  )
  return response.data
}

export const saveCourseObjective = async (courseId: number, objective: any) => {
  const response = await axios.put(
    `${API_URL}/api/v1/teacher/courses/${courseId}/objective`,
    objective,
    authConfig()
  )
  return response.data
}

export const getCourseSyllabus = async (courseId: number) => {
  const response = await axios.get(`${API_URL}/api/v1/teacher/courses/${courseId}/syllabus`, authConfig())
  return response.data
}

export const generateCourseSyllabus = async (courseId: number, prompt: string) => {
  const response = await axios.post(
    `${API_URL}/api/v1/teacher/courses/${courseId}/syllabus-generations`,
    { prompt },
    authConfig()
  )

  return typeof response.data === 'string'
    ? { content: response.data }
    : response.data
}

export const saveCourseSyllabus = async (courseId: number, syllabus: any) => {
  const response = await axios.put(
    `${API_URL}/api/v1/teacher/courses/${courseId}/syllabus`,
    syllabus,
    authConfig()
  )
  return response.data
}

export const getCourseMaterial = async (courseId: number) => {
  const response = await axios.get(`${API_URL}/api/v1/teacher/courses/${courseId}/material`, authConfig())
  return response.data
}

export const generateCourseMaterial = async (courseId: number, courseTitle: string, request: string) => {
  const response = await axios.post(
    `${API_URL}/api/v1/teacher/courses/${courseId}/material-generations`,
    { courseTitle, request },
    authConfig()
  )

  return typeof response.data === 'string'
    ? { content: response.data }
    : response.data
}

export const saveCourseMaterial = async (courseId: number, material: any) => {
  const response = await axios.put(
    `${API_URL}/api/v1/teacher/courses/${courseId}/material`,
    material,
    authConfig()
  )
  return response.data
}

export const getCourseCourseware = async (courseId: number): Promise<CoursewarePayload> => {
  const response = await axios.get(`${API_URL}/api/v1/teacher/courses/${courseId}/courseware`, authConfig())
  return response.data
}

export const generateCourseCourseware = async (courseId: number, prompt: string): Promise<CoursewarePayload> => {
  const response = await axios.post(
    `${API_URL}/api/v1/teacher/courses/${courseId}/courseware-generations`,
    { prompt },
    authConfig()
  )

  return typeof response.data === 'string'
    ? { content: response.data }
    : response.data
}

export const saveCourseCourseware = async (courseId: number, courseware: CoursewarePayload) => {
  const response = await axios.put(
    `${API_URL}/api/v1/teacher/courses/${courseId}/courseware`,
    courseware,
    authConfig()
  )
  return response.data
}

export const getContentVersions = async (
  courseId: number,
  module: ContentVersionModule
): Promise<ContentVersionRecord[]> => {
  const response = await axios.get(`${API_URL}/api/v1/teacher/courses/${courseId}/content-versions`, {
    ...authConfig(),
    params: { module }
  })
  return response.data
}

export const restoreContentVersion = async (
  courseId: number,
  versionId: number
): Promise<ContentVersionRestoreResponse> => {
  const response = await axios.post(
    `${API_URL}/api/v1/teacher/courses/${courseId}/content-versions/${versionId}/restorations`,
    {},
    authConfig()
  )
  return response.data
}

export const getCurrentRestrictions = async (): Promise<Restriction[]> => {
  const response = await axios.get(`${API_URL}/api/v1/teacher/restrictions`, authConfig())
  return response.data
}
