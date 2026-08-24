<template>
  <a
    :href="preview.url"
    target="_blank"
    rel="noopener noreferrer"
    class="url-preview-card"
  >
    <div v-if="preview.imageUrl" class="preview-thumbnail">
      <img :src="preview.imageUrl" :alt="preview.title" @error="hideImage = true" v-if="!hideImage" />
    </div>
    <div class="preview-info">
      <div class="preview-domain">{{ preview.domain }}</div>
      <div class="preview-title">{{ preview.title || preview.url }}</div>
      <div v-if="preview.description" class="preview-desc">{{ preview.description }}</div>
    </div>
  </a>
</template>

<script setup>
import { ref } from 'vue';

defineProps({
  preview: {
    type: Object,
    required: true
  }
});

const hideImage = ref(false);
</script>

<style scoped>
.url-preview-card {
  display: flex;
  flex-direction: column;
  margin-top: 6px;
  background-color: #FFFFFF;
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  max-width: 320px;
}

.url-preview-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.preview-thumbnail {
  width: 100%;
  height: 140px;
  overflow: hidden;
  background-color: #F5F5F5;
}

.preview-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-info {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.preview-domain {
  font-size: 11px;
  color: #8E8E93;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.preview-title {
  font-size: 13px;
  font-weight: 600;
  color: #1E1E1E;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.35;
}

.preview-desc {
  font-size: 11px;
  color: #666666;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.3;
}
</style>
