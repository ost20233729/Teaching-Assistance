<template>
  <div class="teaching-lecture-container">
    <div class="header">
      <button class="back-button" @click="$emit('back')">←</button>
      <h3 class="section-title">教学讲义</h3>
      <div class="header-right">
        <button class="ai-btn" @click="showPrompt = true" :disabled="isGenerating || isLoading || isSaving">
          <span class="ai-icon">✨</span>
          AI生成
        </button>
        <button class="btn btn-secondary" @click="showVersionHistory = true" :disabled="isGenerating || isLoading || isSaving">
          历史版本
        </button>
        <!-- <button class="btn btn-secondary" @click="handleSaveDraft">暂存</button> -->
        <button class="btn btn-primary" @click="handleSave" :disabled="isGenerating || isLoading || isSaving">
          保存
        </button>
      </div>
    </div>

    <div v-if="isLoading" class="status-message loading">
      <div class="spinner"></div>
      <span>{{ loadingMessage }}</span>
    </div>

    <div v-if="isGenerating" class="status-message generating">
      <div class="spinner"></div>
      <span>{{ generatingStatus }}</span>
    </div>

    <div v-if="isSaving" class="status-message saving">
      <div class="spinner"></div>
      <span>正在保存...</span>
    </div>

    <div v-if="showSuccessMessage" class="status-message success">
      <span class="success-icon">✓</span>
      <span>{{ successMessage }}</span>
    </div>

    <div v-if="error" class="status-message error">
      <span class="error-icon">❌</span>
      <span>{{ error }}</span>
    </div>

    <div class="content-container">
      <div class="catalog-panel" :class="{ collapsed: !catalogExpanded }">
        <Catalog
          :content="markdownContent"
          :active-heading="activeHeading"
          @navigate="scrollToHeading"
          @toggle="handleCatalogToggle"
        />
      </div>

      <div class="editor-panel" :class="{ expanded: !catalogExpanded }">
        <Markdown
          ref="markdownEditor"
          :initial-value="markdownContent"
          :height="editorHeight"
          preview-style="tab"
          @update:content="updateContent"
        />
      </div>
    </div>

    <Prompt
      :is-visible="showPrompt"
      title="AI生成讲义内容"
      description="请输入您想让 AI 生成的讲义内容描述或指令"
      placeholder="例如：生成一份关于分布式系统 CAP 理论的教学讲义，包含概念解释、案例分析和课堂讨论问题"
      :templates="promptTemplates"
      :template-loading="promptTemplateLoading"
      :template-error="promptTemplateError"
      @close="showPrompt = false"
      @confirm="handlePromptConfirm"
    />

    <ContentVersionPanel
      :is-visible="showVersionHistory"
      :course-id="props.courseId"
      module="material"
      @close="showVersionHistory = false"
      @restored="handleVersionRestored"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import Catalog from './Catalog.vue';
import ContentVersionPanel from './ContentVersionPanel.vue';
import Markdown from './markdown.vue';
import Prompt from './Prompt.vue';
import {
  generateCourseMaterial,
  getCourseMaterial,
  getPromptTemplates,
  saveCourseMaterial,
  type ContentVersionRestoreResponse,
  type PromptTemplate
} from '../api/functions';

interface LectureUnit {
  unit_number: string;
  unit_title: string;
  lecture_content: string;
  ideological_target?: string;
  time_allocation?: string;
}

interface MaterialResponse {
  content?: string;
  units?: LectureUnit[];
  message?: string;
  status?: string;
}

interface MarkdownInstance {
  setMarkdown: (content: string) => void;
  editor: () => any;
}

const props = defineProps({
  courseId: {
    type: Number,
    required: false,
    default: undefined
  }
});

const emit = defineEmits<{
  (event: 'back'): void;
  (event: 'save'): void;
  (event: 'save-draft', payload: string): void;
  (event: 'module-status-change', payload: { moduleId: 'lecture'; status: 'generated_unsaved' | 'completed' }): void;
}>();

const isLoading = ref(false);
const loadingMessage = ref('正在加载讲义内容...');
const isGenerating = ref(false);
const generatingStatus = ref('正在生成讲义内容...');
const isSaving = ref(false);
const showSuccessMessage = ref(false);
const successMessage = ref('');
const error = ref('');

const showPrompt = ref(false);
const promptTemplates = ref<PromptTemplate[]>([]);
const promptTemplateLoading = ref(false);
const promptTemplateError = ref('');
const showVersionHistory = ref(false);

const catalogExpanded = ref(true);
const markdownContent = ref('');
const activeHeading = ref('');
const markdownEditor = ref<MarkdownInstance | null>(null);
const editorHeight = ref('calc(100vh - 200px)');

const handleCatalogToggle = (expanded: boolean) => {
  catalogExpanded.value = expanded;
};

const updateEditorHeight = () => {
  const calculatedHeight = Math.min(window.innerHeight - 200, 1800);
  editorHeight.value = `${calculatedHeight}px`;
};

const applyMarkdownContent = (content: string) => {
  markdownContent.value = content;

  if (markdownEditor.value?.setMarkdown) {
    setTimeout(() => {
      markdownEditor.value?.setMarkdown(content);
    }, 100);
  }
};

const buildMaterialContent = (response: MaterialResponse) => {
  if (response.content) {
    return response.content;
  }

  if (!response.units?.length) {
    return '';
  }

  return response.units
    .map((unit) => `# 单元${unit.unit_number}: ${unit.unit_title}\n\n${unit.lecture_content}`)
    .join('\n\n---\n\n');
};

const fetchPromptTemplates = async () => {
  promptTemplateLoading.value = true;
  promptTemplateError.value = '';

  try {
    promptTemplates.value = await getPromptTemplates('material');
  } catch (templateError) {
    console.error('获取讲义模板失败:', templateError);
    promptTemplateError.value = '模板加载失败，仍可手动输入提示词。';
  } finally {
    promptTemplateLoading.value = false;
  }
};

const fetchCourseMaterial = async () => {
  if (!props.courseId || Number.isNaN(props.courseId)) {
    error.value = '课程 ID 无效，无法获取讲义内容';
    return;
  }

  isLoading.value = true;
  loadingMessage.value = '正在加载讲义内容...';
  error.value = '';

  try {
    const response: MaterialResponse = await getCourseMaterial(props.courseId);
    applyMarkdownContent(buildMaterialContent(response));
  } catch (fetchError) {
    console.error('获取课程讲义失败:', fetchError);
    error.value = '获取课程讲义失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
};

const handlePromptConfirm = async (content: string) => {
  if (!props.courseId || Number.isNaN(props.courseId)) {
    error.value = '课程 ID 无效，无法生成讲义';
    showPrompt.value = false;
    return;
  }

  if (!content.trim()) {
    error.value = '请输入有效的描述内容';
    return;
  }

  showPrompt.value = false;
  isGenerating.value = true;
  generatingStatus.value = '正在生成讲义内容...';
  error.value = '';

  try {
    const response: MaterialResponse = await generateCourseMaterial(props.courseId, '课程讲义', content);
    const generatedContent = buildMaterialContent(response);

    if (!generatedContent) {
      error.value = '生成结果为空，请尝试提供更具体的提示词';
      return;
    }

    applyMarkdownContent(generatedContent);
    emit('module-status-change', { moduleId: 'lecture', status: 'generated_unsaved' });
    showSuccessMessage.value = true;
    successMessage.value = '生成成功！';
    setTimeout(() => {
      showSuccessMessage.value = false;
    }, 3000);
  } catch (generateError) {
    console.error('生成讲义失败:', generateError);
    error.value = '生成讲义失败，请稍后重试';
  } finally {
    isGenerating.value = false;
  }
};

const updateContent = (content: string) => {
  markdownContent.value = content;
};

const scrollToHeading = (anchor: string) => {
  activeHeading.value = anchor;

  const editor = markdownEditor.value?.editor?.();
  if (!editor) {
    return;
  }

  const lines = markdownContent.value.split('\n');
  const anchorText = anchor.replace(/-/g, ' ');
  const targetTextRegex = new RegExp(`^(#+)\\s+${anchorText}`, 'i');
  const lineNumber = lines.findIndex((line) => targetTextRegex.test(line) || line.toLowerCase().includes(anchorText));

  if (lineNumber < 0) {
    return;
  }

  if (editor.isWysiwygMode?.()) {
    try {
      const wysiwygEl = editor.getEditorElements?.().wysiwyg;
      const headers = wysiwygEl?.querySelectorAll?.('h1, h2, h3, h4, h5, h6') ?? [];
      for (const header of headers) {
        if (header.textContent?.toLowerCase().includes(anchorText)) {
          header.scrollIntoView({ behavior: 'smooth' });
          break;
        }
      }
    } catch (scrollError) {
      console.error('所见即所得模式滚动失败:', scrollError);
    }
    return;
  }

  try {
    editor.setScrollTop(lineNumber * 21);
    setTimeout(() => {
      try {
        editor.setSelection(
          { line: lineNumber, ch: 0 },
          { line: lineNumber, ch: lines[lineNumber]?.length ?? 0 }
        );
      } catch (selectionError) {
        console.error('设置讲义编辑器选区失败:', selectionError);
      }
    }, 100);
  } catch (scrollError) {
    console.error('Markdown 模式滚动失败:', scrollError);
  }
};

const handleSaveDraft = () => {
  emit('save-draft', markdownContent.value);
};

const handleVersionRestored = (response: ContentVersionRestoreResponse) => {
  applyMarkdownContent(response.data.content || '');
  showSuccessMessage.value = true;
  successMessage.value = '已恢复到所选历史版本';
  setTimeout(() => {
    showSuccessMessage.value = false;
  }, 3000);
};

const handleSave = async () => {
  if (!props.courseId || Number.isNaN(props.courseId)) {
    error.value = '课程 ID 无效，无法保存讲义';
    return;
  }

  isSaving.value = true;
  error.value = '';

  try {
    await saveCourseMaterial(props.courseId, {
      content: markdownContent.value
    });

    emit('module-status-change', { moduleId: 'lecture', status: 'completed' });
    showSuccessMessage.value = true;
    successMessage.value = '保存成功！';
    setTimeout(() => {
      showSuccessMessage.value = false;
    }, 3000);
  } catch (saveError) {
    console.error('保存讲义失败:', saveError);
    error.value = '保存讲义失败，请稍后重试';
  } finally {
    isSaving.value = false;
  }
};

watch(
  () => props.courseId,
  (courseId) => {
    if (!courseId) {
      return;
    }
    void fetchCourseMaterial();
  }
);

onMounted(() => {
  updateEditorHeight();
  window.addEventListener('resize', updateEditorHeight);
  void fetchPromptTemplates();
  void fetchCourseMaterial();
});

onUnmounted(() => {
  window.removeEventListener('resize', updateEditorHeight);
});
</script>

<style scoped>
.teaching-lecture-container {
  max-width: 1500px;
  margin: 0 auto;
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
  margin-left: auto;
}

.back-button {
  background-color: transparent;
  border: none;
  color: #2196f3;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
  transition: background-color 0.2s;
}

.back-button:hover {
  background-color: rgba(33, 150, 243, 0.1);
}

.section-title {
  font-size: 24px;
  font-weight: 500;
  color: #333;
  margin: 0;
  text-align: center;
  flex-grow: 1;
  min-width: 0;
}

.status-message {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 15px;
  border-radius: 4px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.loading {
  background-color: rgba(33, 150, 243, 0.1);
}

.generating {
  background-color: rgba(255, 193, 7, 0.1);
}

.saving {
  background-color: rgba(76, 175, 80, 0.1);
}

.success {
  background-color: rgba(76, 175, 80, 0.2);
  animation: fadeOut 3s forwards;
}

.error {
  background-color: rgba(244, 67, 54, 0.1);
}

.success-icon {
  color: #4caf50;
  font-weight: bold;
  font-size: 18px;
}

.error-icon {
  color: #f44336;
  font-weight: bold;
  font-size: 18px;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 3px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  border-top-color: #2196f3;
  animation: spin 1s ease-in-out infinite;
}

@keyframes fadeOut {
  0% {
    opacity: 1;
  }

  70% {
    opacity: 1;
  }

  100% {
    opacity: 0;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.content-container {
  display: flex;
  gap: 20px;
  height: calc(100vh - 120px);
  overflow: hidden;
}

.catalog-panel {
  width: 300px;
  flex-shrink: 0;
  overflow-y: auto;
  transition: width 0.3s ease;
}

.catalog-panel.collapsed {
  width: 40px;
}

.editor-panel {
  flex-grow: 1;
  overflow-y: auto;
  transition: width 0.3s ease;
}

.editor-panel.expanded {
  width: calc(100% - 60px);
}

.ai-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background-color: rgba(76, 175, 80, 0.7);
  color: white;
  border: none;
  border-radius: 12px;
  min-height: 40px;
  padding: 0 18px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.2;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.ai-btn:hover {
  background-color: rgba(76, 175, 80, 0.85);
}

.ai-btn:disabled,
.btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.ai-icon {
  margin-right: 0;
  font-size: 14px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 18px;
  border-radius: 12px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.2;
  cursor: pointer;
  transition: all 0.3s;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.btn-primary {
  background-color: rgba(76, 175, 80, 0.7);
  color: white;
}

.btn-primary:hover {
  background-color: rgba(76, 175, 80, 0.85);
}

.btn-secondary {
  background-color: rgba(245, 245, 245, 0.7);
  color: #333;
}

.btn-secondary:hover {
  background-color: rgba(245, 245, 245, 0.85);
}

.btn:hover {
  opacity: 0.9;
}

@media (max-width: 768px) {
  .content-container {
    flex-direction: column;
  }

  .catalog-panel {
    width: 100%;
    height: auto;
    max-height: 300px;
  }
}
</style>
