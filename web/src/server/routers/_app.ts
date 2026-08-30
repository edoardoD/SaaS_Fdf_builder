import { router } from "../trpc";
import { clientiRouter } from "./clienti";
import { impiantiRouter } from "./impianti";
import { pdfRouter } from "./pdf";

export const appRouter = router({
  clienti: clientiRouter,
  impianti: impiantiRouter,
  pdf: pdfRouter,
});

export type AppRouter = typeof appRouter;
