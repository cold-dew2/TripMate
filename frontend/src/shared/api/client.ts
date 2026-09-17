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

const clearToken = () => {
  localStorage.removeItem("accessToken");
  sessionStorage.removeItem("accessToken");
};

const req = async<T>(endpoint: string, options?: RequestInit): Promise<ApiResult<T>> => {
    try {
      const token = sessionStorage.getItem("accessToken") || localStorage.getItem("accessToken");
      const res = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
          ...options?.headers,
        },
      });

      if (!res.ok) {
        if (res.status === 401) {
          clearToken();
        }
        return {
          success: false,
          status: res.status,
          message: `요청 실패 (${res.status}): ${endpoint}`,
        };
      }

      const data = await res.json();

      // 백엔드는 로그인 필요/처리 실패 등 비즈니스 실패도 HTTP 200으로 내려주고
      // 응답 바디의 success 플래그로만 구분하므로, 여기서도 함께 확인해야 한다.
      if (data && typeof data === "object" && data.success === false) {
        if (data.code === "NEED_LOGIN") {
          clearToken();
        }
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
};
