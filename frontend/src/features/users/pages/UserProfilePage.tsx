import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import usePublicProfile from '../hooks/usePublicProfile';
import { resolveImageUrl } from '@/shared/utils/url';
import FilterTabs from '@/shared/components/filterTabs/FilterTabs';
import PageState from '@/shared/components/pageState/PageState';
import { formatDateWithDow } from '@/shared/utils/date';
import './UserProfilePage.css';

type ProfileTab = 'info' | 'moims' | 'reviews';

const UserProfilePage = () => {
  const { t } = useTranslation();
  const { userId } = useParams<{ userId: string }>();
  const { data: result, isLoading, isError, refetch } = usePublicProfile(userId!);
  const [tab, setTab] = useState<ProfileTab>('info');

  if (isLoading) return <PageState status="loading" />;
  if (isError || !result) return <PageState status="error" onRetry={() => refetch()} />;

  const profile = result.data;
  const moims = result.moims ?? [];
  const reviews = result.reviews ?? [];

  return (
    <div className="user-profile-page">
      <section className="user-profile-summary">
        <div className="user-profile-avatar">
          {profile.profileImageUrl ? (
            <img src={resolveImageUrl(profile.profileImageUrl)} alt={t('image.profilePhoto', { name: profile.userNm })} />
          ) : (
            <span aria-hidden="true">🙂</span>
          )}
        </div>
        <div className="user-profile-summary-info">
          <strong>{profile.userNm}</strong>
          <p>{[profile.areaNm].filter(Boolean).join(' · ')}</p>
        </div>
        <div className="user-profile-rating">
          <strong>{profile.rating.toFixed(1)}</strong>
          <span>/ 5.0</span>
          <p>{t('my.getReview')} {profile.reviewCount}{t('명')}</p>
        </div>
      </section>

      <FilterTabs
        options={[
          { id: 'info', label: t('userProfile.tabInfo') },
          { id: 'moims', label: t('userProfile.tabMoims') },
          { id: 'reviews', label: t('userProfile.tabReviews') },
        ]}
        activeId={tab}
        onChange={(id) => setTab(id as ProfileTab)}
      />

      {tab === 'info' && (
        <section className="user-profile-info">
          {profile.description && (
            <p className="user-profile-info-row"><span>{t('my.introduce')}</span>{profile.description}</p>
          )}
          {profile.languages.length > 0 && (
            <p className="user-profile-info-row">
              <span>{t('my.useLang')}</span>
              {profile.languages.map((lang) => lang.langNm).join(', ')}
            </p>
          )}
          {profile.joinDt && (
            <p className="user-profile-info-row"><span>{t('userProfile.joinDt')}</span>{profile.joinDt}</p>
          )}
        </section>
      )}

      {tab === 'moims' && (
        <section className="user-profile-list">
          {moims.length === 0 ? (
            <PageState status="empty" message={t('moim.emptyMsg')} fullScreen={false} />
          ) : (
            <ul>
              {moims.map((moim) => (
                <li key={moim.moimId}>
                  <Link to={`/moim/${moim.moimId}`}>
                    <strong>{t(moim.moimTitle)}</strong>
                    <span>{formatDateWithDow(moim.moimStartDt)}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {tab === 'reviews' && (
        <section className="user-profile-list">
          {reviews.length === 0 ? (
            <PageState status="empty" message={t('my.noReceivedReviews')} fullScreen={false} />
          ) : (
            <ul className="user-profile-review-list">
              {reviews.map((review, index) => (
                <li key={index}>
                  <div className="user-profile-review-top">
                    <strong>{review.userNm}</strong>
                    <span className="stars" aria-hidden="true">{'★'.repeat(review.reviewScore)}</span>
                    <time>{review.createDt}</time>
                  </div>
                  <p>{review.reviewContent}</p>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  );
};

export default UserProfilePage;
