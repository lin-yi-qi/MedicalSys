<template>
  <div class="appointment-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-clock page-icon"></i>
        <div>
          <h2 class="page-title">预约管理</h2>
          <p class="page-desc">统一查看预约记录，支持筛选、详情查看与取消待就诊预约。</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="search-wrap">
          <i class="fa-solid fa-magnifying-glass search-icon"></i>
          <el-input
            v-model="keyword"
            class="search-input"
            clearable
            placeholder="预约单号 / 时段"
            @clear="loadData"
            @keyup.enter="loadData"
          />
          <el-button class="search-btn" @click="loadData">
            <i class="fa-solid fa-search"></i>
            搜索
          </el-button>
          <el-select
            v-model="deptId"
            class="filter-select"
            clearable
            filterable
            placeholder="科室"
            size="large"
            style="width: 160px"
            @change="handleDeptChange"
          >
            <el-option v-for="d in deptOptions" :key="d.deptId" :label="d.name" :value="d.deptId" />
          </el-select>
          <el-select
            v-model="doctorId"
            class="filter-select"
            clearable
            filterable
            placeholder="医生"
            size="large"
            style="width: 200px"
            @change="loadData"
          >
            <el-option
              v-for="d in doctorOptions"
              :key="d.doctorId"
              :label="`${d.name || d.username} (${d.username})`"
              :value="d.doctorId"
            />
          </el-select>
          <el-date-picker
            v-model="dateFilter"
            class="filter-date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="就诊日期"
            size="large"
            @change="loadData"
          />
          <el-select
            v-model="statusFilter"
            class="filter-select"
            clearable
            placeholder="状态"
            size="large"
            style="width: 120px"
            @change="loadData"
          >
            <el-option :value="0" label="待支付" />
            <el-option :value="1" label="待就诊" />
            <el-option :value="2" label="已就诊" />
            <el-option :value="3" label="已取消" />
            <el-option :value="4" label="爽约" />
          </el-select>
        </div>
        <el-button class="btn-secondary" @click="handleExpireOverdue">
          <i class="fa-solid fa-clock-rotate-left"></i>
          处理过期预约
        </el-button>
      </div>

      <div class="table-wrap" v-loading="loading" element-loading-text="加载中...">
      <el-table
        :data="tableData"
        class="data-table"
        :header-cell-style="headerCellStyle"
        :row-class-name="tableRowClassName"
      >
        <el-table-column prop="appointmentNo" label="预约单号" min-width="165">
          <template #default="{ row }">
            <span class="cell-id">{{ row.appointmentNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="deptName" label="科室" width="120" />
        <el-table-column prop="doctorName" label="医生" min-width="120">
          <template #default="{ row }">{{ row.doctorName || '-' }}{{ row.doctorTitle ? `（${row.doctorTitle}）` : '' }}</template>
        </el-table-column>
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column label="就诊时间" width="190" align="center">
          <template #default="{ row }">
            <span class="cell-time">{{ row.appointmentDate }} {{ row.timeSlot }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="feeAmount" label="费用" width="90" align="right">
          <template #default="{ row }">¥{{ row.feeAmount ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="100" align="center" />
        <el-table-column prop="createdTime" label="创建时间" width="165" align="center">
          <template #default="{ row }">
            <span class="cell-time">{{ row.createdTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">
              <i class="fa-solid fa-eye"></i> 详情
            </el-button>
            <el-button
              v-if="row.status === 1 && row.paid === 1 && !row.queueNo"
              link
              type="success"
              size="small"
              @click="checkinRow(row)"
            >
              <i class="fa-solid fa-check"></i> 签到
            </el-button>
            <el-button
              v-if="row.status === 1"
              link
              type="danger"
              size="small"
              @click="cancelRow(row)"
            >
              <i class="fa-solid fa-ban"></i> 取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
      </div>
    </div>

    <el-dialog
      v-model="detailVisible"
      width="560px"
      class="edit-dialog"
      align-center
      :append-to-body="true"
    >
      <template #header>
        <div class="edit-dialog-header">
          <i class="fa-solid fa-file-lines dialog-icon"></i>
          <div>
            <span class="dialog-title">预约详情</span>
            <span class="dialog-subtitle">查看预约单完整信息</span>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="预约单号" :span="2">{{ detail.appointmentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ detail.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ detail.doctorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ detail.patientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="就诊日期">{{ detail.appointmentDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时段">{{ detail.timeSlot || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="费用">¥{{ detail.feeAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">{{ detail.paid === 1 ? '已支付' : '未支付' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ detail.createdTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="edit-dialog-footer">
          <el-button class="btn-cancel" @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelAdminAppointment,
  getAdminAppointmentDetail,
  getAdminAppointmentPage,
  getDeptOptions,
  getDoctorPage,
  checkInAppointment,
  expireOverdueAppointments
} from '@/api/admin'

const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])

const keyword = ref('')
const deptId = ref(null)
const doctorId = ref(null)
const dateFilter = ref('')
const statusFilter = ref(null)

const deptOptions = ref([])
const doctorOptions = ref([])

const detailVisible = ref(false)
const detail = reactive({
  appointmentId: null,
  appointmentNo: '',
  patientName: '',
  doctorName: '',
  doctorTitle: '',
  deptName: '',
  appointmentDate: '',
  timeSlot: '',
  status: null,
  statusText: '',
  feeAmount: null,
  paid: 0,
  createdTime: ''
})

const headerCellStyle = {
  background: 'rgba(139, 90, 43, 0.08)',
  color: '#5c4a32',
  fontWeight: '600',
  fontSize: '13px',
  borderBottom: '1px solid rgba(139, 90, 43, 0.15)'
}

const tableRowClassName = ({ rowIndex }) => (rowIndex % 2 === 1 ? 'striped-row' : '')

const loadDepts = async () => {
  const list = await getDeptOptions()
  deptOptions.value = Array.isArray(list) ? list : []
}

const loadDoctors = async () => {
  const res = await getDoctorPage({
    current: 1,
    size: 200,
    status: 1,
    deptId: deptId.value ?? undefined
  })
  doctorOptions.value = res?.list || []
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      deptId: deptId.value ?? undefined,
      doctorId: doctorId.value ?? undefined,
      date: dateFilter.value || undefined
    }
    // 处理状态筛选
    if (statusFilter.value === 0) {
      // 待支付
      params.status = 1
      params.paid = 0
    } else if (statusFilter.value === 1) {
      // 待就诊
      params.status = 1
      params.paid = 1
    } else if (statusFilter.value != null) {
      params.status = statusFilter.value
    }
    const res = await getAdminAppointmentPage(params)
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const handleDeptChange = async () => {
  doctorId.value = null
  await loadDoctors()
  currentPage.value = 1
  loadData()
}

const openDetail = async (row) => {
  const d = await getAdminAppointmentDetail(row.appointmentId)
  detail.appointmentId = d.appointmentId
  detail.appointmentNo = d.appointmentNo || ''
  detail.patientName = d.patientName || ''
  detail.doctorName = d.doctorName || ''
  detail.doctorTitle = d.doctorTitle || ''
  detail.deptName = d.deptName || ''
  detail.appointmentDate = d.appointmentDate || ''
  detail.timeSlot = d.timeSlot || ''
  detail.status = d.status ?? null
  detail.statusText = d.statusText || ''
  detail.feeAmount = d.feeAmount ?? null
  detail.paid = d.paid ?? 0
  detail.createdTime = d.createdTime || ''
  detailVisible.value = true
}

const cancelRow = async (row) => {
  if (row.status !== 1) return
  try {
    await ElMessageBox.confirm(`确定取消预约「${row.appointmentNo}」吗？`, '取消预约', { type: 'warning' })
  } catch {
    return
  }
  await cancelAdminAppointment(row.appointmentId)
  ElMessage.success('已取消预约')
  loadData()
}

// 签到方法
const checkinRow = async (row) => {
  if (row.status !== 1) {
    ElMessage.warning('只有待就诊状态的预约可以签到')
    return
  }
  if (row.paid !== 1) {
    ElMessage.warning('请先支付挂号费')
    return
  }
  try {
    await ElMessageBox.confirm(`确定患者「${row.patientName}」已签到就诊吗？`, '签到确认', { type: 'info' })
  } catch {
    return
  }
  try {
    await checkInAppointment(row.appointmentId)
    ElMessage.success('签到成功')
    loadData()
  } catch (error) {
    ElMessage.error(error.message || '签到失败')
  }
}

const handleExpireOverdue = async () => {
  try {
    await ElMessageBox.confirm(
      '将把所有“过期待就诊”的预约转为爽约并释放号源，是否继续？',
      '操作确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  const res = await expireOverdueAppointments()
  const processed = res?.processedCount ?? 0
  ElMessage.success(`处理完成，共转爽约 ${processed} 条`)
  loadData()
}

onMounted(async () => {
  await loadDepts()
  await loadDoctors()
  await loadData()
})
</script>

<style scoped src="@/assets/admin-management-page.css"></style>
