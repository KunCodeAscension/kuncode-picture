<template>
  <div id="MyPublicPicturePage">
    <div>
      <a-radio-group v-model:value="reviewStatus" button-style="solid">
        <a-radio-button :value="1">已通过</a-radio-button>
        <a-radio-button :value="0">审核中</a-radio-button>
        <a-radio-button :value="2">未通过</a-radio-button>
      </a-radio-group>
    </div>
    <div style="margin-bottom: 20px" />
    <a-form name="searchForm" layout="inline" :model="searchParams" @finish="onSearch">
      <a-form-item label="关键词" name="searchText">
        <a-input
          v-model:value="searchParams.searchText"
          placeholder="从名称和简介搜索"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="名称" name="name">
        <a-input v-model:value="searchParams.name" placeholder="请输入名称" allow-clear />
      </a-form-item>
      <a-form-item label="简介" name="introduction">
        <a-input v-model:value="searchParams.introduction" placeholder="请输入简介" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" html-type="submit" style="width: 96px">搜索</a-button>
          <a-button html-type="reset" @click="doClear">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>
    <div style="margin-bottom: 20px" />
    <PictureList :dataList="dataList" :loading="loading" :showOp="false" :onReload="fetchData" />
    <!-- 分页 -->
    <a-pagination
      style="text-align: right"
      v-model:current="searchParams.page"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      @change="onPageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { listPictureVoByPageUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import PictureList from '@/components/PicyureList.vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
const reviewStatus = ref<number>(1)
const loading = ref<boolean>(false)
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)

const DEFAULT_SEARCH_PARAMS: API.PictureQueryRequest = {
  page: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
  reviewStatus: 1, // 初始默认已审核
  searchText: '', // 补充表单字段默认值，避免undefined
  name: '',
  introduction: '',
}
const searchParams = ref<API.PictureQueryRequest>({ ...DEFAULT_SEARCH_PARAMS })

const onPageChange = (page: number, pageSize: number) => {
  searchParams.value.page = page
  searchParams.value.pageSize = pageSize
  fetchData()
}

watch(reviewStatus, () => {
  onSearch() // 不需要传参，直接触发
})

// 搜索
const onSearch = () => {
  searchParams.value = {
    ...searchParams.value,
    reviewStatus: reviewStatus.value,
    userId: useLoginUserStore().loginUser.id,
    page: 1
  }
  fetchData()
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    ...searchParams.value,
  }
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

const doClear = () => {
  searchParams.value = {
    ...DEFAULT_SEARCH_PARAMS,
    reviewStatus: reviewStatus.value,
  }
  onSearch()
}

onMounted(() => {
  onSearch()
})
</script>

<style scoped></style>
