<template>
  <div class="patient-record-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-file-medical page-icon"></i>
        <div>
          <h2 class="page-title">我的病历</h2>
          <p class="page-desc">查看本人就诊病历记录，仅展示已归档病历（只读）</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索病历号 / 医生 / 诊断"
          style="width: 300px"
          @clear="applyFilter"
          @keyup.enter="applyFilter"
        >
          <template #prefix><i class="fa-solid fa-search"></i></template>
        </el-input>
      </div>

      <div v-loading="loading">
        <div v-if="filteredList.length === 0" class="empty-custom">
          <el-empty description="暂无病历记录" />
        </div>
        <div v-else>
          <div
            v-for="item in pagedList"
            :key="item.recordId"
            class="record-card"
          >
            <div class="card-header">
              <span class="record-no">{{ item.recordNo }}</span>
              <el-tag type="info" size="small">{{ item.statusText || '已归档' }}</el-tag>
            </div>
            <div class="card-body">
              <div class="info-row">
                <span class="label">就诊日期</span>
                <span class="value">{{ item.visitDate || '—' }}</span>
              </div>
              <div class="info-row">
                <span class="label">接诊医生</span>
                <span class="value">{{ item.doctorName || '—' }}{{ item.doctorTitle ? `（${item.doctorTitle}）` : '' }}</span>
              </div>
              <div class="info-row">
                <span class="label">诊断</span>
                <span class="value diagnosis">{{ item.diagnosis || '—' }}</span>
              </div>
              <div class="info-row" v-if="item.chiefComplaint">
                <span class="label">主诉</span>
                <span class="value">{{ item.chiefComplaint }}</span>
              </div>
            </div>
            <div class="card-footer">
              <el-button type="primary" link @click="openDetail(item)">
                <i class="fa-solid fa-eye"></i> 查看详情
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

    <el-dialog v-model="detailVisible" title="病历详情" width="640px" class="detail-dialog">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="病历号" :span="2">{{ detail.recordNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="就诊日期">{{ detail.visitDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="接诊医生">
          {{ detail.doctorName || '—' }}{{ detail.doctorTitle ? `（${detail.doctorTitle}）` : '' }}
        </el-descriptions-item>
        <el-descriptions-item label="主诉" :span="2">{{ detail.chiefComplaint || '—' }}</el-descriptions-item>
        <el-descriptions-item label="现病史" :span="2">{{ detail.presentIllness || '—' }}</el-descriptions-item>
        <el-descriptions-item label="既往史" :span="2">{{ detail.pastHistory || '—' }}</el-descriptions-item>
        <el-descriptions-item label="体格检查" :span="2">{{ detail.physicalExam || '—' }}</el-descriptions-item>
        <el-descriptions-item label="诊断" :span="2">{{ detail.diagnosis || '—' }}</el-descriptions-item>
        <el-descriptions-item label="治疗方案" :span="2">{{ detail.treatmentPlan || '—' }}</el-descriptions-item>
        <el-descriptions-item label="记录时间" :span="2">{{ formatTime(detail.createdTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyMedicalRecords, getMyMedicalRecordDetail } from '@/api/patient'

const loading = ref(false)
const list = ref([])
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detail = reactive({})

const filteredList = computed(() => {
  if (!keyword.value?.trim()) return list.value
  const kw = keyword.value.trim().toLowerCase()
  return list.value.filter(
    (r) =>
      (r.recordNo && r.recordNo.toLowerCase().includes(kw)) ||
      (r.doctorName && r.doctorName.toLowerCase().includes(kw)) ||
      (r.diagnosis && r.diagnosis.toLowerCase().includes(kw)) ||
      (r.chiefComplaint && r.chiefComplaint.toLowerCase().includes(kw))
  )
})

const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const formatTime = (t) => (t ? String(t).replace('T', ' ').slice(0, 19) : '—')

const applyFilter = () => {
  currentPage.value = 1
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyMedicalRecords()
    list.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  try {
    const d = await getMyMedicalRecordDetail(row.recordId)
    Object.assign(detail, d || {})
    detailVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.patient-record-page {
  padding: 24px 28px 32px;
  min-height: 100%;
}
.page-header {
  margin-bottom: 20px;
}
.header-left {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.page-icon {
  font-size: 28px;
  color: #3b82f6;
  margin-top: 4px;
}
.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
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
  margin-bottom: 16px;
}
.record-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 14px;
  border: 1px solid rgba(139, 90, 43, 0.12);
  transition: box-shadow 0.2s;
}
.record-card:hover {
  box-shadow: 0 4px 16px rgba(61, 41, 20, 0.08);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.record-no {
  font-weight: 600;
  color: #334155;
}
.info-row {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 14px;
}
.info-row .label {
  color: #94a3b8;
  min-width: 72px;
}
.info-row .value {
  color: #334155;
  flex: 1;
}
.diagnosis {
  font-weight: 500;
  color: #1e40af;
}
.card-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed rgba(139, 90, 43, 0.15);
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.empty-custom {
  padding: 40px 0;
}
</style>
