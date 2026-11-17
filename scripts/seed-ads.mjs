// Carrega backend/api-mock/seed/ads.json chamando POST /api/v1/ads da própria API,
// de modo que cada anúncio passe pela moderação real.
// O seed não traz imagens; usamos uma URL placeholder apenas para demonstração.
//
// Uso: node scripts/seed-ads.mjs            (API em http://localhost:8080)
//      API_URL=... SEED_FILE=... node scripts/seed-ads.mjs
import { readFile } from 'node:fs/promises';

const apiUrl = process.env.API_URL ?? 'http://localhost:8080';
const seedFile = process.env.SEED_FILE ?? new URL('../backend/api-mock/seed/ads.json', import.meta.url);
const placeholderImage = 'https://placehold.co/600x400.png';

const { ads } = JSON.parse(await readFile(seedFile, 'utf8'));
const summary = {};

for (const ad of ads) {
  const response = await fetch(`${apiUrl}/api/v1/ads`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      title: ad.title,
      description: ad.description,
      price: ad.price,
      category: ad.category,
      images: [placeholderImage],
    }),
  });
  const body = await response.json();
  const key = response.ok ? body.status : `http_${response.status}`;
  summary[key] = (summary[key] ?? 0) + 1;
  console.log(`#${ad.id} ${ad.title} -> ${key}`);
}

console.log('Resumo:', summary);
