/* eslint-disable prefer-template */

import DefaultLayout from '@/layout/default-layout.vue';
import { RouteRecordRaw } from 'vue-router';

export default [
  {
    path: '/',
    redirect: `${import.meta.env.VITE_CONTEXT}board`,
  },
  {
    path: import.meta.env.VITE_CONTEXT,
    redirect: { path: `${import.meta.env.VITE_CONTEXT}board` },
  },
  {
    name: 'redirect',
    path: import.meta.env.VITE_CONTEXT + 'redirect',
    component: () => import('@/views/redirect.vue'),
  },
] as RouteRecordRaw[];
