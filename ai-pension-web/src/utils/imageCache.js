const DB_NAME = 'PensionImageCache'
const DB_VERSION = 1
const STORE = 'images'
const LS_PREFIX = 'img:'

let dbPromise = null
let ready = false

function getDB() {
  if (dbPromise) return dbPromise
  dbPromise = new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = (e) => {
      const db = e.target.result
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE, { keyPath: 'key' })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
  return dbPromise
}

function dbPut(key, value) {
  getDB().then((db) => {
    const t = db.transaction(STORE, 'readwrite')
    t.objectStore(STORE).put({ key, value })
  }).catch(() => {})
}

function dbGet(key) {
  return new Promise((resolve) => {
    getDB().then((db) => {
      const t = db.transaction(STORE, 'readonly')
      const req = t.objectStore(STORE).get(key)
      req.onsuccess = () => resolve(req.result?.value || '')
      req.onerror = () => resolve('')
    }).catch(() => resolve(''))
  })
}

function dbDelete(key) {
  getDB().then((db) => {
    const t = db.transaction(STORE, 'readwrite')
    t.objectStore(STORE).delete(key)
  }).catch(() => {})
}

async function initSync() {
  try {
    const OLD_PREFIX = 'imgcache:'
    const keys = []
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)
      if (k && k.startsWith(OLD_PREFIX)) keys.push(k)
    }
    keys.forEach((k) => {
      const v = localStorage.getItem(k)
      const realKey = k.slice(OLD_PREFIX.length)
      dbPut(realKey, v)
      localStorage.removeItem(k)
    })
    if (keys.length > 0) console.log('[imgcache] 迁移旧 localStorage key:', keys.length, '项')

    const db = await getDB()
    let count = 0
    const t = db.transaction(STORE, 'readonly')
    t.objectStore(STORE).openCursor().onsuccess = (e) => {
      const cursor = e.target.result
      if (cursor) {
        const k = cursor.value.key
        const v = cursor.value.value
        try {
          localStorage.setItem(LS_PREFIX + k, v)
          count++
        } catch (err) {}
        cursor.continue()
      } else {
        ready = true
        if (count > 0 || keys.length > 0) {
          window.dispatchEvent(new CustomEvent('image-cache-ready', { detail: { count: count + keys.length } }))
        }
      }
    }
  } catch (e) {}
}

export function saveImage(key, base64) {
  if (!base64) return false
  try {
    localStorage.setItem(LS_PREFIX + key, base64)
  } catch (e) {
    console.warn('[imgcache] localStorage 存满，尝试清理')
    for (let i = localStorage.length - 1; i >= 0; i--) {
      const k = localStorage.key(i)
      if (k && k.startsWith(LS_PREFIX)) {
        localStorage.removeItem(k)
        if (i > 5) break
      }
    }
    try { localStorage.setItem(LS_PREFIX + key, base64) } catch (e2) {}
  }
  dbPut(key, base64)
  return true
}

export function getImage(key) {
  try {
    return localStorage.getItem(LS_PREFIX + key) || ''
  } catch (e) {
    return ''
  }
}

export function removeImage(key) {
  try { localStorage.removeItem(LS_PREFIX + key) } catch (e) {}
  dbDelete(key)
}

export function pkgKey(id) { return `pkg:${id}` }
export function actKey(id) { return `act:${id}` }
export function userKey(id) { return `user:${id}` }

initSync()