import { useCallback, useMemo, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { AlertContext } from '../hooks/useAlert';

let alertSeq = 1;

export default function AlertProvider({ children }) {
  const [alerts, setAlerts] = useState([]);

  const dismissAlert = useCallback((id) => {
    setAlerts((prev) => prev.filter((item) => item.id !== id));
  }, []);

  const pushAlert = useCallback((message, type = 'info') => {
    const id = alertSeq++;
    setAlerts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => dismissAlert(id), 3500);
  }, [dismissAlert]);

  const value = useMemo(() => ({ pushAlert }), [pushAlert]);

  return (
    <AlertContext.Provider value={value}>
      {children}
      <div className="global-toast-stack" aria-live="polite">
        <AnimatePresence>
          {alerts.map((alert) => (
            <motion.div
              key={alert.id}
              initial={{ opacity: 0, y: -18, scale: 0.97 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -14, scale: 0.96 }}
              transition={{ duration: 0.22 }}
              className={`global-toast global-toast-${alert.type}`}
            >
              <span>{alert.message}</span>
              <button type="button" className="toast-close" onClick={() => dismissAlert(alert.id)}>x</button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </AlertContext.Provider>
  );
}
