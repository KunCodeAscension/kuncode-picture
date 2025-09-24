<template>
  <div id="globalHeader">
    <a-row :wrap="false">
      <a-col flex="200px">
        <router-link to="/">
          <div class="title-bar">
            <img class="logo" src="../assets/logo.svg" alt="logo" />
            <div class="title">坤码云图库</div>
          </div>
        </router-link>
      </a-col>
      <a-col flex="auto">
        <a-menu v-model:selectedKeys="current" mode="horizontal" :items="items" @click="doMenuClick"/>
      </a-col>
      <a-col flex="120px">
        <div class="user-login-status">
          <a-button href="/user/login" type="primary">登录</a-button>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import { h, ref } from 'vue';
import { HomeOutlined } from '@ant-design/icons-vue';
import type { MenuProps } from 'ant-design-vue';
import { useRouter }from "vue-router";
const items = ref<MenuProps['items']>([
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/about',
    label: '关于',
    title: '关于',
  },
  {
    key: '/others',
    label: '坤码飞升',
    title: '坤码飞升',
  }
]);
const router =useRouter();
//路由跳转事件
const doMenuClick = ({key}: {key: string})=>{
  router.push({
    path: key,
    });
};

const current = ref<string[]>([]);
router.afterEach((to, from, next) => {
  current.value =  [to.path];
})
</script>
<style scoped>
.title-bar {
  display: flex;
  align-items: center;
}
.title {
  color: black;
  font-size: 18px;
  margin-left: 16px;
}
.logo {
  height: 24px;
}
</style>
