import { defineStore } from 'pinia';
import { messageApi } from '../services/api';
import wsService from '../services/websocket';
import { useAuthStore } from './authStore';

export const useChatStore = defineStore('chat', {
  state: () => ({
    chatType: null, // 'DIRECT' | 'GROUP'
    chatTarget: null, // UserDto or GroupDto
    messages: [],
    loadingHistory: false,
    hasMoreHistory: true,
    directUnread: {}, // userId -> count
    groupUnread: {}   // groupId -> count
  }),

  getters: {
    totalUnreadCount: (state) => {
      const dCount = Object.values(state.directUnread).reduce((acc, c) => acc + (c || 0), 0);
      const gCount = Object.values(state.groupUnread).reduce((acc, c) => acc + (c || 0), 0);
      return dCount + gCount;
    }
  },

  actions: {
    async selectDirectChat(user) {
      this.chatType = 'DIRECT';
      this.chatTarget = user;
      this.messages = [];
      this.hasMoreHistory = true;

      // Clear unread count for this user
      if (this.directUnread[user.id]) {
        this.directUnread[user.id] = 0;
      }

      await this.loadDirectHistory(user.id);
      this.markDirectAsRead(user.id);
    },

    async selectGroupChat(group) {
      this.chatType = 'GROUP';
      this.chatTarget = group;
      this.messages = [];
      this.hasMoreHistory = true;

      // Subscribe to group WebSocket topic
      wsService.subscribeToGroup(
        group.id,
        (msg) => this.receiveMessage(msg),
        (readReceipt) => this.handleGroupReadReceipt(readReceipt)
      );

      // Clear unread count
      if (this.groupUnread[group.id]) {
        this.groupUnread[group.id] = 0;
      }

      await this.loadGroupHistory(group.id);
      this.markGroupAsRead(group.id);
    },

    async loadDirectHistory(userId, beforeId = null) {
      if (this.loadingHistory) return;
      this.loadingHistory = true;
      try {
        const res = await messageApi.getDirectHistory(userId, beforeId, 50);
        const msgs = res.data || [];
        if (msgs.length < 50) {
          this.hasMoreHistory = false;
        }
        if (beforeId) {
          this.messages = [...msgs, ...this.messages];
        } else {
          this.messages = msgs;
        }
      } catch (err) {
        console.error('載入私聊歷史失敗:', err);
      } finally {
        this.loadingHistory = false;
      }
    },

    async loadGroupHistory(groupId, beforeId = null) {
      if (this.loadingHistory) return;
      this.loadingHistory = true;
      try {
        const res = await messageApi.getGroupHistory(groupId, beforeId, 50);
        const msgs = res.data || [];
        if (msgs.length < 50) {
          this.hasMoreHistory = false;
        }
        if (beforeId) {
          this.messages = [...msgs, ...this.messages];
        } else {
          this.messages = msgs;
        }
      } catch (err) {
        console.error('載入群組歷史失敗:', err);
      } finally {
        this.loadingHistory = false;
      }
    },

    async sendMessage(content, type = 'TEXT') {
      if (!content || !content.trim()) return;
      const cleanContent = content.trim();

      if (this.chatType === 'DIRECT' && this.chatTarget) {
        const receiverId = this.chatTarget.id;
        try {
          // Send via REST API for instant persistence & immediate local update
          const res = await messageApi.sendDirect({ receiverId, content: cleanContent, type });
          if (res && res.data) {
            this.receiveMessage(res.data);
          }
        } catch (err) {
          console.warn('REST API 發送失敗，嘗試透過 WebSocket 發送:', err);
          const sent = wsService.sendDirectMessage(receiverId, cleanContent, type);
          if (!sent) {
            alert('訊息發送失敗，請檢查網路連線');
          }
        }
      } else if (this.chatType === 'GROUP' && this.chatTarget) {
        const groupId = this.chatTarget.id;
        try {
          const res = await messageApi.sendGroup({ groupId, content: cleanContent, type });
          if (res && res.data) {
            this.receiveMessage(res.data);
          }
        } catch (err) {
          console.warn('REST API 發送失敗，嘗試透過 WebSocket 發送:', err);
          const sent = wsService.sendGroupMessage(groupId, cleanContent, type);
          if (!sent) {
            alert('訊息發送失敗，請檢查網路連線');
          }
        }
      }
    },

    receiveMessage(msg) {
      const authStore = useAuthStore();
      const currentUserId = authStore.user?.id;

      const isCurrentDirect = this.chatType === 'DIRECT' && this.chatTarget &&
        ((msg.senderId === this.chatTarget.id && msg.receiverId === currentUserId) ||
         (msg.senderId === currentUserId && msg.receiverId === this.chatTarget.id));

      const isCurrentGroup = this.chatType === 'GROUP' && this.chatTarget &&
        msg.groupId === this.chatTarget.id;

      if (isCurrentDirect || isCurrentGroup) {
        // Prevent duplicate appending
        if (!this.messages.some(m => m.id === msg.id)) {
          this.messages.push(msg);
        }

        // If received from the other party while viewing, mark as read immediately
        if (msg.senderId !== currentUserId) {
          if (this.chatType === 'DIRECT') {
            this.markDirectAsRead(this.chatTarget.id);
          } else if (this.chatType === 'GROUP') {
            this.markGroupAsRead(this.chatTarget.id);
          }
        }
      } else {
        // Increment unread count
        if (msg.groupId) {
          this.groupUnread[msg.groupId] = (this.groupUnread[msg.groupId] || 0) + 1;
        } else if (msg.senderId && msg.senderId !== currentUserId) {
          this.directUnread[msg.senderId] = (this.directUnread[msg.senderId] || 0) + 1;
        }
      }
    },

    handleDirectReadReceipt(receipt) {
      // Receiver has read messages sent by current user
      if (this.chatType === 'DIRECT' && this.chatTarget && receipt.readByUserId === this.chatTarget.id) {
        const readIds = new Set(receipt.messageIds || []);
        this.messages.forEach(m => {
          if (readIds.has(m.id)) {
            m.read = true;
          }
        });
      }
    },

    handleGroupReadReceipt(receipt) {
      if (this.chatType === 'GROUP' && this.chatTarget && receipt.groupId === this.chatTarget.id) {
        const readIds = new Set(receipt.messageIds || []);
        this.messages.forEach(m => {
          if (readIds.has(m.id)) {
            m.readCount = (m.readCount || 0) + 1;
          }
        });
      }
    },

    markDirectAsRead(senderId) {
      wsService.sendReadReceipt(senderId, null, []);
      messageApi.markAsRead({ senderId });
    },

    markGroupAsRead(groupId) {
      wsService.sendReadReceipt(null, groupId, []);
      messageApi.markAsRead({ groupId });
    }
  }
});
