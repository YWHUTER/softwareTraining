<template>
  <div class="mesh-container">
    <div class="mesh-gradient">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
      <div class="orb orb-4"></div>
    </div>
    <div class="glass-surface">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.mesh-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background-color: #f8fafc; /* 非常浅的灰/白底色 */
  border-radius: inherit;
}

.mesh-gradient {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  filter: blur(60px); /* 关键：通过高斯模糊融合颜色 */
}

.orb {
  position: absolute;
  border-radius: 50%;
  opacity: 0.6;
  animation: float 10s infinite ease-in-out alternate;
}

/* 淡蓝色光斑 */
.orb-1 {
  width: 60%;
  height: 60%;
  top: -10%;
  left: -10%;
  background: #bfdbfe; /* blue-200 */
  animation-duration: 12s;
}

/* 淡紫色光斑 */
.orb-2 {
  width: 50%;
  height: 50%;
  bottom: -10%;
  right: -10%;
  background: #e9d5ff; /* purple-200 */
  animation-duration: 15s;
  animation-delay: -2s;
}

/* 淡青色光斑 */
.orb-3 {
  width: 40%;
  height: 40%;
  bottom: 20%;
  left: 20%;
  background: #bae6fd; /* sky-200 */
  animation-duration: 18s;
  animation-delay: -5s;
}

/* 纯白光斑（增加通透感） */
.orb-4 {
  width: 30%;
  height: 30%;
  top: 20%;
  right: 20%;
  background: #ffffff;
  animation-duration: 20s;
  animation-delay: -8s;
}

.glass-surface {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.1); /* 极淡的遮罩 */
  backdrop-filter: blur(2px); /* 轻微磨砂 */
}

@keyframes float {
  0% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(30px, 20px) scale(1.05); }
  100% { transform: translate(-20px, -30px) scale(0.95); }
}
</style>
