import { notifyBackendUnreachable } from "./networkStatus";

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

const req = async <T>(
  endpoint: string,
  options?: RequestInit
): Promise<ApiResult<T>> => {
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

    // 응답이 비어 있을 수도 있으므로 text로 먼저 받는다.
    const text = await res.text();

    // 204 또는 빈 응답 처리
    if (!text.trim()) {
      return {
        success: true,
        data: undefined as T,
      };
    }

    let data: any;

    try {
      data = JSON.parse(text);
    } catch (error) {
      console.error(`JSON 파싱 실패: ${endpoint}`, text);
      return {
        success: false,
        status: res.status,
        message: "서버 응답을 처리할 수 없습니다.",
      };
    }

    // 백엔드는 HTTP 200이어도 body의 success=false로
    // 비즈니스 실패를 내려줄 수 있다.
    if (data && typeof data === "object" && data.success === false) {
      return {
        success: false,
        status: typeof data.status === "number" ? data.status : res.status,
        message: data.message ?? `요청 실패: ${endpoint}`,
        code: data.code,
      };
    }

    return {
      success: true,
      data,
    };
  } catch (error) {
    console.error(`API 요청 에러: ${endpoint}`, error);

    notifyBackendUnreachable();

    return {
      success: false,
      status: 0,
      message: error instanceof Error ? error.message : "알 수 없는 오류",
    };
  }
};

const toQueryString = (
  params?: Record<
    string,
    string | number | boolean | undefined | null
  >
) => {
  if (!params) return "";

  const search = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (
      value !== undefined &&
      value !== null &&
      value !== ""
    ) {
      search.append(key, String(value));
    }
  });

  const query = search.toString();

  return query ? `?${query}` : "";
};

export const apiClient = {
  get: <T>(
    endpoint: string,
    params?: Record<
      string,
      string | number | boolean | undefined | null
    >
  ) =>
    req<T>(
      `${endpoint}${toQueryString(params)}`
    ),

  post: <T>(
    endpoint: string,
    body: unknown
  ) =>
    req<T>(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    }),

  put: <T>(
    endpoint: string,
    body: unknown
  ) =>
    req<T>(endpoint, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    }),

  delete: <T>(endpoint: string) =>
    req<T>(endpoint, {
      method: "DELETE",
    }),

  upload: <T>(
    endpoint: string,
    body: FormData
  ) =>
    req<T>(endpoint, {
      method: "POST",
      body,
    }),

  logout: () =>
    req<void>("/login/logout", {
      method: "POST",
    }),
};