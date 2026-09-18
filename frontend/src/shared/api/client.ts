const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

interface ApiSuccess<T> {
  success: true;
  data: T;
}

interface ApiFailure {
  success: false;
  status: number;
  message: string;
  code?: string;
}

type ApiResult<T> = ApiSuccess<T> | ApiFailure;

// 로그인 토큰은 httpOnly 쿠키(accessToken)로 저장/전송되므로 JS에서 직접 읽거나
// 지울 수 없고(보안상 의도된 동작), 지울 필요가 있을 때는 /login/logout 호출로 서버가 쿠키를 만료시킨다.
const req = async<T>(endpoint: string, options?: RequestInit): Promise<ApiResult<T>> => {
    try {
      const res = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        credentials: "include",
        headers: options?.headers,
      });

      if (!res.ok) {
        return {
          success: false,
          status: res.status,
          message: `요청 실패 (${res.status}): ${endpoint}`,
        };
      }

      const data = await res.json();

      // 백엔드는 로그인 필요/처리 실패 등 비즈니스 실패도 HTTP 200으로 내려주고
      // 응답 바디의 success 플래그로만 구분하므로, 여기서도 함께 확인해야 한다.
      // (NEED_LOGIN은 "이 화면/기능은 로그인해야 함"을 뜻할 뿐 세션이 끊어졌다는 뜻은
      // 아니므로, 여기서 더 이상 쿠키를 지우지 않는다 — 실제 로그아웃은 /login/logout에서만 처리)
      if (data && typeof data === "object" && data.success === false) {
        return {
          success: false,
          status: typeof data.status === "number" ? data.status : res.status,
          message: data.message ?? `요청 실패: ${endpoint}`,
          code: data.code,
        };
      }

      return { success: true, data };

  } catch (error) {
    console.error(`API 요청 에러: ${endpoint}`, error);
    return {
      success: false,
      status: 0,
      message: error instanceof Error ? error.message : "알 수 없는 오류",
    };
  }
}

const toQueryString = (params?: Record<string, string | number | boolean | undefined | null>) => {
  if (!params) return "";
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.append(key, String(value));
    }
  });
  const query = search.toString();
  return query ? `?${query}` : "";
};

export const apiClient = {
  get: <T>(endpoint: string, params?: Record<string, string | number | boolean | undefined | null>) =>
    req<T>(`${endpoint}${toQueryString(params)}`),
  post: <T>(endpoint: string, body: unknown) =>
    req<T>(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),
  put: <T>(endpoint: string, body: unknown) =>
    req<T>(endpoint, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),
  delete: <T>(endpoint: string) => req<T>(endpoint, { method: "DELETE" }),
  upload: <T>(endpoint: string, body: FormData) =>
    req<T>(endpoint, { method: "POST", body }),
  logout: () => req<void>("/login/logout", { method: "POST" }),
};
