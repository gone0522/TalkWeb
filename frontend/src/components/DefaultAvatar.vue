<template>
  <div class="avatar-container" :style="{ width: size + 'px', height: size + 'px' }">
    <img
      v-if="hasCustomAvatar && userId"
      :src="avatarUrl"
      :alt="nickname"
      class="avatar-img"
      @error="onImageError"
    />
    <div
      v-else
      class="default-avatar-icon"
      :style="{ backgroundColor: iconBgColor, fontSize: fontSize + 'px' }"
    >
      {{ displayInitial }}
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { userApi } from '../services/api';

const props = defineProps({
  userId: { type: Number, default: null },
  nickname: { type: String, default: '用戶' },
  hasCustomAvatar: { type: Boolean, default: false },
  iconIndex: { type: Number, default: 1 },
  size: { type: Number, default: 42 }
});

const imageFailed = ref(false);

const avatarUrl = computed(() => {
  if (imageFailed.value || !props.userId) return '';
  return userApi.getAvatarUrl(props.userId);
});

const onImageError = () => {
  imageFailed.value = true;
};

const displayInitial = computed(() => {
  if (!props.nickname) return 'U';
  return props.nickname.trim().charAt(0).toUpperCase();
});

const colors = [
  '#D04A02', '#EB8C00', '#E0301E', '#465362',
  '#2C3E50', '#8E44AD', '#16A085', '#D35400'
];

const iconBgColor = computed(() => {
  const idx = ((props.iconIndex || 1) - 1) % colors.length;
  return colors[Math.max(0, idx)];
});

const fontSize = computed(() => {
  return Math.round(props.size * 0.45);
});
</script>

<style scoped>
.avatar-container {
  border-radius: 50%;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  background-color: #E0E0E0;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-avatar-icon {
  width: 100%;
  height: 100%;
  color: #FFFFFF;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
}
</style>
