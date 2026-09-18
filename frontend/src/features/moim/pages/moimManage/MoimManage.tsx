import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useMyMoim from '../../hooks/useMyMoim';
import useMoimMembers from '../../hooks/useMoimMembers';
import { formatDateWithDow } from '@/shared/utils/date';
import PageState from '@/shared/components/pageState/PageState';
import type { MyMoim } from '@/types/moim';
import './MoimManage.css';

const THUMB_COLORS = ['thumb-a', 'thumb-b', 'thumb-c', 'thumb-d'];

const MoimManageRow = ({ moim, index }: { moim: MyMoim; index: number }) => {
  const { t } = useTranslation();
  const { data: members } = useMoimMembers(moim.moimId);
  const pendingCount = (members ?? []).filter((m) => m.roleCd !== 'A' && m.stateCd !== 'Y').length;

  return (
    <li>
      <Link to={`/moimManage/${moim.moimId}`} className="manage-row">
        <div className={`manage-thumb ${THUMB_COLORS[index % THUMB_COLORS.length]}`} aria-hidden="true" />
        <div className="manage-row-info">
          <p className="manage-row-title">{moim.moimTitle}</p>
          <p className="manage-row-meta">{formatDateWithDow(moim.moimStartDt)} · {moim.memberCnt}/{moim.maxMember}{t('명')}</p>
        </div>
        <span className="manage-applicant-badge">{t('my.applicantCount', { count: pendingCount })}</span>
      </Link>
    </li>
  );
};

const MoimManage = () => {
  const { t } = useTranslation();
  const { data: myMoim, isLoading, isError } = useMyMoim();
  const hostedMoims = (myMoim ?? []).filter((moim) => moim.roleCd === 'A');

  return (
    <div className="manage-page">
      {isLoading ? (
        <PageState status="loading" />
      ) : isError ? (
        <PageState status="error" />
      ) : hostedMoims.length === 0 ? (
        <div className="empty-manage">
          <span>🧳</span>
          <p>{t('moim.emptyManageMsg')}</p>
          <Link to="/createMoim">{t('moim.emptyManageCta')}</Link>
        </div>
      ) : (
        <ul className="manage-list">
          {hostedMoims.map((moim, index) => (
            <MoimManageRow key={moim.moimId} moim={moim} index={index} />
          ))}
        </ul>
      )}
    </div>
  );
};

export default MoimManage;
