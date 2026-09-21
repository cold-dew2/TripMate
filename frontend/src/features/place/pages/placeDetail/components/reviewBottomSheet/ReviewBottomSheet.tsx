import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { apiClient } from '@/shared/api/client';
import { useCreateReview } from '@/features/place/hooks/useReveiws';
import Input from '@/shared/components/input/Input';
import Textarea from '@/shared/components/textarea/Textarea';
import Button from '@/shared/components/button/Button';
import './ReviewBottomSheet.css';

interface FormValues { reviewTitle: string; reviewContent: string; reviewScore: number }
const SCORE_LABEL_KEYS: Record<number, string> = {
  1: 'review.scoreLabel1',
  2: 'review.scoreLabel2',
  3: 'review.scoreLabel3',
  4: 'review.scoreLabel4',
  5: 'review.scoreLabel5',
};

export default function ReviewBottomSheet({ tourId, onClose }: { tourId: string; onClose: () => void }) {
  const { t } = useTranslation();
  const { register, handleSubmit, watch, setValue, formState: { errors } } = useForm<FormValues>({ defaultValues: { reviewScore: 5 } });
  const [files, setFiles] = useState<File[]>([]);
  const createReview = useCreateReview();
  const score = watch('reviewScore');

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const addFiles = (list: FileList | null) => {
    setFiles((prev) => [...prev, ...Array.from(list ?? [])].slice(0, 5));
  };
  const removeFile = (index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const submit = async (values: FormValues) => {
    const imageUrls = await Promise.all(files.map(async file => {
      const body = new FormData(); body.append('file', file);
      const result = await apiClient.upload<{ data: { url: string } }>('/uploads', body);
      if (!result.success) throw result;
      return result.data.data.url;
    }));
    await createReview.mutateAsync({ tourId, ...values, imageUrls }); onClose();
  };

  return <div className="bottom-sheet-dim" role="presentation" onMouseDown={onClose}>
    <section className="review-bottom-sheet" role="dialog" aria-modal="true" onMouseDown={event => event.stopPropagation()}>
      <div className="sheet-handle" />
      <button type="button" className="sheet-close-btn" onClick={onClose} aria-label={t('common.close')}>✕</button>
      <h2>{t('place.reviewsBtn')}</h2>
      <form onSubmit={handleSubmit(submit)}>
        <label className="review-score-label">{t('review.score')}
          <div className="review-star-rating" role="radiogroup" aria-label={t('review.score')}>
            {[1, 2, 3, 4, 5].map(star => (
              <button
                key={star}
                type="button"
                role="radio"
                aria-checked={score === star}
                aria-label={`${star} - ${t(SCORE_LABEL_KEYS[star])}`}
                className={star <= score ? 'star-btn filled' : 'star-btn'}
                onClick={() => setValue('reviewScore', star)}
              >★</button>
            ))}
          </div>
          <span className="review-score-value">{score.toFixed(1)} ({t(SCORE_LABEL_KEYS[score])})</span>
        </label>
        <input type="hidden" {...register('reviewScore', { valueAsNumber: true })} />

        <Input
          label={t('review.title')}
          error={errors.reviewTitle && t('review.titleRequired')}
          {...register('reviewTitle', { required: true, maxLength: 60 })}
        />
        <Textarea
          label={t('review.content')}
          rows={5}
          error={errors.reviewContent && t('review.contentRequired')}
          {...register('reviewContent', { required: true, maxLength: 1000 })}
        />

        <label className="review-image-label">{t('review.images')}
          <input type="file" accept="image/*" multiple onChange={event => addFiles(event.target.files)} />
        </label>
        {!!files.length && (
          <ul className="review-image-preview">
            {files.map((file, index) => (
              <li key={`${file.name}-${index}`}>
                <img src={URL.createObjectURL(file)} alt="" />
                <button type="button" className="review-image-remove" onClick={() => removeFile(index)} aria-label={t('common.remove')}>×</button>
              </li>
            ))}
          </ul>
        )}

        <Button type="submit" text={createReview.isPending ? t('common.saving') : t('common.submit')} disabled={createReview.isPending} />
      </form>
    </section>
  </div>;
}
