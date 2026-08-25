import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.client = null;
    this.connected = false;
    this.groupSubscriptions = new Map(); // groupId -> { msgSub, readSub }
    this.pendingGroupSubs = new Map();   // groupId -> { onGroupMessage, onGroupRead }
    this.onMessageReceived = null;
    this.onReadReceiptReceived = null;
    this.onPresenceChanged = null;
  }

  connect(token) {
    if (this.client && this.client.active) {
      return;
    }

    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${wsProtocol}//${window.location.host}/ws/chat`;

    this.client = new Client({
      brokerURL: wsUrl,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
        token: token
      },
      webSocketFactory: () => {
        // Fallback to SockJS for proxies that block native WebSockets
        return new SockJS('/ws/chat');
      },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: (str) => {
        // console.log('[STOMP Debug]', str);
      }
    });

    this.client.onConnect = (frame) => {
      this.connected = true;
      console.log('[WebSocket] 連線成功:', frame);

      // Subscribe to private messages
      this.client.subscribe('/user/queue/messages', (message) => {
        if (message.body) {
          const chatMsg = JSON.parse(message.body);
          if (this.onMessageReceived) {
            this.onMessageReceived(chatMsg);
          }
        }
      });

      // Subscribe to private read receipts
      this.client.subscribe('/user/queue/read-receipts', (message) => {
        if (message.body) {
          const receipt = JSON.parse(message.body);
          if (this.onReadReceiptReceived) {
            this.onReadReceiptReceived(receipt);
          }
        }
      });

      // Subscribe to presence updates
      this.client.subscribe('/topic/presence', (message) => {
        if (message.body) {
          const presence = JSON.parse(message.body);
          if (this.onPresenceChanged) {
            this.onPresenceChanged(presence);
          }
        }
      });

      // Resubscribe to all pending groups
      this.pendingGroupSubs.forEach((handlers, groupId) => {
        this._doSubscribeGroup(groupId, handlers.onGroupMessage, handlers.onGroupRead);
      });
    };

    this.client.onStompError = (frame) => {
      console.error('[WebSocket] STOMP 錯誤:', frame.headers?.message, frame.body);
    };

    this.client.onDisconnect = () => {
      this.connected = false;
      this.groupSubscriptions.clear();
      console.log('[WebSocket] 連線已中斷');
    };

    this.client.activate();
  }

  _doSubscribeGroup(groupId, onGroupMessage, onGroupRead) {
    if (!this.client || !this.connected) return;

    // Unsubscribe existing if any
    const existing = this.groupSubscriptions.get(groupId);
    if (existing) {
      existing.msgSub?.unsubscribe();
      existing.readSub?.unsubscribe();
    }

    const msgSub = this.client.subscribe(`/topic/group.${groupId}`, (message) => {
      if (message.body) {
        const chatMsg = JSON.parse(message.body);
        if (onGroupMessage) onGroupMessage(chatMsg);
      }
    });

    const readSub = this.client.subscribe(`/topic/group.${groupId}.read`, (message) => {
      if (message.body) {
        const receipt = JSON.parse(message.body);
        if (onGroupRead) onGroupRead(receipt);
      }
    });

    this.groupSubscriptions.set(groupId, { msgSub, readSub });
  }

  subscribeToGroup(groupId, onGroupMessage, onGroupRead) {
    this.pendingGroupSubs.set(groupId, { onGroupMessage, onGroupRead });
    if (this.connected) {
      this._doSubscribeGroup(groupId, onGroupMessage, onGroupRead);
    }
  }

  unsubscribeFromGroup(groupId) {
    this.pendingGroupSubs.delete(groupId);
    const subs = this.groupSubscriptions.get(groupId);
    if (subs) {
      subs.msgSub?.unsubscribe();
      subs.readSub?.unsubscribe();
      this.groupSubscriptions.delete(groupId);
    }
  }

  sendDirectMessage(receiverId, content, type = 'TEXT') {
    if (!this.client || !this.connected) {
      console.warn('[WebSocket] 尚未連線，無法發送私聊訊息');
      return false;
    }
    this.client.publish({
      destination: '/app/chat.sendDirect',
      body: JSON.stringify({ receiverId, content, type })
    });
    return true;
  }

  sendGroupMessage(groupId, content, type = 'TEXT') {
    if (!this.client || !this.connected) {
      console.warn('[WebSocket] 尚未連線，無法發送群組訊息');
      return false;
    }
    this.client.publish({
      destination: '/app/chat.sendGroup',
      body: JSON.stringify({ groupId, content, type })
    });
    return true;
  }

  sendReadReceipt(senderId, groupId, messageIds = []) {
    if (!this.client || !this.connected) return false;
    this.client.publish({
      destination: '/app/chat.read',
      body: JSON.stringify({ senderId, groupId, messageIds })
    });
    return true;
  }

  disconnect() {
    if (this.client) {
      this.pendingGroupSubs.clear();
      this.groupSubscriptions.clear();
      this.client.deactivate();
      this.connected = false;
      this.client = null;
    }
  }
}

export const wsService = new WebSocketService();
export default wsService;

