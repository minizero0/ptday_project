/** @type {import('tailwindcss').Config} */
// 디자인 토큰: ~/Desktop/notion-design (notion.so) 기반, 밝은 관리자 테마로 매핑.
// 색은 여기서만 정의하고 컴포넌트에서는 토큰 이름(primary, danger 등)으로만 쓴다.
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        background: '#f9f9f8', // 페이지 배경
        surface: '#ffffff', // 카드/패널 표면
        border: '#dfdcd9', // 구분선/테두리
        text: {
          primary: '#31302e', // 본문 텍스트
          muted: '#78736f', // 보조 텍스트/placeholder
        },
        primary: {
          DEFAULT: '#ffb110', // 강조(브랜드) — 버튼/링크/활성
          hover: '#e89e00', // hover 시 약간 어둡게
        },
        danger: '#f64932', // 삭제/경고 액션
        success: '#1aae39', // 완료/출석
        warning: '#d9730d', // 주의 (팔레트 보완: 진한 앰버)
        info: '#2383e2', // 정보
      },
      fontFamily: {
        // 한글 UI라 NotionInter(라틴) + system-ui(한글) 폴백을 함께 지정
        sans: ['NotionInter', 'Pretendard', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        // DESIGN.md radius 스케일 (4~16px)
        sm: '0.25rem', // 4px
        DEFAULT: '0.375rem', // 6px
        md: '0.5rem', // 8px
        lg: '0.625rem', // 10px
        xl: '0.75rem', // 12px
      },
      boxShadow: {
        // 과한 그림자 금지 — 미묘한 카드 그림자만
        card: '0 1px 2px rgba(15, 15, 15, 0.05), 0 1px 3px rgba(15, 15, 15, 0.04)',
      },
    },
  },
  plugins: [],
};
