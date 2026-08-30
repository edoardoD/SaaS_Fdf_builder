import { z } from "zod";
import { router, protectedProcedure } from "../trpc";
import { TRPCError } from "@trpc/server";
import { s3Internal, s3External, BUCKET_NAME } from "../s3";
import { PutObjectCommand, GetObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";

export const pdfRouter = router({
  generate: protectedProcedure
    .input(
      z.object({
        impiantoId: z.string(),
        frequenza: z.object({
          tipo: z.string(),
          valore: z.number(),
        }),
        clienteNome: z.string().optional(),
      })
    )
    .mutation(async ({ ctx, input }) => {
      const impianto = await ctx.prisma.impianto.findUnique({
        where: { id: input.impiantoId, organizationId: ctx.organizationId },
      });

      if (!impianto) {
        throw new TRPCError({ code: "NOT_FOUND", message: "Impianto not found" });
      }

      const payload = {
        impianto: {
          id: impianto.id,
          _t: impianto.tipoImpianto,
          codIntervento: impianto.codIntervento,
          nomeCompleto: impianto.nomeCompleto,
          premessa: impianto.premessa,
          quantita: impianto.quantita,
          noteSpecifiche: impianto.noteSpecifiche,
          listaAttivita: impianto.listaAttivita,
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          ...(impianto.extraData as any),
        },
        frequenza: input.frequenza,
        clienteNome: input.clienteNome,
      };

      // In Docker, we can use service-kotlin hostname via env var or hardcode for now
      const pdfServiceUrl = process.env.PDF_SERVICE_URL || "http://localhost:8080";

      const response = await fetch(`${pdfServiceUrl}/api/generate-pdf`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new TRPCError({
          code: "INTERNAL_SERVER_ERROR",
          message: `PDF generation failed: ${errorText}`,
        });
      }

      const arrayBuffer = await response.arrayBuffer();
      const buffer = Buffer.from(arrayBuffer);

      const fileName = `schede/${ctx.organizationId}/${impianto.id}_${Date.now()}.pdf`;

      try {
        // Upload to S3
        await s3Internal.send(new PutObjectCommand({
          Bucket: BUCKET_NAME,
          Key: fileName,
          Body: buffer,
          ContentType: "application/pdf"
        }));

        // Generate Presigned URL
        const command = new GetObjectCommand({
          Bucket: BUCKET_NAME,
          Key: fileName,
        });

        const presignedUrl = await getSignedUrl(s3External, command, { expiresIn: 3600 });

        return {
          url: presignedUrl,
          filename: "scheda.pdf"
        };
      } catch (e) {
        console.error("S3 Error: ", e);
        // Fallback to base64 if S3 is down
        const base64Pdf = buffer.toString("base64");
        return {
          pdf: base64Pdf,
          filename: "scheda.pdf"
        };
      }
    }),
});
