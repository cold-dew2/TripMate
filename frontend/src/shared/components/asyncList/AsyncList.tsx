import Card from "../card/Card";

// shared/components/asyncList/AsyncList.tsx
interface AsyncListProps<T> {
  isLoading: boolean;
  isError: boolean;
  data: T[];
  skeletonCount?: number;
  renderSkeleton: () => React.ReactNode;
  renderItem: (item: T) => React.ReactNode;
  errorMsg: string;
  emptyMsg: string;
}

function AsyncList<T>({
  isLoading,
  isError,
  data,
  skeletonCount = 4,
  renderSkeleton,
  renderItem,
  errorMsg,
  emptyMsg,
}: AsyncListProps<T>) {
  if (isLoading) {
    return (
      <>
        {Array.from({ length: skeletonCount }).map((_, i) => (
          <li key={i}>{renderSkeleton()}</li>
        ))}
      </>
    );
  }

  if (isError) {
    return <li><Card error={errorMsg} /></li>;
  }

  if (data.length === 0) {
    return <li><Card error={emptyMsg} /></li>;
  }

  return <>{data.map(renderItem)}</>;
}

export default AsyncList;