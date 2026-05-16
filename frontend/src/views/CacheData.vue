<template>
  <div class="cache-data">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span style="float: left; font-size: 18px; font-weight: bold;">缓存数据</span>
        <el-button style="float: right;" @click="$router.back()">返回</el-button>
      </div>

      <el-alert
        title="当前缓存数据量"
        :description="'共 ' + cacheSize + ' 条数据'"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;">
      </el-alert>

      <el-table :data="cacheData" v-loading="loading" border style="width: 100%" max-height="600">
        <el-table-column v-for="(value, key) in sampleItem" :key="key" :label="key" :min-width="150">
          <template slot-scope="scope">
            {{ scope.row[key] }}
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && cacheData.length === 0" description="暂无缓存数据"></el-empty>
    </el-card>
  </div>
</template>

<script>
import { getCacheData, getCacheSize } from '../api/task'

export default {
  name: 'CacheData',
  data() {
    return {
      cacheData: [],
      cacheSize: 0,
      loading: false
    }
  },
  computed: {
    sampleItem() {
      if (this.cacheData.length > 0) {
        return this.cacheData[0]
      }
      return {}
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      this.loading = true
      Promise.all([getCacheData(), getCacheSize()]).then(([data, size]) => {
        this.cacheData = data
        this.cacheSize = size
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped>
.cache-data {
  width: 100%;
}

.box-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style>
