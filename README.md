# MotoRepuestos Khaory

Aplicación Android para consultar y gestionar un catálogo de repuestos de motocicleta. Esta versión permite probar el registro de usuarios, el carrito, los pedidos, el inventario y la ubicación de entrega. Los pedidos son **de prueba**: la aplicación no cobra ni procesa pagos.

## Resumen para el informe

MotoRepuestos Khaory se desarrolló con Kotlin y Jetpack Compose. Usa Firebase Authentication para el acceso mediante correo y contraseña y Cloud Firestore para sincronizar productos, inventario, perfiles y pedidos. Tiene dos roles: cliente y administrador. El cliente consulta repuestos, arma un carrito y crea un pedido con una dirección o un punto GPS. El administrador gestiona el catálogo, el stock y el estado de los pedidos, y puede abrir en un mapa la ubicación compartida por el cliente.

El proyecto Firebase se llama `tallerkhaory` y la aplicación Android está registrada con el identificador `Com.Moto_Kaori`. Funciona en el plan gratuito Spark. Para evitar depender de Cloud Storage, las fotos agregadas desde la app se comprimen y se guardan en Firestore.

## Problema y objetivo

Una tienda de repuestos necesita mostrar su inventario y registrar solicitudes de compra sin depender de consultas manuales sobre precio o disponibilidad. El objetivo de esta aplicación es ofrecer un catálogo móvil sincronizado y un flujo de pedidos de prueba que permita al cliente indicar dónde recibirlos y al administrador darles seguimiento.

## Tecnologías utilizadas

| Componente | Uso en el proyecto |
| --- | --- |
| Kotlin y Jetpack Compose | Lógica e interfaz Android. |
| Material 3 | Componentes visuales y navegación. |
| Firebase Authentication | Registro, inicio de sesión y recuperación de contraseña. |
| Cloud Firestore | Productos, perfiles de usuario y pedidos sincronizados. |
| Google Play Services Location | Obtención de las coordenadas del teléfono, con permiso del usuario. |
| Google Maps mediante enlace web | Apertura del punto de entrega desde un pedido; no requiere clave de Maps. |
| Gradle Kotlin DSL | Configuración y compilación de la aplicación. |

La compilación usa `compileSdk` y `targetSdk` 36, y requiere Android 8.0 o superior (`minSdk` 26). Las versiones de las bibliotecas están declaradas en `gradle/libs.versions.toml` y `app/build.gradle.kts`.

## Funciones implementadas

### Cliente

1. Registrarse e iniciar sesión con correo y contraseña.
2. Recuperar la contraseña por correo.
3. Buscar repuestos y filtrar por categoría.
4. Ver fotos, precios y existencias.
5. Agregar productos al carrito y modificar cantidades sin superar el stock mostrado.
6. Crear un pedido de prueba con dirección o referencia de entrega.
7. Pulsar **Usar mi ubicación actual** para adjuntar un punto GPS al pedido. La app intenta sugerir una dirección postal y el cliente puede completar la calle, el número y la referencia antes de confirmarla.
8. Consultar el estado del pedido y abrir su punto de entrega en el mapa cuando se adjuntó una ubicación.

### Administrador

1. Consultar un resumen de productos, pedidos y productos con poco stock.
2. Agregar, editar y retirar repuestos del catálogo.
3. Ajustar existencias y agregar una foto desde el teléfono.
4. Ver pedidos y avanzar su estado: **Pendiente → En preparación → En camino → Entregado**.
5. Abrir en el mapa el punto de entrega de los pedidos que tengan coordenadas.

El carrito vive en la memoria del teléfono. Los productos, las fotos comprimidas, el stock y los pedidos se sincronizan con Firestore.

## Funcionamiento de la geolocalización

La app declara los permisos `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION`, pero los solicita al usuario solo cuando pulsa **Usar mi ubicación actual**. Primero consulta la ubicación actual mediante `FusedLocationProviderClient`. Si ese servicio no devuelve un punto, intenta usar una ubicación guardada durante los últimos cinco minutos. Luego usa `Geocoder` de Android para buscar una dirección postal a partir de las coordenadas. Revisa hasta cinco resultados y reintenta una vez si no encuentra una calle. También conserva durante una hora una dirección de calle encontrada para reutilizarla solo si el nuevo punto GPS está a 30 m o menos. La dirección sugerida aparece en un campo editable y el usuario debe confirmar que el texto y el punto GPS correspondan al lugar de entrega. El punto se guarda como `GeoPoint` en el campo `deliveryLocation` del pedido.

La geolocalización es opcional: si se deniega el permiso o el teléfono no consigue un punto reciente, el cliente puede escribir la dirección manualmente. La dirección devuelta por `Geocoder` es una **sugerencia**, no una garantía del número de casa o de la puerta exacta. Si el sistema no devuelve al menos el nombre de una calle, se muestran las coordenadas y el cliente debe escribir una calle o referencia antes de confirmar. El enlace **Ver ubicación en el mapa** abre las coordenadas mediante una URL de Google Maps; no usa una clave de Google Maps API. No se rastrea la ubicación en segundo plano.

La **Geocoding API de Google Maps Platform** no está activada: Google exige una cuenta de facturación para utilizarla y el proyecto se mantiene sin facturación. La búsqueda de calle actual usa el servicio `Geocoder` del dispositivo Android. Para integrar la API oficial de Google habría que habilitar facturación, proteger la clave en un servidor y configurar un límite de uso.

## Arquitectura y datos

La interfaz está construida con pantallas Compose. `AuthViewModel` gestiona la sesión y consulta el rol guardado en Firestore. `DemoStoreViewModel` escucha cambios en productos y pedidos, administra el carrito y realiza las operaciones de catálogo y pedidos. `DemoDashboardScreen.kt` contiene las pantallas de cliente y administrador.

| Colección | Datos principales |
| --- | --- |
| `users/{uid}` | Correo y rol (`client` o `admin`). |
| `products/{id}` | Nombre, categoría, descripción, precio, stock y foto comprimida. |
| `orders/{id}` | Usuario, correo, dirección, líneas del pedido, total, estado, fecha y `deliveryLocation` opcional. |

Al crear un pedido, la app ejecuta una transacción de Firestore: lee el stock actual, comprueba las cantidades, reduce las existencias y crea el documento del pedido. Los pedidos anteriores sin `deliveryLocation` siguen siendo legibles.

## Firebase y seguridad

- Firebase Authentication tiene habilitado **Email/Password**.
- Cloud Firestore `(default)` está creado y el archivo `app/google-services.json` corresponde a `tallerkhaory` y `Com.Moto_Kaori`. Gradle verifica esos dos valores durante la compilación.
- `firestore.rules` y `firestore.indexes.json` están publicados en el proyecto. El índice de bytes de fotos está desactivado para ese campo.
- Al registrarse, la app crea `users/{uid}` con el rol `client`. El cliente no puede asignarse el rol `admin` desde la app. El primer administrador se configuró desde Firebase Console.
- Las reglas permiten leer pedidos propios; el administrador puede leer los pedidos y cambiar su estado. Solo el administrador puede administrar los datos del catálogo, con la excepción de la reducción de stock durante el pedido de prueba.
- Cloud Storage y Cloud Functions no se usan en esta versión, que permanece en Spark.

El catálogo contiene ocho repuestos iniciales de muestra con imágenes. `seed_products.js` permite cargarlos sin reemplazar productos existentes. Las imágenes base también están en `app/src/main/res/drawable-nodpi/`. Las fotos nuevas se comprimen a menos de 300 KB antes de guardarse en documentos de Firestore.

## Requisitos y ejecución

Se necesita Android Studio con un JDK compatible, conexión a Internet y un dispositivo Android 8.0 o superior. Para usar el GPS, el teléfono debe tener activada la ubicación y conceder el permiso cuando la app lo solicite.

1. Abre la carpeta `MOTOREPUESTOS` en Android Studio.
2. Comprueba que `app/google-services.json` sea el del proyecto Firebase `tallerkhaory`.
3. Compila desde PowerShell:

   ```powershell
   .\gradlew.bat assembleDebug
   ```

4. Instala el APK generado en un teléfono con depuración USB autorizada:

   ```powershell
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

El archivo resultante queda en `app/build/outputs/apk/debug/app-debug.apk`.

Para volver a publicar cambios en autenticación, reglas o índices con Firebase CLI autenticado:

```powershell
npx --yes firebase-tools@latest deploy --project tallerkhaory --only auth,firestore:rules,firestore:indexes
```

## Pruebas realizadas

- La compilación `assembleDebug` terminó correctamente.
- El APK se instaló y abrió en un teléfono Android conectado por USB.
- Se comprobó en el teléfono que, al pulsar **Usar mi ubicación actual**, aparecieron las coordenadas en el campo de entrega y en el resumen del punto GPS.
- Después de esa prueba se retiró el producto agregado al carrito. No se creó ningún pedido durante la comprobación del GPS.
- La versión con búsqueda de dirección postal mediante `Geocoder` se instaló y probó en el teléfono: mostró el nombre de la calle, los datos de manzana/lote, la ciudad, las coordenadas y una precisión estimada de 12 m. El resultado sigue siendo una sugerencia que el usuario debe confirmar. No se creó ningún pedido durante esta prueba.
- En una segunda consulta del mismo teléfono, el servicio de direcciones no devolvió una calle y la app mostró solo coordenadas. Se amplió la búsqueda, se añadió un reintento y una caché temporal para puntos cercanos. Con autorización del usuario se instaló la versión corregida; tres consultas posteriores en el mismo lugar mostraron o conservaron una dirección con nombre de calle. Se retiró el producto agregado solo para la prueba y no se creó ningún pedido.

## Alcance y mejoras pendientes

Esta versión es una demostración funcional y **no debe recibir pedidos comerciales reales**. No procesa pagos. La validación de precios y existencias se realiza en una transacción iniciada por la app; un cliente modificado podría falsificar importes o reducir existencias. Antes de aceptar pedidos reales, la creación del pedido y el descuento de stock deben trasladarse a un backend confiable, por ejemplo Cloud Functions, con la facturación que requiera Firebase.

Otras mejoras posibles son guardar el carrito entre sesiones, seleccionar el punto de entrega directamente en un mapa y añadir pruebas automatizadas del flujo completo de pedidos.

## Archivos principales

| Archivo | Propósito |
| --- | --- |
| `app/src/main/java/com/khaory/taller/MainActivity.kt` | Entrada de la app. |
| `app/src/main/java/com/khaory/taller/ui/login/AuthViewModel.kt` | Autenticación y roles. |
| `app/src/main/java/com/khaory/taller/ui/demo/DemoDashboardScreen.kt` | Pantallas, carrito, GPS y mapa. |
| `app/src/main/java/com/khaory/taller/ui/demo/DemoStoreViewModel.kt` | Productos, pedidos y Firestore. |
| `app/src/main/AndroidManifest.xml` | Permisos y actividad principal. |
| `firestore.rules` | Reglas de acceso a la base. |
| `firestore.indexes.json` | Configuración de índices. |
| `FIREBASE_SETUP.md` | Detalles de la configuración Firebase. |
