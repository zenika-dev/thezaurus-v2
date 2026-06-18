import { BookOpen, Calendar, CalendarDays, MicVocal, PenLine, ShieldAlert } from "lucide-react";
import GoogleLoginButton from "./GoogleLoginButton";

interface LandingPageProps {
  onSuccess: (token: string) => void;
  error: string | null;
}

export default function LandingPage({ onSuccess, error }: LandingPageProps) {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-between relative overflow-hidden font-sans font-normal">
      {/* Decorative Background Glows */}
      <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] rounded-full bg-red-100/40 blur-3xl -z-10"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] rounded-full bg-red-50/50 blur-3xl -z-10"></div>

      {/* Header */}
      <header className="w-full max-w-7xl mx-auto px-6 py-6 flex items-center justify-between z-10">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-[#ED213C] flex items-center justify-center text-white shadow-md shadow-red-500/20 transform transition duration-300 hover:scale-105">
            <BookOpen size={22} />
          </div>
          <div>
            <h1 className="text-xl font-bold text-slate-800 tracking-tight leading-none">
              Thezaurus
            </h1>
            <span className="text-[10px] uppercase tracking-wider font-normal text-slate-400">
              by Zenika
            </span>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <main className="flex-1 w-full max-w-7xl mx-auto px-6 py-12 lg:py-20 flex flex-col lg:flex-row items-center justify-between gap-12 z-10">
        {/* Left Column: Tagline & Features */}
        <div className="flex-1 space-y-8 max-w-2xl font-normal">
          <div className="space-y-4">
            <h2 className="text-4xl md:text-5xl font-bold text-slate-900 leading-tight">
              Partagez vos connaissances chez{" "}
              <span className="text-[#ED213C] font-bold">Zenika</span>.
            </h2>
            <p className="text-lg text-slate-600 leading-relaxed font-normal">
              La plateforme pour répertorier, planifier et suivre toutes les initiatives de partage de connaissances au sein de l'agence.
            </p>
          </div>

          {/* Feature Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Feature 1 */}
            <div className="p-4 bg-white/70 backdrop-blur-sm rounded-xl border border-slate-100 shadow-sm flex items-start space-x-3 transition duration-300 hover:shadow-md hover:border-slate-200">
              <div className="p-2 bg-red-50 rounded-lg text-[#ED213C] shrink-0">
                <MicVocal size={18} />
              </div>
              <div className="font-normal">
                <h3 className="font-bold text-slate-800 text-sm">Talks</h3>
                <p className="text-xs text-slate-500 mt-0.5 font-normal">Référencez vos sujets de présentation et suivez vos passages.</p>
              </div>
            </div>

            {/* Feature 2 */}
            <div className="p-4 bg-white/70 backdrop-blur-sm rounded-xl border border-slate-100 shadow-sm flex items-start space-x-3 transition duration-300 hover:shadow-md hover:border-slate-200">
              <div className="p-2 bg-red-50 rounded-lg text-[#ED213C] shrink-0">
                <PenLine size={18} />
              </div>
              <div className="font-normal">
                <h3 className="font-bold text-slate-800 text-sm">Blog Posts</h3>
                <p className="text-xs text-slate-500 mt-0.5 font-normal">Planifiez la rédaction et la publication de vos articles techniques.</p>
              </div>
            </div>

            {/* Feature 3 */}
            <div className="p-4 bg-white/70 backdrop-blur-sm rounded-xl border border-slate-100 shadow-sm flex items-start space-x-3 transition duration-300 hover:shadow-md hover:border-slate-200">
              <div className="p-2 bg-red-50 rounded-lg text-[#ED213C] shrink-0">
                <CalendarDays size={18} />
              </div>
              <div className="font-normal">
                <h3 className="font-bold text-slate-800 text-sm">Conférences</h3>
                <p className="text-xs text-slate-500 mt-0.5 font-normal">Suivez l'actualité des conférences externes de l'écosystème.</p>
              </div>
            </div>

          </div>
        </div>

        {/* Right Column: Google Login Box */}
        <div className="w-full max-w-md shrink-0 font-normal">
          <div className="bg-white/80 backdrop-blur-md border border-slate-200/60 rounded-2xl shadow-xl p-8 space-y-6 relative">
            <div className="space-y-2 text-center">
              <h3 className="text-2xl font-bold text-slate-800">
                Connexion requise
              </h3>
              <p className="text-sm text-slate-500 font-normal">
                Accédez à l'espace membre Thezaurus avec votre compte Google professionnel Zenika.
              </p>
            </div>

            {/* Google Authentication Button */}
            <div className="py-4 flex flex-col items-center justify-center min-h-[80px]">
              <GoogleLoginButton
                onSuccess={onSuccess}
                onError={() => console.error("Login failed")}
              />
            </div>

            {/* Error Message */}
            {error && (
              <div className="p-3 bg-red-50 border border-red-100 rounded-lg flex items-center space-x-2 text-red-600 text-xs font-normal">
                <ShieldAlert size={16} className="shrink-0" />
                <span className="font-normal">{error}</span>
              </div>
            )}

            <div className="pt-4 border-t border-slate-100 text-center">
              <p className="text-[11px] text-slate-400 font-normal">
                La connexion est réservée aux collaborateurs de Zenika.
              </p>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="w-full bg-slate-100/80 backdrop-blur-sm border-t border-slate-200/80 py-8 z-10 font-normal">
        <div className="max-w-7xl mx-auto px-6 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center space-x-2">
            <span className="text-sm text-slate-500 font-normal">
              © 2026 Thezaurus by Zenika. Tous droits réservés.
            </span>
          </div>
          <div className="flex items-center space-x-6 text-sm text-slate-500 font-normal">
            <a
              href="https://zenika.com"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-[#ED213C] transition duration-200 font-normal"
            >
              Zenika.com
            </a>
            <a
              href="https://github.com/zenika-open-source/thezaurus"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-[#ED213C] transition duration-200 font-normal"
            >
              GitHub
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}
