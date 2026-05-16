<template>
  <div class="task-list">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span style="float: left; font-size: 18px; font-weight: bold;">任务列表</span>
        <div style="float: right;">
          <el-button type="primary" @click="showUploadDialog">
            <i class="el-icon-upload"></i> 新建导入任务
          </el-button>
          <el-button type="info" @click="$router.push('/cache')" style="margin-left: 10px;">
            <i class="el-icon-date"></i> 查看缓存数据
          </el-button>
        </div>
      </div>

      <el-table :data="tasks" v-loading="loading" style="width: 100%">
        <el-table-column prop="taskName" label="任务名称" width="150"></el-table-column>
        <el-table-column prop="taskType" label="任务类型" width="120"></el-table-column>
        <el-table-column prop="fileName" label="文件名" width="180"></el-table-column>
        <el-table-column label="文件大小" width="100">
          <template slot-scope="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column label="执行阶段" width="420">
          <template slot-scope="scope">
            <div class="phase-container">
              <div class="phase-item" :class="getPhaseClass(scope.row.status, 'PENDING', 'PARSING')">
                <i :class="getPhaseIcon(scope.row.status, 'PENDING', 'PARSING')"></i>
                <span class="phase-text">解析</span>
              </div>
              <div class="phase-divider" :class="{ active: isPhaseAfter(scope.row.status, 'PENDING') }"></div>
              <div class="phase-item" :class="getPhaseClass(scope.row.status, 'PARSING', 'VALIDATING')">
                <i :class="getPhaseIcon(scope.row.status, 'PARSING', 'VALIDATING')"></i>
                <span class="phase-text">校验</span>
              </div>
              <div class="phase-divider" :class="{ active: isPhaseAfter(scope.row.status, 'PARSING') }"></div>
              <div class="phase-item" :class="getPhaseClass(scope.row.status, 'VALIDATING', 'DEDUPLICATING')">
                <i :class="getPhaseIcon(scope.row.status, 'VALIDATING', 'DEDUPLICATING')"></i>
                <span class="phase-text">去重</span>
              </div>
              <div class="phase-divider" :class="{ active: isPhaseAfter(scope.row.status, 'DEDUPLICATING') }"></div>
              <div class="phase-item" :class="getPhaseClass(scope.row.status, 'DEDUPLICATING', 'CACHING')">
                <i :class="getPhaseIcon(scope.row.status, 'DEDUPLICATING', 'CACHING')"></i>
                <span class="phase-text">入缓存</span>
              </div>
              <div class="phase-divider" :class="{ active: isPhaseAfter(scope.row.status, 'CACHING') }"></div>
              <div class="phase-item" :class="getPhaseClass(scope.row.status, 'CACHING', 'STATISTICS')">
                <i :class="getPhaseIcon(scope.row.status, 'CACHING', 'STATISTICS')"></i>
                <span class="phase-text">统计</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="统计" width="240">
          <template slot-scope="scope">
            <span>总计: {{ scope.row.totalCount || 0 }}</span> /
            <span style="color: #67c23a;">成功: {{ scope.row.successCount || 0 }}</span> /
            <el-tooltip content="点击查看错误详情" placement="top" :disabled="!scope.row.failedCount">
              <span 
                @click="scope.row.failedCount > 0 && viewDetailWithError(scope.row.id)"
                :style="{ color: scope.row.failedCount > 0 ? '#f56c6c' : '#909399', cursor: scope.row.failedCount > 0 ? 'pointer' : 'default', fontWeight: scope.row.failedCount > 0 ? 'bold' : 'normal' }">
                失败: {{ scope.row.failedCount || 0 }}
                <i v-if="scope.row.failedCount > 0" class="el-icon-right" style="margin-left: 2px; font-size: 12px;"></i>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="viewDetail(scope.row.id)">
              详情
            </el-button>
            <el-button
              size="mini"
              type="danger"
              @click="viewDetailWithError(scope.row.id)"
              v-if="scope.row.failedCount > 0">
              <i class="el-icon-document-remove"></i>
              错误
            </el-button>
            <el-button
              size="mini"
              type="warning"
              @click="retryTask(scope.row)"
              v-if="scope.row.status === 'FAILED'"
              class="retry-button">
              <i class="el-icon-refresh-left"></i>
              重试
            </el-button>
            <el-button
              size="mini"
              :type="getRunningButtonType(scope.row.status)"
              disabled
              v-if="isTaskRunning(scope.row.status)">
              <i class="el-icon-loading"></i>
              {{ getRunningStatusText(scope.row.status) }}
            </el-button>
            <el-button size="mini" type="info" @click="deleteTask(scope.row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog title="新建导入任务" :visible.sync="uploadVisible" width="650px">
      <el-form :model="uploadForm" label-width="120px">
        <el-form-item label="任务名称">
          <el-input v-model="uploadForm.taskName" placeholder="请输入任务名称"></el-input>
        </el-form-item>
        <el-form-item label="任务类型" :class="{ 'has-error': taskTypeLocked }">
          <div style="position: relative;">
            <el-input 
              v-model="uploadForm.taskType" 
              placeholder="请输入任务类型（如：user、order）"
              @input="debounceCheckTaskType"
              :disabled="taskTypeLocked">
              <i slot="suffix" v-if="taskTypeChecking" class="el-icon-loading" style="color: #409eff;"></i>
              <i slot="suffix" v-else-if="taskTypeLocked" class="el-icon-lock" style="color: #f56c6c; cursor: pointer;" title="该类型任务正在执行中"></i>
            </el-input>
          </div>
          <div v-if="taskTypeLocked" class="task-type-lock-warning">
            <i class="el-icon-warning"></i>
            <span>该类型任务正在执行中，请等待完成后再提交</span>
          </div>
        </el-form-item>
        <el-form-item label="去重策略">
          <el-select v-model="uploadForm.deduplicateStrategy" placeholder="请选择去重策略" :disabled="taskTypeLocked">
            <el-option label="跳过重复" value="SKIP"></el-option>
            <el-option label="覆盖重复" value="OVERWRITE"></el-option>
            <el-option label="合并数据" value="MERGE"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="uploadForm.priority" placeholder="请选择优先级" :disabled="taskTypeLocked">
            <el-option label="高" value="HIGH"></el-option>
            <el-option label="中" value="MEDIUM"></el-option>
            <el-option label="低" value="LOW"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            ref="upload"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            :disabled="taskTypeLocked"
            accept=".csv,.xlsx,.xls">
            <el-button slot="trigger" size="small" type="primary" :disabled="taskTypeLocked">选取文件</el-button>
            <div slot="tip" class="el-upload__tip">支持 CSV、Excel 文件</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="submitUpload" 
          :loading="uploading"
          :disabled="taskTypeLocked">
          {{ taskTypeLocked ? '类型任务执行中' : '开始导入' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getTaskList, uploadTask, retryTask, deleteTask, checkTaskTypeRunning } from '../api/task'

export default {
  name: 'TaskList',
  data() {
    return {
      tasks: [],
      loading: false,
      uploadVisible: false,
      uploading: false,
      taskTypeLocked: false,
      taskTypeChecking: false,
      checkTaskTypeTimer: null,
      uploadForm: {
        taskName: '',
        taskType: '',
        deduplicateStrategy: 'SKIP',
        priority: 'MEDIUM'
      },
      selectedFile: null,
      refreshTimer: null
    }
  },
  created() {
    this.fetchTaskList()
    this.startAutoRefresh()
  },
  beforeDestroy() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
    }
    if (this.checkTaskTypeTimer) {
      clearTimeout(this.checkTaskTypeTimer)
    }
  },
  methods: {
    fetchTaskList() {
      this.loading = true
      getTaskList().then(res => {
        this.tasks = res
      }).finally(() => {
        this.loading = false
      })
    },
    startAutoRefresh() {
      this.refreshTimer = setInterval(() => {
        this.fetchTaskList()
        if (this.uploadVisible && this.uploadForm.taskType && this.taskTypeLocked) {
          this.checkTaskType()
        }
      }, 3000)
    },
    showUploadDialog() {
      this.uploadVisible = true
      this.taskTypeLocked = false
      this.taskTypeChecking = false
      this.uploadForm = {
        taskName: '',
        taskType: '',
        deduplicateStrategy: 'SKIP',
        priority: 'MEDIUM'
      }
      this.selectedFile = null
      if (this.$refs.upload) {
        this.$refs.upload.clearFiles()
      }
    },
    debounceCheckTaskType() {
      if (this.checkTaskTypeTimer) {
        clearTimeout(this.checkTaskTypeTimer)
      }
      if (!this.uploadForm.taskType) {
        this.taskTypeLocked = false
        return
      }
      this.checkTaskTypeTimer = setTimeout(() => {
        this.checkTaskType()
      }, 300)
    },
    checkTaskType() {
      if (!this.uploadForm.taskType) {
        this.taskTypeLocked = false
        return
      }
      this.taskTypeChecking = true
      checkTaskTypeRunning(this.uploadForm.taskType).then(res => {
        this.taskTypeLocked = res.isRunning
      }).finally(() => {
        this.taskTypeChecking = false
      })
    },
    handleFileChange(file) {
      this.selectedFile = file.raw
    },
    submitUpload() {
      if (!this.uploadForm.taskName) {
        this.$message.warning('请输入任务名称')
        return
      }
      if (!this.uploadForm.taskType) {
        this.$message.warning('请输入任务类型')
        return
      }
      if (!this.selectedFile) {
        this.$message.warning('请选择文件')
        return
      }

      this.uploading = true
      const formData = new FormData()
      formData.append('file', this.selectedFile)
      formData.append('taskName', this.uploadForm.taskName)
      formData.append('taskType', this.uploadForm.taskType)
      formData.append('deduplicateStrategy', this.uploadForm.deduplicateStrategy)
      formData.append('priority', this.uploadForm.priority)

      uploadTask(formData).then(() => {
        this.$message.success('任务创建成功')
        this.uploadVisible = false
        this.fetchTaskList()
      }).finally(() => {
        this.uploading = false
      })
    },
    viewDetail(id) {
      this.$router.push(`/task/${id}`)
    },
    viewDetailWithError(id) {
      this.$router.push(`/task/${id}?showError=true`)
    },
    retryTask(task) {
      this.$confirm(
        `确定要重试任务「${task.taskName}」吗？\n重试后将重新解析并导入数据，成功数据不会重复写入。`, 
        '重试确认', {
          confirmButtonText: '立即重试',
          cancelButtonText: '取消',
          type: 'warning',
          distinguishCancelAndClose: true
      }).then(() => {
        const loading = this.$loading({
          lock: true,
          text: '正在提交重试...',
          spinner: 'el-icon-loading',
          background: 'rgba(255, 255, 255, 0.7)'
        })
        
        retryTask(task.id).then(() => {
          loading.close()
          this.$message({
            message: '重试任务已提交，正在执行中...',
            type: 'success',
            duration: 3000
          })
          this.fetchTaskList()
        }).catch(err => {
          loading.close()
          this.$message.error(err.message || '重试提交失败，请稍后重试')
        })
      }).catch(() => {})
    },
    isTaskRunning(status) {
      const runningStatuses = ['PENDING', 'QUEUED', 'PARSING', 'VALIDATING', 'DEDUPLICATING', 'CACHING', 'STATISTICS']
      return runningStatuses.includes(status)
    },
    getRunningButtonType(status) {
      if (status === 'QUEUED') return 'info'
      return ''
    },
    getRunningStatusText(status) {
      const map = {
        'PENDING': '等待中',
        'QUEUED': '排队中',
        'PARSING': '解析中',
        'VALIDATING': '校验中',
        'DEDUPLICATING': '去重中',
        'CACHING': '写入中',
        'STATISTICS': '统计中'
      }
      return map[status] || '执行中'
    },
    deleteTask(id) {
      this.$confirm('确定要删除该任务吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteTask(id).then(() => {
          this.$message.success('删除成功')
          this.fetchTaskList()
        })
      }).catch(() => {})
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
    getProgressPercent(progress) {
      if (!progress) return 0
      return parseInt(progress.replace('%', ''))
    },
    getProgressStatus(status) {
      if (status === 'SUCCESS') return 'success'
      if (status === 'FAILED') return 'exception'
      return null
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
    },
    formatFileSize(size) {
      if (!size) return '0B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(size) / Math.log(k))
      return (size / Math.pow(k, i)).toFixed(2) + sizes[i]
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      return date.toLocaleString('zh-CN')
    }
  }
}
</script>

<style scoped>
.task-list {
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

.task-type-lock-warning {
  display: flex;
  align-items: center;
  padding: 10px 15px;
  margin-top: 8px;
  background-color: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 13px;
  animation: fadeIn 0.3s ease-in-out;
}

.task-type-lock-warning i {
  margin-right: 8px;
  font-size: 16px;
}

.has-error >>> .el-form-item__label {
  color: #f56c6c;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.retry-button {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(230, 162, 60, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(230, 162, 60, 0);
  }
}
</style>
