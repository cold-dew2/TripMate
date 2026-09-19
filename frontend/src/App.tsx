import Router from './routes/Router'
import AppFrame from './layouts/components/appFrame/AppFrame'
import { AlertProvider } from '@/shared/contexts/AlertContext'

function App() {
  return (
    <AlertProvider>
      <AppFrame>
        <Router />
      </AppFrame>
    </AlertProvider>
  )
}

export default App
