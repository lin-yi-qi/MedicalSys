/**
 * 前端路由角色权限（与后端 SecurityConfig 及 menu-config 保持一致）
 * 规则按前缀从长到短匹配，先匹配者优先。
 */

/** 所有业务角色 */
export const ALL_ROLES = [
  'SUPER_ADMIN',
  'ADMIN',
  'DOCTOR',
  'PATIENT',
  'RECEPTIONIST',
  'NURSE'
]

/**
 * 路径前缀 → 允许访问的角色列表
 * ADMIN / SUPER_ADMIN 可进入管理端；各业务端与后端 API 权限对齐。
 */
export const ROUTE_PREFIX_RULES = [
  { prefix: '/admin/system/', roles: ['SUPER_ADMIN'] },
  { prefix: '/admin/', roles: ['ADMIN', 'SUPER_ADMIN'] },
  { prefix: '/doctor/', roles: ['DOCTOR', 'ADMIN', 'SUPER_ADMIN'] },
  { prefix: '/patient/', roles: ['PATIENT', 'DOCTOR', 'RECEPTIONIST'] },
  { prefix: '/reception/', roles: ['RECEPTIONIST', 'ADMIN', 'SUPER_ADMIN'] },
  { prefix: '/nurse/', roles: ['NURSE', 'ADMIN', 'SUPER_ADMIN'] },
  { prefix: '/user/', roles: ALL_ROLES }
]

/**
 * 规范化路径（去掉 query、末尾斜杠）
 */
export function normalizePath(path) {
  if (!path) return '/'
  const base = path.split('?')[0] || '/'
  if (base.length > 1 && base.endsWith('/')) {
    return base.slice(0, -1)
  }
  return base
}

/**
 * 从 sessionStorage 读取角色列表
 */
export function getRolesFromStorage() {
  try {
    const u = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
    return Array.isArray(u.roles) ? u.roles : []
  } catch {
    return []
  }
}

/**
 * 判断角色是否可访问指定路径
 */
export function canAccessPath(path, roles) {
  const normalized = normalizePath(path)
  if (normalized === '/' || normalized === '') {
    return true
  }

  const roleList = roles || []
  if (!roleList.length) {
    return false
  }

  for (const rule of ROUTE_PREFIX_RULES) {
    if (normalized === rule.prefix.slice(0, -1) || normalized.startsWith(rule.prefix)) {
      return roleList.some((r) => rule.roles.includes(r))
    }
  }

  // 未配置前缀的路由：已登录即可访问
  return true
}

/**
 * 结合路由 meta 做额外校验（requiresSuperAdmin 等）
 */
export function canAccessRoute(to, roles) {
  if (!canAccessPath(to.path, roles)) {
    return false
  }
  if (to.meta?.requiresSuperAdmin && !roles.includes('SUPER_ADMIN')) {
    return false
  }
  return true
}
