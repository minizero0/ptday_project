import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import { useAuthStore } from './features/auth/store/authStore'
import { setSessionExpiredHandler } from './lib/api'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

// 요청 모듈은 로그인 저장소를 모른다(순환 참조 방지). 둘을 아는 앱 진입점에서 이어 준다.
// 로그인 상태가 풀리면 App 의 라우트 보호가 로그인 화면으로 보낸다.
setSessionExpiredHandler(() => useAuthStore.getState().expireSession())

// 로그인 상태가 풀리는 순간(로그아웃 버튼·세션 만료 모두) 서버 데이터 캐시를 비운다.
// 같은 기기에서 다음 사람이 로그인했을 때 이전 사람이 보던 회원 정보가 잠깐이라도 비치지 않게 한다.
useAuthStore.subscribe((state, previous) => {
  if (previous.isAuthenticated && !state.isAuthenticated) {
    queryClient.clear()
  }
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
