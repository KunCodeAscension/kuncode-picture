<template>
  <a-form layout="inline" :model="searchParams" @finish="doSearch">
    <a-form-item label="关键词">
      <a-input v-model:value="searchParams.searchText" placeholder="从名称和简介搜索" allow-clear />
    </a-form-item>
    <a-form-item label="类型">
      <a-input v-model:value="searchParams.category" placeholder="请输入类型" allow-clear />
    </a-form-item>
    <a-form-item label="标签">
      <a-select
        v-model:value="searchParams.tags"
        mode="tags"
        style="min-width: 200px"
        placeholder="请输入标签"
        allowClear
      />
    </a-form-item>
    <a-form-item>
      <a-button type="primary" html-type="submit">搜索</a-button>
    </a-form-item>
  </a-form>
  <div style="margin-bottom: 16px"></div>
  <a-table
    :columns="columns"
    :data-source="dataList"
    :pagination="pagination"
    @change="doTableChange"
    :scroll="{ x: 'max-content' }"
  >
  <template #bodyCell="{ column, record }">
    <template v-if="column.dataIndex === 'PictureName'">
      <a-tag color="orange">{{ record.PictureName }}</a-tag>
    </template>
    <template v-else-if="column.dataIndex === 'url'">
      <a-image :src="record.url" :width="60" />
    </template>
    <template v-else-if="column.dataIndex === 'tags'">
      <a-space wrap>
        <a-tag color="green" v-for="tag in JSON.parse(record.tags || '[]')" :key="tag">
          {{ tag }}
        </a-tag>
      </a-space>
    </template>
    <template v-else-if="column.dataIndex === 'picInfo'">
      <div>格式: {{ record.picFormat }}</div>
      <div>宽度:{{ record.picWidth }}</div>
      <div>高度:{{ record.picHeight }}</div>
      <div>宽高比:{{ record.picScale }}</div>
      <div>大小:{{ record.picSize }}KB</div>
    </template>
    <template v-else-if="column.dataIndex === 'createTime'">
      {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
    </template>
    <template v-else-if="column.dataIndex === 'editTime'">
      {{ dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss') }}
    </template>
    <template v-else-if="column.key === 'action'">
      <a-button type="link" :href="`/add_picture?id=${record.id}`" target="_blank">编辑</a-button>
      <a-button danger @click="doDelete(record.id)">删除</a-button>
    </template>
  </template>
  </a-table>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { deletePictureUsingPost, listPictureByPageUsingPost } from '@/api/pictureController.ts'

// 表格列定义
const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '图片',
    dataIndex: 'url',
  },
  {
    title: '名称',
    dataIndex: 'name',
  },
  {
    title: '简介',
    dataIndex: 'introduction',
    ellipsis: true,
  },
  {
    title: '类型',
    dataIndex: 'category',
  },
  {
    title: '标签',
    dataIndex: 'tags',
  },
  {
    title: '图片信息',
    dataIndex: 'picInfo',
  },
  {
    title: '用户 id',
    dataIndex: 'userId',
    width: 80,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '编辑时间',
    dataIndex: 'editTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 表格数据
const dataList = ref([])
const total = ref(0)

// 搜索参数
const searchParams = reactive<API.PictureQueryRequest>({
  page: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 分页配置
const pagination = computed(() => {
  return {
    current: searchParams.page ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total) => `共 ${total} 条`,
  }
})
// 获取数据
const fetchData = async () => {
  const res = await listPictureByPageUsingPost({
    ...searchParams,
  })
  if (res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 页面加载时获取数据
onMounted(() => {
  fetchData()
})

// 搜索
const doSearch = () => {
  searchParams.page = 1
  fetchData()
}

// 表格分页变化
const doTableChange = (page: any) => {
  searchParams.page = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 删除
const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
  } else {
    message.error('删除失败')
  }
}
</script>
