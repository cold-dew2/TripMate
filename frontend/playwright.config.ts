import { defineConfig, devices } from "@playwright/test";

// 로컬에서 띄워둔 프론트(vite, 5173)와 백엔드(8080)를 대상으로 돌린다.
// 계정 정보는 저장소에 올리지 않고 환경변수로만 받는다(e2e/README.md 참고).
export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  reporter: [["list"], ["html", { open: "never", outputFolder: "e2e-report" }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:5173",
    locale: "ko-KR",
    viewport: { width: 420, height: 900 },
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"], viewport: { width: 420, height: 900 } } }],
});
