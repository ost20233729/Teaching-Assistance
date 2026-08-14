<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, restrictionOptions, type Restriction, type User } from '@/api/admin'

const loadingUsers = ref(false)
const loadingRestrictions = ref(false)
const selectedUserId = ref<number | null>(null)
const users = ref<User[]>([])
const restrictions = ref<Restriction[]>([])
const keyword = ref('')
const savingFunctionName = ref<string | null>(null)
const removingRestrictionId = ref<number | null>(null)

const filteredUsers = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  if (!search) {
    return users.value
  }

  return users.value.filter((user) => {
    return user.username.toLowerCase().includes(search) || user.email.toLowerCase().includes(search)
  })
})

const selectedUser = computed(() => {
  return users.value.find((user) => user.id === selectedUserId.value) || null
})

const assignedFunctionNames = computed(() => {
  return new Set(restrictions.value.map((item) => item.functionName))
})

const restrictionCards = computed(() => {
  return restrictionOptions.map((option) => {
    const restriction = restrictions.value.find((item) => item.functionName === option.value)
    return {
      ...option,
      assigned: assignedFunctionNames.value.has(option.value),
      restrictionId: restriction?.id
    }
  })
})

const formatTime = (value?: string | Date) => {
  if (!value) {
    return '暂无时间'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '暂无时间'
  }

  return date.toLocaleString('zh-CN', { hour12: false })
}

const getRestrictionLabel = (functionName: string) => {
  return restrictionOptions.find((option) => option.value === functionName)?.label || functionName
}

const fetchUsers = async () => {
  loadingUsers.value = true

  try {
    const teacherList = await adminApi.getUserList()
    users.value = teacherList.sort((left, right) => (left.id || 0) - (right.id || 0))

    if (!selectedUserId.value && users.value.length > 0) {
      selectedUserId.value = users.value[0].id || null
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : '获取教师列表失败'
    ElMessage.error(message)
  } finally {
    loadingUsers.value = false
  }
}

const fetchRestrictions = async (userId: number) => {
  loadingRestrictions.value = true

  try {
    restrictions.value = await adminApi.getUserRestrictions(userId)
  } catch (error) {
    restrictions.value = []
    const message = error instanceof Error ? error.message : '获取功能限制失败'
    ElMessage.error(message)
  } finally {
    loadingRestrictions.value = false
  }
}

const addRestriction = async (functionName: string) => {
  if (!selectedUserId.value) {
    ElMessage.warning('请先选择教师')
    return
  }

  savingFunctionName.value = functionName

  try {
    await adminApi.addRestriction({
      userId: selectedUserId.value,
      functionName
    })
    await fetchRestrictions(selectedUserId.value)
    ElMessage.success('功能限制已添加')
  } catch (error) {
    const message = error instanceof Error ? error.message : '添加功能限制失败'
    ElMessage.error(message)
  } finally {
    savingFunctionName.value = null
  }
}

const removeRestriction = async (restriction: Restriction) => {
  if (!restriction.id || !selectedUserId.value) {
    ElMessage.warning('限制记录不存在，无法删除')
    return
  }

  removingRestrictionId.value = restriction.id

  try {
    await adminApi.removeRestriction(selectedUserId.value, restriction.id)
    await fetchRestrictions(selectedUserId.value)
    ElMessage.success('功能限制已移除')
  } catch (error) {
    const message = error instanceof Error ? error.message : '移除功能限制失败'
    ElMessage.error(message)
  } finally {
    removingRestrictionId.value = null
  }
}

const removeRestrictionByFunctionName = async (functionName: string) => {
  const restriction = restrictions.value.find((item) => item.functionName === functionName)
  if (!restriction) {
    ElMessage.warning('未找到对应的限制记录')
    return
  }

  await removeRestriction(restriction)
}

watch(selectedUserId, (userId) => {
  if (userId) {
    void fetchRestrictions(userId)
  } else {
    restrictions.value = []
  }
})

onMounted(fetchUsers)
</script>

<template>
  <section class="panel-shell">
    <div class="panel-header">
      <div>
        <h2>功能限制</h2>
        <p>按教师维度配置可用功能，沿用现有四个教学模块。</p>
      </div>
    </div>

    <div class="restriction-layout">
      <aside class="teacher-panel">
        <div class="teacher-panel-header">
          <h3>教师列表</h3>
          <span>{{ users.length }} 人</span>
        </div>

        <input
          v-model="keyword"
          type="text"
          class="search-input"
          placeholder="搜索教师姓名或邮箱"
        />

        <div v-if="loadingUsers" class="empty-state">正在加载教师列表...</div>

        <div v-else-if="filteredUsers.length === 0" class="empty-state">没有匹配的教师。</div>

        <div v-else class="teacher-list">
          <button
            v-for="user in filteredUsers"
            :key="user.id"
            class="teacher-card"
            :class="{ active: selectedUserId === user.id }"
            @click="selectedUserId = user.id || null"
          >
            <div class="teacher-name-row">
              <strong>{{ user.username }}</strong>
              <span>#{{ user.id }}</span>
            </div>
            <div class="teacher-email">{{ user.email }}</div>
          </button>
        </div>
      </aside>

      <div class="restriction-panel">
        <div v-if="selectedUser" class="selected-user-card">
          <div>
            <div class="selected-user-label">当前教师</div>
            <h3>{{ selectedUser.username }}</h3>
            <p>{{ selectedUser.email }}</p>
          </div>
          <div class="selected-user-meta">
            <span>已限制 {{ restrictions.length }} 项功能</span>
          </div>
        </div>

        <div v-if="loadingRestrictions" class="empty-state">
          正在加载功能限制...
        </div>

        <template v-else-if="selectedUser">
          <div class="restriction-grid">
            <article
              v-for="item in restrictionCards"
              :key="item.value"
              class="restriction-card"
              :class="{ assigned: item.assigned }"
            >
              <div class="restriction-card-top">
                <span class="restriction-status" :class="{ assigned: item.assigned }">
                  {{ item.assigned ? '已限制' : '可用' }}
                </span>
              </div>

              <h4>{{ item.label }}</h4>
              <p>{{ item.description }}</p>

              <button
                v-if="!item.assigned"
                class="action-button add-button"
                :disabled="savingFunctionName === item.value"
                @click="addRestriction(item.value)"
              >
                {{ savingFunctionName === item.value ? '处理中...' : '添加限制' }}
              </button>

              <button
                v-else
                class="action-button remove-button"
                :disabled="removingRestrictionId === item.restrictionId"
                @click="removeRestrictionByFunctionName(item.value)"
              >
                {{ removingRestrictionId === item.restrictionId ? '处理中...' : '解除限制' }}
              </button>
            </article>
          </div>

          <div class="restriction-records">
            <div class="records-header">
              <h3>当前限制记录</h3>
            </div>

            <div v-if="restrictions.length === 0" class="empty-state compact">
              该教师当前没有限制项。
            </div>

            <div v-else class="record-list">
              <article v-for="restriction in restrictions" :key="restriction.id" class="record-card">
                <div>
                  <strong>{{ getRestrictionLabel(restriction.functionName) }}</strong>
                  <p>创建时间：{{ formatTime(restriction.createdAt) }}</p>
                </div>
                <button
                  class="record-remove"
                  :disabled="removingRestrictionId === restriction.id"
                  @click="removeRestriction(restriction)"
                >
                  {{ removingRestrictionId === restriction.id ? '处理中...' : '移除' }}
                </button>
              </article>
            </div>
          </div>
        </template>

        <div v-else class="empty-state">
          请先从左侧选择教师，再配置功能限制。
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.panel-shell {
  margin: 0 20px 20px;
  padding: 24px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(255, 255, 255, 0.42);
  box-shadow: 0 20px 48px rgba(31, 38, 135, 0.12);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.panel-header {
  margin-bottom: 20px;
}

.panel-header h2 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #111827;
}

.panel-header p {
  margin: 0;
  color: #6b7280;
}

.restriction-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
}

.teacher-panel,
.restriction-panel {
  min-width: 0;
}

.teacher-panel {
  padding: 18px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.8);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.teacher-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.teacher-panel-header h3,
.records-header h3 {
  margin: 0;
  color: #0f172a;
}

.teacher-panel-header span {
  color: #64748b;
  font-size: 13px;
}

.search-input {
  width: 100%;
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.8);
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.search-input:focus {
  border-color: rgba(59, 130, 246, 0.5);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12);
}

.teacher-list {
  display: grid;
  gap: 12px;
  max-height: 540px;
  overflow-y: auto;
}

.teacher-card {
  width: 100%;
  padding: 14px 16px;
  text-align: left;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: rgba(255, 255, 255, 0.88);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.teacher-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(31, 38, 135, 0.08);
}

.teacher-card.active {
  border-color: rgba(59, 130, 246, 0.45);
  box-shadow: 0 14px 24px rgba(59, 130, 246, 0.12);
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.9), rgba(239, 246, 255, 0.92));
}

.teacher-name-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: #0f172a;
}

.teacher-name-row span,
.teacher-email,
.selected-user-card p,
.record-card p {
  color: #64748b;
  margin: 0;
  font-size: 13px;
}

.selected-user-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  margin-bottom: 18px;
  padding: 20px 22px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.92), rgba(224, 242, 254, 0.9));
  border: 1px solid rgba(186, 230, 253, 0.8);
}

.selected-user-label {
  font-size: 13px;
  color: #1d4ed8;
  margin-bottom: 6px;
}

.selected-user-card h3 {
  margin: 0 0 6px;
  color: #0f172a;
}

.selected-user-meta span {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  color: #1e40af;
  font-size: 13px;
}

.restriction-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.restriction-card {
  padding: 18px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.88);
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 14px 28px rgba(31, 38, 135, 0.08);
}

.restriction-card.assigned {
  background: linear-gradient(135deg, rgba(254, 242, 242, 0.95), rgba(255, 247, 237, 0.95));
  border-color: rgba(252, 165, 165, 0.7);
}

.restriction-card-top {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.restriction-status {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(226, 232, 240, 0.75);
  color: #475569;
}

.restriction-status.assigned {
  background: rgba(239, 68, 68, 0.14);
  color: #b91c1c;
}

.restriction-card h4 {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 18px;
}

.restriction-card p {
  margin: 0 0 16px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.action-button,
.record-remove {
  border: none;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.action-button:hover,
.record-remove:hover {
  transform: translateY(-1px);
}

.action-button:disabled,
.record-remove:disabled {
  cursor: not-allowed;
  opacity: 0.75;
}

.action-button {
  width: 100%;
  padding: 12px 14px;
  border-radius: 14px;
  color: #fff;
  font-weight: 600;
}

.add-button {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  box-shadow: 0 12px 24px rgba(59, 130, 246, 0.18);
}

.remove-button,
.record-remove {
  background: linear-gradient(135deg, #fb7185, #ef4444);
  box-shadow: 0 12px 24px rgba(239, 68, 68, 0.18);
}

.restriction-records {
  padding: 18px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.8);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.records-header {
  margin-bottom: 14px;
}

.record-list {
  display: grid;
  gap: 12px;
}

.record-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.record-card strong {
  display: block;
  margin-bottom: 6px;
  color: #0f172a;
}

.record-remove {
  padding: 10px 14px;
  border-radius: 12px;
  color: #fff;
  white-space: nowrap;
}

.empty-state {
  padding: 28px 18px;
  text-align: center;
  color: #64748b;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.68);
}

.empty-state.compact {
  padding: 18px;
}

@media (max-width: 1080px) {
  .restriction-layout {
    grid-template-columns: 1fr;
  }

  .selected-user-card,
  .record-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .record-remove {
    width: 100%;
  }
}
</style>
