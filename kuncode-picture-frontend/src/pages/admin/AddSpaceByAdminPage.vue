<template>
  <div id="addSpacePage">
    <h2 style="margin-bottom: 16px">管理员创建空间</h2>
    <!-- 空间信息表单 -->
    <a-form name="spaceForm" layout="vertical" :model="spaceForm" @finish="handleSubmit">
      <a-form-item name="spaceName" label="空间名称">
        <a-input v-model:value="spaceForm.spaceName" placeholder="请输入空间" allow-clear />
      </a-form-item>
      <a-form-item name="userId" label="用户ID">
        <a-input v-model:value="spaceForm.userId" placeholder="请输入被创建的用户ID" allow-clear />
      </a-form-item>
      <a-form-item name="spaceLevel" label="空间类型">
        <a-select
          v-model:value="spaceForm.spaceType"
          style="min-width: 180px"
          placeholder="请选择空间类型"
          :options="SPACE_TYPE_OPTIONS"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="spaceLevel" label="空间级别">
        <a-select
          v-model:value="spaceForm.spaceLevel"
          style="min-width: 180px"
          placeholder="请选择空间级别"
          :options="SPACE_LEVEL_OPTIONS"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading" style="width: 100%">
          提交
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref } from 'vue'
import { SPACE_LEVEL_OPTIONS, SPACE_TYPE_OPTIONS } from '@/constants/space.ts'
import { message } from 'ant-design-vue'
import {
  addSpaceByAdminUsingPost,
  listSpaceLevelUsingGet,
} from '@/api/spaceController.ts'
import { useRouter } from 'vue-router'

const spaceForm = reactive<API.SpaceAddByAdminRequest>({})
const loading = ref(false)

const spaceLevelList = ref<API.SpaceLevel[]>([])

// 获取空间级别
const fetchSpaceLevelList = async () => {
  const res = await listSpaceLevelUsingGet()
  if (res.data.code === 0 && res.data.data) {
    spaceLevelList.value = res.data.data
  } else {
    message.error('获取空间级别失败，' + res.data.message)
  }
}

onMounted(() => {
  fetchSpaceLevelList()
})

const router = useRouter()

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  loading.value = true
  const res = await addSpaceByAdminUsingPost({
    ...spaceForm
  })
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success('操作成功')
    // 跳转到空间详情页
    router.push({
      path: `/admin/spaceManage`,
    })
  } else {
    message.error('操作失败，' + res.data.message)
  }
  loading.value = false
}
</script>

<style scoped>
#addSpacePage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
