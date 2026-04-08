<template>
  <div class="dept-list-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-building page-icon"></i>
        <div>
          <h2 class="page-title">科室列表</h2>
          <p class="page-desc">
            维护医院的临床与护理科室信息，可设置层级结构、负责人与联系方式，用于医生、护士档案以及排班、预约时的科室选择。
          </p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="search-wrap">
          <i class="fa-solid fa-magnifying-glass search-icon"></i>
          <el-input
            v-model="keyword"
            placeholder="搜索科室名称或代码"
            clearable
            class="search-input"
            @clear="loadData"
            @keyup.enter="loadData"
          />
          <el-button class="search-btn" @click="loadData">
            <i class="fa-solid fa-search"></i>
            搜索
          </el-button>
          <el-select
            v-model="statusFilter"
            placeholder="状态"
            clearable
            size="large"
            class="filter-select"
            style="width: 120px"
            @change="loadData"
          >
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="停用" />
          </el-select>
        </div>
        <div class="toolbar-actions">
          <el-button class="add-btn" @click="openCreate">
            <i class="fa-solid fa-plus"></i>
            新增科室
          </el-button>
        </div>
      </div>

      <div class="table-wrap" v-loading="loading" element-loading-text="加载中...">
        <el-table
          :data="tableData"
          class="data-table"
          :header-cell-style="headerCellStyle"
          :row-class-name="tableRowClassName"
        >
          <el-table-column prop="deptId" label="ID" width="72" align="center">
            <template #default="{ row }">
              <span class="cell-id">{{ row.deptId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="科室名称" min-width="180">
            <template #default="{ row }">
              <div class="dept-name-wrap">
                <span class="dept-name">{{ row.name }}</span>
                <div class="dept-jump-actions">
                <el-button link class="jump-link" size="small" @click="goStaffList('DOCTOR', row)">
                  医生
                </el-button>
                <span class="sep">|</span>
                <el-button link class="jump-link" size="small" @click="goStaffList('NURSE', row)">
                  护士
                </el-button>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="code" label="科室代码" width="130">
            <template #default="{ row }">
              <span class="cell-code">{{ row.code || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="parentName" label="上级科室" width="120">
            <template #default="{ row }">
              {{ row.parentName || '—' }}
            </template>
          </el-table-column>
          <el-table-column prop="manager" label="负责人" width="100" />
          <el-table-column prop="phone" label="联系电话" width="120" />
          <el-table-column prop="sortOrder" label="排序" width="72" align="center" />
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <span :class="['status-dot', row.status === 1 ? 'enabled' : 'disabled']"></span>
              <span class="status-text">{{ row.status === 1 ? '启用' : '停用' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdTime" label="创建时间" width="165" align="center">
            <template #default="{ row }">
              <span class="cell-time">{{ row.createdTime ?? '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openEdit(row)">
                <i class="fa-solid fa-pen"></i> 编辑
              </el-button>
              <el-button link type="danger" size="small" @click="confirmDelete(row)">
                <i class="fa-solid fa-trash"></i> 删除
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
      v-model="dialogVisible"
      width="860px"
      class="dept-dialog edit-dialog dept-form-dialog"
      :close-on-click-modal="false"
      align-center
      @close="resetForm"
    >
      <template #header>
        <div class="edit-dialog-header">
          <i class="fa-solid fa-building dialog-icon"></i>
          <div>
            <span class="dialog-title">{{ isEdit ? '编辑科室' : '新增科室' }}</span>
            <span class="dialog-subtitle">科室代码建议与业务角色名称保持一致，便于后续管理和统计。</span>
          </div>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="edit-form dept-edit-form">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="上级科室" prop="parentId">
              <el-select
                v-model="form.parentId"
                placeholder="顶级可选「（顶级）」"
                clearable
                filterable
                class="form-input"
                style="width: 100%"
              >
                <el-option :value="0" label="（顶级）" />
                <el-option
                  v-for="o in parentOptions"
                  :key="o.deptId"
                  :label="`${o.name}${o.code ? ' (' + o.code + ')' : ''}`"
                  :value="o.deptId"
                  :disabled="isEdit && o.deptId === form.deptId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="科室名称" prop="name" required>
              <el-input v-model="form.name" placeholder="必填" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="科室代码" prop="code">
              <el-input v-model="form.code" placeholder="可选" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="负责人">
              <el-input v-model="form.manager" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" :max="99999" style="width: 100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option :value="1" label="启用" />
                <el-option :value="0" label="停用" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址">
          <el-input v-model="form.address" maxlength="200" placeholder="选填" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 3 }"
            maxlength="500"
            show-word-limit
            placeholder="选填"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="edit-dialog-footer">
          <el-button class="btn-cancel" @click="dialogVisible = false">取消</el-button>
          <el-button class="btn-save" :loading="submitting" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeptPage, getDeptOptions, createDept, updateDept, deleteDept } from '@/api/admin'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const statusFilter = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const parentOptions = ref([])

const form = reactive({
  deptId: null,
  parentId: 0,
  name: '',
  code: '',
  manager: '',
  phone: '',
  address: '',
  sortOrder: 0,
  status: 1,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入科室名称', trigger: 'blur' }]
}

const headerCellStyle = {
  background: 'rgba(139, 90, 43, 0.08)',
  color: '#5c4a32',
  fontWeight: '600',
  fontSize: '13px',
  borderBottom: '1px solid rgba(139, 90, 43, 0.15)'
}

const tableRowClassName = ({ rowIndex }) => (rowIndex % 2 === 1 ? 'striped-row' : '')

const loadParentOptions = async () => {
  try {
    const list = await getDeptOptions()
    parentOptions.value = Array.isArray(list) ? list : []
  } catch {
    parentOptions.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDeptPage({
      current: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value || undefined,
      status: statusFilter.value ?? undefined
    })
    tableData.value = res.list || []
    total.value = res.total || 0
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.deptId = null
  form.parentId = 0
  form.name = ''
  form.code = ''
  form.manager = ''
  form.phone = ''
  form.address = ''
  form.sortOrder = 0
  form.status = 1
  form.remark = ''
  formRef.value?.resetFields()
}

const openCreate = async () => {
  await loadParentOptions()
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEdit = async (row) => {
  await loadParentOptions()
  isEdit.value = true
  form.deptId = row.deptId
  form.parentId = row.parentId != null ? row.parentId : 0
  form.name = row.name || ''
  form.code = row.code || ''
  form.manager = row.manager || ''
  form.phone = row.phone || ''
  form.address = row.address || ''
  form.sortOrder = row.sortOrder ?? 0
  form.status = row.status !== 0 ? 1 : 0
  form.remark = row.remark || ''
  dialogVisible.value = true
}

const buildPayload = () => ({
  parentId: form.parentId == null || form.parentId === '' ? 0 : form.parentId,
  name: form.name.trim(),
  code: form.code?.trim() || undefined,
  manager: form.manager?.trim() || undefined,
  phone: form.phone?.trim() || undefined,
  address: form.address?.trim() || undefined,
  sortOrder: form.sortOrder ?? 0,
  status: form.status,
  remark: form.remark?.trim() || undefined
})

const submitForm = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value) {
      await updateDept(form.deptId, payload)
      ElMessage.success('保存成功')
    } else {
      await createDept(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

const confirmDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除科室「${row.name}」吗？`, '删除科室', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
  } catch {
    return
  }
  try {
    await deleteDept(row.deptId)
    ElMessage.success('已删除')
    loadData()
  } catch {
    // 已提示
  }
}

const goStaffList = (roleCode, row) => {
  const path = roleCode === 'NURSE' ? '/admin/nurse' : '/admin/doctor'
  router.push({
    path,
    query: {
      deptId: String(row.deptId),
      deptName: row.name || ''
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dept-list-page {
  padding: 24px 28px 32px;
  min-height: 100%;
}

.page-header {
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.page-icon {
  width: 48px;
  height: 48px;
  line-height: 48px;
  text-align: center;
  font-size: 22px;
  color: #fff;
  background: linear-gradient(135deg, #8b6f47, #6b4f2a);
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(107, 79, 42, 0.35);
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #2c1810;
}

.page-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: #5c4a32;
}

.content-card {
  background: rgba(255, 252, 248, 0.95);
  border-radius: 16px;
  padding: 20px 22px 24px;
  box-shadow: 0 8px 40px rgba(61, 41, 20, 0.08), 0 0 0 1px rgba(139, 90, 43, 0.08);
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.search-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-icon {
  color: #8b6f47;
}

.search-input {
  width: 220px;
}

.search-btn,
.add-btn {
  border-radius: 10px;
  background: linear-gradient(135deg, #e8a54b, #d48232);
  border: none;
  color: #fff;
}

.filter-select :deep(.el-input__wrapper) {
  border-radius: 10px;
}

.data-table {
  width: 100%;
}

.cell-id,
.cell-code {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  color: #5c4a32;
}

.dept-name-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.dept-name {
  font-weight: 600;
  color: #4a3924;
  white-space: nowrap;
}

.dept-jump-actions {
  display: inline-flex;
  align-items: center;
  line-height: 1;
  white-space: nowrap;
  color: #8f7a63;
  opacity: 0.9;
}

.dept-jump-actions :deep(.jump-link.el-button) {
  --el-button-text-color: #9a8369;
  --el-button-hover-text-color: #7f684d;
  font-size: 12px;
  padding: 0;
  min-height: auto;
  height: auto;
}

.dept-jump-actions :deep(.jump-link.el-button:hover) {
  text-decoration: underline;
}

.dept-jump-actions .sep {
  color: #b7a58f;
  margin: 0 2px;
  font-size: 12px;
}

.cell-time {
  font-size: 12px;
  color: #6b5a48;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.status-dot.enabled {
  background: #67c23a;
}

.status-dot.disabled {
  background: #c0c4cc;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.edit-dialog-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.dialog-icon {
  width: 44px;
  height: 44px;
  line-height: 44px;
  text-align: center;
  font-size: 20px;
  color: #fff;
  background: linear-gradient(135deg, #8b6f47, #6b4f2a);
  border-radius: 12px;
}

.dialog-title {
  display: block;
  font-size: 17px;
  font-weight: 700;
  color: #2c1810;
}

.dialog-subtitle {
  font-size: 12px;
  color: #5c4a32;
  margin-top: 2px;
}

.edit-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-cancel {
  border-radius: 10px;
}

.btn-save {
  border-radius: 10px;
  background: linear-gradient(135deg, #e8a54b, #d48232);
  border: none;
  color: #fff;
}

.dept-form-dialog :deep(.el-dialog__header) {
  padding: 16px 20px 12px;
  margin: 0;
}

.dept-form-dialog :deep(.el-dialog__body) {
  padding: 8px 20px 4px;
}

.dept-form-dialog :deep(.el-dialog__footer) {
  padding: 12px 20px 16px;
}

.dept-edit-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.dept-edit-form :deep(.el-form-item__label) {
  margin-bottom: 4px;
  line-height: 1.3;
}
</style>

