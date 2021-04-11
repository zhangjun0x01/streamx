import axios from 'axios'
import $qs from 'qs'
import notification from 'ant-design-vue/es/notification'
import { TOKEN,INVALID } from '@/store/mutation-types'
import storage from '@/utils/storage'
import store from '@/store'
import moment from 'moment'
import { message, Modal } from 'ant-design-vue'

import {baseUrl} from '@/api/baseUrl'

const http = axios.create({
  baseURL: baseUrl(),
  withCredentials: false,
  timeout: 1000 * 10, // 请求超时时间
  responseType: 'json',
  validateStatus (status) {
    // 200 外的状态码都认定为失败
    return status === 200
  }
})

// request interceptor
http.interceptors.request.use(config => {
  const expire = store.getters.expire
  const now = moment().format('YYYYMMDDHHmmss')
  // 让token早10秒种过期，提升“请重新登录”弹窗体验
  if (now - expire >= -10) {
    Modal.error({
      title: 'Sign in expired',
      content: 'Sorry, Sign in has expired. Please Sign in again',
      okText: 'Sign in',
      mask: false,
      onOk: () => {
        return new Promise((resolve, reject) => {
          storage.clear()
          location.reload()
        })
      }
    })
  }
  config.headers = {
    'X-Requested-With': 'XMLHttpRequest',
    'Content-Type': config.headers['Content-Type'] || 'application/x-www-form-urlencoded; charset=UTF-8',
    'Access-Control-Allow-Origin': '*'
  }
  const token = storage.get(TOKEN)
  if (token) {
    config.headers['Authorization'] = token
  }
  return config
}, error => {
  return Promise.reject(error)
})

// response interceptor
http.interceptors.response.use((response) => {
  return response.data
}, error => {
  if (error.response) {
    const errorMessage = error.response.data === null ? 'System error，Please contact the administrator' : error.response.data.message
    switch (error.response.status) {
      case 404:
        notification.error({
          message: 'Sorry, resource not found',
          duration: 4
        })
        break
      case 403:
      case 401:
        //避免在某些页面有密集的ajax请求数据时反复的弹窗
        if (!storage.get(INVALID,false)) {
          storage.set(INVALID,true)
          notification.warn({
            message: 'Sorry, you can\'t access. May be because you don\'t have permissions or the Sign In is invalid',
            duration: 4
          })
          store.dispatch('SignOut', {}).then((resp) => {
            storage.clear()
            location.reload()
          })
        }
        break
      default:
        notification.error({
          message: errorMessage,
          duration: 4
        })
        break
    }
  }
  return Promise.reject(error)
})

const respBlob = (content, fileName) => {
  const blob = new Blob([content])
  fileName = fileName || `${new Date().getTime()}_export.xlsx`
  if ('download' in document.createElement('a')) {
    const link = document.createElement('a')
    link.download = fileName
    link.style.display = 'none'
    link.href = URL.createObjectURL(blob)
    document.body.appendChild(link)
    link.click()
    URL.revokeObjectURL(link.href)
    document.body.removeChild(link)
  } else {
    navigator.msSaveBlob(blob, fileName)
  }
}

const blobTimeout = 1000 * 60 * 10
export default {
  get (url, data = {}, headers = null) {
    if (headers) {
    }
    return http.get(url, { params: data })
  },
  post (url, data = {}, headers = null) {
    return http.post(url, $qs.stringify(data))
  },
  put (url, data = {}, headers = null) {
    return http.put(url, $qs.stringify(data))
  },
  delete (url, params = {}, headers = null) {
    return http.delete(url, { data: $qs.stringify(params) })
  },
  patch (url, data = {}, headers = null) {
    return http.patch(url, $qs.stringify(data))
  },
  download (url, params, filename) {
    message.loading('File transfer in progress')
    return http.post(url, params, {
      transformRequest: [(params) => {
        return $qs.stringify(params)
      }],
      responseType: 'blob',
      timeout: blobTimeout // 上传文件超时10分钟
    }).then((resp) => {
      respBlob(resp, filename)
    }).catch((r) => {
      console.error(r)
      message.error('下载失败')
    })
  },
  upload (url, params) {
    return http.post(url, params, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: blobTimeout // 上传文件超时10分钟
    })
  },
  export (url, params = {}, blobCallback, msg) {
    if (blobCallback == null) {
      blobCallback = respBlob
    }
    msg = msg == null ? {} : msg
    message.loading(msg.loading || '导入文件中...')
    return http.post(url, params, {
      transformRequest: [(params) => {
        return $qs.stringify(params)
      }],
      responseType: 'blob'
    }).then((resp) => {
      blobCallback(resp)
    }).catch((r) => {
      console.error(r)
      message.error(msg.error || '导出文件失败!')
    })
  },

}
