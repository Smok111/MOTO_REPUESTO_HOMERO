# Firebase para MotoRepuestos Khaory

El proyecto es `tallerkhaory` y el identificador Android registrado es `Com.Moto_Kaori`. El paquete interno del código Kotlin (`com.khaory.taller`) puede ser distinto del identificador de instalación Android.

## Estado del proyecto

- Firebase Authentication con **Email/Password** está activado.
- Cloud Firestore `(default)` está creado en `nam5`, edición Standard y nivel gratuito.
- `firestore.rules` y `firestore.indexes.json` están publicados en `tallerkhaory`.
- La app Android `Com.Moto_Kaori` está registrada y `app/google-services.json` apunta a este proyecto.
- Los ocho repuestos iniciales están cargados en la colección `products`.
- La cuenta `homeroquispe06@gmail.com` existe en Authentication y tiene `role: "admin"` en Firestore. Firebase envió un correo para establecer su contraseña. El dueño del correo debe abrir el enlace, elegir una contraseña y entrar en la app con la opción **Administrador**.

Cloud Storage no se usa porque el proyecto permanece en Spark. Las fotos nuevas se comprimen a menos de 300 KB y se guardan en documentos de Firestore.

## Volver a publicar cambios

Si modificas `firebase.json`, `firestore.rules` o `firestore.indexes.json`, vuelve a publicar desde `MOTOREPUESTOS` con Firebase CLI autenticado:

```powershell
npx --yes firebase-tools@latest deploy --project tallerkhaory --only auth,firestore:rules,firestore:indexes
```

Las reglas están en `firestore.rules`; la exención de índice para los bytes de fotos está en `firestore.indexes.json`. La configuración de Email/Password está en `firebase.json`.

## Administradores adicionales

Registra la cuenta desde la app y cambia `role` a `admin` en su documento `users/{uid}` desde Firestore Console con una cuenta que administre el proyecto. El cliente no puede cambiar su propio rol mediante la app porque las reglas lo bloquean.

## Catálogo inicial

El catálogo ya contiene ocho repuestos de muestra con sus imágenes integradas en la app. `seed_products.js` permite repetir la carga sin reemplazar productos existentes; necesita credenciales administrativas de Google Cloud fuera del repositorio.

## Alcance actual

- El carrito sigue en memoria del teléfono; los productos, sus fotos comprimidas, el stock y los pedidos se sincronizan con Firestore. Cada documento tiene un límite de 1 MiB, por eso las fotos se reducen antes de guardarlas.
- La compra no procesa pagos.
- La validación de precios y existencias ocurre en la transacción de la app. En Spark no se pueden desplegar Cloud Functions; por ello, los pedidos son para pruebas. Un cliente modificado podría falsificar importes o reducir existencias. No aceptes compras reales hasta migrar esa operación a un backend confiable.
