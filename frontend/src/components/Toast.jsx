export default function Toast({ message, type = 'info', onClose }) {
  if (!message) return null;

  return (
    <div className={`toast toast-${type}`}>
      <span>{message}</span>
      <button type="button" onClick={onClose} className="ghost-btn compact-btn">Dismiss</button>
    </div>
  );
}
