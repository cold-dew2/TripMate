import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { apiClient } from '@/shared/api/client';
import useMoimDetail from '../../hooks/useMoimDetail';
import useCreateMoimReview from '../../hooks/useCreateMoimReview';
import Textarea from '@/shared/components/textarea/Textarea';
import Button from '@/shared/components/button/Button';
import PageHeader from '@/layouts/components/header/pageHeader/PageHeader';
import './MoimReviewPage.css';

interface FormValues {
  reviewContent: string;
  reviewScore: number;
}

const SCORE_LABEL_KEYS: Record<number, string> = {
  1: 'review.scoreLabel1',
  2: 'review.scoreLabel2',
  3: 'review.scoreLabel3',
  4: 'review.scoreLabel4',
  5: 'review.scoreLabel5',
};

const MoimReviewPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { moimId } = useParams<{ moimId: string }>();
  const { data: result } = useMoimDetail(moimId!);
  const createReview = useCreateMoimReview(moimId!);
  const [files, setFiles] = useState<File[]>([]);

  const { register, handleSubmit, watch, setValue, formState: { errors } } = useForm<FormValues>({
    defaultValues: { reviewContent: '', reviewScore: 5 },
  });
  const score = watch('reviewScore');

  const addFiles = (list: FileList | null) => {
    setFiles((prev) => [...prev, ...Array.from(list ?? [])].slice(0, 5));
  };
  const removeFile = (index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const submit = async (values: FormValues) => {
    const imageUrls = await Promise.all(files.map(async (file) => {
      const body = new FormData();
      body.append('file', file);
      const uploadResult = await apiClient.upload<{ data: { url: string } }>('/uploads', body);
      if (!uploadResult.success) throw uploadResult;
      return uploadResult.data.data.url;
    }));
    await createReview.mutateAsync({ ...values, imageUrls });
    navigate(`/moim/${moimId}`);
  };

  const title = result ? `${t(result.data.moimTitle)} ${t('review.registerTitle')}` : t('review.registerTitle');

  return (
    <div className="moim-review-page">
      <PageHeader contentTitle={title} onBack={() => navigate(-1)} />

      <form className="moim-review-form" onSubmit={handleSubmit(submit)}>
        <p className="moim-review-prompt">{t('review.moimPrompt')}</p>

        <div className="moim-review-score-row">
          <div className="review-star-rating" role="radiogroup" aria-label={t('review.score')}>
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                key={star}
                type="button"
                role="radio"
                aria-checked={score === star}
                className={star <= score ? 'star-btn filled' : 'star-btn'}
                onClick={() => setValue('reviewScore', star)}
              >★</button>
            ))}
          </div>
          <span className="moim-review-score-value">{score.toFixed(1)} ({t(SCORE_LABEL_KEYS[score])})</span>
        </div>
        <input type="hidden" {...register('reviewScore', { valueAsNumber: true })} />

        <Textarea
          label={t('review.content')}
          blind
          rows={5}
          placeholder={t('review.moimContentPlaceholder')}
          error={errors.reviewContent && t('review.contentRequired')}
          {...register('reviewContent', { required: true, maxLength: 1000 })}
        />

        <label className="moim-review-image-label">{t('review.images')}
          <input type="file" accept="image/*" multiple onChange={(event) => addFiles(event.target.files)} />
        </label>
        <ul className="moim-review-image-preview">
          {files.map((file, index) => (
            <li key={`${file.name}-${index}`}>
              <img src={URL.createObjectURL(file)} alt="" />
              <button type="button" className="moim-review-image-remove" onClick={() => removeFile(index)} aria-label={t('common.remove')}>×</button>
            </li>
          ))}
          {files.length < 5 && (
            <li className="moim-review-image-add">
              <label>
                +
                <input type="file" accept="image/*" multiple onChange={(event) => addFiles(event.target.files)} />
              </label>
            </li>
          )}
        </ul>

        <Button
          type="submit"
          text={createReview.isPending ? t('common.saving') : t('review.registerTitle')}
          disabled={createReview.isPending}
        />
      </form>
    </div>
  );
};

export default MoimReviewPage;
