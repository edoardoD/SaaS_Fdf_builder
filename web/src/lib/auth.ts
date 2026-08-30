import { NextAuthOptions } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";
import { PrismaAdapter } from "@auth/prisma-adapter";
import { PrismaClient } from "@prisma/client";

const prisma = new PrismaClient();

export const authOptions: NextAuthOptions = {
  // eslint-disable-next-line @typescript-eslint/ban-ts-comment
  // @ts-ignore PrismaAdapter type mismatch
  adapter: PrismaAdapter(prisma),
  providers: [
    CredentialsProvider({
      name: "Credentials",
      credentials: {
        email: { label: "Email", type: "text", placeholder: "test@example.com" },
        password: { label: "Password", type: "password" },
      },
      async authorize(credentials) {
        if (!credentials?.email) return null;

        const user = await prisma.user.findUnique({
          where: { email: credentials.email },
        });

        if (user) {
          return {
            id: user.id,
            email: user.email,
            name: user.name,
            organizationId: user.organizationId,
          };
        }

        return null;
      },
    }),
  ],
  callbacks: {
    async session({ session, token }) {
      if (session.user && token) {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore custom session properties
        session.user.id = token.id;
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore custom session properties
        session.user.organizationId = token.organizationId;
      }
      return session;
    },
    async jwt({ token, user }) {
      if (user) {
        token.id = user.id;
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore custom token properties
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        token.organizationId = (user as any).organizationId;
      }
      return token;
    },
  },
  session: {
    strategy: "jwt",
  },
};
