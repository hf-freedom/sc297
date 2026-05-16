<template>
  <div id="app">
    <el-container style="height: 100vh; border: 1px solid #eee;">
      <el-header style="background: #409EFF; color: #fff; font-size: 20px; font-weight: bold; line-height: 60px; padding: 0 20px;">
        数据导入任务系统
      </el-header>
      <el-main style="background-color: #f5f7fa; padding: 20px;">
        <router-view/>
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}
</style>
