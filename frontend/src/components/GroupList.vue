<template>
  <div class="group-list-panel">
    <div class="panel-header">
      <div class="header-top-row">
        <div class="panel-title">
          群組 <span class="count-badge">({{ groupStore.groups.length }})</span>
        </div>
        <button
          class="create-group-btn"
          title="建立新群組"
          @click="$emit('openCreateGroup')"
        >
          ➕ 建立群組
        </button>
      </div>
    </div>

    <!-- Group Item List -->
    <div class="groups-scroll-area">
      <div v-if="groupStore.loading" class="list-loading">
        載入群組中...
      </div>

      <div
        v-else-if="groupStore.groups.length === 0"
        class="empty-groups"
      >
        尚未加入任何群組，點擊上方按鈕建立新群組
      </div>

      <div
        v-for="group in groupStore.groups"
        :key="group.id"
        :class="['group-item', { active: isCurrentActive(group.id) }]"
        @click="selectGroup(group)"
      >
        <div class="avatar-col">
          <div class="group-icon-avatar">
            👥
          </div>
        </div>

        <div class="info-col">
          <div class="name-row">
            <span class="group-name">{{ group.name }}</span>
            <span class="member-count-tag">({{ group.members?.length || 0 }})</span>
          </div>
          <div class="announcement-sub">
            {{ group.announcement || '點擊進入群組對話' }}
          </div>
        </div>

        <div v-if="getUnread(group.id) > 0" class="badge-col">
          <span class="badge-unread">{{ getUnread(group.id) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useChatStore } from '../stores/chatStore';
import { useGroupStore } from '../stores/groupStore';

defineEmits(['openCreateGroup']);

const groupStore = useGroupStore();
const chatStore = useChatStore();

onMounted(() => {
  groupStore.fetchGroups();
});

const isCurrentActive = (groupId) => {
  return chatStore.chatType === 'GROUP' && chatStore.chatTarget?.id === groupId;
};

const getUnread = (groupId) => {
  return chatStore.groupUnread[groupId] || 0;
};

const selectGroup = (group) => {
  chatStore.selectGroupChat(group);
};
</script>

<style scoped>
.group-list-panel {
  width: 280px;
  height: 100%;
  background-color: var(--line-panel-bg);
  border-right: 1px solid var(--line-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.panel-header {
  padding: 16px 14px 12px;
  border-bottom: 1px solid var(--line-border);
  background-color: var(--line-panel-header-bg);
}

.header-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--line-text-primary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.count-badge {
  font-size: 13px;
  color: var(--line-text-secondary);
  font-weight: 500;
}

.create-group-btn {
  background-color: #E8F8EE;
  color: var(--line-primary-hover);
  border: 1px solid rgba(6, 199, 85, 0.3);
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.create-group-btn:hover {
  background-color: var(--line-primary);
  color: #FFFFFF;
}

.groups-scroll-area {
  flex: 1;
  overflow-y: auto;
}

.list-loading, .empty-groups {
  padding: 24px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--line-text-secondary);
  line-height: 1.5;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid #F5F5F5;
  transition: background-color 0.15s ease;
}

.group-item:hover {
  background-color: #F7F9FA;
}

.group-item.active {
  background-color: var(--line-active-chat);
}

.group-icon-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #06C755 0%, #05A648 100%);
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.info-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.group-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--line-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-count-tag {
  font-size: 12px;
  color: var(--line-text-secondary);
}

.announcement-sub {
  font-size: 12px;
  color: var(--line-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.badge-col {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .group-list-panel {
    width: 100%;
    border-right: none;
  }
}
</style>
