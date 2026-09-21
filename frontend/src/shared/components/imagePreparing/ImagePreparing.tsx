import { useTranslation } from 'react-i18next';
import { getApiLang } from '@/shared/utils/lang';
import './ImagePreparing.css';

const ImagePreparing = () => {
  const { t } = useTranslation();
  return (
    <div className="image-preparing">
      <img src={`/images/places/image-preparing-${getApiLang()}.svg`} alt={t('common.imagePreparing')} />
    </div>
  );
};

export default ImagePreparing;
