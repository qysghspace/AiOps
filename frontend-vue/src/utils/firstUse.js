const PREFIX = 'aiops_first_use_'

export function isFirstUse(feature) {
  if (!feature) return false
  return localStorage.getItem(`${PREFIX}${feature}`) !== 'used'
}

export function markFeatureUsed(feature) {
  if (!feature) return
  localStorage.setItem(`${PREFIX}${feature}`, 'used')
}

export function resetFeatureUsed(feature) {
  if (!feature) return
  localStorage.removeItem(`${PREFIX}${feature}`)
}
