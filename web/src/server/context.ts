import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import { PrismaClient } from "@prisma/client";

const prisma = new PrismaClient();

export async function createContext() {
  const session = await getServerSession(authOptions);

  return {
    prisma,
    session,
    // @ts-expect-error custom property
    organizationId: session?.user?.organizationId as string | undefined,
  };
}

export type Context = Awaited<ReturnType<typeof createContext>>;
