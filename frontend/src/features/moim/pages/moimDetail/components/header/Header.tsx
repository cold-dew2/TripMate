import type { MoimDetail } from '@/types/moim';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import './Header.css';

interface moimDetailProps { 
  moim: MoimDetail;
}
const images = [
  "/images/places/T001.jpeg",
  "/images/places/T002.jpeg",
  "/images/places/T003.jpeg",
  "/images/places/T004.jpeg",
];

const Header = ({ moim }: moimDetailProps) => {
  const { t } = useTranslation();
  const [randomImage] = useState(() => images[Math.floor(Math.random() * images.length)]);

  return (
    <div className="detail-header">
      <div className="detail-img">
        {/* <img src={`place.firstImage`} alt={`${place.tourNm}의 이미지`} /> */}
        <img src={randomImage} alt="" />
      </div>
      <div className="detail-info">
        <div className="place-name">{t(moim.moimTitle)}</div>
        <div className="tags">
        </div>
      </div>

      <ul className="plan-list">
        
      </ul>
    </div>
  )
}

export default Header