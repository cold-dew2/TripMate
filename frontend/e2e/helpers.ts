import { expect, type Page } from "@playwright/test";

export const API_BASE = process.env.E2E_API_URL ?? "http://localhost:8080";

// 데이터를 새로 만드는 시나리오(회원가입, 소모임 생성/참여, 채팅 전송)는 공용 DB에
// 흔적이 남으므로 E2E_ALLOW_WRITES=1 일 때만 실행한다.
export const ALLOW_WRITES = process.env.E2E_ALLOW_WRITES === "1";

export interface Account {
  id: string;
  pw: string;
  name: string;
}

const account = (key: string, name: string): Account => {
  const id = process.env[`E2E_${key}_ID`];
  const pw = process.env[`E2E_${key}_PW`];
  if (!id || !pw) throw new Error(`E2E_${key}_ID / E2E_${key}_PW 환경변수가 필요합니다 (e2e/README.md 참고)`);
  return { id, pw, name: process.env[`E2E_${key}_NAME`] ?? name };
};

// A: 기본 사용자, B: 채팅 상대(A와 같은 채팅방 멤버), C: 소모임 참여 신청용
export const userA = () => account("USER_A", "트립메이트");
export const userB = () => account("USER_B", "연찬민");
export const userC = () => account("USER_C", "김민국");

export async function login(page: Page, user: Account) {
  await page.goto("/auth");
  await page.locator("#userId").fill(user.id);
  await page.locator("#userPw").fill(user.pw);
  await page.locator("button[type=submit]").click();
  await expect(page).toHaveURL(/\/$/);
}

export const HANGUL = /[가-힣]/;
