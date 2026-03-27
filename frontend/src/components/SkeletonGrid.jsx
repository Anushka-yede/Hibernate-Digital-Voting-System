export default function SkeletonGrid({ count = 3 }) {
  return (
    <div className="card-grid">
      {Array.from({ length: count }).map((_, idx) => (
        <article key={idx} className="panel skeleton-card">
          <div className="skeleton-line skeleton-title" />
          <div className="skeleton-line" />
          <div className="skeleton-line" />
          <div className="skeleton-line skeleton-short" />
        </article>
      ))}
    </div>
  );
}
