import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { apiClient } from '@/shared/api/client';
import { resolveImageUrl } from '@/shared/utils/url';
import './MyPageScreen.css';

interface LanguageCard {
  langCd: string;
  langNm: string;
  levelNm: string;
}

interface RecentReview {
  reviewId: string;
  reviewerName: string;
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

// .env의 VITE_API_BASE_URL이 /data(로컬 목업 JSON)를 가리킬 때는
// 백엔드/로그인 없이도 마이페이지 디자인을 확인할 수 있도록 게스트 화면을 건너뛴다.
const isMock = (import.meta.env.VITE_API_BASE_URL ?? '').startsWith('/data');

const MyPageScreen = () => {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const isLoggedIn = isMock || !!(localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken'));

  const profile = useQuery({
    queryKey: ['myProfile'],
    queryFn: async () => {
      const r = await apiClient.get<{ data: MyProfile }>('/mypage/profile');
      if (!r.success) throw r;
      return r.data.data;
    },
    enabled: isLoggedIn,
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
      queryClient.invalidateQueries({ queryKey: ['myProfile'] });
    },
  });

  const handlePhotoSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) uploadPhoto.mutate(file);
    event.target.value = '';
  };

  if (!isLoggedIn) {
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
  const langSummary = languages.map((lang) => lang.langNm).join(', ');

  return (
    <main className="mypage-screen">
      <div className="mypage-hero-bg" />

      <section className="mypage-profile">
        <div className="mypage-avatar-row">
          <div className="mypage-avatar-wrap">
            <div className="mypage-avatar">
              {p?.profileImageUrl ? (
                <img src={resolveImageUrl(p.profileImageUrl)} alt={t('image.profilePhoto', { name: p.userNm })} />
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

        {profile.isLoading ? (
          <p className="mypage-loading">{t('account.loading')}</p>
        ) : (
          <>
            <h1 className="mypage-name">{p?.userNm}</h1>
            <p className="mypage-meta">{[p?.areaNm, langSummary].filter(Boolean).join(' · ')}</p>
            <p className="mypage-rating">
              <span className="stars" aria-hidden="true">{'★'.repeat(Math.round(p?.rating ?? 0))}</span>
              <strong>{(p?.rating ?? 0).toFixed(1)}</strong>
              <span className="count">({p?.reviewCount ?? 0})</span>
            </p>
            {p?.description && <p className="mypage-desc">{p.description}</p>}
          </>
        )}

        <div className="mypage-stats">
          <div>
            <strong>{p?.ongoingMoimCount ?? 0}</strong>
            <span>{t('my.moiming')}</span>
          </div>
          <div>
            <strong>{p?.memberCount ?? 0}</strong>
            <span>{t('my.member')}</span>
          </div>
          <div>
            <strong>{p?.reviewCount ?? 0}</strong>
            <span>{t('my.getReview')}</span>
          </div>
        </div>
      </section>

      {languages.length > 0 && (
        <section className="mypage-langs">
          <h2>{t('my.useLang')}</h2>
          <div className="mypage-lang-cards">
            {languages.map((lang) => (
              <div key={lang.langCd} className="mypage-lang-card">
                <span className="flag" aria-hidden="true">{FLAG_BY_LANG[lang.langCd] ?? '🌐'}</span>
                <strong>{lang.langNm}</strong>
                <span className="level">{lang.levelNm}</span>
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
                <div className="mypage-review-avatar" aria-hidden="true" />
                <div className="mypage-review-body">
                  <div className="mypage-review-top">
                    <div>
                      <strong>{review.reviewerName}</strong>
                      <span className="stars" aria-hidden="true">{'★'.repeat(review.reviewScore)}</span>
                    </div>
                    <time>{review.createDt}</time>
                  </div>
                  <p>{review.reviewContent}</p>
                  {review.imgUrls && (
                    <ul className="mypage-review-images">
                      {review.imgUrls.split(',').map((url, index) => (
                        <li key={url}><img src={resolveImageUrl(url)} alt={t('image.reviewPhoto', { index: index + 1 })} /></li>
                      ))}
                    </ul>
                  )}
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

      <button type="button" className="mypage-logout" onClick={() => {
        localStorage.removeItem('accessToken');
        sessionStorage.removeItem('accessToken');
        window.location.href = '/auth';
      }}>
        {t('my.logout')}
      </button>
    </main>
  );
};

export default MyPageScreen;
