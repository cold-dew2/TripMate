import { useState } from 'react';
import type { MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { resolveImageUrl } from '@/shared/utils/url';
import './ReviewImageGrid.css';

interface ReviewImageGridProps {
  urls: string[];
  maxVisible?: number;
}

const ReviewImageGrid = ({ urls, maxVisible = 4 }: ReviewImageGridProps) => {
  const { t } = useTranslation();
  const [activeIndex, setActiveIndex] = useState<number | null>(null);

  if (!urls.length) return null;

  const visibleCount = Math.min(urls.length, maxVisible);
  const hiddenCount = urls.length - visibleCount;

  return (
    <>
      <ul className="review-image-grid">
        {urls.slice(0, visibleCount).map((url, index) => {
          const showMoreOverlay = index === visibleCount - 1 && hiddenCount > 0;
          return (
            <li key={`${url}-${index}`}>
              <button
                type="button"
                className="review-image-grid-item"
                onClick={() => setActiveIndex(index)}
              >
                <img src={resolveImageUrl(url)} alt={t('image.reviewPhoto', { index: index + 1 })} />
                {showMoreOverlay && <span className="review-image-grid-more">+{hiddenCount}</span>}
              </button>
            </li>
          );
        })}
      </ul>
      {activeIndex !== null && (
        <ReviewImageLightbox urls={urls} startIndex={activeIndex} onClose={() => setActiveIndex(null)} />
      )}
    </>
  );
};

interface ReviewImageLightboxProps {
  urls: string[];
  startIndex: number;
  onClose: () => void;
}

const ReviewImageLightbox = ({ urls, startIndex, onClose }: ReviewImageLightboxProps) => {
  const { t } = useTranslation();
  const [index, setIndex] = useState(startIndex);

  const showPrev = (event: MouseEvent) => {
    event.stopPropagation();
    setIndex((prev) => (prev - 1 + urls.length) % urls.length);
  };
  const showNext = (event: MouseEvent) => {
    event.stopPropagation();
    setIndex((prev) => (prev + 1) % urls.length);
  };

  return (
    <div className="review-image-lightbox-dim" role="presentation" onMouseDown={onClose}>
      <div
        className="review-image-lightbox"
        role="dialog"
        aria-modal="true"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button type="button" className="review-image-lightbox-close" onClick={onClose} aria-label={t('common.close')}>
          ✕
        </button>
        <img src={resolveImageUrl(urls[index])} alt={t('image.reviewPhoto', { index: index + 1 })} />
        {urls.length > 1 && (
          <>
            <button type="button" className="review-image-lightbox-nav prev" onClick={showPrev} aria-label={t('common.previous')}>
              ‹
            </button>
            <button type="button" className="review-image-lightbox-nav next" onClick={showNext} aria-label={t('common.next')}>
              ›
            </button>
            <div className="review-image-lightbox-counter">{index + 1} / {urls.length}</div>
          </>
        )}
      </div>
    </div>
  );
};

export default ReviewImageGrid;
