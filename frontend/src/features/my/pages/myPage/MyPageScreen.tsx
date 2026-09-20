import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { apiClient } from '@/shared/api/client';
import { resolveImageUrl } from '@/shared/utils/url';
import { getApiLang } from '@/shared/utils/lang';
import PageState from '@/shared/components/pageState/PageState';
import './MyPageScreen.css';

interface LanguageCard {
  langCd: string;
  langNm: string;
  levelNm: string;
}

interface RecentReview {
  reviewId: string;
  reviewerName: string;
  reviewerProfileImgUrl?: string | null;
  reviewScore: number;
  reviewContent: string;
  createDt: string;
  imgUrls?: string | null;
}

interface MyProfile {
  userNm: string;
  areaNm: string;
  description: string;
  profileImageUrl: string;
  rating: number;
  reviewCount: number;
  ongoingMoimCount: number;
  memberCount: number;
  languages: LanguageCard[];
  recentReviews: RecentReview[];
}

const FLAG_BY_LANG: Record<string, string> = { ko: '🇰🇷', en: '🇺🇸', ja: '🇯🇵' };
// 서버의 LANG_NM은 공통코드명이라 항상 한국어로 내려온다. 언어 코드 자체는 ko/en/ja로
// 고정돼 있으므로 AI 번역 없이 프론트 i18n 키로 바로 옮긴다(다른 화면의 언어 선택 드롭다운과 동일한 키).
const LANG_NAME_KEY: Record<string, string> = { ko: 'lang.ko', en: 'lang.en', ja: 'lang.jp' };

const MyPageScreen = () => {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  // 업로드 기록은 있는데 실제 파일이 사라졌거나 경로가 잘못된 경우(깨진 이미지 아이콘) 조용히
  // 기본 아바타로 대체하기 위한 상태. 프로필 사진 하나 + 최신 후기 목록(reviewId로 구분) 각각 추적.
  const [avatarBroken, setAvatarBroken] = useState(false);
  const [brokenReviewAvatars, setBrokenReviewAvatars] = useState<Set<string>>(new Set());

  // 로그인 토큰이 httpOnly 쿠키라 JS에서 로그인 여부를 미리 알 수 없으므로,
  // 항상 호출해보고 결과(성공/NEED_LOGIN)로 로그인 여부를 판단한다.
  const profile = useQuery({
    queryKey: ['myProfile', getApiLang()],
    queryFn: async () => {
      const r = await apiClient.get<{ data: MyProfile }>(`/mypage/profile?lang=${getApiLang()}`);
      if (!r.success) throw r;
      return r.data.data;
    },
  });

  const uploadPhoto = useMutation({
    mutationFn: async (file: File) => {
      const body = new FormData();
      body.append('file', file);
      const uploadResult = await apiClient.upload<{ data: { url: string } }>('/uploads', body);
      if (!uploadResult.success) throw uploadResult;

      const putResult = await apiClient.put<{ data: MyProfile }>('/mypage/profile', {
        profileImageUrl: uploadResult.data.data.url,
      });
      if (!putResult.success) throw putResult;
      return putResult.data.data;
    },
    onSuccess: () => {
      setAvatarBroken(false);
      queryClient.invalidateQueries({ queryKey: ['myProfile'] });
    },
  });

  const handlePhotoSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) uploadPhoto.mutate(file);
    event.target.value = '';
  };

  if (profile.isLoading) {
    return (
      <main className="mypage-screen">
        <PageState status="loading" />
      </main>
    );
  }

  const needsLogin = (profile.error as { code?: string } | null)?.code === 'NEED_LOGIN';

  if (profile.isError && !needsLogin) {
    return (
      <main className="mypage-screen">
        <PageState status="error" onRetry={() => profile.refetch()} />
      </main>
    );
  }

  if (!profile.data) {
    return (
      <main className="mypage-screen">
        <div className="mypage-hero-bg" />
        <section className="mypage-guest">
          <div className="mypage-avatar">
            <span aria-hidden="true">🙂</span>
          </div>
          <h1 className="mypage-name">{t('home.guest')}</h1>
          <p className="mypage-guest-desc">{t('my.guestDesc')}</p>
          <Link to="/auth" className="mypage-guest-login">{t('account.login')}</Link>
        </section>
      </main>
    );
  }

  const p = profile.data;
  const languages = p?.languages ?? [];
  const reviews = p?.recentReviews ?? [];
  const langLabel = (lang: LanguageCard) => (LANG_NAME_KEY[lang.langCd] ? t(LANG_NAME_KEY[lang.langCd]) : lang.langNm);
  const langSummary = languages.map(langLabel).join(', ');

  return (
    <main className="mypage-screen">
      <div className="mypage-hero-bg" />

      <section className="mypage-profile">
        <div className="mypage-avatar-row">
          <div className="mypage-avatar-wrap">
            <div className="mypage-avatar">
              {p?.profileImageUrl && !avatarBroken ? (
                <img
                  src={resolveImageUrl(p.profileImageUrl)}
                  alt={t('image.profilePhoto', { name: p.userNm })}
                  onError={() => setAvatarBroken(true)}
                />
              ) : (
                <span aria-hidden="true">🙂</span>
              )}
            </div>
            <label className="mypage-avatar-badge" aria-label={t('my.uploadPhoto')}>
              📷
              <input type="file" accept="image/*" onChange={handlePhotoSelect} disabled={uploadPhoto.isPending} />
            </label>
          </div>
          <Link to="/my/edit" className="mypage-edit-btn">{t('account.edit')}</Link>
        </div>

        <h1 className="mypage-name">{p?.userNm}</h1>
        <p className="mypage-meta">{[p?.areaNm, langSummary].filter(Boolean).join(' · ')}</p>
        <p className="mypage-rating">
          <span className="stars" aria-hidden="true">{'★'.repeat(Math.round(p?.rating ?? 0))}</span>
          <strong>{(p?.rating ?? 0).toFixed(1)}</strong>
          <span className="count">({p?.reviewCount ?? 0})</span>
        </p>
        {p?.description && <p className="mypage-desc">{p.description}</p>}

        <div className="mypage-stats">
          <Link to="/moimManage">
            <strong>{p?.ongoingMoimCount ?? 0}</strong>
            <span>{t('my.moiming')}</span>
          </Link>
          <div>
            <strong>{p?.memberCount ?? 0}</strong>
            <span>{t('my.member')}</span>
          </div>
          <Link to="/my/reviews">
            <strong>{p?.reviewCount ?? 0}</strong>
            <span>{t('my.getReview')}</span>
          </Link>
        </div>
      </section>

      {languages.length > 0 && (
        <section className="mypage-langs">
          <h2>{t('my.useLang')}</h2>
          <div className="mypage-lang-cards">
            {languages.map((lang) => (
              <div key={lang.langCd} className="mypage-lang-card">
                <span className="flag" aria-hidden="true">{FLAG_BY_LANG[lang.langCd] ?? '🌐'}</span>
                <strong>{langLabel(lang)}</strong>
                <span className="level" aria-label={t('my.langLevel', { level: Number(lang.levelNm) || 0 })}>
                  {'★'.repeat(Number(lang.levelNm) || 0)}
                  {'☆'.repeat(Math.max(0, 5 - (Number(lang.levelNm) || 0)))}
                </span>
              </div>
            ))}
          </div>
        </section>
      )}

      <section className="mypage-reviews">
        <div className="mypage-section-head">
          <h2>{t('my.recentlyReviews')}</h2>
          <Link to="/my/reviews">{t('home.viewAll')} ›</Link>
        </div>
        {reviews.length === 0 ? (
          <p className="mypage-empty">{t('my.noReviewsYet')}</p>
        ) : (
          <ul className="mypage-review-list">
            {reviews.map((review) => (
              <li key={review.reviewId}>
                {review.reviewerProfileImgUrl && !brokenReviewAvatars.has(review.reviewId) ? (
                  <img
                    className="mypage-review-avatar"
                    src={resolveImageUrl(review.reviewerProfileImgUrl)}
                    alt=""
                    onError={() => setBrokenReviewAvatars((prev) => new Set(prev).add(review.reviewId))}
                  />
                ) : (
                  <div className="mypage-review-avatar" aria-hidden="true" />
                )}
                <div className="mypage-review-body">
                  <div className="mypage-review-top">
                    <div>
                      <strong>{review.reviewerName}</strong>
                      <span className="stars" aria-hidden="true">{'★'.repeat(review.reviewScore)}</span>
                    </div>
                    <time>{review.createDt}</time>
                  </div>
                  <div className="mypage-review-content">
                    <p>{review.reviewContent}</p>
                    {review.imgUrls && (
                      <ul className="mypage-review-images">
                        {review.imgUrls.split(',').map((url, index) => (
                          <li key={url}><img src={resolveImageUrl(url)} alt={t('image.reviewPhoto', { index: index + 1 })} /></li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <ul className="mypage-menu" aria-label={t('account.activity')}>
        <li><Link to="/my/schedule">{t('my.myPlan')}<i aria-hidden="true">›</i></Link></li>
        <li><Link to="/moimManage">{t('my.myMoim')}<i aria-hidden="true">›</i></Link></li>
        <li><Link to="/safetyReport">{t('my.declaration')}<i aria-hidden="true">›</i></Link></li>
      </ul>

      <button type="button" className="mypage-logout" onClick={async () => {
        await apiClient.logout();
        window.location.href = '/auth';
      }}>
        {t('my.logout')}
      </button>
    </main>
  );
};

export default MyPageScreen;
