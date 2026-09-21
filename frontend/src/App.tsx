import Router from './routes/Router'
import AppFrame from './layouts/components/appFrame/AppFrame'
import { AlertProvider } from '@/shared/contexts/AlertContext'
import { AiWaitProvider } from '@/shared/contexts/AiWaitContext'

function App() {
  return (
    <AlertProvider>
      <AiWaitProvider>
        <AppFrame>
          <Router />
        </AppFrame>
      </AiWaitProvider>
    </AlertProvider>
  )
}

export default App
