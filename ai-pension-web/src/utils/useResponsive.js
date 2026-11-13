import { ref, onMounted, onBeforeUnmount } from 'vue'

const MOBILE_BREAKPOINT = 768

export function useResponsive() {
  const width = ref(typeof window !== 'undefined' ? window.innerWidth : 1280)
  const isMobile = ref(width.value < MOBILE_BREAKPOINT)

  function onResize() {
    width.value = window.innerWidth
    isMobile.value = width.value < MOBILE_BREAKPOINT
  }

  onMounted(() => {
    window.addEventListener('resize', onResize)
    onResize()
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', onResize)
  })

  return { width, isMobile, MOBILE_BREAKPOINT }
}