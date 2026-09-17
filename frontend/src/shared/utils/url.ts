const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const resolveImageUrl = (url?: string | null) => {
  if (!url) return "";
  if (/^https?:\/\//.test(url)) return url;
  return `${API_BASE_URL}${url}`;
};
