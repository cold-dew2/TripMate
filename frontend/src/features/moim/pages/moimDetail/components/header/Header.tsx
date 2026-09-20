import type { MoimCategory, MoimDetail } from '@/types/moim';
import { useTranslation } from 'react-i18next';
import { resolveImageUrl } from '@/shared/utils/url';
import './Header.css';

interface moimDetailProps {
  moim: MoimDetail;
  cate: MoimCategory[];
  onShare: () => void;
}

const Header = ({ moim, cate, onShare }: moimDetailProps) => {
  const { t } = useTranslation();

  return (
    <div className="detail-header">
      <div className="detail-img">
        <img src={resolveImageUrl(moim.imageUrl) || "/images/places/no-image.svg"} alt={t('image.alt', { title: moim.moimTitle })} />
        <button type="button" className="detail-share-btn" onClick={onShare} aria-label={t('moim.share')}>
          <span aria-hidden="true">🔗</span>
        </button>
      </div>
      <div className="detail-info">
        <div className="place-name">{moim.moimTitle}</div>
        <div className="tags">
          {cate.map((category) => (
            <span key={category.cateCd} className="tag-badge">{t(category.cateNm)}</span>
          ))}
          <span className="tag-badge tag-badge-muted">{moim.moimStartDt}</span>
          <span className="tag-badge tag-badge-muted">{moim.memberCnt}/{moim.maxMember}{t("명")}</span>
        </div>
      </div>
    </div>
  )
}

export default Header
