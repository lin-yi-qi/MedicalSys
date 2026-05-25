<template>
  <div class="patient-rx-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-prescription page-icon"></i>
        <div>
          <h2 class="page-title">我的处方</h2>
          <p class="page-desc">查看本人处方及发药状态，待缴费处方可在线支付（内部记账）</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <el-select v-model="statusFilter" clearable placeholder="处方状态" style="width: 140px" @change="applyFilter">
          <el-option label="待发药" :value="1" />
          <el-option label="已发药" :value="2" />
          <el-option label="已取消" :value="3" />
        </el-select>
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索处方号 / 医生"
          style="width: 280px"
          @clear="applyFilter"
          @keyup.enter="applyFilter"
        >
          <template #prefix><i class="fa-solid fa-search"></i></template>
        </el-input>
      </div>

      <div v-loading="loading">
        <div v-if="filteredList.length === 0" class="empty-custom">
          <el-empty description="暂无处方记录" />
        </div>
        <div v-else>
          <div
            v-for="item in pagedList"
            :key="item.prescriptionId"
            class="rx-card"
          >
            <div class="card-header">
              <span class="rx-no">{{ item.prescriptionNo }}</span>
              <div class="tags">
                <el-tag :type="statusTagType(item.status)" size="small">{{ item.statusText }}</el-tag>
                <el-tag :type="item.paid === 1 ? 'success' : 'warning'" size="small">
                  {{ item.paid === 1 ? '已缴费' : '未缴费' }}
                </el-tag>
              </div>
            </div>
            <div class="card-body">
              <div class="info-row">
                <span class="label">开方医生</span>
                <span class="value">{{ item.doctorName || '—' }}{{ item.doctorTitle ? `（${item.doctorTitle}）` : '' }}</span>
              </div>
              <div class="info-row">
                <span class="label">处方金额</span>
                <span class="value amount">¥{{ item.totalAmount ?? '—' }}</span>
              </div>
              <div class="info-row">
                <span class="label">开方时间</span>
                <span class="value">{{ formatTime(item.createdTime) }}</span>
              </div>
              <div class="info-row" v-if="item.details?.length">
                <span class="label">药品</span>
                <span class="value">{{ item.details.length }} 种</span>
              </div>
            </div>
            <div class="card-footer">
              <el-button type="primary" link @click="openDetail(item)">
                <i class="fa-solid fa-eye"></i> 查看详情
              </el-button>
              <el-button
                v-if="item.status === 1 && item.paid !== 1"
                type="warning"
                link
                @click="handlePay(item)"
              >
                <i class="fa-solid fa-credit-card"></i> 去缴费
              </el-button>
            </div>
          </div>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="filteredList.length"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              background
            />
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="处方详情" width="720px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="处方号" :span="3">{{ detail.prescriptionNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="开方医生">{{ detail.doctorName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusText || '—' }}</el-descriptions-item>
        <el-descriptions-item label="缴费">{{ detail.paid === 1 ? '已缴费' : '未缴费' }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ detail.totalAmount ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="开方时间" :span="2">{{ formatTime(detail.createdTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.details || []" style="margin-top: 14px" size="small">
        <el-table-column prop="medicineName" label="药品" min-width="120" />
        <el-table-column prop="spec" label="规格" min-width="100" />
        <el-table-column prop="dosage" label="用法用量" min-width="120" />
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column label="小计" width="90">
          <template #default="{ row }">¥{{ row.amount ?? '—' }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detail.status === 1 && detail.paid !== 1"
          type="primary"
          @click="handlePay(detail)"
        >去缴费</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMyPrescriptions,
  getMyPrescriptionDetail,
  payMyPrescription
} from '@/api/patient'

const loading = ref(false)
const list = ref([])
const keyword = ref('')
const statusFilter = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detail = reactive({})

const filteredList = computed(() => {
  let data = list.value
  if (statusFilter.value != null) {
    data = data.filter((r) => r.status === statusFilter.value)
  }
  if (keyword.value?.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    data = data.filter(
      (r) =>
        (r.prescriptionNo && r.prescriptionNo.toLowerCase().includes(kw)) ||
        (r.doctorName && r.doctorName.toLowerCase().includes(kw))
    )
  }
  return data
})

const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const statusTagType = (status) => {
  if (status === 2) return 'success'
  if (status === 3) return 'info'
  return 'warning'
}

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '—')

const applyFilter = () => {
  currentPage.value = 1
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyPrescriptions()
    list.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  try {
    const d = await getMyPrescriptionDetail(row.prescriptionId)
    Object.assign(detail, d || {})
    detailVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

const handlePay = async (item) => {
  try {
    const res = await payMyPrescription(item.prescriptionId)
    ElMessage.success(`缴费成功${res?.paymentNo ? '，流水号：' + res.paymentNo : ''}`)
    detailVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error(e.message || '缴费失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.patient-rx-page {
  padding: 24px 28px 32px;
  min-height: 100%;
}
.page-header { margin-bottom: 20px; }
.header-left {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.page-icon {
  font-size: 28px;
  color: #8b5cf6;
  margin-top: 4px;
}
.page-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0;
  color: #1e293b;
}
.page-desc {
  font-size: 14px;
  color: #64748b;
  margin: 6px 0 0;
}
.content-card {
  background: rgba(255, 252, 248, 0.95);
  border-radius: 16px;
  padding: 20px 22px 24px;
  box-shadow: 0 8px 40px rgba(61, 41, 20, 0.08), 0 0 0 1px rgba(139, 90, 43, 0.08);
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.rx-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 14px;
  border: 1px solid rgba(139, 90, 43, 0.12);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.rx-no { font-weight: 600; color: #334155; }
.tags { display: flex; gap: 8px; }
.info-row {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 14px;
}
.info-row .label { color: #94a3b8; min-width: 72px; }
.info-row .value { color: #334155; }
.amount { font-weight: 600; color: #b45309; }
.card-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed rgba(139, 90, 43, 0.15);
  display: flex;
  gap: 16px;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.empty-custom { padding: 40px 0; }
</style>
