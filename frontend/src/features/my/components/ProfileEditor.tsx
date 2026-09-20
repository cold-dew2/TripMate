import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/shared/api/client';
import { resolveImageUrl } from '@/shared/utils/url';
import Input from '@/shared/components/input/Input';
import Textarea from '@/shared/components/textarea/Textarea';
import Button from '@/shared/components/button/Button';
import './ProfileEditor.css';

const MAX_LANGUAGES = 3;
const DEFAULT_LEVEL = 3;

interface Profile {
  userNm: string;
  areaNm: string;
  description: string;
  profileImageUrl: string;
  languages?: { langCd: string; levelNm?: string }[];
}

interface LanguageSelection {
  langCd: string;
  levelCd: number;
}

interface FormValues {
  userNm: string;
  areaNm: string;
  description: string;
}

export default function ProfileEditor() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const client = useQueryClient();

  const profile = useQuery({
    queryKey: ['myProfile'],
    queryFn: async () => {
      const r = await apiClient.get<{ data: Profile }>('/mypage/profile');
      if (!r.success) throw r;
      return r.data.data;
    },
  });

  const { register, handleSubmit, reset, formState: { isSubmitting } } = useForm<FormValues>({
    defaultValues: { userNm: '', areaNm: '', description: '' },
  });
  const [languages, setLanguages] = useState<LanguageSelection[]>([]);
  const [uploadError, setUploadError] = useState('');
  // 새로 고른 프로필 이미지는 "완료"를 눌러 저장하기 전까지는 미리보기용으로만 들고
  // 있는다. 이 값이 없으면 업로드를 아예 안 한 것이므로 기존 이미지를 그대로 쓴다.
  const [pendingImageUrl, setPendingImageUrl] = useState<string | null>(null);

  useEffect(() => {
    if (profile.data) {
      reset({
        userNm: profile.data.userNm,
        areaNm: profile.data.areaNm ?? '',
        description: profile.data.description ?? '',
      });
      setLanguages((profile.data.languages ?? []).map((lang) => ({
        langCd: lang.langCd,
        levelCd: Number(lang.levelNm) || DEFAULT_LEVEL,
      })));
      setPendingImageUrl(null);
    }
  }, [profile.data, reset]);

  const languageOptions = [
    { value: 'ko', label: t('lang.ko') },
    { value: 'en', label: t('lang.en') },
    { value: 'ja', label: t('lang.jp') },
  ];

  const toggleLanguage = (value: string) => {
    setLanguages((prev) => {
      if (prev.some((lang) => lang.langCd === value)) {
        return prev.filter((lang) => lang.langCd !== value);
      }
      if (prev.length >= MAX_LANGUAGES) return prev;
      return [...prev, { langCd: value, levelCd: DEFAULT_LEVEL }];
    });
  };

  const setLanguageLevel = (value: string, levelCd: number) => {
    setLanguages((prev) => prev.map((lang) => (lang.langCd === value ? { ...lang, levelCd } : lang)));
  };

  const update = useMutation({
    mutationFn: async (data: Partial<FormValues> & { profileImageUrl?: string; languages?: LanguageSelection[] }) => {
      const r = await apiClient.put<{ data: Profile }>('/mypage/profile', data);
      if (!r.success) throw r;
      return r.data.data;
    },
    onSuccess: () => {
      client.invalidateQueries({ queryKey: ['myProfile'] });
      navigate('/my');
    },
    onError: () => {
      setUploadError(t('my.saveFailed'));
    },
  });

  const uploadImage = async (file?: File) => {
    if (!file) return;
    setUploadError('');
    try {
      const body = new FormData();
      body.append('file', file);
      const r = await apiClient.upload<{ data: { url: string } }>('/uploads', body);
      if (!r.success) throw r;
      // 서버에 올리기만 하고, 실제 프로필 반영은 "완료"를 눌렀을 때 onSubmit에서 한다.
      setPendingImageUrl(r.data.data.url);
    } catch {
      setUploadError(t('my.uploadFailed'));
    }
  };

  const onSubmit = (values: FormValues) => update.mutate({
    ...values,
    languages,
    ...(pendingImageUrl ? { profileImageUrl: pendingImageUrl } : {}),
  });

  if (profile.isLoading) return <p className="profile-editor-loading">{t('account.loading')}</p>;
  if (profile.isError || !profile.data) return <p className="profile-editor-loading">{t('common.loadError')}</p>;

  return (
    <section className="profile-editor">
      <div className="profile-editor-avatar-row">
        <img className="profile-editor-avatar" src={pendingImageUrl ? resolveImageUrl(pendingImageUrl) : profile.data.profileImageUrl ? resolveImageUrl(profile.data.profileImageUrl) : '/images/places/no-image.png'} alt={t('image.profilePhoto', { name: profile.data.userNm })} />
        <label className="profile-editor-upload">
          {t('account.edit')}
          <input type="file" accept="image/*" onChange={(event) => void uploadImage(event.target.files?.[0])} />
        </label>
      </div>
      {uploadError && <p className="profile-editor-error" role="alert">{uploadError}</p>}

      <form onSubmit={handleSubmit(onSubmit)}>
        <Input label={t('account.name')} {...register('userNm')} />
        <Input label={t('place.location')} {...register('areaNm')} />
        <Textarea label={t('my.introduce')} rows={4} {...register('description')} />

        <div className="profile-editor-langs">
          <span className="profile-editor-langs-label">{t('my.useLang')} ({t('my.langMaxHint', { max: MAX_LANGUAGES })})</span>
          <div className="profile-editor-lang-options">
            {languageOptions.map((option) => {
              const checked = languages.some((lang) => lang.langCd === option.value);
              return (
                <label key={option.value} className={checked ? 'profile-editor-lang-chip checked' : 'profile-editor-lang-chip'}>
                  <input
                    type="checkbox"
                    checked={checked}
                    disabled={!checked && languages.length >= MAX_LANGUAGES}
                    onChange={() => toggleLanguage(option.value)}
                  />
                  {option.label}
                </label>
              );
            })}
          </div>

          {languages.length > 0 && (
            <div className="profile-editor-lang-levels">
              {languages.map((lang) => {
                const label = languageOptions.find((option) => option.value === lang.langCd)?.label ?? lang.langCd;
                return (
                  <div key={lang.langCd} className="profile-editor-lang-level-row">
                    <span className="profile-editor-lang-level-name">{label}</span>
                    <div className="profile-editor-lang-level-picker" role="radiogroup" aria-label={t('my.langLevelLabel')}>
                      {[1, 2, 3, 4, 5].map((level) => (
                        <button
                          key={level}
                          type="button"
                          role="radio"
                          aria-checked={lang.levelCd === level}
                          aria-label={t('my.langLevel', { level })}
                          className={lang.levelCd >= level ? 'profile-editor-lang-level-star active' : 'profile-editor-lang-level-star'}
                          onClick={() => setLanguageLevel(lang.langCd, level)}
                        >
                          ★
                        </button>
                      ))}
                      <span className="profile-editor-lang-level-value">{t('my.langLevel', { level: lang.levelCd })}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <Button type="submit" text={isSubmitting || update.isPending ? t('common.saving') : t('common.submit')} disabled={isSubmitting || update.isPending} />
      </form>
    </section>
  );
}
