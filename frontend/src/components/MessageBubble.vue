<template>
  <div :class="['message-row', isMe ? 'me' : 'other']">
    <!-- Avatar (only for other users) -->
    <div v-if="!isMe" class="message-avatar-wrap">
      <DefaultAvatar
        :user-id="message.senderId"
        :nickname="message.senderNickname || message.senderUsername"
        :has-custom-avatar="message.senderHasCustomAvatar"
        :icon-index="message.senderDefaultAvatarIcon"
        :size="36"
      />
    </div>

    <!-- Message Content & Meta -->
    <div class="message-body-wrap">
      <!-- Sender Nickname (for group chat or others) -->
      <div v-if="!isMe && showSenderName" class="sender-nickname">
        {{ message.senderNickname || message.senderUsername }}
      </div>

      <div class="bubble-with-meta">
        <!-- Status Meta for ME (on the left of bubble) -->
        <div v-if="isMe" class="meta-col me-meta">
          <span v-if="readStatusText" class="read-status">{{ readStatusText }}</span>
          <span class="message-time">{{ formattedTime }}</span>
        </div>

        <!-- The Message Bubble -->
        <div :class="['message-bubble', isMe ? 'bubble-me' : 'bubble-other', { 'pure-emoji': isPureEmoji }]">
          <!-- Text Content with Auto-Link -->
          <div class="bubble-text" v-html="renderedContent"></div>

          <!-- Link Preview Card if present -->
          <UrlPreviewCard
            v-if="message.linkPreview"
            :preview="message.linkPreview"
          />
        </div>

        <!-- Status Meta for OTHER (on the right of bubble) -->
        <div v-if="!isMe" class="meta-col other-meta">
          <span class="message-time">{{ formattedTime }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import DefaultAvatar from './DefaultAvatar.vue';
import UrlPreviewCard from './UrlPreviewCard.vue';

const props = defineProps({
  message: {
    type: Object,
    required: true
  },
  currentUserId: {
    type: Number,
    required: true
  },
  showSenderName: {
    type: Boolean,
    default: true
  }
});

const isMe = computed(() => {
  return props.message.senderId === props.currentUserId;
});

const formattedTime = computed(() => {
  if (!props.message.createdAt) return '';
  const d = new Date(props.message.createdAt);
  const hours = d.getHours().toString().padStart(2, '0');
  const minutes = d.getMinutes().toString().padStart(2, '0');
  return `${hours}:${minutes}`;
});

const readStatusText = computed(() => {
  if (!isMe.value) return '';
  if (props.message.groupId) {
    return props.message.readCount > 0 ? `已讀 ${props.message.readCount}` : '';
  } else {
    return props.message.read ? '已讀' : '';
  }
});

// Escape HTML and replace URLs with safe clickable anchor tags
const renderedContent = computed(() => {
  const text = props.message.content || '';
  // 1. Escape HTML
  const div = document.createElement('div');
  div.textContent = text;
  const escaped = div.innerHTML;

  // 2. Replace URLs with link
  const urlRegex = /(https?:\/\/[^\s<]+)/g;
  return escaped.replace(urlRegex, (url) => {
    return `<a href="${url}" target="_blank" rel="noopener noreferrer" class="bubble-link">${url}</a>`;
  });
});

// Check if content is purely emojis (1-3 emojis)
const isPureEmoji = computed(() => {
  const text = props.message.content?.trim();
  if (!text) return false;
  // Emoji regex check
  const emojiRegex = /^(\p{Extended_Pictographic}|\u200d|\ufe0f){1,3}$/u;
  return emojiRegex.test(text);
});
</script>

<style scoped>
.message-row {
  display: flex;
  margin-bottom: 12px;
  gap: 8px;
  animation: fadeIn 0.15s ease;
}

.message-row.me {
  justify-content: flex-end;
}

.message-row.other {
  justify-content: flex-start;
}

.message-avatar-wrap {
  margin-top: 2px;
  flex-shrink: 0;
}

.message-body-wrap {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}

.sender-nickname {
  font-size: 12px;
  color: var(--line-text-secondary);
  margin-bottom: 3px;
  margin-left: 4px;
}

.bubble-with-meta {
  display: flex;
  align-items: flex-end;
  gap: 6px;
}

.message-row.me .bubble-with-meta {
  flex-direction: row;
}

.meta-col {
  display: flex;
  flex-direction: column;
  font-size: 11px;
  color: var(--line-text-secondary);
  line-height: 1.2;
  user-select: none;
  flex-shrink: 0;
}

.me-meta {
  align-items: flex-end;
}

.other-meta {
  align-items: flex-start;
}

.read-status {
  color: var(--line-primary);
  font-size: 10px;
  font-weight: 600;
  margin-bottom: 1px;
}

.message-time {
  font-size: 10px;
  color: var(--line-text-muted);
}

/* Message Bubbles */
.message-bubble {
  position: relative;
  padding: 10px 14px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.45;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.bubble-me {
  background-color: var(--line-bubble-me);
  color: #111111;
  border-top-right-radius: 4px;
}

.bubble-other {
  background-color: var(--line-bubble-other);
  color: #1E1E1E;
  border: 1px solid var(--line-bubble-border);
  border-top-left-radius: 4px;
}

.bubble-text :deep(.bubble-link) {
  color: #0366D6;
  text-decoration: underline;
  word-break: break-all;
}

.bubble-me .bubble-text :deep(.bubble-link) {
  color: #0D47A1;
}

.pure-emoji {
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
  font-size: 40px !important;
  padding: 4px 8px !important;
}
</style>
