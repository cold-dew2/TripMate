import { useParams } from 'react-router-dom'
import useMoimDetail from '../../hooks/useMoimDetail'
import Header from './components/header/Header';

const MoimDetail = () => {
  const { moimId } = useParams<{ moimId: string }>();
  const { data: moim, isLoading, isError } = useMoimDetail(moimId!);

  if (isLoading) return <div>{moimId}로딩</div>;
  if (isError || !moim) return <div>{moimId}nodata</div>;

  return (
    <div className="moim-detail">
      <Header moim={moim.data} cate={moim.cate} />
      {moimId}
    </div>
  );
};

export default MoimDetail;