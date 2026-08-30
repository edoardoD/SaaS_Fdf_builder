import { z } from "zod";
import { router, protectedProcedure } from "../trpc";

export const clientiRouter = router({
  list: protectedProcedure.query(async ({ ctx }) => {
    return ctx.prisma.cliente.findMany({
      where: {
        organizationId: ctx.organizationId,
      },
    });
  }),
  create: protectedProcedure
    .input(
      z.object({
        nome: z.string(),
        indirizzo: z.string().optional(),
        partitaIva: z.string().optional(),
      })
    )
    .mutation(async ({ ctx, input }) => {
      return ctx.prisma.cliente.create({
        data: {
          ...input,
          organizationId: ctx.organizationId,
        },
      });
    }),
});
