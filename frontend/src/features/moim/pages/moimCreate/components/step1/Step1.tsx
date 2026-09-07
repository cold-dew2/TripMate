import ContentTitle from '@/shared/components/contentTitle/ContentTitle'
import { useTranslation } from 'react-i18next';
import Button from '@/shared/components/button/Button';
import Checkbox from '@/shared/components/checkbox/Checkbox';
import './Step1.css'
import type { UseFormSetValue } from 'react-hook-form';
import type { MoimCreateForm } from '@/types/moim';
import { useState } from 'react';

interface Step1Props {
  setValue: UseFormSetValue<MoimCreateForm>;
  onNext: () => void;
}
const themeList = [
  { 
    id: "theme1", 
    icon: "icon_theme1.png", 
    title: "moimCreate.step1.theme1",
    categories: [
      { cateCd: "BCH", cateNm: "해변" },
      { cateCd: "CAM", cateNm: "캠핑" },
      { cateCd: "HEA", cateNm: "힐링" },
      { cateCd: "ISL", cateNm: "섬" },
      { cateCd: "MNT", cateNm: "산" },
      { cateCd: "NAT", cateNm: "자연" },
      { cateCd: "PAR", cateNm: "공원" },
      { cateCd: "RIV", cateNm: "강·호수" },
      { cateCd: "SEA", cateNm: "바다" },
      { cateCd: "SPA", cateNm: "온천·스파" },
      { cateCd: "WAL", cateNm: "산책" },
    ],
  },
  { 
    id: "theme2", 
    icon: "icon_theme2.png", 
    title: "moimCreate.step1.theme2",
    categories: [
      { cateCd: "ART", cateNm: "미술관" },
      { cateCd: "CUL", cateNm: "문화" },
      { cateCd: "HIS", cateNm: "역사" },
      { cateCd: "MUS", cateNm: "박물관" },
      { cateCd: "PAL", cateNm: "궁궐" },
      { cateCd: "TEM", cateNm: "사찰" },
    ],
  },
  { 
    id: "theme3", 
    icon: "icon_theme3.png", 
    title: "moimCreate.step1.theme3",
    categories: [
      { cateCd: "CAF", cateNm: "카페" },
      { cateCd: "RES", cateNm: "맛집" },
    ],
  },
  { 
    id: "theme4", 
    icon: "icon_theme4.png", 
    title: "moimCreate.step1.theme4",
    categories: [
      { cateCd: "ACT", cateNm: "액티비티" },
      { cateCd: "DRI", cateNm: "드라이브" },
      { cateCd: "LEI", cateNm: "레저" },
    ],
  },
  { 
    id: "theme5", 
    icon: "icon_theme5.png", 
    title: "moimCreate.step1.theme5",
    categories: [
      { cateCd: "SHO", cateNm: "쇼핑" },
    ],
  },
  { 
    id: "theme6", 
    icon: "icon_theme6.png", 
    title: "moimCreate.step1.theme6",
    categories: [
      { cateCd: "COU", cateNm: "커플여행" },
      { cateCd: "FAM", cateNm: "가족여행" },
      { cateCd: "FES", cateNm: "축제" },
      { cateCd: "KID", cateNm: "아이와 함께" },
      { cateCd: "NIG", cateNm: "야경" },
      { cateCd: "PET", cateNm: "반려동물 동반" },
      { cateCd: "PHO", cateNm: "사진명소" },
    ],
  },
]
const Step1 = ({ setValue, onNext }: Step1Props) => {
  const { t } = useTranslation();
  const [selectedThemes, setSelectedThemes] = useState<string[]>([]);

  const handleThemeChange = (themeId: string) => {
    const isSelected = selectedThemes.includes(themeId);
    const newSelected = isSelected ? selectedThemes.filter((id) => id !== themeId) : [...selectedThemes, themeId];
    setSelectedThemes(newSelected);
    
    const categories = themeList.filter((theme) => newSelected.includes(theme.id)).flatMap((theme) => theme.categories);

    setValue("moimCateData", categories);
  }
  return (
    <div className="create-content">
      <ContentTitle title={t("moimCreate.step1.title")} />

      <ul className="themeList">
        {themeList.map((theme) => (
          <li key={theme.id}>
            <Checkbox 
              id={theme.id} 
              label={t(theme.title)} 
              icon={theme.icon}
              checked={selectedThemes.includes(theme.id)}
              onChange={() => handleThemeChange(theme.id)}
            />
          </li>
        ))}
      </ul>

      <div className="buttons fixed">
        <Button text={t("다음")} variant="fixed" onClick={onNext}/>
      </div>
    </div>
  )
}

export default Step1;