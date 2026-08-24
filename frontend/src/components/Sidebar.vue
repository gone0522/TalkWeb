<template>
  <aside class="line-sidebar">
    <div class="sidebar-top">
      <div class="user-avatar-wrap" @click="$emit('openProfile')" title="個人設定">
        <DefaultAvatar
          :user-id="authStore.user?.id"
          :nickname="authStore.user?.nickname"
          :has-custom-avatar="authStore.user?.hasCustomAvatar"
          :icon-index="authStore.user?.avatarDefaultIcon"
          :size="36"
        />
        <span class="presence-dot online"></span>
      </div>

      <!-- Navigation Tabs -->
      <nav class="sidebar-nav">
        <button
          :class="['nav-item', { active: currentTab === 'contacts' }]"
          title="好友與通訊錄"
          @click="$emit('switchTab', 'contacts')"
        >
          <span class="nav-icon">👤</span>
          <span class="nav-label">好友</span>
        </button>

        <button
          :class="['nav-item', { active: currentTab === 'groups' }]"
          title="群組與聊天"
          @click="$emit('switchTab', 'groups')"
        >
          <span class="nav-icon">👥</span>
          <span class="nav-label">群組</span>
          <span v-if="chatStore.totalUnreadCount > 0" class="badge-unread nav-badge">
            {{ chatStore.totalUnreadCount > 99 ? '99+' : chatStore.totalUnreadCount }}
          </span>
        </button>

        <button
          v-if="authStore.isAdmin"
          :class="['nav-item', { active: currentTab === 'admin' }]"
          title="管理員面板"
          @click="$emit('openAdmin')"
        >
          <span class="nav-icon">🛡️</span>
          <span class="nav-label">管理</span>
        </button>
      </nav>
    </div>

    <!-- Bottom Actions -->
    <div class="sidebar-bottom">
      <button class="nav-item" title="個人設定" @click="$emit('openProfile')">
        <span class="nav-icon">⚙️</span>
        <span class="nav-label">設定</span>
      </button>
      <button class="nav-item logout-btn" title="登出" @click="handleLogout">
        <span class="nav-icon">🚪</span>
        <span class="nav-label">登出</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { useAuthStore } from '../stores/authStore';
import { useChatStore } from '../stores/chatStore';
import DefaultAvatar from './DefaultAvatar.vue';

defineProps({
  currentTab: {
    type: String,
    default: 'contacts'
  }
});

const emit = defineEmits(['switchTab', 'openProfile', 'openAdmin']);

const authStore = useAuthStore();
const chatStore = useChatStore();

const handleLogout = () => {
  if (confirm('確定要登出系統嗎？')) {
    authStore.logout();
    window.location.reload();
  }
};
</script>

<style scoped>
.line-sidebar {
  width: 56px;
  height: 100%;
  background-color: var(--line-sidebar-bg);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 12px;
  flex-shrink: 0;
  user-select: none;
}

.sidebar-top {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  width: 100%;
}

.user-avatar-wrap {
  position: relative;
  cursor: pointer;
  transition: transform 0.15s ease;
}

.user-avatar-wrap:hover {
  transform: scale(1.05);
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.nav-item {
  width: 100%;
  height: 48px;
  background: none;
  border: none;
  color: #8E9BAE;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  transition: all 0.15s ease;
}

.nav-icon {
  font-size: 20px;
}

.nav-label {
  display: none;
}

.nav-item:hover {
  color: #FFFFFF;
  background-color: var(--line-sidebar-hover);
}

.nav-item.active {
  color: #FFFFFF;
  background-color: var(--line-sidebar-hover);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  background-color: var(--line-primary);
  border-top-right-radius: 2px;
  border-bottom-right-radius: 2px;
}

.nav-badge {
  position: absolute;
  top: 6px;
  right: 8px;
}

.sidebar-bottom {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.logout-btn:hover {
  color: #FF6B6B;
}

/* Mobile Bottom Navigation Bar */
@media (max-width: 768px) {
  .line-sidebar {
    width: 100% !important;
    height: 56px !important;
    flex-direction: row !important;
    justify-content: space-around !important;
    padding: 0 6px !important;
    border-top: 1px solid rgba(255, 255, 255, 0.1) !important;
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.15) !important;
    z-index: 50 !important;
  }

  .sidebar-top {
    flex-direction: row !important;
    align-items: center !important;
    gap: 0 !important;
    flex: 1 !important;
    height: 100% !important;
    justify-content: space-around !important;
  }

  .user-avatar-wrap {
    display: none !important;
  }

  .sidebar-nav {
    flex-direction: row !important;
    gap: 0 !important;
    flex: 1 !important;
    height: 100% !important;
    justify-content: space-around !important;
  }

  .sidebar-bottom {
    flex-direction: row !important;
    gap: 0 !important;
    width: auto !important;
    flex: 0.7 !important;
    height: 100% !important;
    justify-content: space-around !important;
  }

  .nav-item {
    width: auto !important;
    flex: 1 !important;
    height: 100% !important;
    flex-direction: column !important;
    gap: 2px !important;
    padding: 4px 0 !important;
  }

  .nav-icon {
    font-size: 18px;
    line-height: 1;
  }

  .nav-label {
    display: block !important;
    font-size: 10px !important;
    font-weight: 500 !important;
    color: #8E9BAE !important;
    line-height: 1.2 !important;
  }

  .nav-item.active .nav-label {
    color: #FFFFFF !important;
    font-weight: 600 !important;
  }

  .nav-item.active::before {
    left: 20% !important;
    right: 20% !important;
    top: 0 !important;
    bottom: auto !important;
    width: 60% !important;
    height: 3px !important;
    border-radius: 0 0 2px 2px !important;
  }

  .nav-badge {
    top: 2px !important;
    right: calc(50% - 18px) !important;
  }
}
</style>
