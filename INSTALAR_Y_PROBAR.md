# Instalar y probar MotoRepuestos Khaory

1. Revisa el estado ya configurado en [FIREBASE_SETUP.md](FIREBASE_SETUP.md).
2. Abre `MOTOREPUESTOS` en Android Studio o compila con `./gradlew.bat assembleDebug`.
3. Instala `app/build/outputs/apk/debug/app-debug.apk` en un Android con Google Play Services. El APK de prueba ya fue instalado en el teléfono conectado por USB.
4. Para entrar como administrador, abre el correo de Firebase enviado a `homeroquispe06@gmail.com`, establece la contraseña y elige **Administrador** en la app.
5. Para probar como cliente, registra otra cuenta desde la app.

Los pedidos son reales en Firestore, pero la app no procesa pagos. Antes de recibir compras reales, implementa el backend de validación indicado en `FIREBASE_SETUP.md`.
