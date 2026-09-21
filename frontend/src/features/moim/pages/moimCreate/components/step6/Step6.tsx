import { useMemo, useState, type ChangeEvent } from "react";
import { useTranslation } from "react-i18next";
import Button from "@/shared/components/button/Button";
import { apiClient } from "@/shared/api/client";
import { formatDateWithDow, formatMonthDayWithDow } from "@/shared/utils/date";
import { translateCategoryList } from "@/shared/utils/category";
import type { MoimCreateForm } from "@/types/moim";
import type { PlanItem } from "../../MoimCreate";
import type { UseFormSetValue, UseFormWatch } from "react-hook-form";
import "./Step6.css";

interface Step6Props {
  watch: UseFormWatch<MoimCreateForm>;
  setValue: UseFormSetValue<MoimCreateForm>;
  itemsByDay: Record<number, PlanItem[]>;
  onPrev: () => void;
  isSubmitting: boolean;
}

const Step6 = ({ watch, setValue, itemsByDay, onPrev, isSubmitting }: Step6Props) => {
  const { t } = useTranslation();
  const moimTitle = watch("moimTitle");
  const moimStartDt = watch("moimStartDt");
  const moimEndDt = watch("moimEndDt");
  const maxMember = watch("maxMember");
  const moimCateData = watch("moimCateData");

  const [preview, setPreview] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const dayCount = Object.keys(itemsByDay).length || 1;

  const regionLabel = useMemo(() => {
    const allItems = Object.values(itemsByDay).flat();
    const withSido = allItems.find((item) => item.sidoNm);
    if (!withSido) return null;

    const sggNames = Array.from(new Set(
      allItems
        .filter((item) => item.sidoNm === withSido.sidoNm && item.sggNm)
        .map((item) => item.sggNm as string)
    )).map((name) => t(name));
    const sidoLabel = t(withSido.sidoNm as string);
    return sggNames.length ? `${sidoLabel} ${sggNames.join(', ')}` : sidoLabel;
  }, [itemsByDay, t]);

  const handlePhotoSelect = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;

    const localUrl = URL.createObjectURL(file);
    setPreview(localUrl);

    setIsUploading(true);
    try {
      const body = new FormData();
      body.append("file", file);
      const result = await apiClient.upload<{ data: { url: string } }>("/uploads", body);
      if (result.success) {
        setValue("moimImgUrl", result.data.data.url);
      }
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="create-content step6-content">
      <label className="step6-photo">
        {preview ? (
          <img src={preview} alt={t("moimCreate.step6.photoLabel")} />
        ) : (
          <span>{isUploading ? t("common.saving") : t("moimCreate.step6.photoLabel")}</span>
        )}
        <input type="file" accept="image/*" className="blind" onChange={handlePhotoSelect} />
      </label>

      <h2 className="step6-title">{moimTitle}</h2>

      <div className="step6-tags">
        {(moimCateData ?? []).slice(0, 1).map((cate) => (
          <span key={cate.cateCd} className="tag-badge">{translateCategoryList(cate.cateNm, t)}</span>
        ))}
        <span className="tag-badge tag-badge-muted">{t("moimCreate.step3.daysOption", { count: dayCount })}</span>
        <span className="tag-badge tag-badge-muted">{t("moimCreate.step6.tagRecruit")}</span>
      </div>

      <ul className="step6-meta">
        <li>
          <span>{t("moimCreate.step6.scheduleLabel")}</span>
          <strong>
            {formatDateWithDow(moimStartDt)} - {formatMonthDayWithDow(moimEndDt)}
            {" "}({t("moimCreate.step6.nightsLabel", { count: dayCount, nights: Math.max(0, dayCount - 1) })})
          </strong>
        </li>
        {regionLabel && (
          <li>
            <span>{t("moimCreate.step6.regionLabel")}</span>
            <strong>{regionLabel}</strong>
          </li>
        )}
        <li>
          <span>{t("moimCreate.step6.memberLabel")}</span>
          <strong>{t("people", { count: 1 })} / {t("people", { count: maxMember })}</strong>
        </li>
      </ul>

      <div className="buttons fixed">
        <Button text={t("common.previous")} variant="secondary" onClick={onPrev} disabled={isSubmitting} />
        <Button
          text={isSubmitting ? t("common.saving") : t("moimCreate.step6.register")}
          type="submit"
          disabled={isSubmitting}
        />
      </div>
    </div>
  );
};

export default Step6;
