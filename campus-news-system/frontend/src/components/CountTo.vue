<template>
  <span class="count-to-wrapper">
    {{ displayValue }}
  </span>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  startVal: {
    type: Number,
    default: 0
  },
  endVal: {
    type: Number,
    required: true
  },
  duration: {
    type: Number,
    default: 2000
  },
  autoplay: {
    type: Boolean,
    default: true
  },
  decimals: {
    type: Number,
    default: 0
  },
  prefix: {
    type: String,
    default: ''
  },
  suffix: {
    type: String,
    default: ''
  },
  separator: {
    type: String,
    default: ','
  },
  decimal: {
    type: String,
    default: '.'
  }
})

const localStartVal = ref(props.startVal)
const displayValue = ref(formatNumber(props.startVal))
const printVal = ref(null)
const paused = ref(false)
const startTime = ref(null)
const timestamp = ref(null)
const remaining = ref(null)
const rAF = ref(null)
const duration = ref(props.duration)
const useEasing = ref(true)

const count = (timestamp) => {
  if (!startTime.value) startTime.value = timestamp
  
  const progress = timestamp - startTime.value
  remaining.value = duration.value - progress
  
  if (useEasing.value) {
    if (countDown.value) {
      printVal.value = localStartVal.value - easingFn(progress, 0, localStartVal.value - props.endVal, duration.value)
    } else {
      printVal.value = easingFn(progress, localStartVal.value, props.endVal - localStartVal.value, duration.value)
    }
  } else {
    if (countDown.value) {
      printVal.value = localStartVal.value - ((localStartVal.value - props.endVal) * (progress / duration.value))
    } else {
      printVal.value = localStartVal.value + ((props.endVal - localStartVal.value) * (progress / duration.value))
    }
  }

  if (countDown.value) {
    printVal.value = printVal.value < props.endVal ? props.endVal : printVal.value
  } else {
    printVal.value = printVal.value > props.endVal ? props.endVal : printVal.value
  }

  displayValue.value = formatNumber(printVal.value)

  if (progress < duration.value) {
    rAF.value = requestAnimationFrame(count)
  } else {
    emit('mounted')
  }
}

const start = () => {
  localStartVal.value = props.startVal
  startTime.value = null
  duration.value = props.duration
  paused.value = false
  rAF.value = requestAnimationFrame(count)
}

const pauseResume = () => {
  if (paused.value) {
    resume()
    paused.value = false
  } else {
    pause()
    paused.value = true
  }
}

const pause = () => {
  cancelAnimationFrame(rAF.value)
}

const resume = () => {
  startTime.value = null
  localStartVal.value = printVal.value
  duration.value = remaining.value
  localStartVal.value = printVal.value
  requestAnimationFrame(count)
}

const reset = () => {
  startTime.value = null
  cancelAnimationFrame(rAF.value)
  displayValue.value = formatNumber(props.startVal)
}

const countDown = computed(() => {
  return props.startVal > props.endVal
})

function formatNumber(num) {
  num = Number(num).toFixed(props.decimals)
  num += ''
  const x = num.split('.')
  let x1 = x[0]
  const x2 = x.length > 1 ? props.decimal + x[1] : ''
  const rgx = /(\d+)(\d{3})/
  if (props.separator && !isNumber(props.separator)) {
    while (rgx.test(x1)) {
      x1 = x1.replace(rgx, '$1' + props.separator + '$2')
    }
  }
  return props.prefix + x1 + x2 + props.suffix
}

function isNumber(val) {
  return !isNaN(parseFloat(val))
}

// Robert Penner's easeOutExpo
function easingFn(t, b, c, d) {
  return c * (-Math.pow(2, -10 * t / d) + 1) * 1024 / 1023 + b
}

const emit = defineEmits(['mounted', 'callback'])

watch(() => props.startVal, () => {
  if (props.autoplay) {
    start()
  }
})

watch(() => props.endVal, () => {
  if (props.autoplay) {
    start()
  }
})

onMounted(() => {
  if (props.autoplay) {
    start()
  }
})

onUnmounted(() => {
  cancelAnimationFrame(rAF.value)
})
</script>
