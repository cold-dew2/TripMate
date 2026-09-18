// 실 백엔드/한국관광공사 API 연동이 아직 불안정한 동안, 요청 실패 시
// public/data 아래의 로컬 목업 JSON으로 임시 대체하기 위한 헬퍼.
// 목업 파일이 없거나 404가 나면 null을 반환해 호출 측에서 원래 에러 처리로 넘어가게 한다.
export const fetchMockJson = async <T>(mockPath: string): Promise<T | null> => {
  try {
    const res = await fetch(mockPath);
    if (!res.ok) return null;

    // Vite dev 서버는 존재하지 않는 정적 경로도 SPA fallback으로 index.html(200)을 내려줄 수 있으므로
    // JSON 응답인지 content-type으로 한 번 더 확인한다.
    const contentType = res.headers.get("content-type") ?? "";
    if (!contentType.includes("application/json")) return null;

    console.warn(`[mockFallback] 실 API 대신 목업 데이터를 사용합니다: ${mockPath}`);
    return (await res.json()) as T;
  } catch {
    return null;
  }
};
