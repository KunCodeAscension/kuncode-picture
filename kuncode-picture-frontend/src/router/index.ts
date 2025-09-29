import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '主页',
      component: HomePage,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: UserManagePage,
    },
    // 添加外部链接跳转的路由配置
    {
      path: '/others',
      name: '坤码飞升',
      // 添加空组件以满足类型要求
      component: () => null,
      // 使用beforeEnter实现外部链接跳转
      beforeEnter(to, from, next) {
        window.location.href = 'https://www.kuncodeascension.fun/'
        next(false) // 阻止默认路由行为
      }
    }
  ],
})

export default router
