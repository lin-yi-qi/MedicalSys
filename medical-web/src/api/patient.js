import request from '@/utils/request'

// ==================== 我的病历 ====================

export function getMyMedicalRecords() {
  return request({
    url: '/patient/medical-record/my',
    method: 'get'
  })
}

export function getMyMedicalRecordDetail(recordId) {
  return request({
    url: `/patient/medical-record/${recordId}`,
    method: 'get'
  })
}

// ==================== 我的处方 ====================

export function getMyPrescriptions() {
  return request({
    url: '/patient/prescription/my',
    method: 'get'
  })
}

export function getMyPrescriptionDetail(prescriptionId) {
  return request({
    url: `/patient/prescription/${prescriptionId}`,
    method: 'get'
  })
}

export function payMyPrescription(prescriptionId) {
  return request({
    url: `/patient/payment/prescription/${prescriptionId}`,
    method: 'put'
  })
}
