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

interface Profile {
  userNm: string;
  areaNm: string;
  description: string;
  profileImageUrl: string;
  languages?: { langCd: string }[];
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
  const [langCds, setLangCds] = useState<string[]>([]);
  const [uploadError, setUploadError] = useState('');

  useEffect(() => {
    if (profile.data) {
      reset({
        userNm: profile.data.userNm,
        areaNm: profile.data.areaNm ?? '',
        description: profile.data.description ?? '',
      });
      setLangCds((profile.data.languages ?? []).map((lang) => lang.langCd));
    }
  }, [profile.data, reset]);

  const languageOptions = [
    { value: 'ko', label: t('lang.ko') },
    { value: 'en', label: t('lang.en') },
    { value: 'ja', label: t('lang.jp') },
  ];

  const toggleLanguage = (value: string) => {
    setLangCds((prev) => {
      if (prev.includes(value)) return prev.filter((code) => code !== value);
      if (prev.length >= MAX_LANGUAGES) return prev;
      return [...prev, value];
    });
  };

  const update = useMutation({
    mutationFn: async (data: Partial<FormValues> & { profileImageUrl?: string; langCds?: string[] }) => {
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
      update.mutate({ profileImageUrl: r.data.data.url });
    } catch {
      setUploadError(t('my.uploadFailed'));
    }
  };

  const onSubmit = (values: FormValues) => update.mutate({ ...values, langCds });

  if (profile.isLoading) return <p className="profile-editor-loading">{t('account.loading')}</p>;
  if (profile.isError || !profile.data) return <p className="profile-editor-loading">{t('common.loadError')}</p>;

  return (
    <section className="profile-editor">
      <div className="profile-editor-avatar-row">
        <img className="profile-editor-avatar" src={profile.data.profileImageUrl ? resolveImageUrl(profile.data.profileImageUrl) : '/images/places/no-image.png'} alt={t('image.profilePhoto', { name: profile.data.userNm })} />
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
              const checked = langCds.includes(option.value);
              return (
                <label key={option.value} className={checked ? 'profile-editor-lang-chip checked' : 'profile-editor-lang-chip'}>
                  <input
                    type="checkbox"
                    checked={checked}
                    disabled={!checked && langCds.length >= MAX_LANGUAGES}
                    onChange={() => toggleLanguage(option.value)}
                  />
                  {option.label}
                </label>
              );
            })}
          </div>
        </div>

        <Button type="submit" text={isSubmitting || update.isPending ? t('common.saving') : t('common.submit')} disabled={isSubmitting || update.isPending} />
      </form>
    </section>
  );
}
