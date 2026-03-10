# OpenCameraHouse 🏠

Un sistema de videovigilancia casero de código abierto para Android, estructurado bajo una arquitectura cliente-servidor. Diseñado para reutilizar teléfonos antiguos (como un Galaxy S10) y convertirlos en cámaras de seguridad inteligentes, autónomas y seguras.

* **Camera-Server:** App para reutilizar un teléfono como cámara de monitoreo, detector de movimiento y servidor de almacenamiento local.
* **Camera-Client:** App visor moderna (Material Design 3) para conectarse de forma segura, visualizar transmisiones y reproducir eventos grabados.

<img width="2816" height="1536" alt="Gemini_Generated_Image_tl36gutl36gutl36" src="https://github.com/user-attachments/assets/e6c884db-e152-4792-8aba-b378a4d2de3c" />

## 🛠️ Arquitectura del Proyecto

El sistema se divide en dos aplicaciones nativas de Android y una capa de red segura:

1. **Camera-Server (App Servidor):** Instalada en el dispositivo de captura. Utiliza `CameraX` para grabar, cuenta con un algoritmo de detección de movimiento propio, y levanta un servidor HTTP ligero en segundo plano con `Ktor` para servir los archivos.
2. **Camera-Client (App Visor):** Aplicación cliente para el teléfono principal del usuario. Escanea QR para agregar cámaras, consume la API del servidor y reproduce el video en *streaming* usando `ExoPlayer` (Media3) con una interfaz moderna Edge-to-Edge.
3. **Capa de Red (Tailscale):** Ambos dispositivos se conectan a través de una VLAN WireGuard (Tailscale) para permitir una conexión segura, encriptada (P2P) y remota sin necesidad de abrir puertos en el router.

## ✨ Características Principales

### Camera-Server (Dispositivo de Captura)
* **Grabación por Eventos:** Algoritmo de análisis de luminosidad que dispara la grabación únicamente cuando detecta movimiento físico, optimizando el almacenamiento.
* **Grabación en Bloques (Chunks):** Los videos se guardan de forma segura e ininterrumpida por intervalos para evitar corrupción.
* **API RESTful Integrada:** Ktor sirve los videos localmente exponiendo los endpoints `/videos` (lista JSON) y `/video/{nombre}` (archivo MP4).
* **Blindaje de Sistema (24/7):** Uso de `Wakelocks` y exclusiones nativas de batería de Android para evitar que el sistema cierre el servidor en segundo plano.

### Camera-Client (App Visor)
* **Interfaz Moderna:** Diseño Material Design 3 limpio con navegación inferior oculta dinámicamente al reproducir video.
* **Escáner QR:** Agrega nuevas cámaras instantáneamente escaneando el código de emparejamiento del servidor.
* **Reproductor Nativo:** Integración fluida con ExoPlayer para streaming de video remoto de baja latencia.
* **Gestión de Eventos:** Lista automática de grabaciones de movimiento disponibles en el servidor, listas para reproducir con un toque.
* **Persistencia Local:** Almacenamiento seguro de la configuración y lista de cámaras en el dispositivo.

## 🚀 Estado del Desarrollo

El sistema se encuentra en una fase funcional y estable:

- [x] **Fase 1: Hardware y Captura.** Integración de CameraX y algoritmo de detección de movimiento.
- [x] **Fase 2: Servidor Web Interno.** Motor Ktor y endpoints configurados.
- [x] **Fase 3: Enlace de Red Seguro.** Configuración exitosa de Tailscale (VLAN) para comunicación remota.
- [x] **Fase 4: App Cliente (Visor).** Interfaz moderna, escáner QR, persistencia y reproductor ExoPlayer 100% operativos.

## 💻 Tecnologías Utilizadas

* **Lenguaje:** Kotlin
* **Cámara (Servidor):** AndroidX CameraX (Preview, VideoCapture, ImageAnalysis)
* **Red / API (Servidor):** Ktor Server (Netty, ContentNegotiation, CORS)
* **Reproductor (Cliente):** Media3 ExoPlayer
* **UI (Cliente):** Material Design 3, ViewBinding, Navigation Component
* **Capa de Red:** Tailscale (WireGuard VPN)
* **Formato de Video:** MP4 (1080p FHD optimizado)

## ⚙️ Instalación Avanzada (Servidor - ADB)

Para garantizar que el dispositivo servidor no apague la pantalla ni suspenda los procesos, se recomienda otorgar permisos de ajustes seguros vía ADB tras la instalación:

```bash
adb shell pm grant com.carlengosez.open_camera_server android.permission.WRITE_SECURE_SETTINGS
