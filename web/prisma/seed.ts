import { PrismaClient } from '@prisma/client';
import fs from 'fs';
import path from 'path';

const prisma = new PrismaClient();

async function main() {
  console.log('Starting seed...');

  // Read exports
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let clientiData: any[] = [];
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let impiantiData: any[] = [];

  try {
    const clientiPath = path.resolve(__dirname, '../../clienti_export.json');
    if (fs.existsSync(clientiPath)) {
      clientiData = JSON.parse(fs.readFileSync(clientiPath, 'utf8'));
      console.log(`Read ${clientiData.length} clienti from export.`);
    }
  } catch(e) {
    console.error("Error reading clienti_export.json:", e);
  }

  try {
    const impiantiPath = path.resolve(__dirname, '../../impianti_export.json');
    if (fs.existsSync(impiantiPath)) {
      impiantiData = JSON.parse(fs.readFileSync(impiantiPath, 'utf8'));
      console.log(`Read ${impiantiData.length} impianti from export.`);
    }
  } catch (e) {
    console.error("Error reading impianti_export.json:", e);
  }

  // Clean DB
  await prisma.impianto.deleteMany({});
  await prisma.cantiere.deleteMany({});
  await prisma.cliente.deleteMany({});
  await prisma.user.deleteMany({});
  await prisma.organization.deleteMany({});

  // 1. Create a Default Organization
  const org = await prisma.organization.create({
    data: {
      name: 'Default Organization',
    },
  });
  console.log(`Created organization: ${org.name}`);

  // 2. Create a Test User
  const user = await prisma.user.create({
    data: {
      name: 'Test User',
      email: 'test@example.com',
      organizationId: org.id,
    }
  });
  console.log(`Created user: ${user.email}`);

  // 3. Create Clienti
  const clienteMap = new Map();
  for (const c of clientiData) {
    if (!clienteMap.has(c.id)) {
      const created = await prisma.cliente.create({
        data: {
          originalId: c.id,
          nome: c.nome || "Cliente Senza Nome",
          indirizzo: c.indirizzo,
          partitaIva: c.partitaIva,
          organizationId: org.id
        }
      });
      clienteMap.set(c.id, created.id);
    }
  }
  console.log(`Imported ${clienteMap.size} clienti.`);

  // 4. Create Impianti (we won't link to cantieri for now if they don't exist in export)
  let impiantiCount = 0;
  const impiantoIdSet = new Set();

  for (const imp of impiantiData) {
    if (impiantoIdSet.has(imp.id)) continue;
    impiantoIdSet.add(imp.id);

    // Map listAttivita to the embedded format
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let mappedAttivita: any[] = [];
    if (imp.listaAttivita && Array.isArray(imp.listaAttivita)) {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      mappedAttivita = imp.listaAttivita.map((a: any) => ({
        nAttivita: a.nAttivita || 0,
        tipoAttivita: a.tipoAttivita,
        descrizione: a.descrizione,
        visibile: a.visibile ?? true,
        componentRef: a.componentRef,
        targetImpiantoCod: a.targetImpiantoCod,
        frequenza: {
          tipo: a.frequenza?.tipo || 'M',
          valore: a.frequenza?.valore || 1,
        }
      }));
    }

    // extraData mapping
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { id, _id, _t, codIntervento, nomeCompleto, premessa, quantita, noteSpecifiche, listaAttivita, ...extraData } = imp;

    await prisma.impianto.create({
      data: {
        originalId: id,
        tipoImpianto: _t || 'ImpiantoStandard',
        codIntervento: codIntervento || 'UNK',
        nomeCompleto: nomeCompleto || 'Impianto Sconosciuto',
        premessa: premessa,
        quantita: quantita || 1,
        noteSpecifiche: noteSpecifiche,
        organizationId: org.id,
        listaAttivita: mappedAttivita,
        extraData: Object.keys(extraData).length > 0 ? extraData : undefined
      }
    });
    impiantiCount++;
  }
  console.log(`Imported ${impiantiCount} impianti.`);

  console.log('Seed completed successfully!');
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
