import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import router from '@/router'


let firstFetchLoginUser = true;


router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser

  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false;
  }
  const toUrl = to.fullPath
  if (toUrl.startsWith('/admin')) {
    if (!loginUser) {
      message.error('没有权限')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }else if(loginUser && loginUser.userRole !== 'admin') {
      message.error('没有权限')
      next(`/?redirect=${to.fullPath}`)
      return
    }
  }
  next()
})
