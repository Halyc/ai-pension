<template>
  <div class="image-upload">
    <el-upload
      class="uploader"
      :show-file-list="false"
      :action="uploadAction"
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      :accept="accept"
      :disabled="disabled"
    >
      <div v-if="modelValue" class="preview">
        <el-image
          :src="modelValue"
          :fit="fit"
          :style="imgStyle"
          :preview-src-list="[modelValue]"
          preview-teleported
        />
        <div v-if="!disabled" class="preview-overlay">
          <el-icon class="overlay-icon" :size="20" @click.stop="triggerUpload"><Upload /></el-icon>
          <el-icon class="overlay-icon" :size="20" @click.stop="clearImage"><Delete /></el-icon>
        </div>
      </div>
      <div v-else class="placeholder" :style="imgStyle">
        <el-icon :size="32" color="#909399"><Plus /></el-icon>
        <div class="placeholder-text">{{ placeholder }}</div>
      </div>
    </el-upload>
    <div v-if="tip" class="upload-tip">{{ tip }}</div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Delete, Plus } from '@element-plus/icons-vue'
import { uploadApi } from '@/api/upload'

const props = defineProps({
  modelValue: { type: String, default: '' },
  accept: { type: String, default: 'image/jpeg,image/png,image/webp,image/gif' },
  maxSize: { type: Number, default: 5 },
  width: { type: [Number, String], default: 120 },
  height: { type: [Number, String], default: 90 },
  fit: { type: String, default: 'cover' },
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '点击上传图片' },
  tip: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const loading = ref(false)
const uploadAction = '#'

const imgStyle = computed(() => ({
  width: typeof props.width === 'number' ? props.width + 'px' : props.width,
  height: typeof props.height === 'number' ? props.height + 'px' : props.height
}))

function triggerUpload() {
  const input = document.querySelector('.image-upload .uploader input[type="file"]')
  input?.click()
}

function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLtMax = file.size / 1024 / 1024 < props.maxSize
  if (!isLtMax) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

async function handleUpload({ file }) {
  loading.value = true
  try {
    const res = await uploadApi.uploadImage(file)
    if (res.code === 200 && res.data?.url) {
      emit('update:modelValue', res.data.url)
      ElMessage.success('上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败，请重试')
  } finally {
    loading.value = false
  }
}

function clearImage() {
  emit('update:modelValue', '')
}
</script>

<style scoped>
.image-upload {
  display: inline-block;
}
.uploader {
  display: inline-block;
  cursor: pointer;
}
.uploader :deep(.el-upload) {
  border: none;
  padding: 0;
}
.preview {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
}
.preview .el-image {
  display: block;
}
.preview-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  opacity: 0;
  transition: opacity 0.2s ease;
  color: #fff;
}
.preview:hover .preview-overlay {
  opacity: 1;
}
.overlay-icon {
  cursor: pointer;
  transition: transform 0.15s ease;
}
.overlay-icon:hover {
  transform: scale(1.2);
  color: #f6a23b;
}
.placeholder {
  border: 1.5px dashed var(--el-border-color);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.03);
  transition: border-color 0.2s ease, background 0.2s ease;
}
.uploader:hover .placeholder {
  border-color: var(--el-color-primary);
  background: rgba(102, 126, 234, 0.08);
}
.placeholder-text {
  font-size: 12px;
  color: var(--text-muted);
}
.upload-tip {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}
</style>