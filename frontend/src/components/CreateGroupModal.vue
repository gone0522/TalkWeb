<template>
  <div class="modal-backdrop" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>建立新群組</h3>
        <button class="modal-close" @click="$emit('close')">✕</button>
      </div>

      <div class="modal-body">
        <div class="form-group">
          <label>群組名稱 <span class="required">*</span></label>
          <input
            v-model="groupName"
            type="text"
            class="input-control"
            placeholder="請輸入群組名稱 (例如: 專案開發小組)"
            maxlength="50"
            required
          />
        </div>

        <div class="form-group">
          <label>群組公告 / 簡介</label>
          <textarea
            v-model="announcement"
            class="input-control"
            placeholder="請輸入群組公告或目的 (選填)"
            rows="2"
          ></textarea>
        </div>

        <!-- Member Selection List -->
        <div class="form-group">
          <label>選擇初始成員 (已選 {{ selectedMemberIds.length }} 人)</label>
          <input
            v-model="searchQuery"
            type="text"
            class="input-control member-search-input"
            placeholder="搜尋聯絡人..."
          />

          <div class="members-selection-box">
            <div
              v-for="contact in filteredContacts"
              :key="contact.id"
              class="member-select-row"
              @click="toggleMember(contact.id)"
            >
              <input
                type="checkbox"
                :checked="selectedMemberIds.includes(contact.id)"
                @click.stop="toggleMember(contact.id)"
              />
              <DefaultAvatar
                :user-id="contact.id"
                :nickname="contact.nickname"
                :has-custom-avatar="contact.hasCustomAvatar"
                :icon-index="contact.avatarDefaultIcon"
                :size="32"
              />
              <span class="member-name">{{ contact.nickname }} (@{{ contact.username }})</span>
            </div>
          </div>
        </div>

        <div v-if="errorMessage" class="error-text">
          {{ errorMessage }}
        </div>
      </div>

      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" @click="$emit('close')">
          取消
        </button>
        <button
          type="button"
          class="btn btn-primary"
          :disabled="creating || !groupName.trim() || selectedMemberIds.length === 0"
          @click="handleCreateGroup"
        >
          {{ creating ? '建立中...' : '建立群組' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useContactsStore } from '../stores/contactsStore';
import { useGroupStore } from '../stores/groupStore';
import DefaultAvatar from './DefaultAvatar.vue';

const emit = defineEmits(['close', 'created']);

const groupStore = useGroupStore();
const contactsStore = useContactsStore();

const groupName = ref('');
const announcement = ref('');
const searchQuery = ref('');
const selectedMemberIds = ref([]);
const creating = ref(false);
const errorMessage = ref('');

const filteredContacts = computed(() => {
  if (!searchQuery.value.trim()) {
    return contactsStore.contacts;
  }
  const q = searchQuery.value.toLowerCase();
  return contactsStore.contacts.filter(c =>
    (c.nickname && c.nickname.toLowerCase().includes(q)) ||
    (c.username && c.username.toLowerCase().includes(q))
  );
});

const toggleMember = (userId) => {
  const idx = selectedMemberIds.value.indexOf(userId);
  if (idx === -1) {
    selectedMemberIds.value.push(userId);
  } else {
    selectedMemberIds.value.splice(idx, 1);
  }
};

const handleCreateGroup = async () => {
  if (!groupName.value.trim() || selectedMemberIds.value.length === 0) return;

  creating.value = true;
  errorMessage.value = '';
  try {
    const newGroup = await groupStore.createGroup(
      groupName.value.trim(),
      selectedMemberIds.value,
      1,
      announcement.value.trim()
    );
    emit('created', newGroup);
    emit('close');
  } catch (err) {
    errorMessage.value = err.message || '建立群組失敗';
  } finally {
    creating.value = false;
  }
};
</script>

<style scoped>
.required {
  color: #FF3B30;
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

.member-search-input {
  margin-bottom: 6px;
}

.members-selection-box {
  max-height: 180px;
  overflow-y: auto;
  border: 1px solid var(--line-border);
  border-radius: var(--radius-sm);
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.member-select-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.member-select-row:hover {
  background-color: #F5F5F5;
}

.member-name {
  font-size: 13px;
  color: var(--line-text-primary);
}

.error-text {
  color: #FF3B30;
  font-size: 13px;
}
</style>
