import { DataErrorBoundary } from "@/shared/ui";
import { Suspense } from "react";
import { ConferencesListSkeleton } from "./ConferencesSkeleton";
import { ConferencesList } from "./ConferencesList";

export function Conferences() {
  return (
    <div className="p-8">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-[34px] leading-[1.235] font-bold text-text m-0 tracking-[0.25px]">
            Conférences et CFP
          </h1>
          <p className="text-[14px] leading-[1.43] text-text-muted m-0 tracking-[0.15px]">
            Suivez les conférences et les deadlines de CFP.
          </p>
        </div>
        {/* <Button
          variant="contained"
          onClick={() => setOpen(true)}
          className="gap-2! font-bold! py-2! shadow-none! rounded-[20px]! bg-[linear-gradient(135deg,#ed213c_0%,#BF1D67_100%)]! transition-transform! duration-200! hover:shadow-none! hover:-translate-y-[2px]!"
        >
          <PenLine size={16} />
          Nouveau billet
        </Button> */}
      </div>

      <Suspense fallback={<ConferencesListSkeleton />}>
        <DataErrorBoundary>
          <ConferencesList />
        </DataErrorBoundary>
      </Suspense>

      {/* <CreateBlogPostDialog open={open} onClose={() => setOpen(false)} onSubmit={handleSubmit} /> */}
    </div>
    // <div className="flex flex-col items-center justify-center min-h-[60vh] gap-2">
    //   <h1 className="text-3xl font-extrabold text-text tracking-tight">Conférences</h1>
    //   <p className="text-[1.1rem] text-text-disabled">
    //     Cette section est en cours de construction.
    //   </p>
    // </div>
  );
}

export default Conferences;
