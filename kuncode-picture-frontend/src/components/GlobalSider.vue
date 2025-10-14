<template>
  <div id="globalSider">
    <a-layout-sider v-if="loginUser.loginUser.id" width="200" breakpoint="lg" collapsed-width="0">
      <a-menu mode="inline" v-model:selectedKeys="current" :items="originItems" @click="doMenuClick" />
    </a-layout-sider>
  </div>
</template>

<script lang="ts" setup>
import { h, ref } from 'vue'
import { PictureOutlined,UserOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
const originItems = [
  {
    key: '/',
    icon: () => h(PictureOutlined),
    label: '公共图库'
  },
  {
    key: '/my_space',
    icon: () => h(UserOutlined),
    label: '我的空间',
  }
]

const loginUser = useLoginUserStore()

const router = useRouter()
//路由跳转事件
const doMenuClick = ({ key }: { key: string }) => {
  router.push({
    path: key,
  })
}
const current = ref<string[]>([])
router.afterEach((to) => {
  current.value = [to.path]
})
</script>
<style scoped>
#globalSider .ant-layout-sider{
  background: #fff;
}
</style>

