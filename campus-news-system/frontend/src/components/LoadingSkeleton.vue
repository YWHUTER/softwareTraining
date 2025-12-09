<template>
  <div class="animate-pulse space-y-4">
    <!-- Type: Card (Default) -->
    <div v-if="type === 'card'" class="bg-white rounded-xl overflow-hidden p-4 border border-gray-100">
      <div class="h-48 bg-gray-200 rounded-lg mb-4"></div>
      <div class="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
      <div class="h-4 bg-gray-200 rounded w-1/2 mb-4"></div>
      <div class="flex justify-between items-center mt-4">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 rounded-full bg-gray-200"></div>
          <div class="h-3 w-16 bg-gray-200 rounded"></div>
        </div>
        <div class="h-3 w-12 bg-gray-200 rounded"></div>
      </div>
    </div>

    <!-- Type: List Item -->
    <div v-else-if="type === 'list'" class="flex gap-4 p-4 bg-white rounded-lg border border-gray-100">
      <div class="w-24 h-24 bg-gray-200 rounded-lg flex-shrink-0"></div>
      <div class="flex-1 space-y-2 py-1">
        <div class="h-4 bg-gray-200 rounded w-3/4"></div>
        <div class="h-4 bg-gray-200 rounded w-1/2"></div>
        <div class="h-3 bg-gray-200 rounded w-full mt-4"></div>
      </div>
    </div>

    <!-- Type: Article Detail -->
    <div v-else-if="type === 'article'" class="space-y-6">
      <div class="h-8 bg-gray-200 rounded w-3/4"></div>
      <div class="flex gap-4">
        <div class="h-4 bg-gray-200 rounded w-20"></div>
        <div class="h-4 bg-gray-200 rounded w-20"></div>
      </div>
      <div class="h-64 bg-gray-200 rounded-xl w-full"></div>
      <div class="space-y-3">
        <div class="h-4 bg-gray-200 rounded w-full"></div>
        <div class="h-4 bg-gray-200 rounded w-full"></div>
        <div class="h-4 bg-gray-200 rounded w-5/6"></div>
        <div class="h-4 bg-gray-200 rounded w-full"></div>
      </div>
    </div>
    
    <!-- Type: Stats -->
    <div v-else-if="type === 'stats'" class="grid grid-cols-4 gap-4">
      <div v-for="i in 4" :key="i" class="h-24 bg-gray-200 rounded-xl"></div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'card',
    validator: (value) => ['card', 'list', 'article', 'stats'].includes(value)
  }
})
</script>
