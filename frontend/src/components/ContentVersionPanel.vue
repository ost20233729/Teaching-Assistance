<template>
  <div v-if="isVisible" class="history-overlay" @click.self="emit('close')">
    <div class="history-dialog">
      <div class="history-header">
        <div>
          <h3>历史版本</h3>
          <p>查看当前模块的历史保存记录，并恢复到指定版本。</p>
        </div>
        <button class="close-button" @click="emit('close')">×</button>
      </div>

      <div v-if="loading" class="history-state">正在加载历史版本...</div>
      <div v-else-if="errorMessage" class="history-state history-error">{{ errorMessage }}</div>
      <div v-else-if="versions.length === 0" class="history-state">当前还没有历史版本记录。</div>

      <div v-else class="history-body">
        <aside class="history-list">
          <button
            v-for="version in versions"
            :key="version.id"
            class="version-card"
            :class="{ active: selectedVersion?.id === version.id }"
            @click="selectedVersion = version"
          >
            <strong>版本 #{{ version.id }}</strong>
            <span>{{ formatTime(version.createdAt) }}</span>
            <p>{{ version.preview }}</p>
          </button>
        </aside>

        <section class="history-preview">
          <template v-if="selectedVersion">
            <div class="preview-meta">
              <div>
                <strong>版本 #{{ selectedVersion.id }}</strong>
                <span>保存于 {{ formatTime(selectedVersion.createdAt) }}</span>
              </div>
              <button
                class="restore-button"
                :disabled="restoring"
                @click="handleRestore"
              >
                {{ restoring ? '正在恢复...' : '恢复这个版本' }}
              </button>
            </div>
            <pre class="preview-content">{{ selectedVersion.content }}</pre>
          </template>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  getContentVersions,
  restoreContentVersion,
  type ContentVersionModule,
  type ContentVersionRecord,
  type ContentVersionRestoreResponse
} from '@/api/functions'

const props = defineProps<{
  isVisible: boolean
  courseId?: number
  module: ContentVersionModule
}>()

const emit = defineEmits<{
  (event: 'close'): void
  (event: 'restored', payload: ContentVersionRestoreResponse): void
}>()

const loading = ref(false)
const restoring = ref(false)
const errorMessage = ref('')
const versions = ref<ContentVersionRecord[]>([])
const selectedVersion = ref<ContentVersionRecord | null>(null)

const fetchVersions = async () => {
  if (!props.isVisible || !props.courseId) {
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await getContentVersions(props.courseId, props.module)
    versions.value = response
    selectedVersion.value = response[0] || null
  } catch (error) {
    console.error('获取历史版本失败:', error)
    errorMessage.value = error instanceof Error ? error.message : '获取历史版本失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

const handleRestore = async () => {
  if (!props.courseId || !selectedVersion.value) {
    return
  }

  if (!window.confirm('确定恢复到这个历史版本吗？当前编辑区内容会被覆盖。')) {
    return
  }

  restoring.value = true
  errorMessage.value = ''

  try {
    const response = await restoreContentVersion(props.courseId, selectedVersion.value.id)
    emit('restored', response)
    emit('close')
  } catch (error) {
    console.error('恢复历史版本失败:', error)
    errorMessage.value = error instanceof Error ? error.message : '恢复历史版本失败，请稍后重试。'
  } finally {
    restoring.value = false
  }
}

const formatTime = (value: string | Date) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '未知时间'
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

watch(
  () => [props.isVisible, props.courseId, props.module],
  () => {
    if (props.isVisible) {
      void fetchVersions()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.history-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(15, 23, 42, 0.48);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.history-dialog {
  width: min(1100px, 100%);
  max-height: min(760px, calc(100vh - 40px));
  background: rgba(255, 255, 255, 0.96);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.24);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.history-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  padding: 24px 24px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.history-header h3 {
  margin: 0 0 6px;
  color: #0f172a;
  font-size: 24px;
}

.history-header p {
  margin: 0;
  color: #64748b;
}

.close-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: rgba(226, 232, 240, 0.82);
  color: #334155;
  font-size: 24px;
  cursor: pointer;
}

.history-state {
  padding: 32px 24px;
  text-align: center;
  color: #64748b;
}

.history-error {
  color: #b91c1c;
}

.history-body {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  min-height: 0;
  flex: 1;
}

.history-list {
  padding: 20px;
  border-right: 1px solid rgba(226, 232, 240, 0.9);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.version-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  border: 1px solid rgba(203, 213, 225, 0.85);
  border-radius: 16px;
  padding: 14px 16px;
  background: rgba(248, 250, 252, 0.92);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.version-card:hover {
  transform: translateY(-1px);
  border-color: rgba(99, 102, 241, 0.45);
}

.version-card.active {
  border-color: rgba(99, 102, 241, 0.56);
  box-shadow: 0 12px 24px rgba(99, 102, 241, 0.14);
  background: rgba(238, 242, 255, 0.98);
}

.version-card strong,
.version-card span,
.version-card p {
  display: block;
}

.version-card strong {
  color: #0f172a;
}

.version-card span {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.version-card p {
  margin: 10px 0 0;
  color: #334155;
  font-size: 13px;
  line-height: 1.5;
}

.history-preview {
  padding: 20px 24px 24px;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.preview-meta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
}

.preview-meta strong,
.preview-meta span {
  display: block;
}

.preview-meta strong {
  color: #0f172a;
  margin-bottom: 4px;
}

.preview-meta span {
  color: #64748b;
  font-size: 13px;
}

.restore-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 10px 16px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
  font-weight: 600;
  line-height: 1.2;
  cursor: pointer;
  box-shadow: 0 14px 24px rgba(34, 197, 94, 0.18);
}

.restore-button:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.preview-content {
  margin: 0;
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 18px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.96);
  color: #0f172a;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: "Fira Code", "Consolas", monospace;
}

@media (max-width: 960px) {
  .history-dialog {
    max-height: calc(100vh - 24px);
  }

  .history-body {
    grid-template-columns: 1fr;
  }

  .history-list {
    border-right: none;
    border-bottom: 1px solid rgba(226, 232, 240, 0.9);
    max-height: 240px;
  }

  .preview-meta {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
