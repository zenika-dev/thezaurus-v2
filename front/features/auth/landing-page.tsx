"use client";

import { signIn } from "next-auth/react";
import {
  BookOpen,
  CalendarDays,
  MicVocal,
  PenLine,
  ShieldAlert,
} from "lucide-react";

interface LandingPageProps {
  error?: string | null;
}

export function LandingPage({ error }: LandingPageProps) {
  return (
    <div className="min-h-screen w-full bg-slate-50 flex flex-col justify-between relative overflow-hidden font-sans font-normal">
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
              La plateforme pour répertorier, planifier et suivre toutes les
              initiatives de partage de connaissances au sein de l&apos;agence.
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
                <p className="text-xs text-slate-500 mt-0.5 font-normal">
                  Référencez vos sujets de présentation et suivez vos passages.
                </p>
              </div>
            </div>

            {/* Feature 2 */}
            <div className="p-4 bg-white/70 backdrop-blur-sm rounded-xl border border-slate-100 shadow-sm flex items-start space-x-3 transition duration-300 hover:shadow-md hover:border-slate-200">
              <div className="p-2 bg-red-50 rounded-lg text-[#ED213C] shrink-0">
                <PenLine size={18} />
              </div>
              <div className="font-normal">
                <h3 className="font-bold text-slate-800 text-sm">Blog Posts</h3>
                <p className="text-xs text-slate-500 mt-0.5 font-normal">
                  Planifiez la rédaction et la publication de vos articles
                  techniques.
                </p>
              </div>
            </div>

            {/* Feature 3 */}
            <div className="p-4 bg-white/70 backdrop-blur-sm rounded-xl border border-slate-100 shadow-sm flex items-start space-x-3 transition duration-300 hover:shadow-md hover:border-slate-200">
              <div className="p-2 bg-red-50 rounded-lg text-[#ED213C] shrink-0">
                <CalendarDays size={18} />
              </div>
              <div className="font-normal">
                <h3 className="font-bold text-slate-800 text-sm">
                  Conférences
                </h3>
                <p className="text-xs text-slate-500 mt-0.5 font-normal">
                  Suivez l&apos;actualité des conférences externes de
                  l&apos;écosystème.
                </p>
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
                Accédez à l&apos;espace membre Thezaurus avec votre compte
                Google professionnel Zenika.
              </p>
            </div>

            {/* Google Authentication Button (géré par NextAuth) */}
            <div className="py-4 flex flex-col items-center justify-center min-h-[80px]">
              <button
                onClick={() => signIn("google")}
                className="flex items-center justify-center gap-3 w-full px-4 py-3 bg-white border border-slate-200 rounded-xl shadow-sm hover:shadow-md hover:border-slate-300 transition duration-200 text-sm font-medium text-slate-700"
              >
                <GoogleLogo />
                Continuer avec Google
              </button>
            </div>

            {/* Error Message */}
            {error && (
              <div className="p-3 bg-red-50 border border-red-100 rounded-lg flex items-center space-x-2 text-red-600 text-xs font-normal">
                <ShieldAlert size={16} className="shrink-0" />
                <span className="font-normal">{error}</span>
              </div>
            )}
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

function GoogleLogo() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <path
        fill="#4285F4"
        d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.259h2.908c1.702-1.567 2.684-3.874 2.684-6.617z"
      />
      <path
        fill="#34A853"
        d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332C2.438 16.018 5.482 18 9 18z"
      />
      <path
        fill="#FBBC05"
        d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332z"
      />
      <path
        fill="#EA4335"
        d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0 5.482 0 2.438 1.982.957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58z"
      />
    </svg>
  );
}
