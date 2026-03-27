import { createContext, useContext } from 'react';

export const AlertContext = createContext({
  pushAlert: () => {}
});

export function useAlert() {
  return useContext(AlertContext);
}
