// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** Health GET /api/health */
export async function healthUsingGet(options?: { [key: string]: any }) {
  return request<string>('/api/health', {
    method: 'GET',
    ...(options || {}),
  })
}
