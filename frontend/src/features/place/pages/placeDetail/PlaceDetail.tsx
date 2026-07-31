import { useParams } from 'react-router-dom'
import usePlaceDetail from '../../hooks/usePlaceDetail';

const PlaceDetail = () => {
  const { tourId } = useParams<{ tourId: string }>();
  const { data: moim, isLoading, isError } = usePlaceDetail(tourId!);

  if (isLoading) return <div>{tourId}로딩</div>;
  if (isError || !moim) return <div>{tourId}nodata</div>;

  return (
    <div>
      {tourId}
    </div>
  );
};

export default PlaceDetail;