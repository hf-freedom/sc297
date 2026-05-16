<template>
  <div class="task-detail">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span style="float: left; font-size: 18px; font-weight: bold;">任务详情</span>
        <div style="float: right;">
          <el-button 
            type="warning" 
            @click="retryTask" 
            v-if="task.status === 'FAILED'"
            style="margin-right: 10px;">
            <i class="el-icon-refresh-left"></i>
            重新执行
          </el-button>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </div>

      <el-descriptions :column="2" border v-loading="loading">
        <el-descriptions-item label="任务ID">{{ task.id }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ task.taskName }}</el-descriptions-item>
        <el-descriptions-item label="任务类型">{{ task.taskType }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ task.fileName }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatFileSize(task.fileSize) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(task.status)">{{ task.statusDesc }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="去重策略">{{ getStrategyText(task.deduplicateStrategy) }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ getPriorityText(task.priority) }}</el-descriptions-item>
        <el-descriptions-item label="重试次数">{{ task.retryCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(task.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDate(task.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatDate(task.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="执行阶段" :span="2">
          <div class="phase-container">
            <div class="phase-item" :class="getPhaseClass(task.status, 'PENDING', 'PARSING')">
              <i :class="getPhaseIcon(task.status, 'PENDING', 'PARSING')"></i>
              <span class="phase-text">解析</span>
            </div>
            <div class="phase-divider" :class="{ active: isPhaseAfter(task.status, 'PENDING') }"></div>
            <div class="phase-item" :class="getPhaseClass(task.status, 'PARSING', 'VALIDATING')">
              <i :class="getPhaseIcon(task.status, 'PARSING', 'VALIDATING')"></i>
              <span class="phase-text">校验</span>
            </div>
            <div class="phase-divider" :class="{ active: isPhaseAfter(task.status, 'PARSING') }"></div>
            <div class="phase-item" :class="getPhaseClass(task.status, 'VALIDATING', 'DEDUPLICATING')">
              <i :class="getPhaseIcon(task.status, 'VALIDATING', 'DEDUPLICATING')"></i>
              <span class="phase-text">去重</span>
            </div>
            <div class="phase-divider" :class="{ active: isPhaseAfter(task.status, 'DEDUPLICATING') }"></div>
            <div class="phase-item" :class="getPhaseClass(task.status, 'DEDUPLICATING', 'CACHING')">
              <i :class="getPhaseIcon(task.status, 'DEDUPLICATING', 'CACHING')"></i>
              <span class="phase-text">入缓存</span>
            </div>
            <div class="phase-divider" :class="{ active: isPhaseAfter(task.status, 'CACHING') }"></div>
            <div class="phase-item" :class="getPhaseClass(task.status, 'CACHING', 'STATISTICS')">
              <i :class="getPhaseIcon(task.status, 'CACHING', 'STATISTICS')"></i>
              <span class="phase-text">统计</span>
            </div>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2">
          <span style="color: #F56C6C;">{{ task.errorMessage }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">数据统计</el-divider>

      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover">
            <div style="text-align: center;">
              <div style="font-size: 36px; color: #409EFF; font-weight: bold;">{{ task.totalCount || 0 }}</div>
              <div style="color: #909399; margin-top: 10px;">总数据量</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div style="text-align: center;">
              <div style="font-size: 36px; color: #67C23A; font-weight: bold;">{{ task.successCount || 0 }}</div>
              <div style="color: #909399; margin-top: 10px;">成功导入</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div style="text-align: center;">
              <div style="font-size: 36px; color: #F56C6C; font-weight: bold;">{{ task.failedCount || 0 }}</div>
              <div style="color: #909399; margin-top: 10px;">失败数据</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div style="text-align: center;">
              <div style="font-size: 36px; color: #E6A23C; font-weight: bold;">{{ task.duplicateCount || 0 }}</div>
              <div style="color: #909399; margin-top: 10px;">重复数据</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-divider v-if="task.errorList && task.errorList.length > 0" content-position="left" class="error-list-divider">
        <span class="error-title">
          <i class="el-icon-document-remove"></i>
          失败数据列表 ({{ task.errorList.length }} 条)
        </span>
      </el-divider>

      <el-table 
        v-if="task.errorList && task.errorList.length > 0" 
        :data="task.errorList" 
        border 
        style="width: 100%"
        class="error-table"
        ref="errorTable">
        <el-table-column prop="rowIndex" label="行号" width="80"></el-table-column>
        <el-table-column prop="errorMessage" label="错误原因" width="200"></el-table-column>
        <el-table-column prop="data" label="数据内容">
          <template slot-scope="scope">
            <pre style="margin: 0; white-space: pre-wrap; word-wrap: break-word;">{{ JSON.stringify(scope.row.data, null, 2) }}</pre>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getTaskDetail, retryTask } from '../api/task'

export default {
  name: 'TaskDetail',
  data() {
    return {
      task: {},
      loading: false,
      retryLoading: false
    }
  },
  created() {
    this.fetchDetail()
  },
  mounted() {
    this.checkAndScrollToError()
  },
  updated() {
    this.checkAndScrollToError()
  },
  methods: {
    fetchDetail() {
      this.loading = true
      const taskId = this.$route.params.id
      getTaskDetail(taskId).then(res => {
        this.task = res
      }).finally(() => {
        this.loading = false
      })
    },
    retryTask() {
      this.$confirm(
        `确定要重试任务「${this.task.taskName}」吗？\n重试后将重新解析并导入数据，成功数据不会重复写入。`, 
        '重试确认', {
          confirmButtonText: '立即重试',
          cancelButtonText: '取消',
          type: 'warning',
          distinguishCancelAndClose: true
      }).then(() => {
        this.retryLoading = true
        retryTask(this.task.id).then(() => {
          this.$message({
            message: '重试任务已提交，正在执行中...',
            type: 'success',
            duration: 3000
          })
          this.fetchDetail()
        }).catch(err => {
          this.$message.error(err.message || '重试提交失败，请稍后重试')
        }).finally(() => {
          this.retryLoading = false
        })
      }).catch(() => {})
    },
    checkAndScrollToError() {
      if (this.$route.query.showError === 'true' && this.task.errorList && this.task.errorList.length > 0) {
        this.$nextTick(() => {
          const divider = document.querySelector('.error-list-divider')
          if (divider) {
            divider.scrollIntoView({ behavior: 'smooth', block: 'start' })
          }
        })
      }
    },
    getStatusType(status) {
      const map = {
        'PENDING': 'info',
        'QUEUED': 'info',
        'PARSING': '',
        'VALIDATING': '',
        'DEDUPLICATING': '',
        'CACHING': '',
        'STATISTICS': '',
        'SUCCESS': 'success',
        'FAILED': 'danger',
        'CANCELLED': 'info'
      }
      return map[status] || ''
    },
    getStrategyText(strategy) {
      const map = {
        'SKIP': '跳过重复',
        'OVERWRITE': '覆盖重复',
        'MERGE': '合并数据'
      }
      return map[strategy] || strategy
    },
    getPriorityText(priority) {
      const map = {
        'HIGH': '高',
        'MEDIUM': '中',
        'LOW': '低'
      }
      return map[priority] || priority
    },
    formatFileSize(size) {
      if (!size) return '0B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(size) / Math.log(k))
      return (size / Math.pow(k, i)).toFixed(2) + sizes[i]
    },
    formatDate(dateStr) {
      if (!dateStr) return '-'
      const date = new Date(dateStr)
      return date.toLocaleString('zh-CN')
    },
    getPhaseOrder() {
      return ['PENDING', 'PARSING', 'VALIDATING', 'DEDUPLICATING', 'CACHING', 'STATISTICS', 'SUCCESS', 'FAILED', 'CANCELLED']
    },
    getPhaseClass(status, prevPhase, currentPhase) {
      const order = this.getPhaseOrder()
      const statusIndex = order.indexOf(status)
      const currentIndex = order.indexOf(currentPhase)
      const prevIndex = order.indexOf(prevPhase)
      
      if (status === 'FAILED' || status === 'CANCELLED') {
        if (statusIndex > prevIndex && statusIndex < currentIndex) {
          return 'phase-processing'
        } else if (statusIndex >= currentIndex) {
          return 'phase-error'
        }
        return ''
      }
      
      if (status === 'SUCCESS') {
        return 'phase-success'
      }
      
      if (statusIndex >= currentIndex) {
        return 'phase-success'
      } else if (statusIndex > prevIndex && statusIndex < currentIndex) {
        return 'phase-processing'
      }
      return ''
    },
    getPhaseIcon(status, prevPhase, currentPhase) {
      const order = this.getPhaseOrder()
      const statusIndex = order.indexOf(status)
      const currentIndex = order.indexOf(currentPhase)
      const prevIndex = order.indexOf(prevPhase)
      
      if (status === 'SUCCESS') {
        return 'el-icon-check phase-icon-success'
      }
      
      if (status === 'FAILED' || status === 'CANCELLED') {
        if (statusIndex > prevIndex && statusIndex < currentIndex) {
          return 'el-icon-loading phase-icon-loading'
        } else if (statusIndex >= currentIndex) {
          return 'el-icon-close phase-icon-error'
        }
        return ''
      }
      
      if (statusIndex >= currentIndex) {
        return 'el-icon-check phase-icon-success'
      } else if (statusIndex > prevIndex && statusIndex < currentIndex) {
        return 'el-icon-loading phase-icon-loading'
      }
      return ''
    },
    isPhaseAfter(status, phase) {
      const order = this.getPhaseOrder()
      const statusIndex = order.indexOf(status)
      const phaseIndex = order.indexOf(phase)
      return statusIndex > phaseIndex || status === 'SUCCESS'
    }
  }
}
</script>

<style scoped>
.task-detail {
  width: 100%;
}

.box-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.phase-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 0;
  width: 100%;
}

.phase-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 50px;
}

.phase-item i {
  font-size: 16px;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  background-color: #f0f0f0;
  color: #909399;
  margin-bottom: 4px;
}

.phase-text {
  font-size: 12px;
  color: #909399;
}

.phase-divider {
  flex: 1;
  height: 2px;
  background-color: #f0f0f0;
  margin: 0 5px;
  margin-bottom: 18px;
}

.phase-divider.active {
  background-color: #67c23a;
}

.phase-item.phase-success i {
  background-color: #67c23a;
  color: #fff;
}

.phase-item.phase-success .phase-text {
  color: #67c23a;
}

.phase-item.phase-processing i {
  background-color: #409eff;
  color: #fff;
}

.phase-item.phase-processing .phase-text {
  color: #409eff;
}

.phase-item.phase-error i {
  background-color: #f56c6c;
  color: #fff;
}

.phase-item.phase-error .phase-text {
  color: #f56c6c;
}

.phase-icon-loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.error-list-divider {
  border-color: #f56c6c !important;
}

.error-list-divider .el-divider__text {
  background-color: #fef0f0 !important;
  color: #f56c6c !important;
  font-weight: bold;
}

.error-title {
  display: flex;
  align-items: center;
  font-size: 16px;
}

.error-title i {
  margin-right: 8px;
  font-size: 18px;
}

.error-table {
  box-shadow: 0 2px 12px 0 rgba(245, 108, 108, 0.15);
}

.error-table th {
  background-color: #fef0f0 !important;
  color: #f56c6c !important;
}
</style>
