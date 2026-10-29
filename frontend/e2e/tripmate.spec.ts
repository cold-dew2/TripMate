import { expect, test, type Page } from "@playwright/test";
import { ALLOW_WRITES, API_BASE, HANGUL, login, userA, userB, userC } from "./helpers";

const WRITE_SKIP_REASON = "공용 DB에 데이터가 생기는 시나리오 - E2E_ALLOW_WRITES=1 일 때만 실행";

const listItems = (page: Page) => page.getByRole("main").getByRole("listitem");
const placeLinks = (page: Page) => page.getByRole("main").locator('a[href^="/place/"]');
const moimLinks = (page: Page) => page.getByRole("main").locator('a[href^="/moim/"]');

// ─────────────────────────── 인증 ───────────────────────────

test("E2E-01 회원가입", async ({ page, request }) => {
  test.skip(!ALLOW_WRITES, WRITE_SKIP_REASON);
  const stamp = Date.now();
  await page.goto("/auth/signup");
  await page.getByRole("textbox", { name: "이름" }).fill(`E2E${String(stamp).slice(-6)}`);
  await page.getByRole("textbox", { name: "이메일" }).fill(`e2e+${stamp}@tripmate.test`);
  await page.getByRole("textbox", { name: "비밀번호" }).fill("E2eTest!2345");
  await page.getByRole("combobox", { name: "국적 선택" }).selectOption({ label: "대한민국" });
  await page.getByRole("combobox", { name: "언어 선택" }).selectOption({ label: "한국어" });
  await page.getByRole("button", { name: "다음" }).click();
  // 추가 단계가 있으면 끝까지 진행한다(가입 버튼이 나올 때까지 "다음"/"가입" 클릭).
  for (let i = 0; i < 5 && !/\/auth$/.test(page.url()); i++) {
    const submit = page.getByRole("button", { name: /가입|완료|다음/ }).last();
    if (!(await submit.isVisible().catch(() => false))) break;
    await submit.click();
    await page.waitForTimeout(800);
  }
  await expect(page).toHaveURL(/\/auth$/);
  await expect(page.locator(".auth-success")).toBeVisible();

  // 가입 확인이 끝난 테스트 계정은 회원 탈퇴로 바로 정리한다(운영 DB에 쓸 계정이 쌓이지 않게).
  const login = await (await request.post(`${API_BASE}/login/login`, {
    data: { userId: `e2e+${stamp}@tripmate.test`, userPw: "E2eTest!2345" },
  })).json();
  expect(login.success).toBe(true);
  const withdraw = await (await request.post(`${API_BASE}/login/withdraw`, {
    data: { userPw: "E2eTest!2345" },
    headers: { Authorization: `Bearer ${login.token}` },
  })).json();
  expect(withdraw.success).toBe(true);
});

test("E2E-02 로그인 - 메인 이동 및 인증 상태 유지", async ({ page }) => {
  const user = userA();
  await login(page, user);
  await page.goto("/my");
  await expect(page.getByRole("heading", { level: 1, name: user.name })).toBeVisible();
  // 새로고침해도(httpOnly 쿠키) 로그인 상태가 유지되어야 한다.
  await page.reload();
  await expect(page.getByRole("link", { name: "로그아웃" })).toBeVisible();
  await expect(page.getByRole("navigation").getByRole("link", { name: /채팅/ })).toBeVisible();
});

test("E2E-03 잘못된 로그인 - 실패 메시지", async ({ page }) => {
  await page.goto("/auth");
  await page.locator("#userId").fill("not-a-user@tripmate.test");
  await page.locator("#userPw").fill("wrong-password");
  await page.locator("button[type=submit]").click();
  await expect(page.locator(".auth-error")).toBeVisible();
  await expect(page).toHaveURL(/\/auth$/);
});

// ─────────────────────────── 여행지 ───────────────────────────

test("E2E-04 여행지 검색", async ({ page }) => {
  await page.goto("/placeList");
  const box = page.getByRole("main").getByRole("textbox").first();
  await box.fill("해운대");
  await box.press("Enter");
  await expect(placeLinks(page).first()).toBeVisible();
  // 가장 관련도 높은 첫 결과는 검색어를 이름/주소에 포함해야 한다.
  await expect(placeLinks(page).first()).toContainText("해운대");
});

test("E2E-05 여행지 지역 필터", async ({ page }) => {
  await page.goto("/placeList");
  await expect(placeLinks(page).first()).toBeVisible();
  const tab = page.getByRole("tab", { name: "부산" });
  await tab.click();
  await expect(tab).toHaveAttribute("aria-selected", "true");
  await expect(placeLinks(page).first()).toContainText("부산");
  // 주소가 있는 카드는 전부 부산이어야 한다(코스형 관광지는 주소가 없음).
  const texts = await placeLinks(page).allInnerTexts();
  const withAddress = texts.filter((t) => /(특별시|광역시|특별자치도|특별자치시|[가-힣]+도)\s/.test(t));
  expect(withAddress.length).toBeGreaterThan(0);
  for (const t of withAddress) expect(t, `부산이 아닌 카드: ${t}`).toContain("부산");
});

test("E2E-06 여행지 무한 스크롤", async ({ page }) => {
  await page.goto("/placeList");
  await expect(placeLinks(page).first()).toBeVisible();
  const firstPage = await placeLinks(page).evaluateAll((els) => els.map((e) => e.getAttribute("href")));
  expect(firstPage.length).toBeGreaterThanOrEqual(10);

  await page.mouse.wheel(0, 20000);
  await expect.poll(async () => placeLinks(page).count(), { timeout: 15_000 }).toBeGreaterThan(firstPage.length);

  const after = await placeLinks(page).evaluateAll((els) => els.map((e) => e.getAttribute("href")));
  // 기존 데이터가 그대로 앞에 남아 있고, 중복 없이 뒤에 붙어야 한다.
  expect(after.slice(0, firstPage.length)).toEqual(firstPage);
  expect(new Set(after).size, "무한 스크롤 중 중복 카드").toBe(after.length);
});

test("E2E-07 여행지 상세", async ({ page }) => {
  await page.goto("/placeList");
  const card = placeLinks(page).first();
  const name = (await card.locator("p").first().innerText()).trim();
  const href = await card.getAttribute("href");
  await card.click();
  await expect(page).toHaveURL(new RegExp(`${href}$`));
  await expect(page.getByRole("heading", { level: 1, name: "관광지 상세" })).toBeVisible();
  await expect(page.getByRole("main")).toContainText(name);
  await expect(page.getByRole("button", { name: "소개" })).toBeVisible();
});

test("E2E-08 여행지 리뷰", async ({ page }) => {
  // 홈의 인기 관광지는 리뷰 평점 상위라 리뷰가 반드시 있다.
  await page.goto("/");
  const card = page.getByRole("main").locator('a[href^="/place/"]').first();
  await expect(card).toBeVisible();
  const score = (await card.innerText()).match(/★\s*([\d.]+)/)?.[1];
  await card.click();
  await page.getByRole("button", { name: "후기", exact: true }).click();
  await expect(page.getByText("아직 등록된 후기가 없어요")).toHaveCount(0);
  // 후기 작성자는 마스킹(예: 기***석)되어 표시되고, 상단에 평점이 나와야 한다.
  await expect(page.getByRole("main")).toContainText(/[가-힣A-Za-z]\*{3}/);
  if (score) await expect(page.getByRole("main").getByText(score, { exact: true }).first()).toBeVisible();
});

test("E2E-09 AI 여행지 검색 (API)", async ({ request }) => {
  // 프론트에는 AI 여행지 검색 화면이 없어 백엔드 API로 검증한다.
  const res = await request.get(`${API_BASE}/tourList/tourAiSearch`, {
    params: { keyword: "바다가 보이는 조용한 곳", lang: "ko" },
    timeout: 60_000,
  });
  expect(res.ok()).toBeTruthy();
  const body = await res.json();
  expect(body.success, JSON.stringify(body).slice(0, 300)).toBe(true);
  expect(Array.isArray(body.data) ? body.data.length : Object.keys(body.data ?? {}).length).toBeGreaterThan(0);
});

test("E2E-10 다국어 변경 (한국어 → 영어)", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("navigation").getByRole("link", { name: /홈/ })).toBeVisible();
  await page.getByRole("button", { name: "언어" }).click();
  await page.getByRole("button", { name: "English" }).click();

  // 고정 UI 문구
  await expect(page.getByRole("navigation").getByRole("link", { name: /Home/ })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Popular Destinations" })).toBeVisible();

  // 동적 콘텐츠(인기 관광지 이름/주소). 캐시가 없으면 첫 응답은 한국어일 수 있고
  // 백그라운드 번역이 캐시에 저장되므로, 다시 불러오면서 번역될 때까지 기다린다.
  await expect
    .poll(
      async () => {
        await page.reload();
        const cards = page.getByRole("main").locator('a[href^="/place/"]');
        await cards.first().waitFor();
        return (await cards.allInnerTexts()).filter((t) => HANGUL.test(t));
      },
      { timeout: 45_000, intervals: [3_000] },
    )
    .toEqual([]);
});

// ─────────────────────────── 소모임 ───────────────────────────

test("E2E-11 소모임 검색/필터", async ({ page }) => {
  await page.goto("/moimList");
  const box = page.getByRole("main").getByRole("textbox").first();
  await box.fill("세종");
  await box.press("Enter");
  await expect(moimLinks(page).first()).toBeVisible();
  await expect(moimLinks(page).first()).toContainText("세종");

  await page.goto("/moimList");
  const tab = page.getByRole("tab", { name: "부산" });
  await tab.click();
  await expect(tab).toHaveAttribute("aria-selected", "true");
  await expect(moimLinks(page).first()).toContainText("부산");
});

test("E2E-12 소모임 상세", async ({ page }) => {
  await login(page, userA());
  await page.goto("/moimList");
  // E2E-13b가 일정 없이 만든 모임이 맨 앞에 올 수 있어, 그 모임은 빼고 고른다.
  const card = moimLinks(page).filter({ hasNotText: "[E2E] 테스트 모임" }).first();
  const title = (await card.locator("p").first().innerText()).trim();
  await card.click();
  await expect(page).toHaveURL(/\/moim\/M/);
  const main = page.getByRole("main");
  await expect(main).toContainText(title);
  await expect(page.getByRole("heading", { name: "여행 일정" })).toBeVisible();
  await expect(main).toContainText(/DAY 1/);
  await expect(main).toContainText(/\d+\/\d+명/);
});

async function fillMoimStep1And2(page: Page, title: string, dscr: string) {
  await page.goto("/createMoim");
  await expect(page.getByText("1/7")).toBeVisible();
  // 체크박스 input은 시각적으로 숨겨져(blind) 있어 보이는 카드(listitem)를 누른다.
  await page.getByRole("listitem").first().click();
  await expect(page.getByRole("checkbox").first()).toBeChecked();
  await page.getByRole("button", { name: "다음" }).click();

  await expect(page.getByText("2/7")).toBeVisible();
  const inputs = page.locator('input[name="moimTitle"], input[name="moimDscr"]');
  await inputs.nth(0).fill(title);
  await inputs.nth(1).fill(dscr);
  await page.locator(".btn-datePicker").first().click();
  await page.getByRole("button", { name: /next month|다음 달/i }).click();
  const days = page.locator(".rdp-day:not(.rdp-disabled):not(.rdp-outside) button");
  await days.nth(9).click();
  await days.nth(11).click();
  await page.locator("select#region").selectOption("세종");
}

test("E2E-13 소모임 생성 - 단계 이동 시 입력값 유지", async ({ page }) => {
  await login(page, userA());
  const title = `E2E 입력유지 ${Date.now()}`;
  await fillMoimStep1And2(page, title, "E2E 소개글");
  const start = await page.locator(".btn-datePicker").nth(0).innerText();
  const end = await page.locator(".btn-datePicker").nth(1).innerText();
  await page.getByRole("button", { name: "다음" }).click();
  await expect(page.getByText("3/7")).toBeVisible();

  // 헤더 "이전"으로 되돌아가도 2단계 입력값이 그대로여야 한다.
  await page.getByRole("button", { name: "이전" }).first().click();
  await expect(page.getByText("2/7")).toBeVisible();
  await expect(page.locator('input[name="moimTitle"]')).toHaveValue(title);
  await expect(page.locator('input[name="moimDscr"]')).toHaveValue("E2E 소개글");
  await expect(page.locator(".btn-datePicker").nth(0)).toHaveText(start);
  await expect(page.locator(".btn-datePicker").nth(1)).toHaveText(end);
  await expect(page.locator("select#region")).toHaveValue("세종");
});

test("E2E-13b 소모임 생성 - 최종 등록", async ({ page }) => {
  test.skip(!ALLOW_WRITES, WRITE_SKIP_REASON);
  await login(page, userA());
  await fillMoimStep1And2(page, `[E2E] 테스트 모임 ${Date.now()}`, "E2E 자동 테스트로 생성된 모임입니다.");
  await page.getByRole("button", { name: "다음" }).click();
  // 3~6단계: 일정은 비워 둔 채 "다음"/"등록"으로 끝까지 진행.
  // 마지막(7단계) 완료 화면은 단계 표시(n/7)가 없으므로 완료 문구로 판단한다.
  const done = page.getByText("소모임이 등록되었습니다!");
  for (let i = 0; i < 6 && !(await done.isVisible()); i++) {
    await page.getByRole("button", { name: /다음|등록|생성|완료/ }).last().click();
    await page.waitForTimeout(1500);
  }
  await expect(done).toBeVisible({ timeout: 30_000 });
  await expect(page).toHaveURL(/step=7/);
});

test("E2E-14 소모임 참여 신청", async ({ page }) => {
  test.skip(!ALLOW_WRITES, WRITE_SKIP_REASON);
  await login(page, userC());
  await page.goto("/moimList");
  // 목록이 비동기로 그려지므로 카드가 뜬 뒤에 링크를 모은다.
  await expect(moimLinks(page).first()).toBeVisible();
  const hrefs = await moimLinks(page).evaluateAll((els) => els.map((e) => e.getAttribute("href")!));
  let applied = false;
  for (const href of hrefs) {
    await page.goto(href);
    // isVisible은 기다리지 않으므로, 상세가 다 그려진 뒤(일정 제목 표시)에 신청 버튼 유무를 본다.
    await expect(page.getByRole("heading", { name: "여행 일정" })).toBeVisible();
    const apply = page.getByRole("button", { name: "신청하기" });
    if (!(await apply.isVisible())) continue;
    await apply.click();
    await expect(page.getByRole("button", { name: /신청 ?중|승인 대기|참여 중|신청완료|신청 완료/ })).toBeVisible();
    applied = true;
    break;
  }
  expect(applied, "신청 가능한 소모임이 없음").toBe(true);
});

// ─────────────────────────── 채팅 ───────────────────────────

test("E2E-15 채팅방 진입 - 기존 메시지 조회", async ({ page }) => {
  await login(page, userA());
  await page.goto("/chat");
  const room = page.getByRole("main").locator('a[href^="/chat/"]').first();
  await expect(room).toBeVisible();
  await room.click();
  await expect(page).toHaveURL(/\/chat\/.+/);
  await expect(page.locator("article").first()).toBeVisible();
  await expect(page.getByRole("textbox", { name: "메시지를 입력하세요" })).toBeVisible();
});

test("E2E-16/17 실시간 채팅 + 읽음 처리", async ({ browser }) => {
  test.skip(!ALLOW_WRITES, WRITE_SKIP_REASON);
  const a = await (await browser.newContext()).newPage();
  const b = await (await browser.newContext()).newPage();
  await login(a, userA());
  await login(b, userB());

  // A/B가 같이 속한 채팅방을 찾는다(목록이 다 뜬 뒤에 비교).
  const shared = await sharedRoom(a, b);
  expect(shared, "A/B 공통 채팅방이 없음").toBeTruthy();

  // E2E-16: A가 보낸 메시지가 B 화면에 새로고침 없이 나타나야 한다.
  await a.goto(shared!);
  await b.goto(shared!);
  const msg = `[E2E] 실시간 ${Date.now()}`;
  await a.getByRole("textbox", { name: "메시지를 입력하세요" }).fill(msg);
  await a.getByRole("button", { name: "전송" }).click();
  await expect(b.getByText(msg)).toBeVisible({ timeout: 10_000 });

  // E2E-17: B가 목록에 있을 때 온 메시지는 "읽지 않음"에 잡히고, 방에 들어가면 빠져야 한다.
  await b.goto("/chat");
  const msg2 = `[E2E] 읽음 ${Date.now()}`;
  await a.getByRole("textbox", { name: "메시지를 입력하세요" }).fill(msg2);
  await a.getByRole("button", { name: "전송" }).click();
  await expect(a.getByText(msg2)).toBeVisible();
  // 채팅 목록은 실시간 갱신이 없어 다시 불러와서 확인한다.
  await b.reload();
  await b.getByRole("tab", { name: "안읽음" }).click();
  await expect(b.getByRole("main").locator(`a[href="${shared}"]`)).toBeVisible({ timeout: 10_000 });
  await b.goto(shared!);
  await expect(b.getByText(msg2)).toBeVisible();
  await b.goto("/chat");
  await b.getByRole("tab", { name: "안읽음" }).click();
  await expect(b.getByRole("main").locator(`a[href="${shared}"]`)).toHaveCount(0);
});

// ─────────────────────────── 알림 / 마이 ───────────────────────────

test("E2E-18 알림 확인", async ({ page }) => {
  await login(page, userA());
  await page.goto("/notifications");
  const items = listItems(page);
  await expect(items.first()).toBeVisible();
  await expect(items.first().locator("time")).toBeVisible();
  const before = page.url();
  await items.first().getByRole("button").click();
  // 알림을 누르면 해당 화면(채팅방/소모임 등)으로 이동한다.
  await expect.poll(() => page.url()).not.toBe(before);
});

test("E2E-19 마이페이지", async ({ page }) => {
  const user = userA();
  await login(page, user);
  await page.goto("/my");
  await expect(page.getByRole("heading", { level: 1, name: user.name })).toBeVisible();
  await expect(page.getByRole("link", { name: /진행 중 소모임/ })).toBeVisible();
  await expect(page.getByRole("link", { name: "프로필 편집" })).toBeVisible();
  await expect(page.getByRole("heading", { level: 2 }).first()).toBeVisible();
});

test("E2E-20 로그아웃 - 보호 페이지 접근 제한", async ({ page }) => {
  await login(page, userA());
  await page.goto("/my");
  await page.getByRole("link", { name: "로그아웃" }).click();
  await expect(page).toHaveURL(/\/auth$/);
  // 로그인이 필요한 소모임 생성 화면은 로그인 화면으로 돌려보내야 한다.
  await page.goto("/createMoim");
  await expect(page).toHaveURL(/\/auth$/);
  await page.goto("/");
  await expect(page.getByRole("navigation").getByRole("link", { name: /채팅/ })).toHaveCount(0);
});

// ─────────────────────────── 채팅 권한 (보안) ───────────────────────────

async function sharedRoom(a: Page, b: Page) {
  const rooms = async (p: Page) => {
    await p.goto("/chat");
    await p.getByRole("main").locator('a[href^="/chat/"]').first().waitFor();
    return p.getByRole("main").locator('a[href^="/chat/"]').evaluateAll((els) => els.map((e) => e.getAttribute("href")!));
  };
  const bRooms = await rooms(b);
  return (await rooms(a)).find((r) => bRooms.includes(r));
}

test("E2E-26 회원 탈퇴 - 비밀번호 재확인 후 탈퇴, 이후 로그인/기존 토큰 차단", async ({ page, request }) => {
  test.skip(!ALLOW_WRITES, WRITE_SKIP_REASON);
  // 이 시나리오에서만 쓸 계정을 새로 만든다(기존 테스트 계정을 지우지 않도록).
  const stamp = Date.now();
  const user = { id: `e2e+wd${stamp}@tripmate.test`, pw: "E2eTest!2345", name: `E2EWD${String(stamp).slice(-6)}` };
  const signup = await request.post(`${API_BASE}/login/signup`, {
    data: { userId: user.id, userPw: user.pw, userNm: user.name, langCd: "ko", stateCd: "Y", roleCd: "U",
            birthDt: new Date().toISOString().slice(0, 10), genderCd: "", phoneNum: "" },
  });
  expect((await signup.json()).success).toBe(true);
  const token = (await (await request.post(`${API_BASE}/login/login`, { data: { userId: user.id, userPw: user.pw } })).json()).token;

  await login(page, user);
  await page.goto("/my");
  await page.getByRole("link", { name: "회원 탈퇴" }).click();
  await expect(page).toHaveURL(/\/my\/withdraw$/);

  // 동의 전에는 버튼이 비활성, 틀린 비밀번호는 거절
  const submit = page.getByRole("button", { name: "탈퇴하기" });
  await page.getByLabel("비밀번호 확인").fill("wrong-password");
  await expect(submit).toBeDisabled();
  await page.getByText("안내 내용을 확인했고 탈퇴에 동의합니다.").click();
  await submit.click();
  await expect(page.getByRole("alert")).toContainText("비밀번호가 올바르지 않습니다.");

  await page.getByLabel("비밀번호 확인").fill(user.pw);
  await submit.click();
  await expect(page.getByRole("heading", { name: "탈퇴가 완료되었습니다" })).toBeVisible();

  // 다시 로그인할 수 없고, 탈퇴 전에 받은 토큰으로도 로그인 상태가 아니어야 한다.
  const relogin = await (await request.post(`${API_BASE}/login/login`, { data: { userId: user.id, userPw: user.pw } })).json();
  expect(relogin.success).toBe(false);
  const profile = await (await request.get(`${API_BASE}/mypage/profile`, { headers: { Authorization: `Bearer ${token}` } })).json();
  expect(profile.code).toBe("NEED_LOGIN");
});

test("E2E-21 비멤버는 남의 소모임 채팅을 조회/실시간 수신할 수 없다", async ({ browser }) => {
  test.skip(!ALLOW_WRITES, WRITE_SKIP_REASON);
  const a = await (await browser.newContext()).newPage();
  const b = await (await browser.newContext()).newPage();
  const c = await (await browser.newContext()).newPage();
  await login(a, userA());
  await login(b, userB());
  await login(c, userC());
  const room = await sharedRoom(a, b);
  expect(room, "A/B 공통 채팅방이 없음").toBeTruthy();
  const roomId = room!.replace("/chat/", "");

  // REST: C의 로그인 쿠키로 직접 호출해도 403이어야 한다.
  const res = await c.request.get(`${API_BASE}/chat/rooms/${roomId}/messages`);
  const body = await res.json();
  expect(body.status, JSON.stringify(body).slice(0, 200)).toBe(403);
  expect(body.code).toBe("NOT_MEMBER");
  const send = await c.request.post(`${API_BASE}/chat/rooms/${roomId}/messages`, { data: { content: "[E2E] 침입 시도" } });
  expect((await send.json()).code).toBe("NOT_MEMBER");

  // 화면: C가 방 URL로 들어가면 대화가 보이지 않는다.
  await c.goto(room!);
  await expect(c.locator("article")).toHaveCount(0);

  // 실시간: A가 보낸 메시지가 B에게는 오고, C에게는 오지 않는다.
  await a.goto(room!);
  await b.goto(room!);
  await b.locator("article").first().waitFor();
  const msg = `[E2E] 권한 ${Date.now()}`;
  await a.getByRole("textbox", { name: "메시지를 입력하세요" }).fill(msg);
  await a.getByRole("button", { name: "전송" }).click();
  await expect(b.getByText(msg)).toBeVisible({ timeout: 10_000 });
  await c.waitForTimeout(3_000);
  await expect(c.getByText(msg)).toHaveCount(0);
});

test("E2E-27 채팅 번역 - 요청 시 메시지 한 건만 번역하고, 실패해도 원문을 유지하며 비멤버는 거절된다", async ({ browser }) => {
  test.skip(!ALLOW_WRITES, WRITE_SKIP_REASON);
  const a = await (await browser.newContext()).newPage();
  const b = await (await browser.newContext()).newPage();
  const c = await (await browser.newContext()).newPage();
  await login(a, userA());
  await login(b, userB());
  await login(c, userC());
  const room = await sharedRoom(a, b);
  expect(room, "A/B 공통 채팅방이 없음").toBeTruthy();
  const roomId = room!.replace("/chat/", "");

  // 전송은 번역과 무관하게 바로 성공하고, 응답에 원문 언어가 실린다(미리 번역하지 않음).
  const sent = await (await a.request.post(`${API_BASE}/chat/rooms/${roomId}/messages`, {
    data: { content: `Hello from E2E ${Date.now()}` },
  })).json();
  expect(sent.success, JSON.stringify(sent).slice(0, 200)).toBe(true);
  expect(sent.data.srcLangCd).toBe("en");
  expect(sent.data.contentKo ?? null).toBeNull();
  const messageId = sent.data.messageId;
  const translate = (page: Page, lang: string) =>
    page.request.post(`${API_BASE}/chat/rooms/${roomId}/messages/${messageId}/translate?lang=${lang}`, { data: {} });

  // 원문과 같은 언어는 AI 없이 원문 그대로(SOURCE).
  const same = await (await translate(b, "en")).json();
  expect(same.data.state).toBe("SOURCE");
  expect(same.data.content).toBe(sent.data.content);

  // 한국어 요청: AI가 되면 TRANSLATED/CACHED(한글), 안 되면 FAILED + 원문. 어느 쪽이든 요청 자체는 성공.
  const ko = await (await translate(b, "ko")).json();
  expect(ko.success, JSON.stringify(ko).slice(0, 200)).toBe(true);
  expect(["TRANSLATED", "CACHED", "FAILED"]).toContain(ko.data.state);
  if (ko.data.state === "FAILED") expect(ko.data.content).toBe(sent.data.content);
  else expect(HANGUL.test(ko.data.content)).toBe(true);

  // 비멤버/지원하지 않는 언어는 거절.
  expect((await (await translate(c, "ko")).json()).code).toBe("NOT_MEMBER");
  expect((await (await translate(b, "zh")).json()).code).toBe("UNSUPPORTED_LANG");

  // 화면: 한국어 화면에서 영어 메시지에는 번역 버튼이 보이고, 한국어 메시지에는 보이지 않는다.
  await b.goto(room!);
  const english = b.locator("article", { hasText: sent.data.content });
  await expect(english.getByRole("button", { name: /번역|원문 보기/ })).toBeVisible();
  const korean = b.locator("article", { hasText: "[E2E]" }).last();
  if (await korean.count()) await expect(korean.getByRole("button", { name: "번역" })).toHaveCount(0);
});

test("E2E-22 채팅 목록의 방은 모두 열 수 있다(예전 자동 입장으로 남은 방 점검)", async ({ browser }) => {
  const broken: string[] = [];
  for (const user of [userA(), userB(), userC()]) {
    const p = await (await browser.newContext()).newPage();
    await login(p, user);
    const res = await p.request.get(`${API_BASE}/chat/rooms`, { params: { lang: "ko" } });
    const rooms: { roomId: string; title?: string }[] = (await res.json()).data ?? [];
    for (const r of rooms) {
      const m = await (await p.request.get(`${API_BASE}/chat/rooms/${r.roomId}/messages`)).json();
      if (!m.success) broken.push(`${user.id} → ${r.roomId} (${r.title ?? ""}) ${m.code}`);
    }
  }
  expect(broken, "목록에는 있는데 열 수 없는 방").toEqual([]);
});

// ─────────────────────────── 이용정보 (TourAPI 기반) ───────────────────────────

const openInfoTab = async (page: Page, tourId: string, lang: "ko" | "en" | "ja") => {
  await page.addInitScript((l) => localStorage.setItem("i18nextLng", l), lang);
  await page.goto(`/place/${tourId}`);
  const label = { ko: "이용 정보", en: "Information", ja: "利用案内" }[lang];
  const tab = page.getByRole("button", { name: new RegExp(label, "i") }).first();
  if (await tab.count()) await tab.click();
  return page.locator(".info-content");
};

test("E2E-23 이용정보 - 경복궁 ko/en/ja가 TourAPI 값으로 표시되고, 없는 항목(입장료)은 숨긴다", async ({ page }) => {
  // 경복궁: KorService2에는 운영시간/휴무일/주차/문의/홈페이지가 있고 입장료 항목은 없다.
  // en/ja는 EngService2/JpnService2 공식 값(언어 매칭 완료)으로 나와야 한다.
  const cases = [
    { lang: "ko", hours: "운영시간", text: "매주 화요일", fee: "입장료", note: "출처: 한국관광공사 TourAPI" },
    { lang: "en", hours: "Opening Hours", text: "Tuesdays", fee: "Admission Fee", note: "Source: Korea Tourism Organization TourAPI" },
    { lang: "ja", hours: "営業時間", text: "火曜日", fee: "入場料", note: "出典: 韓国観光公社 TourAPI" },
  ] as const;
  for (const c of cases) {
    const p = await page.context().newPage();
    const info = await openInfoTab(p, "126508", c.lang);
    await expect(info.getByText(c.hours, { exact: true })).toBeVisible({ timeout: 15_000 });
    await expect(info).toContainText(c.text);
    await expect(info.getByText(c.fee, { exact: true }), "TourAPI에 없는 항목은 숨김").toHaveCount(0);
    await expect(info).toContainText("royal.khs.go.kr");
    await expect(info).toContainText(c.note);
    if (c.lang !== "ko") {
      // 공식 값만 쓰는 경우 이용정보 목록에 한글이 남지 않아야 한다
      expect(HANGUL.test(await info.locator(".place-infoList").innerText())).toBe(false);
    }
    await p.close();
  }
});

test("E2E-24 이용정보 - 번역이 없으면 한국어 원문으로 대체 표시하고 그 사실을 안내한다", async ({ page }) => {
  // 김춘수 유품전시관: 영문 공식 데이터 없음. (Gemini 사용 불가 상태에서는 번역 저장분도 없음)
  const res = await page.request.get(`${API_BASE}/tourList/tourAIDetail`, { params: { tourId: "1000981", lang: "en" } });
  const data = (await res.json()).data;
  test.skip(data?.source !== "TOURAPI_KO", `이미 번역이 저장되어 대체 표시 상황이 아님(source=${data?.source})`);
  const info = await openInfoTab(page, "1000981", "en");
  await expect(info.getByText("Opening Hours", { exact: true })).toBeVisible({ timeout: 15_000 });
  await expect(info).toContainText("매주 월요일");
  await expect(info).toContainText("shown in Korean because no translation is available");
});

test("E2E-25 이용정보 - AI가 출처 확인 후 보완한 항목은 공식 값과 구분해 출처/확인일을 보여준다 (응답 모킹)", async ({ page }) => {
  // 실제 AI 조사 결과가 DB에 없을 수 있어(운영 DB에 가짜 조사값을 넣지 않음) 화면 표시만 응답 모킹으로 검증한다.
  await page.route("**/tourList/tourAIDetail**", (route) =>
    route.fulfill({
      json: {
        success: true, status: 200, code: "SUCCESS", message: "", path: "", token: "",
        data: {
          operatingHours: "09:00~18:00", closedDays: "매주 화요일", admissionFeeDetails: "대인 3,000원",
          websiteUrl: "https://royal.khs.go.kr/", source: "TOURAPI", researchPending: false,
          fields: {
            operatingHours: { origin: "TOURAPI", translation: "NONE" },
            closedDays: { origin: "TOURAPI", translation: "NONE" },
            admissionFeeDetails: { origin: "AI_RESEARCH", translation: "NONE", sourceUrl: "https://royal.khs.go.kr/fee", sourceTitle: "royal.khs.go.kr", checkedAt: "2026-10-25T10:00:00" },
            websiteUrl: { origin: "TOURAPI", translation: "NONE" },
          },
        },
      },
    }),
  );
  const info = await openInfoTab(page, "126508", "ko");
  const feeRow = info.locator("li", { hasText: "입장료" });
  await expect(feeRow).toContainText("대인 3,000원");
  await expect(feeRow).toContainText("AI가 공식 출처에서 확인한 정보예요");
  await expect(feeRow.getByRole("link", { name: /royal\.khs\.go\.kr/ })).toHaveAttribute("href", "https://royal.khs.go.kr/fee");
  await expect(feeRow).toContainText("2026-10-25");
  // 공식 값 항목에는 AI 표시가 없어야 한다
  await expect(info.locator("li", { hasText: "운영시간" })).not.toContainText("AI가");
});
