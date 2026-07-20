/// <reference types="vite/client" />

// 프로젝트에서 사용하는 환경변수 타입 선언
interface ImportMetaEnv {
  /** API 서버 베이스 URL (예: http://localhost:8080) */
  readonly VITE_API_BASE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
