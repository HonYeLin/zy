import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';

const routes = [
  {
    path: '/',
    name: 'Home',
    component: HomeView
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/admin/Dashboard.vue')
      },
      {
        path: 'animals',
        name: 'AnimalManage',
        component: () => import('../views/admin/AnimalManage.vue')
      },
      {
        path: 'comments',
        name: 'CommentManage',
        component: () => import('../views/admin/CommentManage.vue')
      },
      {
        path: 'images',
        name: 'ImageManage',
        component: () => import('../views/admin/ImageManage.vue')
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// Admin Route Guard
router.beforeEach((to, _from, next) => {
  if (to.path.startsWith('/admin')) {
    const token = localStorage.getItem('admin_token');
    if (!token && to.path !== '/admin/login') {
      // Prompt for login (we will do a simple modal or redirect to home for now, 
      // but let's just let it pass and we'll handle login in AdminLayout)
      next();
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router;
