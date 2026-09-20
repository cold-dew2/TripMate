import { useTranslation } from 'react-i18next';
import type { TransportLeg } from '@/features/moim/hooks/useTransportRecommend';
import './TransportLegView.css';

const TRANSPORT_ICON: Record<string, string> = {
  지하철: '🚇', 버스: '🚌', 도보: '🚶', 택시: '🚕', '자가용/렌터카': '🚗',
};

const CONGESTION_STYLE: Record<string, { icon: string; className: string }> = {
  원활: { icon: '🟢', className: 'smooth' },
  보통: { icon: '🟡', className: 'moderate' },
  혼잡: { icon: '🔴', className: 'heavy' },
};

const TransportLegView = ({ leg }: { leg: TransportLeg }) => {
  const { t } = useTranslation();
  const congestion = leg.congestionLevel ? CONGESTION_STYLE[leg.congestionLevel] : undefined;
  const isHeavy = leg.congestionLevel === '혼잡';
  const modeLabel = (mode: string) => t(`transport.mode.${mode}`, mode);

  return (
    <div className={`transport-leg ${isHeavy ? 'transport-leg-heavy' : ''}`}>
      <div className="transport-leg-main">
        <span aria-hidden="true">{TRANSPORT_ICON[leg.mode] ?? '🧭'}</span>
        <span>
          {modeLabel(leg.mode)}
          {leg.durationMinutes != null && ` · ${t('transport.duration', { count: leg.durationMinutes })}`}
          {leg.cost != null && ` · ${t('transport.cost', { cost: leg.cost.toLocaleString() })}`}
          {leg.transferCount != null && ` · ${t('transport.transfer', { count: leg.transferCount })}`}
        </span>
        {congestion && (
          <span className={`transport-congestion-badge transport-congestion-${congestion.className}`}>
            {congestion.icon} {t(`transport.congestion.${congestion.className}`)}
          </span>
        )}
      </div>

      {isHeavy && (
        <div className="transport-alert" role="alert">
          <p className="transport-alert-title">
            ⚠️ {t('transport.delayWarning', { count: leg.delayRiskMinutes ?? 0 })}
          </p>
          {leg.alternativeMode && (
            <p className="transport-alert-alt">
              {t('transport.alternative')}: <strong>{TRANSPORT_ICON[leg.alternativeMode] ?? '🧭'} {modeLabel(leg.alternativeMode)}</strong>
              {leg.alternativeReason && ` — ${leg.alternativeReason}`}
            </p>
          )}
        </div>
      )}
    </div>
  );
};

export default TransportLegView;
