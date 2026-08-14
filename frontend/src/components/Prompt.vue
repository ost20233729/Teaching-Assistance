<template>
  <div class="prompt-overlay" v-if="isVisible" @click.self="handleClose">
    <div class="prompt-container">
      <div class="prompt-header">
        <h2>{{ title }}</h2>
        <button class="close-button" @click="handleClose">
          <span>×</span>
        </button>
      </div>
      
      <div class="prompt-body">
        <p class="prompt-description">{{ description }}</p>
        <div v-if="templateLoading || templates.length || templateError" class="template-section">
          <div class="template-section-header">
            <span class="template-section-title">模板选择</span>
            <button
              v-if="selectedTemplateId || inputContent"
              type="button"
              class="template-clear-button"
              @click="clearSelectedTemplate"
            >
              清空
            </button>
          </div>

          <p v-if="templateLoading" class="template-tip">正在加载模板...</p>
          <p v-else-if="templateError" class="template-tip template-error">{{ templateError }}</p>

          <div v-if="templates.length" class="template-grid">
            <button
              v-for="template in templates"
              :key="template.id"
              type="button"
              class="template-card"
              :class="{ active: selectedTemplateId === template.id }"
              @click="applyTemplate(template)"
            >
              <span class="template-category">{{ template.category }}</span>
              <strong class="template-name">{{ template.name }}</strong>
              <span class="template-description">{{ template.description }}</span>
            </button>
          </div>

          <p v-if="templates.length" class="template-tip">选择模板后会自动填入下方提示词，你仍可继续修改。</p>
        </div>

        <div class="input-container">
          <textarea 
            v-model="inputContent" 
            :placeholder="placeholder"
            :maxlength="maxLength"
            rows="8"
            @input="handleInput"
          ></textarea>
          <span class="character-count">{{ inputContent.length }} / {{ maxLength }}</span>
        </div>
      </div>
      
      <div class="prompt-footer">
        <button class="cancel-button" @click="handleClose">取消</button>
        <button class="confirm-button" @click="handleConfirm">确认</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits, watch, type PropType } from 'vue';

interface PromptTemplateOption {
  id: string
  category: string
  name: string
  description: string
  prompt: string
}

const props = defineProps({
  title: {
    type: String,
    default: '生成教学大纲'
  },
  description: {
    type: String,
    default: '请输入课程名称或关键词，AI将为您生成教学大纲。'
  },
  placeholder: {
    type: String,
    default: ''
  },
  maxLength: {
    type: Number,
    default: 1000
  },
  isVisible: {
    type: Boolean,
    default: false
  },
  templates: {
    type: Array as PropType<PromptTemplateOption[]>,
    default: () => []
  },
  templateLoading: {
    type: Boolean,
    default: false
  },
  templateError: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['close', 'confirm', 'update:content']);

const inputContent = ref('');
const selectedTemplateId = ref('');

// 监听isVisible属性变化，当显示时重置输入内容
watch(() => props.isVisible, (newValue) => {
  if (newValue === true) {
    inputContent.value = '';
    selectedTemplateId.value = '';
  }
}, { immediate: false });

const handleInput = () => {
  emit('update:content', inputContent.value);
};

const applyTemplate = (template: PromptTemplateOption) => {
  selectedTemplateId.value = template.id;
  inputContent.value = template.prompt;
  emit('update:content', inputContent.value);
};

const clearSelectedTemplate = () => {
  selectedTemplateId.value = '';
  inputContent.value = '';
  emit('update:content', inputContent.value);
};

const handleClose = () => {
  emit('close');
};

const handleConfirm = () => {
  if (inputContent.value.trim()) {
    emit('confirm', inputContent.value);
  }
};

// 添加默认导出
defineExpose({
  handleClose,
  handleConfirm
});
</script>

<style scoped>
.prompt-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.prompt-container {
  width: 90%;
  max-width: 600px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.18);
  overflow: hidden;
  padding: 1.5rem;
}

.prompt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.prompt-header h2 {
  font-size: 1.5rem;
  margin: 0;
  font-weight: 600;
  color: #333;
}

.close-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: none;
  border: none;
  border-radius: 50%;
  font-size: 1.5rem;
  cursor: pointer;
  color: #666;
  padding: 0;
  line-height: 1;
}

.prompt-body {
  margin-bottom: 1.5rem;
}

.prompt-description {
  margin: 0 0 1rem 0;
  color: #555;
}

.template-section {
  margin-bottom: 1rem;
}

.template-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.template-section-title {
  font-weight: 600;
  color: #334155;
}

.template-clear-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: rgba(15, 23, 42, 0.06);
  color: #475569;
  border-radius: 999px;
  min-height: 32px;
  padding: 0.35rem 0.8rem;
  cursor: pointer;
  font-size: 0.85rem;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.template-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.35rem;
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(255, 255, 255, 0.72);
  border-radius: 10px;
  padding: 0.85rem;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s ease;
}

.template-card:hover {
  border-color: rgba(59, 130, 246, 0.55);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.12);
}

.template-card.active {
  border-color: #3b82f6;
  background: rgba(219, 234, 254, 0.82);
}

.template-category {
  display: inline-flex;
  align-items: center;
  padding: 0.15rem 0.55rem;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-size: 0.75rem;
  font-weight: 600;
}

.template-name {
  color: #0f172a;
  font-size: 0.95rem;
}

.template-description {
  color: #64748b;
  font-size: 0.82rem;
  line-height: 1.45;
}

.template-tip {
  margin: 0 0 0.75rem 0;
  color: #64748b;
  font-size: 0.85rem;
}

.template-error {
  color: #dc2626;
}

.input-container {
  position: relative;
}

textarea {
  width: 100%;
  padding: 0.75rem;
  font-size: 1rem;
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: rgba(255, 255, 255, 0.5);
  resize: none;
  outline: none;
  transition: border-color 0.2s ease;
}

textarea:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}

.character-count {
  position: absolute;
  bottom: 0.5rem;
  right: 0.75rem;
  font-size: 0.8rem;
  color: #999;
}

.prompt-footer {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 1rem;
}

.cancel-button, .confirm-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 1.5rem;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  line-height: 1.2;
  transition: all 0.2s ease;
}

.cancel-button {
  background: transparent;
  border: 1px solid rgba(0, 0, 0, 0.2);
  color: #555;
}

.cancel-button:hover {
  background: rgba(0, 0, 0, 0.05);
}

.confirm-button {
  background: #6366f1;
  color: white;
  border: none;
}

.confirm-button:hover {
  background: #4f46e5;
}

@media (max-width: 640px) {
  .prompt-container {
    width: calc(100% - 24px);
    padding: 1rem;
  }

  .template-grid {
    grid-template-columns: 1fr;
  }
}
</style>
