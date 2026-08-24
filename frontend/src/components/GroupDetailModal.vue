<template>
  <div class="modal-backdrop" @click="$emit('close')">
    <div class="modal-content group-detail-dialog" @click.stop>
      <div class="modal-header">
        <h3>群組設定與成員</h3>
        <button class="modal-close" @click="$emit('close')">✕</button>
      </div>

      <div class="modal-body" v-if="group">
        <!-- Group Info Edit -->
        <div class="group-info-header">
          <div class="group-icon-avatar-large">
            👥
          </div>
          <div class="group-title-edit">
            <input
              v-if="isPrivileged"
              v-model="editName"
              type="text"
              class="input-control name-edit-input"
              placeholder="群組名稱"
            />
            <h4 v-else class="group-static-name">{{ group.name }}</h4>
            <div class="created-meta">由 {{ group.createdByNickname || '管理員' }} 建立</div>
          </div>
        </div>

        <div class="form-group">
          <label>群組公告</label>
          <textarea
            v-if="isPrivileged"
            v-model="editAnnouncement"
            class="input-control"
            placeholder="點擊輸入最新群組公告..."
            rows="2"
          ></textarea>
          <div v-else class="static-announcement">
            {{ group.announcement || '目前無群組公告' }}
          </div>
        </div>

        <button
          v-if="isPrivileged && (editName !== group.name || editAnnouncement !== (group.announcement || ''))"
          class="btn btn-primary btn-sm save-info-btn"
          @click="saveGroupInfo"
        >
          儲存群組修改
        </button>

        <hr class="divider" />

        <!-- Member Section -->
        <div class="members-header-row">
          <label class="members-section-title">成員清單 ({{ group.members?.length || 0 }})</label>
          <button class="btn btn-secondary btn-sm" @click="showInviteBox = !showInviteBox">
            {{ showInviteBox ? '關閉邀請' : '➕ 邀請成員' }}
          </button>
        </div>

        <!-- Invite box -->
        <div v-if="showInviteBox" class="invite-box">
          <div class="invite-title">選擇欲加入群組的成員：</div>
          <div class="invite-list">
            <div
              v-for="c in invitableContacts"
              :key="c.id"
              class="invite-item"
              @click="toggleInvite(c.id)"
            >
              <input type="checkbox" :checked="inviteIds.includes(c.id)" @click.stop="toggleInvite(c.id)" />
              <DefaultAvatar :user-id="c.id" :nickname="c.nickname" :has-custom-avatar="c.hasCustomAvatar" :size="28" />
              <span>{{ c.nickname }}</span>
            </div>
            <div v-if="invitableContacts.length === 0" class="no-invitable">
              所有成員皆已在此群組中
            </div>
          </div>
          <button
            class="btn btn-primary btn-sm invite-submit-btn"
            :disabled="inviteIds.length === 0"
            @click="submitInvite"
          >
            確認加入 ({{ inviteIds.length }})
          </button>
        </div>

        <!-- Current Members List -->
        <div class="members-list">
          <div
            v-for="m in group.members"
            :key="m.userId"
            class="member-row"
          >
            <div class="member-left">
              <DefaultAvatar
                :user-id="m.userId"
                :nickname="m.nickname"
                :has-custom-avatar="m.hasCustomAvatar"
                :icon-index="m.avatarDefaultIcon"
                :size="36"
              />
              <div class="member-name-wrap">
                <span class="member-nick">{{ m.nickname }}</span>
                <span v-if="m.role === 'OWNER'" class="role-tag owner">群主</span>
                <span v-else-if="m.role === 'ADMIN'" class="role-tag admin">管理員</span>
              </div>
            </div>

            <!-- Remove / Kick button -->
            <button
              v-if="canRemove(m)"
              class="remove-btn"
              title="移除成員"
              @click="removeMember(m.userId)"
            >
              移除
            </button>
          </div>
        </div>

        <hr class="divider" />

        <!-- Leave Group Action -->
        <div class="leave-action-row">
          <button class="btn btn-danger btn-sm" @click="handleLeave">
            退出此群組
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useAuthStore } from '../stores/authStore';
import { useContactsStore } from '../stores/contactsStore';
import { useGroupStore } from '../stores/groupStore';
import DefaultAvatar from './DefaultAvatar.vue';

const props = defineProps({
  group: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['close', 'left']);

const authStore = useAuthStore();
const groupStore = useGroupStore();
const contactsStore = useContactsStore();

const editName = ref(props.group.name);
const editAnnouncement = ref(props.group.announcement || '');
const showInviteBox = ref(false);
const inviteIds = ref([]);

const currentMember = computed(() => {
  return props.group.members?.find(m => m.userId === authStore.user?.id);
});

const isPrivileged = computed(() => {
  const role = currentMember.value?.role;
  return role === 'OWNER' || role === 'ADMIN';
});

const invitableContacts = computed(() => {
  const existingUserIds = new Set(props.group.members?.map(m => m.userId) || []);
  return contactsStore.contacts.filter(c => !existingUserIds.has(c.id));
});

const toggleInvite = (id) => {
  const idx = inviteIds.value.indexOf(id);
  if (idx === -1) inviteIds.value.push(id);
  else inviteIds.value.splice(idx, 1);
};

const saveGroupInfo = async () => {
  try {
    await groupStore.updateGroupInfo(props.group.id, editName.value, editAnnouncement.value);
    alert('群組資訊已更新');
  } catch (err) {
    alert(err.message || '更新失敗');
  }
};

const submitInvite = async () => {
  if (inviteIds.value.length === 0) return;
  try {
    await groupStore.addMembers(props.group.id, inviteIds.value);
    inviteIds.value = [];
    showInviteBox.value = false;
  } catch (err) {
    alert(err.message || '邀請成員失敗');
  }
};

const canRemove = (m) => {
  if (m.userId === authStore.user?.id) return false;
  if (m.role === 'OWNER') return false;
  return isPrivileged.value;
};

const removeMember = async (userId) => {
  if (!confirm('確定要將此成員移出群組嗎？')) return;
  try {
    await groupStore.leaveGroup(props.group.id, userId);
  } catch (err) {
    alert(err.message || '移除成員失敗');
  }
};

const handleLeave = async () => {
  if (!confirm('確定要退出此群組嗎？')) return;
  try {
    await groupStore.leaveGroup(props.group.id, authStore.user?.id);
    emit('left');
    emit('close');
  } catch (err) {
    alert(err.message || '退出群組失敗');
  }
};
</script>

<style scoped>
.group-detail-dialog {
  max-width: 520px;
}

.group-info-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.group-icon-avatar-large {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: linear-gradient(135deg, #06C755 0%, #05A648 100%);
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.group-title-edit {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.name-edit-input {
  font-weight: 600;
  font-size: 15px;
}

.group-static-name {
  font-size: 16px;
  font-weight: 700;
}

.created-meta {
  font-size: 12px;
  color: var(--line-text-secondary);
}

.static-announcement {
  background-color: #F8F8F8;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--line-text-primary);
  line-height: 1.4;
}

.save-info-btn {
  margin-top: 6px;
  align-self: flex-start;
}

.btn-sm {
  padding: 4px 10px;
  font-size: 12px;
}

.divider {
  border: none;
  border-top: 1px solid var(--line-border);
  margin: 16px 0;
}

.members-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.members-section-title {
  font-size: 13px;
  font-weight: 600;
}

.invite-box {
  background-color: #F8F9FA;
  border: 1px solid var(--line-border);
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.invite-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--line-text-secondary);
}

.invite-list {
  max-height: 120px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.invite-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px;
  cursor: pointer;
  font-size: 13px;
}

.no-invitable {
  font-size: 12px;
  color: var(--line-text-secondary);
  text-align: center;
  padding: 6px 0;
}

.invite-submit-btn {
  align-self: flex-end;
}

.members-list {
  max-height: 200px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.member-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 4px;
}

.member-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-name-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
}

.member-nick {
  font-size: 13px;
  font-weight: 500;
}

.role-tag {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 4px;
  font-weight: 600;
}

.role-tag.owner {
  background-color: #E8F8EE;
  color: var(--line-primary-hover);
}

.role-tag.admin {
  background-color: #E6F0FF;
  color: #0366D6;
}

.remove-btn {
  background: none;
  border: none;
  color: #FF3B30;
  font-size: 12px;
  cursor: pointer;
}

.remove-btn:hover {
  text-decoration: underline;
}

.leave-action-row {
  display: flex;
  justify-content: center;
}
</style>
