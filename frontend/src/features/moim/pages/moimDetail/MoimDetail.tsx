import { useParams } from 'react-router-dom'
import useMoimDetail from '../../hooks/useMoimDetail'

const MoimDetail = () => {
  const { moimId } = useParams<{ moimId: string }>();
  const { data: moim, isLoading, isError } = useMoimDetail(moimId!);

  if (isLoading) return <div>{moimId}로딩</div>;
  if (isError || !moim) return <div>{moimId}nodata</div>;

  return (
    <div>
      {moimId}
    </div>
  );
};

export default MoimDetail;