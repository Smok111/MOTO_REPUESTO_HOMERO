#!/usr/bin/env node
// Ejecutar una sola vez con credenciales de administrador de Google Cloud.
// GOOGLE_APPLICATION_CREDENTIALS=<ruta a la clave JSON> node seed_products.js
const admin = require('firebase-admin');

admin.initializeApp({ credential: admin.credential.applicationDefault(), projectId: 'tallerkhaory' });
const db = admin.firestore();

const products = [
  [1, 'Aceite 4T 20W-50', 'Lubricantes', 'Botella de 1 litro para moto', 32, 18],
  [2, 'Filtro de aceite', 'Filtros', 'Repuesto universal', 18, 12],
  [3, 'Pastillas de freno', 'Frenos', 'Juego delantero', 45, 9],
  [4, 'Bujía NGK', 'Motor', 'Encendido confiable', 22, 24],
  [5, 'Cadena de transmisión', 'Transmisión', 'Cadena reforzada 428H', 85, 7],
  [6, 'Batería 12V', 'Eléctrico', 'Batería sellada para moto', 139, 5],
  [7, 'Kit de arrastre', 'Transmisión', 'Piñón, corona y cadena', 175, 4],
  [8, 'Casco urbano', 'Accesorios', 'Casco integral talla M', 210, 6]
];

(async () => {
  for (const [id, name, category, description, price, stock] of products) {
    const ref = db.collection('products').doc(String(id));
    if (!(await ref.get()).exists) {
      await ref.set({ name, category, description, price, stock });
      console.log(`Creado: ${name}`);
    }
  }
  console.log('Catálogo listo. Los productos existentes no se modificaron.');
})().catch(error => { console.error(error); process.exitCode = 1; });
