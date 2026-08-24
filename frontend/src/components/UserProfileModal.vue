<template>
  <div class="modal-backdrop" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>個人檔案與設定</h3>
        <button class="modal-close" @click="$emit('close')">✕</button>
      </div>

      <div class="modal-body">
        <!-- Avatar Section -->
        <div class="avatar-edit-section">
          <div class="avatar-preview-wrap">
            <DefaultAvatar
              :user-id="authStore.user?.id"
              :nickname="nickname || authStore.user?.nickname"
              :has-custom-avatar="authStore.user?.hasCustomAvatar"
              :icon-index="selectedIcon"
              :size="80"
            />
            <label class="avatar-upload-btn" title="上傳照片">
              📷
              <input
                type="file"
                accept="image/png, image/jpeg, image/webp"
                style="display: none"
                @change="handleAvatarFileChange"
              />
            </label>
          </div>

          <div class="avatar-tips">
            支援 JPG / PNG / WebP（系統會自動裁切並最佳化壓縮後上傳）
          </div>
        </div>

        <!-- Default Avatar Color Preset Picker -->
        <div class="form-group">
          <label>選擇預設代表色</label>
          <div class="color-preset-grid">
            <div
              v-for="idx in 8"
              :key="idx"
              :class="['color-preset-item', { active: selectedIcon === idx }]"
              @click="selectedIcon = idx"
            >
              <DefaultAvatar
                :nickname="nickname || 'U'"
                :has-custom-avatar="false"
                :icon-index="idx"
                :size="32"
              />
            </div>
          </div>
        </div>

        <!-- Nickname Input -->
        <div class="form-group">
          <label>暱稱</label>
          <input
            v-model="nickname"
            type="text"
            class="input-control"
            placeholder="請輸入顯示暱稱"
            maxlength="50"
            required
          />
        </div>

        <div class="form-group">
          <label>登入帳號</label>
          <input
            :value="authStore.user?.username"
            type="text"
            class="input-control"
            disabled
          />
        </div>

        <!-- Password Change Action -->
        <div class="security-action-row">
          <span>登入密碼</span>
          <button
            type="button"
            class="btn btn-secondary"
            style="padding: 4px 12px; font-size: 12px;"
            @click="$emit('openChangePassword')"
          >
            修改密碼
          </button>
        </div>

        <div v-if="statusMessage" :class="isSuccess ? 'success-text' : 'error-text'">
          {{ statusMessage }}
        </div>
      </div>

      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" @click="$emit('close')">
          取消
        </button>
        <button
          type="button"
          class="btn btn-primary"
          :disabled="saving || !nickname.trim()"
          @click="saveProfile"
        >
          {{ saving ? '儲存中...' : '儲存變更' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { userApi } from '../services/api';
import { useAuthStore } from '../stores/authStore';
import DefaultAvatar from './DefaultAvatar.vue';

const emit = defineEmits(['close', 'openChangePassword']);

const authStore = useAuthStore();

const nickname = ref(authStore.user?.nickname || '');
const selectedIcon = ref(authStore.user?.avatarDefaultIcon || 1);
const saving = ref(false);
const statusMessage = ref('');
const isSuccess = ref(false);

// Helper to compress and square-crop image before upload
const compressAvatar = (file, targetSize = 256, quality = 0.85) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error('讀取圖片檔案失敗'));
    reader.onload = (e) => {
      const img = new Image();
      img.onerror = () => reject(new Error('圖片格式無效或已損壞'));
      img.onload = () => {
        try {
          const canvas = document.createElement('canvas');
          const ctx = canvas.getContext('2d');

          // Calculate center crop square
          const minDim = Math.min(img.width, img.height);
          const startX = (img.width - minDim) / 2;
          const startY = (img.height - minDim) / 2;

          const size = Math.min(minDim, targetSize);
          canvas.width = size;
          canvas.height = size;

          // Draw cropped & scaled square image
          ctx.drawImage(
            img,
            startX, startY, minDim, minDim,
            0, 0, size, size
          );

          canvas.toBlob(
            (blob) => {
              if (blob) {
                const compressedFile = new File([blob], 'avatar.png', {
                  type: 'image/png',
                  lastModified: Date.now()
                });
                resolve(compressedFile);
              } else {
                reject(new Error('圖片壓縮失敗'));
              }
            },
            'image/png'
          );
        } catch (err) {
          reject(err);
        }
      };
      img.src = e.target.result;
    };
    reader.readAsDataURL(file);
  });
};

const handleAvatarFileChange = async (event) => {
  const file = event.target.files?.[0];
  if (!file) return;

  try {
    statusMessage.value = '圖片處理與最佳化壓縮中...';
    isSuccess.value = false;

    // 1. Client-side square-crop & compress to tiny PNG (< 50KB)
    const compressedFile = await compressAvatar(file, 256, 0.85);

    statusMessage.value = '頭像上傳中...';
    const formData = new FormData();
    formData.append('file', compressedFile);

    await userApi.uploadAvatar(formData);
    await authStore.fetchCurrentUser();
    statusMessage.value = '頭像更新成功！';
    isSuccess.value = true;
  } catch (err) {
    statusMessage.value = err.message || '頭像上傳失敗';
    isSuccess.value = false;
  } finally {
    event.target.value = '';
  }
};

const saveProfile = async () => {
  if (!nickname.value.trim()) return;

  saving.value = true;
  statusMessage.value = '';
  try {
    await userApi.updateMe({
      nickname: nickname.value.trim(),
      avatarDefaultIcon: selectedIcon.value
    });
    await authStore.fetchCurrentUser();
    statusMessage.value = '個人資料已更新！';
    isSuccess.value = true;
    setTimeout(() => {
      emit('close');
    }, 800);
  } catch (err) {
    statusMessage.value = err.message || '儲存失敗';
    isSuccess.value = false;
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.avatar-edit-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
}

.avatar-preview-wrap {
  position: relative;
  width: 80px;
  height: 80px;
}

.avatar-upload-btn {
  position: absolute;
  bottom: -2px;
  right: -2px;
  background-color: var(--line-primary);
  color: #FFFFFF;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  border: 2px solid #FFFFFF;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
  transition: transform 0.15s ease;
}

.avatar-upload-btn:hover {
  transform: scale(1.1);
}

.avatar-tips {
  font-size: 11px;
  color: var(--line-text-secondary);
  text-align: center;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--line-text-primary);
}

.color-preset-grid {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.color-preset-item {
  cursor: pointer;
  border-radius: 50%;
  padding: 2px;
  border: 2px solid transparent;
  transition: transform 0.15s ease, border-color 0.15s ease;
}

.color-preset-item:hover {
  transform: scale(1.1);
}

.color-preset-item.active {
  border-color: var(--line-primary);
}

.security-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-top: 1px solid var(--line-border);
  font-size: 13px;
  font-weight: 500;
}

.error-text {
  color: #FF3B30;
  font-size: 13px;
  margin-top: 8px;
}

.success-text {
  color: var(--line-primary);
  font-size: 13px;
  font-weight: 600;
  margin-top: 8px;
}
</style>
