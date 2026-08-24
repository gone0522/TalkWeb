<template>
  <div class="modal-backdrop">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>{{ isForced ? '首次登入請變更密碼' : '變更密碼' }}</h3>
        <button v-if="!isForced" class="modal-close" @click="$emit('close')">✕</button>
      </div>

      <div class="modal-body">
        <div v-if="isForced" class="forced-notice">
          為了保護您的企業帳號安全，系統要求首次登入時必須設定新的登入密碼。
        </div>

        <form @submit.prevent="handleSubmit" class="modal-form">
          <div class="form-group">
            <label>新密碼</label>
            <input
              v-model="newPassword"
              type="password"
              class="input-control"
              placeholder="請輸入新密碼 (至少 6 碼)"
              required
              minlength="6"
            />
          </div>

          <div class="form-group">
            <label>確認新密碼</label>
            <input
              v-model="confirmPassword"
              type="password"
              class="input-control"
              placeholder="請再次輸入新密碼"
              required
            />
          </div>

          <div v-if="errorMessage" class="error-text">
            {{ errorMessage }}
          </div>

          <div v-if="successMessage" class="success-text">
            {{ successMessage }}
          </div>

          <div class="modal-footer-inside">
            <button
              v-if="!isForced"
              type="button"
              class="btn btn-secondary"
              @click="$emit('close')"
            >
              取消
            </button>
            <button
              type="submit"
              class="btn btn-primary"
              :disabled="loading || !newPassword || newPassword !== confirmPassword"
            >
              {{ loading ? '更新中...' : '確認變更' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAuthStore } from '../stores/authStore';

const props = defineProps({
  isForced: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['close', 'success']);

const authStore = useAuthStore();
const newPassword = ref('');
const confirmPassword = ref('');
const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

const handleSubmit = async () => {
  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = '兩次輸入的密碼不一致';
    return;
  }
  if (newPassword.value.length < 6) {
    errorMessage.value = '密碼長度需至少 6 碼';
    return;
  }

  loading.value = true;
  errorMessage.value = '';
  try {
    await authStore.changePassword(newPassword.value);
    successMessage.value = '密碼已成功更新！';
    setTimeout(() => {
      emit('success');
      emit('close');
    }, 1000);
  } catch (err) {
    errorMessage.value = err.message || '變更密碼失敗';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.forced-notice {
  background-color: #FFF3CD;
  color: #856404;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 13px;
  margin-bottom: 16px;
  line-height: 1.4;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--line-text-primary);
}

.error-text {
  color: #FF3B30;
  font-size: 13px;
}

.success-text {
  color: var(--line-primary);
  font-size: 13px;
  font-weight: 600;
}

.modal-footer-inside {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}
</style>
