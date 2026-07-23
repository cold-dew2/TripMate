import { createRoot } from 'react-dom/client'
import "./i18n";
import App from './App.tsx'
import "@/shared/styles/index.css";
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient();

createRoot(document.getElementById('root')!).render(
    <QueryClientProvider client={queryClient}>
        <App />
    </QueryClientProvider>
)
