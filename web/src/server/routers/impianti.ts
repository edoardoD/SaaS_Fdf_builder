import { z } from "zod";
import { router, protectedProcedure } from "../trpc";

const PeriodoSchema = z.object({
  tipo: z.string(), // 'M' or 'A'
  valore: z.number(),
});

const AttivitaItemSchema = z.object({
  nAttivita: z.number(),
  tipoAttivita: z.string().nullable().optional(),
  descrizione: z.string().nullable().optional(),
  visibile: z.boolean().default(true),
  componentRef: z.string().nullable().optional(),
  targetImpiantoCod: z.string().nullable().optional(),
  frequenza: PeriodoSchema,
});

export const impiantiRouter = router({
  list: protectedProcedure.query(async ({ ctx }) => {
    return ctx.prisma.impianto.findMany({
      where: {
        organizationId: ctx.organizationId,
      },
    });
  }),
  getById: protectedProcedure
    .input(z.object({ id: z.string() }))
    .query(async ({ ctx, input }) => {
      return ctx.prisma.impianto.findUnique({
        where: {
          id: input.id,
          organizationId: ctx.organizationId,
        },
      });
    }),
  create: protectedProcedure
    .input(
      z.object({
        tipoImpianto: z.string(),
        codIntervento: z.string(),
        nomeCompleto: z.string(),
        premessa: z.string().optional(),
        quantita: z.number().default(1),
        noteSpecifiche: z.string().optional(),
        listaAttivita: z.array(AttivitaItemSchema).default([]),
      })
    )
    .mutation(async ({ ctx, input }) => {
      return ctx.prisma.impianto.create({
        data: {
          ...input,
          organizationId: ctx.organizationId,
        },
      });
    }),
  update: protectedProcedure
    .input(
      z.object({
        id: z.string(),
        tipoImpianto: z.string().optional(),
        codIntervento: z.string().optional(),
        nomeCompleto: z.string().optional(),
        premessa: z.string().optional(),
        quantita: z.number().optional(),
        noteSpecifiche: z.string().optional(),
        listaAttivita: z.array(AttivitaItemSchema).optional(),
      })
    )
    .mutation(async ({ ctx, input }) => {
      const { id, ...data } = input;
      return ctx.prisma.impianto.update({
        where: {
          id: id,
          organizationId: ctx.organizationId,
        },
        data: data,
      });
    }),
});
