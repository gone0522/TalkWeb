<template>
  <Teleport to="body">
    <div
      class="lightbox-overlay"
      @click.self="close"
      @keydown.esc="close"
      tabindex="-1"
      ref="overlayRef"
    >
      <!-- Top Close Button -->
      <button class="lightbox-close-btn" title="關閉 (ESC)" @click="close">
        <svg viewBox="0 0 24 24" width="26" height="26" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>

      <!-- Main Image Viewport with gesture handlers -->
      <div
        class="lightbox-viewport"
        @wheel.prevent="handleWheel"
        @mousedown="handleMouseDown"
        @touchstart="handleTouchStart"
        @touchmove="handleTouchMove"
        @touchend="handleTouchEnd"
        @dblclick="handleDoubleClick"
      >
        <img
          ref="imgRef"
          :src="src"
          alt="聊天圖片預覽"
          class="lightbox-image"
          :style="imageStyle"
          @dragstart.prevent
        />
      </div>

      <!-- Bottom Floating Controls -->
      <div class="lightbox-controls">
        <button class="ctrl-btn" title="縮小" @click="zoomOut">
          <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            <line x1="8" y1="11" x2="14" y2="11"></line>
          </svg>
        </button>

        <span class="zoom-percentage">{{ Math.round(scale * 100) }}%</span>

        <button class="ctrl-btn" title="放大" @click="zoomIn">
          <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            <line x1="11" y1="8" x2="11" y2="14"></line>
            <line x1="8" y1="11" x2="14" y2="11"></line>
          </svg>
        </button>

        <button class="ctrl-btn" title="順時針旋轉" @click="rotate">
          <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none">
            <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"></path>
          </svg>
        </button>

        <button class="ctrl-btn reset-btn" title="重置視圖" @click="resetTransform">
          重置
        </button>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';

const props = defineProps({
  src: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['close']);

const overlayRef = ref(null);
const imgRef = ref(null);

const scale = ref(1);
const translateX = ref(0);
const translateY = ref(0);
const rotation = ref(0);

// Dragging state for desktop
let isDragging = false;
let startDragX = 0;
let startDragY = 0;

// Touch tracking for mobile pinch-to-zoom
let initialDistance = 0;
let initialScale = 1;
let touchStartX = 0;
let touchStartY = 0;
let isTouching = false;
let isPinching = false;

const imageStyle = computed(() => {
  return {
    transform: `translate(${translateX.value}px, ${translateY.value}px) scale(${scale.value}) rotate(${rotation.value}deg)`,
    transition: isDragging || isTouching ? 'none' : 'transform 0.15s ease-out',
    cursor: isDragging ? 'grabbing' : scale.value > 1 ? 'grab' : 'default'
  };
});

onMounted(() => {
  document.body.style.overflow = 'hidden';
  if (overlayRef.value) {
    overlayRef.value.focus();
  }
  window.addEventListener('keydown', onKeyDown);
});

onUnmounted(() => {
  document.body.style.overflow = '';
  window.removeEventListener('keydown', onKeyDown);
});

const onKeyDown = (e) => {
  if (e.key === 'Escape') {
    close();
  }
};

const close = () => {
  emit('close');
};

const zoomIn = () => {
  scale.value = Math.min(scale.value * 1.3, 5);
};

const zoomOut = () => {
  scale.value = Math.max(scale.value / 1.3, 0.3);
};

const rotate = () => {
  rotation.value = (rotation.value + 90) % 360;
};

const resetTransform = () => {
  scale.value = 1;
  translateX.value = 0;
  translateY.value = 0;
  rotation.value = 0;
};

const handleDoubleClick = () => {
  if (scale.value > 1) {
    resetTransform();
  } else {
    scale.value = 2;
  }
};

// Mouse wheel zoom
const handleWheel = (e) => {
  const delta = e.deltaY > 0 ? 0.9 : 1.1;
  const newScale = Math.min(Math.max(scale.value * delta, 0.3), 6);
  scale.value = newScale;
};

// Mouse drag pan
const handleMouseDown = (e) => {
  if (e.button !== 0) return; // Only left click
  isDragging = true;
  startDragX = e.clientX - translateX.value;
  startDragY = e.clientY - translateY.value;

  window.addEventListener('mousemove', onMouseMove);
  window.addEventListener('mouseup', onMouseUp);
};

const onMouseMove = (e) => {
  if (!isDragging) return;
  translateX.value = e.clientX - startDragX;
  translateY.value = e.clientY - startDragY;
};

const onMouseUp = () => {
  isDragging = false;
  window.removeEventListener('mousemove', onMouseMove);
  window.removeEventListener('mouseup', onMouseUp);
};

// Mobile Touch gestures: Pinch-to-zoom and Pan
const getDistance = (touch1, touch2) => {
  const dx = touch1.clientX - touch2.clientX;
  const dy = touch1.clientY - touch2.clientY;
  return Math.hypot(dx, dy);
};

const handleTouchStart = (e) => {
  if (e.touches.length === 1) {
    isTouching = true;
    isPinching = false;
    touchStartX = e.touches[0].clientX - translateX.value;
    touchStartY = e.touches[0].clientY - translateY.value;
  } else if (e.touches.length === 2) {
    isPinching = true;
    isTouching = false;
    initialDistance = getDistance(e.touches[0], e.touches[1]);
    initialScale = scale.value;
  }
};

const handleTouchMove = (e) => {
  if (isPinching && e.touches.length === 2) {
    const currentDistance = getDistance(e.touches[0], e.touches[1]);
    if (initialDistance > 0) {
      const pinchScale = (currentDistance / initialDistance) * initialScale;
      scale.value = Math.min(Math.max(pinchScale, 0.4), 6);
    }
  } else if (isTouching && e.touches.length === 1) {
    translateX.value = e.touches[0].clientX - touchStartX;
    translateY.value = e.touches[0].clientY - touchStartY;
  }
};

const handleTouchEnd = (e) => {
  if (e.touches.length === 0) {
    isTouching = false;
    isPinching = false;
    if (scale.value < 0.8) {
      resetTransform();
    }
  } else if (e.touches.length === 1) {
    isPinching = false;
    isTouching = true;
    touchStartX = e.touches[0].clientX - translateX.value;
    touchStartY = e.touches[0].clientY - translateY.value;
  }
};
</script>

<style scoped>
.lightbox-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background-color: rgba(0, 0, 0, 0.88);
  backdrop-filter: blur(8px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  outline: none;
  user-select: none;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.lightbox-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10001;
  transition: all 0.2s ease;
}

.lightbox-close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.lightbox-viewport {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  touch-action: none;
}

.lightbox-image {
  max-width: 90vw;
  max-height: 85vh;
  object-fit: contain;
  border-radius: 6px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.6);
  user-select: none;
  -webkit-user-drag: none;
}

.lightbox-controls {
  position: absolute;
  bottom: 24px;
  background: rgba(30, 30, 30, 0.75);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 30px;
  padding: 6px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 10001;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
}

.ctrl-btn {
  background: none;
  border: none;
  color: #FFFFFF;
  padding: 6px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.ctrl-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.ctrl-btn.reset-btn {
  border-radius: 12px;
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.1);
}

.ctrl-btn.reset-btn:hover {
  background: rgba(255, 255, 255, 0.25);
}

.zoom-percentage {
  color: #E0E0E0;
  font-size: 13px;
  font-weight: 600;
  min-width: 44px;
  text-align: center;
}

@media (max-width: 768px) {
  .lightbox-close-btn {
    top: 12px;
    right: 12px;
    width: 40px;
    height: 40px;
  }

  .lightbox-controls {
    bottom: calc(16px + env(safe-area-inset-bottom, 0px));
    gap: 8px;
    padding: 4px 10px;
  }

  .lightbox-image {
    max-width: 96vw;
    max-height: 80vh;
  }
}
</style>
