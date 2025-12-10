<template>
  <div class="particle-container" ref="containerRef">
    <!-- 粒子画布 - 仅暗黑模式显示 -->
    <Transition name="particle-fade">
      <canvas v-show="isDark" ref="canvasRef" class="particle-canvas"></canvas>
    </Transition>
    <!-- 渐变流动背景 -->
    <div class="gradient-bg" :class="{ 'dark-mode': isDark }"></div>
    <!-- 几何装饰元素 - 仅暗黑模式显示 -->
    <Transition name="particle-fade">
      <div v-show="isDark" class="geometric-shapes dark-mode">
        <div class="shape circle-1"></div>
        <div class="shape circle-2"></div>
        <div class="shape circle-3"></div>
        <div class="shape square-1"></div>
        <div class="shape triangle-1"></div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { storeToRefs } from 'pinia'

const themeStore = useThemeStore()
const { isDark } = storeToRefs(themeStore)

const containerRef = ref(null)
const canvasRef = ref(null)
let animationId = null
let particles = []

// 粒子配置
const config = {
  particleCount: 50,
  particleSize: { min: 1, max: 3 },
  speed: { min: 0.2, max: 0.8 },
  connectionDistance: 150,
  mouseRadius: 100
}

// 鼠标位置
const mouse = { x: null, y: null }

class Particle {
  constructor(canvas, isDark) {
    this.canvas = canvas
    this.x = Math.random() * canvas.width
    this.y = Math.random() * canvas.height
    this.size = Math.random() * (config.particleSize.max - config.particleSize.min) + config.particleSize.min
    this.speedX = (Math.random() - 0.5) * config.speed.max
    this.speedY = (Math.random() - 0.5) * config.speed.max
    this.opacity = Math.random() * 0.5 + 0.2
    this.isDark = isDark
  }

  update() {
    this.x += this.speedX
    this.y += this.speedY

    // 边界反弹
    if (this.x < 0 || this.x > this.canvas.width) this.speedX *= -1
    if (this.y < 0 || this.y > this.canvas.height) this.speedY *= -1

    // 鼠标交互
    if (mouse.x !== null && mouse.y !== null) {
      const dx = mouse.x - this.x
      const dy = mouse.y - this.y
      const distance = Math.sqrt(dx * dx + dy * dy)
      
      if (distance < config.mouseRadius) {
        const force = (config.mouseRadius - distance) / config.mouseRadius
        this.x -= dx * force * 0.02
        this.y -= dy * force * 0.02
      }
    }
  }

  draw(ctx) {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fillStyle = this.isDark 
      ? `rgba(147, 197, 253, ${this.opacity})` 
      : `rgba(102, 126, 234, ${this.opacity})`
    ctx.fill()
  }
}

const initCanvas = () => {
  const canvas = canvasRef.value
  const container = containerRef.value
  if (!canvas || !container) return

  canvas.width = container.offsetWidth
  canvas.height = container.offsetHeight

  // 初始化粒子
  particles = []
  for (let i = 0; i < config.particleCount; i++) {
    particles.push(new Particle(canvas, isDark.value))
  }
}

const drawConnections = (ctx) => {
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < config.connectionDistance) {
        const opacity = (1 - distance / config.connectionDistance) * 0.3
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.strokeStyle = isDark.value 
          ? `rgba(147, 197, 253, ${opacity})` 
          : `rgba(102, 126, 234, ${opacity})`
        ctx.lineWidth = 0.5
        ctx.stroke()
      }
    }
  }
}

const animate = () => {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  // 更新并绘制粒子
  particles.forEach(particle => {
    particle.isDark = isDark.value
    particle.update()
    particle.draw(ctx)
  })

  // 绘制连线
  drawConnections(ctx)

  animationId = requestAnimationFrame(animate)
}

const handleMouseMove = (e) => {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (rect) {
    mouse.x = e.clientX - rect.left
    mouse.y = e.clientY - rect.top
  }
}

const handleMouseLeave = () => {
  mouse.x = null
  mouse.y = null
}

const handleResize = () => {
  initCanvas()
}

// 启动粒子动画
const startAnimation = () => {
  if (animationId) return // 已经在运行
  initCanvas()
  animate()
}

// 停止粒子动画
const stopAnimation = () => {
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
  // 清空画布
  const canvas = canvasRef.value
  if (canvas) {
    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, canvas.width, canvas.height)
  }
  particles = []
}

onMounted(() => {
  // 仅在暗黑模式下启动粒子动画
  if (isDark.value) {
    startAnimation()
  }

  window.addEventListener('resize', handleResize)
  containerRef.value?.addEventListener('mousemove', handleMouseMove)
  containerRef.value?.addEventListener('mouseleave', handleMouseLeave)
})

onUnmounted(() => {
  stopAnimation()
  window.removeEventListener('resize', handleResize)
})

// 监听主题变化，启动/停止粒子动画
watch(isDark, (newVal) => {
  if (newVal) {
    // 切换到暗黑模式，启动粒子动画
    startAnimation()
  } else {
    // 切换到浅色模式，停止粒子动画
    stopAnimation()
  }
})
</script>

<style scoped>
.particle-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: auto;
}

/* 粒子画布过渡动画 */
.particle-fade-enter-active,
.particle-fade-leave-active {
  transition: opacity 0.5s ease;
}

.particle-fade-enter-from,
.particle-fade-leave-to {
  opacity: 0;
}

/* 渐变流动背景 */
.gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    -45deg,
    rgba(102, 126, 234, 0.08),
    rgba(118, 75, 162, 0.08),
    rgba(33, 150, 243, 0.08),
    rgba(102, 126, 234, 0.08)
  );
  background-size: 400% 400%;
  animation: gradientFlow 15s ease infinite;
  z-index: -1;
}

.gradient-bg.dark-mode {
  background: linear-gradient(
    -45deg,
    rgba(30, 41, 59, 0.9),
    rgba(15, 23, 42, 0.9),
    rgba(30, 58, 95, 0.9),
    rgba(30, 41, 59, 0.9)
  );
}

@keyframes gradientFlow {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

/* 几何装饰元素 */
.geometric-shapes {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: -1;
}

.shape {
  position: absolute;
  opacity: 0.15;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
}

.circle-1 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  top: -100px;
  right: -50px;
  animation: float1 20s infinite;
}

.circle-2 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  border-radius: 50%;
  bottom: 10%;
  left: -50px;
  animation: float2 25s infinite;
}

.circle-3 {
  width: 150px;
  height: 150px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  border-radius: 50%;
  top: 40%;
  right: 10%;
  animation: float3 18s infinite;
}

.square-1 {
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  top: 20%;
  left: 10%;
  animation: rotate1 30s linear infinite;
  border-radius: 20px;
}

.triangle-1 {
  width: 0;
  height: 0;
  border-left: 60px solid transparent;
  border-right: 60px solid transparent;
  border-bottom: 100px solid rgba(102, 126, 234, 0.3);
  bottom: 20%;
  right: 20%;
  animation: float1 22s infinite;
}

/* 暗黑模式几何元素调整 */
.geometric-shapes.dark-mode .circle-1 {
  background: linear-gradient(135deg, #1e3a5f 0%, #2d1b4e 100%);
  opacity: 0.3;
}

.geometric-shapes.dark-mode .circle-2 {
  background: linear-gradient(135deg, #4a1942 0%, #2d1f3d 100%);
  opacity: 0.3;
}

.geometric-shapes.dark-mode .circle-3 {
  background: linear-gradient(135deg, #1a365d 0%, #1e4d5c 100%);
  opacity: 0.3;
}

.geometric-shapes.dark-mode .square-1 {
  background: linear-gradient(135deg, #4a1942 0%, #3d2914 100%);
  opacity: 0.3;
}

.geometric-shapes.dark-mode .triangle-1 {
  border-bottom-color: rgba(30, 58, 95, 0.4);
}

@keyframes float1 {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  33% {
    transform: translateY(-30px) rotate(5deg);
  }
  66% {
    transform: translateY(20px) rotate(-3deg);
  }
}

@keyframes float2 {
  0%, 100% {
    transform: translateX(0) translateY(0);
  }
  25% {
    transform: translateX(30px) translateY(-20px);
  }
  50% {
    transform: translateX(0) translateY(-40px);
  }
  75% {
    transform: translateX(-30px) translateY(-20px);
  }
}

@keyframes float3 {
  0%, 100% {
    transform: scale(1) translateY(0);
  }
  50% {
    transform: scale(1.1) translateY(-25px);
  }
}

@keyframes rotate1 {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
