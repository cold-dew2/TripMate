const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

interface ApiSuccess<T> {
  success: true;
  data: T;
}

interface ApiFailure {
  success: false;
  status: number;
  message: string;
}

type ApiResult<T> = ApiSuccess<T> | ApiFailure;

const req = async<T>(endpoint: string, options?: RequestInit): Promise<ApiResult<T>> => {
    try {
      const res = await fetch(`${API_BASE_URL}${endpoint}`, options);

      if (!res.ok) {
        return {
          success: false,
          status: res.status,
          message: `요청 실패 (${res.status}): ${endpoint}`,
        };
      }

      const data = await res.json();
      return { success: true, data };

  } catch (error) {
    console.error("API 요청 에러");
    return {
      success: false,
      status: 0,
      message: error instanceof Error ? error.message : "알 수 없는 오류",
    };
  }
}

export const apiClient = {
  get: <T>(endpoint: string) => req<T>(endpoint),
  post: <T>(endpoint: string, body: unknown) =>
    req<T>(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),
};