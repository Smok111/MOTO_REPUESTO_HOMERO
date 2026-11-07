#!/usr/bin/env node
/**
 * Script para poblar Firestore con tiendas y productos de ejemplo
 * Uso: node seed_firestore.js
 *
 * Requisitos:
 * 1. npm install firebase-admin
 * 2. Descargar serviceAccount.json desde Firebase Console
 *    → Project Settings → Service Accounts → Generate new private key
 * 3. Ajusta la ruta del serviceAccount.json abajo
 */

const admin = require('firebase-admin');
const path = require('path');

// ⚠️ CAMBIA ESTA RUTA a donde guardaste tu serviceAccount.json
const serviceAccountPath = path.resolve(process.env.USERPROFILE, 'keys', 'serviceAccount.json');

try {
  const serviceAccount = require(serviceAccountPath);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
} catch (err) {
  console.error('❌ Error: No se encontró serviceAccount.json');
  console.error(`   Busca en: ${serviceAccountPath}`);
  console.error('   O descárgalo desde: https://console.firebase.google.com/');
  process.exit(1);
}

const db = admin.firestore();

const stores = [
  {
    id: 'la-esquina-burger',
    data: {
      name: 'La Esquina Burger',
      category: 'Restaurantes',
      emoji: '🍔',
      shortDescription: 'Hamburguesas, papas y combos completos.',
      rating: 4.7,
      deliveryTime: '20-30 min'
    },
    products: [
      { name: 'Hamburguesa Clásica', price: 15.0, icon: '🍔', category: 'Restaurantes' },
      { name: 'Papas Fritas', price: 6.0, icon: '🍟', category: 'Restaurantes' },
      { name: 'Combo Burger + Bebida', price: 22.0, icon: '🥤', category: 'Restaurantes' }
    ]
  },
  {
    id: 'starbucks',
    data: {
      name: 'Starbucks',
      category: 'Restaurantes',
      emoji: '☕',
      shortDescription: 'Café, postres y desayuno listo para llevar.',
      rating: 4.8,
      deliveryTime: '15-25 min'
    },
    products: [
      { name: 'Latte Grande', price: 10.0, icon: '☕', category: 'Restaurantes' },
      { name: 'Croissant', price: 6.0, icon: '🥐', category: 'Restaurantes' },
      { name: 'Frappuccino de Vainilla', price: 9.0, icon: '🧋', category: 'Restaurantes' }
    ]
  },
  {
    id: 'sabor-andino',
    data: {
      name: 'Sabor Andino',
      category: 'Restaurantes',
      emoji: '🍗',
      shortDescription: 'Pollo a la brasa y platos criollos bien servidos.',
      rating: 4.6,
      deliveryTime: '25-35 min'
    },
    products: [
      { name: 'Pollo a la Brasa (1/2)', price: 25.0, icon: '🍗', category: 'Restaurantes' },
      { name: 'Papa Dorada', price: 7.0, icon: '🥔', category: 'Restaurantes' },
      { name: 'Ensalada de la Casa', price: 12.0, icon: '🥗', category: 'Restaurantes' }
    ]
  },
  {
    id: 'mercado-express',
    data: {
      name: 'Mercado Express',
      category: 'Supermercado',
      emoji: '🛒',
      shortDescription: 'Compra el súper básico y recibe rápido en casa.',
      rating: 4.5,
      deliveryTime: '10-20 min'
    },
    products: [
      { name: 'Leche Entera (1L)', price: 4.5, icon: '🥛', category: 'Supermercado' },
      { name: 'Huevos (12 un)', price: 8.5, icon: '🥚', category: 'Supermercado' },
      { name: 'Pan de Molde', price: 6.0, icon: '🍞', category: 'Supermercado' },
      { name: 'Mantequilla', price: 7.5, icon: '🧈', category: 'Supermercado' }
    ]
  },
  {
    id: 'fresh-market',
    data: {
      name: 'Fresh Market',
      category: 'Supermercado',
      emoji: '🥦',
      shortDescription: 'Frutas, verduras y productos frescos del día.',
      rating: 4.4,
      deliveryTime: '18-28 min'
    },
    products: [
      { name: 'Manzanas (kg)', price: 5.5, icon: '🍎', category: 'Supermercado' },
      { name: 'Plátanos (kg)', price: 4.0, icon: '🍌', category: 'Supermercado' },
      { name: 'Tomates (kg)', price: 6.0, icon: '🍅', category: 'Supermercado' },
      { name: 'Lechuga', price: 3.5, icon: '🥬', category: 'Supermercado' }
    ]
  }
];

async function seed() {
  console.log('🌱 Iniciando población de Firestore...\n');

  try {
    for (const store of stores) {
      console.log(`📍 Creando tienda: ${store.data.name}`);
      const storeRef = db.collection('stores').doc(store.id);
      await storeRef.set(store.data);

      for (const product of store.products) {
        console.log(`   └─ Agregando: ${product.name}`);
        await storeRef.collection('products').add(product);
      }
      console.log('');
    }

    console.log('✅ ¡Seed completado exitosamente!');
    console.log('\n📱 La app debería mostrar ahora:');
    console.log('   • Tiendas destacadas en el Home');
    console.log('   • Productos al abrir una tienda');
    console.log('   • Opción de agregar al carrito');
    console.log('   • Creación de pedidos en Firestore\n');

    process.exit(0);
  } catch (error) {
    console.error('❌ Error durante el seed:', error);
    process.exit(1);
  }
}

seed();

