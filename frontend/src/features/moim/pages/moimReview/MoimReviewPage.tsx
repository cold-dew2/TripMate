import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import { apiClient } from '@/shared/api/client';
import { useAlert } from '@/shared/contexts/AlertContext';
import useMoimDetail from '../../hooks/useMoimDetail';
import useCreateMoimReview from '../../hooks/useCreateMoimReview';
import { useCreateReview } from '@/features/place/hooks/useReveiws';
import Input from '@/shared/components/input/Input';
import Select from '@/shared/components/select/Select';
import Textarea from '@/shared/components/textarea/Textarea';
import Button from '@/shared/components/button/Button';
import PageHeader from '@/layouts/components/header/pageHeader/PageHeader';
import './MoimReviewPage.css';

type FlowStep = 'choose' | 'writeTour' | 'writeMoim';

interface MoimFormValues {
  reviewContent: string;
  reviewScore: number;
}

interface TourFormValues {
  tourId: string;
  reviewTitle: string;
  reviewContent: string;
  reviewScore: number;
}

const uploadFiles = async (files: File[]) => {
  return Promise.all(files.map(async (file) => {
    const body = new FormData();
    body.append('file', file);
    const uploadResult = await apiClient.upload<{ data: { url: string } }>('/uploads', body);
    if (!uploadResult.success) throw uploadResult;
    return uploadResult.data.data.url;
  }));
};

const SCORE_LABEL_KEYS: Record<number, string> = {
  1: 'review.scoreLabel1',
  2: 'review.scoreLabel2',
  3: 'review.scoreLabel3',
  4: 'review.scoreLabel4',
  5: 'review.scoreLabel5',
};

const StarRating = ({ score, onChange, label }: { score: number; onChange: (value: number) => void; label: string }) => {
  const { t } = useTranslation();
  return (
    <div className="review-score-block">
      <span className="review-score-block-label">{label}</span>
      <div className="review-star-row">
        <div className="review-star-rating" role="radiogroup" aria-label={label}>
          {[1, 2, 3, 4, 5].map((star) => (
            <button
              key={star}
              type="button"
              role="radio"
              aria-checked={score === star}
              aria-label={`${star} - ${t(SCORE_LABEL_KEYS[star])}`}
              className={star <= score ? 'star-btn filled' : 'star-btn'}
              onClick={() => onChange(star)}
            >★</button>
          ))}
        </div>
        <span className="review-score-value">{score.toFixed(1)}</span>
      </div>
    </div>
  );
};

const ImagePicker = ({ files, onAdd, onRemove, label, removeLabel }: { files: File[]; onAdd: (list: FileList | null) => void; onRemove: (index: number) => void; label: string; removeLabel: string }) => (
  <div className="review-image-block">
    <label className="review-image-label">{label}
      <input type="file" accept="image/*" multiple onChange={(event) => onAdd(event.target.files)} />
    </label>
    <ul className="review-image-preview">
      {files.map((file, index) => (
        <li key={`${file.name}-${index}`}>
          <img src={URL.createObjectURL(file)} alt="" />
          <button type="button" className="review-image-remove" onClick={() => onRemove(index)} aria-label={removeLabel}>×</button>
        </li>
      ))}
      {files.length < 5 && (
        <li className="review-image-add">
          <label>
            <span aria-hidden="true">+</span>
            <input type="file" accept="image/*" multiple onChange={(event) => onAdd(event.target.files)} />
          </label>
        </li>
      )}
    </ul>
  </div>
);

const MoimReviewPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showAlert } = useAlert();
  const { moimId } = useParams<{ moimId: string }>();
  const { data: result } = useMoimDetail(moimId!);
  const createMoimReview = useCreateMoimReview(moimId!);
  const createTourReview = useCreateReview();

  const [step, setStep] = useState<FlowStep>('choose');
  const [tourWritten, setTourWritten] = useState(false);
  const [moimWritten, setMoimWritten] = useState(false);
  // 이번 방문 중에 이미 추가리뷰로 남긴 관광지들. 다음 관광지 선택 목록에서 빼서
  // 같은 곳을 실수로 중복 선택하지 않도록 한다.
  const [reviewedTourIds, setReviewedTourIds] = useState<string[]>([]);
  const [tourFiles, setTourFiles] = useState<File[]>([]);
  const [moimFiles, setMoimFiles] = useState<File[]>([]);

  const places = useMemo(() => {
    const seen = new Map<string, string>();
    (result?.plan ?? []).forEach((item) => {
      if (item.tourId && !seen.has(item.tourId)) seen.set(item.tourId, item.tourNm);
    });
    return Array.from(seen, ([tourId, tourNm]) => ({ tourId, tourNm }));
  }, [result?.plan]);

  const availableTourPlaces = useMemo(
    () => places.filter((place) => !reviewedTourIds.includes(place.tourId)),
    [places, reviewedTourIds]
  );

  const {
    register: registerTour,
    handleSubmit: handleTourSubmit,
    watch: watchTour,
    setValue: setTourValue,
    reset: resetTourForm,
    formState: { errors: tourErrors },
  } = useForm<TourFormValues>({ defaultValues: { reviewScore: 5, tourId: '' } });
  const tourScore = watchTour('reviewScore');

  const {
    register: registerMoim,
    handleSubmit: handleMoimSubmit,
    watch: watchMoim,
    setValue: setMoimValue,
    formState: { errors: moimErrors },
  } = useForm<MoimFormValues>({ defaultValues: { reviewContent: '', reviewScore: 5 } });
  const moimScore = watchMoim('reviewScore');

  useEffect(() => {
    if (availableTourPlaces.length > 0) setTourValue('tourId', availableTourPlaces[0].tourId);
  }, [availableTourPlaces, setTourValue]);

  useEffect(() => {
    if (result?.reviewStatus) {
      setTourWritten(result.reviewStatus.tourReviewedYn === 'Y');
      setMoimWritten(result.reviewStatus.moimReviewedYn === 'Y');
    }
  }, [result?.reviewStatus]);

  const addTourFiles = (list: FileList | null) => setTourFiles((prev) => [...prev, ...Array.from(list ?? [])].slice(0, 5));
  const removeTourFile = (index: number) => setTourFiles((prev) => prev.filter((_, i) => i !== index));
  const addMoimFiles = (list: FileList | null) => setMoimFiles((prev) => [...prev, ...Array.from(list ?? [])].slice(0, 5));
  const removeMoimFile = (index: number) => setMoimFiles((prev) => prev.filter((_, i) => i !== index));

  const submitTourReview = async (values: TourFormValues) => {
    try {
      const imageUrls = await uploadFiles(tourFiles);
      await createTourReview.mutateAsync({ ...values, moimId, imageUrls });
    } catch (error) {
      showAlert((error as { message?: string })?.message ?? t('review.submitFailed'));
      return;
    }
    setReviewedTourIds((prev) => [...prev, values.tourId]);
    setTourWritten(true);
    if (moimWritten) {
      showAlert(t('review.flowComplete'));
      navigate(`/moim/${moimId}`);
    } else {
      setStep('choose');
    }
  };

  // "추가리뷰 남기기": 현재 작성한 리뷰만 저장하고, 다음 관광지를 이어서 쓸 수 있게
  // writeTour 단계에 그대로 남아 폼을 초기화한다(다른 리뷰 유형으로 넘어가지 않음).
  const addAnotherTourReview = async (values: TourFormValues) => {
    try {
      const imageUrls = await uploadFiles(tourFiles);
      await createTourReview.mutateAsync({ ...values, moimId, imageUrls });
    } catch (error) {
      showAlert((error as { message?: string })?.message ?? t('review.submitFailed'));
      return;
    }
    setReviewedTourIds((prev) => [...prev, values.tourId]);
    setTourWritten(true);
    setTourFiles([]);
    resetTourForm({ reviewScore: 5, tourId: '', reviewTitle: '', reviewContent: '' });
  };

  const submitMoimReview = async (values: MoimFormValues) => {
    try {
      const imageUrls = await uploadFiles(moimFiles);
      await createMoimReview.mutateAsync({ ...values, imageUrls });
    } catch (error) {
      showAlert((error as { message?: string })?.message ?? t('review.submitFailed'));
      return;
    }
    setMoimWritten(true);
    if (tourWritten || places.length === 0) {
      showAlert(t('review.flowComplete'));
      navigate(`/moim/${moimId}`);
    } else {
      setStep('choose');
    }
  };

  const handleBack = () => {
    if (step === 'choose') {
      navigate(-1);
    } else {
      setStep('choose');
    }
  };

  // moimTitle은 사용자가 지은 모임 이름(서버가 이미 요청 언어로 번역해 내려줌)이라
  // t()로 감싸면 안 된다 — t()는 translation.json의 고정 키를 찾는 함수라, 여기 감싸면
  // 매번 없는 키를 찾다가 원본 문자열을 그대로 돌려주는 것뿐이라 사실상 아무 효과가
  // 없지만 의미상 잘못됐고, 혹시라도 제목이 흔한 단어와 우연히 겹치면 엉뚱하게
  // 바뀔 수도 있다.
  const title = result ? `${result.data.moimTitle} ${t('review.flowTitle')}` : t('review.flowTitle');

  if (!result) {
    return (
      <div className="moim-review-page">
        <PageHeader contentTitle={t('review.flowTitle')} onBack={() => navigate(-1)} />
        <p className="review-state">{t('account.loading')}</p>
      </div>
    );
  }

  return (
    <div className="moim-review-page">
      <PageHeader contentTitle={title} onBack={handleBack} />

      {step === 'choose' && (
        <div className="review-choose">
          <div className="review-choose-hero">
            <span className="review-choose-emoji" aria-hidden="true">🎉</span>
            <p className="review-choose-title">{t('review.choosePrompt')}</p>
          </div>

          <div className="review-choose-cards">
            {!tourWritten && places.length > 0 && (
              <button type="button" className="review-choose-card" onClick={() => setStep('writeTour')}>
                <span className="review-choose-card-icon" aria-hidden="true">🏞️</span>
                <span className="review-choose-card-body">
                  <strong>{t('review.writeTourBtn')}</strong>
                  <span>{t('review.tourCardDesc')}</span>
                </span>
                <span className="review-choose-card-arrow" aria-hidden="true">›</span>
              </button>
            )}
            {!moimWritten && (
              <button type="button" className="review-choose-card" onClick={() => setStep('writeMoim')}>
                <span className="review-choose-card-icon" aria-hidden="true">👥</span>
                <span className="review-choose-card-body">
                  <strong>{t('review.writeMoimBtn')}</strong>
                  <span>{t('review.moimCardDesc')}</span>
                </span>
                <span className="review-choose-card-arrow" aria-hidden="true">›</span>
              </button>
            )}
          </div>

          {places.length === 0 && <p className="review-choose-note">{t('review.noPlaceToReview')}</p>}

          <button type="button" className="review-choose-finish" onClick={() => navigate(`/moim/${moimId}`)}>
            {t('review.finishBtn')}
          </button>
        </div>
      )}

      {step === 'writeTour' && (
        <form className="review-form" onSubmit={handleTourSubmit(submitTourReview)}>
          <div className="review-form-scroll">
            <p className="review-form-prompt">{t('review.tourPrompt')}</p>

            <div className="review-form-card">
              {availableTourPlaces.length > 0 && (
                <Select
                  label={t('review.selectPlace')}
                  options={availableTourPlaces.map((place) => ({ value: place.tourId, option: place.tourNm }))}
                  {...registerTour('tourId', { required: true })}
                />
              )}

              <StarRating score={tourScore} onChange={(value) => setTourValue('reviewScore', value)} label={t('review.score')} />
              <input type="hidden" {...registerTour('reviewScore', { valueAsNumber: true })} />

              <Input
                label={t('review.title')}
                error={tourErrors.reviewTitle && t('review.titleRequired')}
                {...registerTour('reviewTitle', { required: true, maxLength: 60 })}
              />
              <Textarea
                label={t('review.content')}
                blind
                rows={5}
                error={tourErrors.reviewContent && t('review.contentRequired')}
                {...registerTour('reviewContent', { required: true, maxLength: 1000 })}
              />

              <ImagePicker files={tourFiles} onAdd={addTourFiles} onRemove={removeTourFile} label={t('review.images')} removeLabel={t('common.remove')} />
            </div>
          </div>

          <div className="review-form-actions">
            <Button
              type="button"
              variant="secondary"
              text={t('review.addAnotherTourBtn')}
              disabled={createTourReview.isPending || availableTourPlaces.length <= 1}
              onClick={handleTourSubmit(addAnotherTourReview)}
            />
            <Button
              type="submit"
              text={createTourReview.isPending ? t('common.saving') : t('review.registerTitle')}
              disabled={createTourReview.isPending}
            />
          </div>
        </form>
      )}

      {step === 'writeMoim' && (
        <form className="review-form" onSubmit={handleMoimSubmit(submitMoimReview)}>
          <div className="review-form-scroll">
            <p className="review-form-prompt">{t('review.moimPrompt')}</p>

            <div className="review-form-card">
              <StarRating score={moimScore} onChange={(value) => setMoimValue('reviewScore', value)} label={t('review.score')} />
              <input type="hidden" {...registerMoim('reviewScore', { valueAsNumber: true })} />

              <Textarea
                label={t('review.content')}
                blind
                rows={5}
                placeholder={t('review.moimContentPlaceholder')}
                error={moimErrors.reviewContent && t('review.contentRequired')}
                {...registerMoim('reviewContent', { required: true, maxLength: 1000 })}
              />

              <ImagePicker files={moimFiles} onAdd={addMoimFiles} onRemove={removeMoimFile} label={t('review.images')} removeLabel={t('common.remove')} />
            </div>
          </div>

          <div className="review-form-actions">
            <Button
              type="submit"
              text={createMoimReview.isPending ? t('common.saving') : t('review.registerTitle')}
              disabled={createMoimReview.isPending}
            />
          </div>
        </form>
      )}
    </div>
  );
};

export default MoimReviewPage;
