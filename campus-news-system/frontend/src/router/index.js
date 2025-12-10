import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue')
      },
      {
        path: '/follow',
        name: 'Follow',
        component: () => import('@/views/Follow.vue'),
        meta: { requireAuth: true }
      },
      {
        path: '/board/:type',
        name: 'Board',
        component: () => import('@/views/Board.vue')
      },
      {
        path: '/article/:id',
        name: 'ArticleDetail',
        component: () => import('@/views/ArticleDetail.vue')
      },
      {
        path: '/publish',
        name: 'Publish',
        component: () => import('@/views/Publish.vue'),
        meta: { requireAuth: true }
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { requireAuth: true }
      },
      {
        path: '/ai-assistant',
        name: 'AiAssistant',
        component: () => import('@/views/AiAssistant.vue'),
        meta: { requireAuth: true }
      },
      {
        path: '/about',
        name: 'About',
        component: () => import('@/views/About.vue')
      },
      {
        path: '/contact',
        name: 'Contact',
        component: () => import('@/views/Contact.vue')
      },
      {
        path: '/privacy',
        name: 'Privacy',
        component: () => import('@/views/Privacy.vue')
      },
      {
        path: '/search',
        name: 'Search',
        component: () => import('@/views/Search.vue')
      },
      {
        path: '/ai-help',
        name: 'AiHelp',
        component: () => import('@/views/AiHelp.vue')
      },
      {
        path: '/ai-summary',
        name: 'AiSummary',
        component: () => import('@/views/AiSummary.vue'),
        meta: { requireAuth: true }
      },
      {
        path: '/ai-sentiment',
        name: 'AiSentiment',
        component: () => import('@/views/AiSentiment.vue'),
        meta: { requireAuth: true }
      },
      {
        path: '/comments',
        name: 'Comments',
        component: () => import('@/views/Comments.vue'),
        meta: { requireAuth: true }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requireAuth: true, requireAdmin: true },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue')
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue')
      },
      {
        path: 'articles',
        name: 'AdminArticles',
        component: () => import('@/views/admin/Articles.vue')
      },
      {
        path: 'colleges',
        name: 'AdminColleges',
        component: () => import('@/views/admin/Colleges.vue')
      }
    ]
  },
  {
    path: '/data-screen',
    name: 'DataScreen',
    component: () => import('@/views/admin/DataScreen.vue'),
    meta: { requireAuth: true, requireAdmin: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  
  // 未登录用户访问首页，重定向到登录页
  if (to.path === '/' && !token) {
    next('/login')
    return
  }
  
  if (to.meta.requireAuth && !token) {
    next('/login')
  } else if (to.meta.requireAdmin) {
    const isAdmin = user?.roles?.some(role => role.roleName === 'ADMIN')
    if (!isAdmin) {
      next('/')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
