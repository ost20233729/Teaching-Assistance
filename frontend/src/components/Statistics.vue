<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DataLine, Reading, Refresh, User } from '@element-plus/icons-vue'
import { getDashboardData, type DashboardData, type LlmCallLog } from '@/api/admin'

const loading = ref(false)

const createDefaultDashboardData = (): DashboardData => ({
  userStats: {
    totalUsers: 0,
    teacherCount: 0,
    adminCount: 0
  },
  courseStats: {
    totalCourses: 0,
    pendingCount: 0,
    approvedCount: 0,
    rejectedCount: 0
  },
  llmStats: {
    totalCalls: 0,
    successCount: 0,
    failedCount: 0,
    successRate: 0,
    objectiveCount: 0,
    syllabusCount: 0,
    materialCount: 0,
    coursewareCount: 0,
    markdownCount: 0,
    recentCalls: []
  }
})

const dashboardData = ref<DashboardData>(createDefaultDashboardData())

const summaryCards = computed(() => [
  {
    title: '总用户数',
    value: dashboardData.value.userStats.totalUsers,
    icon: User,
    iconClass: 'user-icon'
  },
  {
    title: '总课程数',
    value: dashboardData.value.courseStats.totalCourses,
    icon: Reading,
    iconClass: 'course-icon'
  },
  {
    title: 'LLM 调用总数',
    value: dashboardData.value.llmStats.totalCalls,
    icon: DataLine,
    iconClass: 'llm-icon'
  },
  {
    title: 'LLM 成功率',
    value: `${dashboardData.value.llmStats.successRate.toFixed(1)}%`,
    icon: Refresh,
    iconClass: 'rate-icon'
  }
])

const courseStatusCards = computed(() => [
  {
    label: '待审核课程',
    value: dashboardData.value.courseStats.pendingCount,
    accentClass: 'pending-card'
  },
  {
    label: '已通过课程',
    value: dashboardData.value.courseStats.approvedCount,
    accentClass: 'approved-card'
  },
  {
    label: '已驳回课程',
    value: dashboardData.value.courseStats.rejectedCount,
    accentClass: 'rejected-card'
  }
])

const llmStatusCards = computed(() => [
  {
    label: '成功调用',
    value: dashboardData.value.llmStats.successCount,
    accentClass: 'approved-card'
  },
  {
    label: '失败调用',
    value: dashboardData.value.llmStats.failedCount,
    accentClass: 'rejected-card'
  },
  {
    label: '课程介绍与教学目标生成',
    value: dashboardData.value.llmStats.objectiveCount,
    accentClass: 'llm-accent'
  },
  {
    label: '课程大纲生成',
    value: dashboardData.value.llmStats.syllabusCount,
    accentClass: 'course-accent'
  },
  {
    label: '教学讲义生成',
    value: dashboardData.value.llmStats.materialCount,
    accentClass: 'teacher-accent'
  },
  {
    label: '教学课件提纲生成',
    value: dashboardData.value.llmStats.coursewareCount,
    accentClass: 'courseware-accent'
  },
  {
    label: 'Markdown 转换',
    value: dashboardData.value.llmStats.markdownCount,
    accentClass: 'user-accent'
  }
])

const moduleLabelMap: Record<string, string> = {
  objective: '课程介绍与教学目标',
  syllabus: '课程大纲',
  material: '教学讲义',
  courseware: '教学课件提纲',
  markdown_conversion: 'Markdown 转换',
  markdown_conversion_batch: '批量 Markdown 转换'
}

const getModuleLabel = (moduleType: string) => moduleLabelMap[moduleType] ?? moduleType

const getStatusTagType = (status: string) => (status === 'success' ? 'success' : 'danger')
const getStatusLabel = (status: string) => (status === 'success' ? '成功' : '失败')

const formatTime = (value: string | Date) => {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '-'
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

const resolveCourseId = (log: LlmCallLog) => (log.courseId == null ? '-' : log.courseId)

const fetchData = async () => {
  loading.value = true

  try {
    dashboardData.value = await getDashboardData()
  } catch (error) {
    const message = error instanceof Error ? error.message : '获取统计数据失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <section class="statistics-container">
    <div class="title-section">
      <div>
        <h2>系统数据统计</h2>
        <p>管理员可以在这里同时查看用户、课程审核进度以及 LLM 调用情况。</p>
      </div>
      <el-button type="primary" size="small" :loading="loading" @click="fetchData">
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </div>

    <div class="stat-grid">
      <article v-for="card in summaryCards" :key="card.title" class="stat-card">
        <div class="stat-card-inner">
          <div class="stat-icon" :class="card.iconClass">
            <el-icon><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-title">{{ card.title }}</div>
            <div class="stat-value">{{ card.value }}</div>
          </div>
        </div>
      </article>
    </div>

    <section class="section-card">
      <div class="section-header">
        <h3>课程审核概览</h3>
        <span>当前课程主流程状态分布</span>
      </div>
      <div class="status-grid">
        <article
          v-for="card in courseStatusCards"
          :key="card.label"
          class="status-card"
          :class="card.accentClass"
        >
          <div class="status-label">{{ card.label }}</div>
          <div class="status-value">{{ card.value }}</div>
        </article>
      </div>
    </section>

    <section class="section-card">
      <div class="section-header">
        <h3>LLM 调用概览</h3>
        <span>自动记录课程介绍与教学目标、课程大纲、教学讲义、教学课件提纲和 Markdown 转换调用</span>
      </div>
      <div class="status-grid llm-grid">
        <article
          v-for="card in llmStatusCards"
          :key="card.label"
          class="status-card"
          :class="card.accentClass"
        >
          <div class="status-label">{{ card.label }}</div>
          <div class="status-value">{{ card.value }}</div>
        </article>
      </div>
    </section>

    <section class="section-card">
      <div class="section-header">
        <h3>最近调用记录</h3>
        <span>最近 10 条 LLM 调用日志，便于排查失败原因</span>
      </div>

      <el-empty
        v-if="dashboardData.llmStats.recentCalls.length === 0 && !loading"
        description="暂无 LLM 调用记录"
      />

      <el-table
        v-else
        :data="dashboardData.llmStats.recentCalls"
        border
        stripe
        class="log-table"
      >
        <el-table-column prop="createdAt" label="调用时间" min-width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column prop="username" label="调用用户" min-width="120" />

        <el-table-column prop="courseId" label="课程 ID" width="90">
          <template #default="{ row }">
            {{ resolveCourseId(row) }}
          </template>
        </el-table-column>

        <el-table-column prop="moduleType" label="调用模块" min-width="150">
          <template #default="{ row }">
            {{ getModuleLabel(row.moduleType) }}
          </template>
        </el-table-column>

        <el-table-column prop="requestSummary" label="请求摘要" min-width="280" show-overflow-tooltip />

        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="errorMessage" label="失败原因" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.errorMessage || '-' }}
          </template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<style scoped>
.statistics-container {
  padding: 20px;
}

.title-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.title-section h2 {
  margin: 0 0 6px;
  font-size: 28px;
  color: #1f2937;
}

.title-section p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card,
.section-card {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow: 0 16px 40px rgba(31, 38, 135, 0.12);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.stat-card {
  min-height: 120px;
}

.stat-card-inner {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 24px 22px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 16px;
  box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.4);
}

.stat-icon .el-icon {
  font-size: 28px;
  color: #fff;
}

.user-icon {
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
}

.course-icon {
  background: linear-gradient(135deg, #10b981, #34d399);
}

.llm-icon {
  background: linear-gradient(135deg, #f97316, #fb923c);
}

.rate-icon {
  background: linear-gradient(135deg, #8b5cf6, #a78bfa);
}

.stat-info {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #111827;
}

.section-card {
  padding: 20px;
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.section-header span {
  color: #6b7280;
  font-size: 13px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 16px;
}

.llm-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.status-card {
  padding: 18px 20px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 12px 28px rgba(31, 38, 135, 0.08);
}

.status-label {
  font-size: 13px;
  margin-bottom: 8px;
  color: #6b7280;
}

.status-value {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.pending-card {
  border-left: 4px solid #f59e0b;
}

.approved-card {
  border-left: 4px solid #10b981;
}

.rejected-card {
  border-left: 4px solid #ef4444;
}

.llm-accent {
  border-left: 4px solid #8b5cf6;
}

.course-accent {
  border-left: 4px solid #14b8a6;
}

.teacher-accent {
  border-left: 4px solid #f97316;
}

.courseware-accent {
  border-left: 4px solid #6366f1;
}

.user-accent {
  border-left: 4px solid #3b82f6;
}

.log-table {
  width: 100%;
}

@media (max-width: 768px) {
  .title-section,
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .statistics-container {
    padding: 16px;
  }
}
</style>
