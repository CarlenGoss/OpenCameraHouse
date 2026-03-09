# OpenCameraHouse
Un sistema de videovigilancia casero de código abierto para Android, estructurado bajo una arquitectura cliente-servidor. Diseñado para reutilizar teléfonos antiguos (como un Galaxy S10) y convertirlos en cámaras de seguridad inteligentes, autónomas y seguras.

* **Camera-Server:** App para reutilizar un teléfono (ej. Galaxy S10) como cámara de monitoreo y servidor de almacenamiento local.
* **Camera-Client:** App visor para conectarse de forma segura y reproducir las grabaciones.

<img width="2816" height="1536" alt="Gemini_Generated_Image_tl36gutl36gutl36" src="https://github.com/user-attachments/assets/e6c884db-e152-4792-8aba-b378a4d2de3c" />

## 🛠️ Arquitectura del Proyecto

El sistema se divide en dos aplicaciones y una capa de red:

1. **Camera-Server (App Servidor):** Instalada en el dispositivo de captura. Utiliza `CameraX` para grabar, cuenta con un algoritmo de detección de movimiento propio, y levanta un servidor HTTP ligero en segundo plano con `Ktor` para servir los archivos.
2. **Camera-Client (App Visor):** *(En desarrollo)* Aplicación para el teléfono principal del usuario. Consume la API del servidor y reproduce el video en *streaming* usando `ExoPlayer`.
3. **Capa de Red (Tailscale):** Ambos dispositivos se conectan a través de una VLAN WireGuard (Tailscale) para permitir una conexión segura, encriptada (P2P) y remota sin necesidad de abrir puertos en el router.

## ✨ Características Principales (Servidor)

* **Grabación por Eventos:** Algoritmo de análisis de luminosidad que dispara la grabación únicamente cuando detecta movimiento físico, optimizando el almacenamiento.
* **Grabación en Bloques (Chunks):** Los videos se guardan de forma segura e ininterrumpida por intervalos (ej. 1 hora) para evitar corrupción de archivos pesados.
* **API RESTful Integrada:** Ktor sirve los videos localmente exponiendo los endpoints `/videos` (lista JSON) y `/video/{nombre}` (archivo MP4).
* **Blindaje de Sistema (24/7):** Uso de `Wakelocks` y exclusiones nativas de batería de Android para evitar que la capa de personalización (ej. One UI) cierre el servidor en segundo plano.

## 🚀 Estado del Desarrollo (Roadmap)

- [x] **Fase 1: Hardware y Captura.** Integración de CameraX y algoritmo de detección de movimiento.
- [x] **Fase 2: Servidor Web Interno.** Motor Ktor y endpoints configurados.
- [x] **Fase 2.5: Blindaje de Energía.** Permisos avanzados para ejecución perpetua y exclusión de batería.
- [ ] **Fase 3: Enlace de Red Seguro.** Configuración de Tailscale (VLAN).
- [ ] **Fase 4: App Cliente.** Construcción del visor y reproductor con ExoPlayer.

## 💻 Tecnologías Utilizadas

* **Lenguaje:** Kotlin
* **Cámara:** AndroidX CameraX (Preview, VideoCapture, ImageAnalysis)
* **Red / API:** Ktor Server (Netty, ContentNegotiation, CORS)
* **Formato de Video:** MP4 (1080p FHD optimizado)
* **Próximamente:** Media3 ExoPlayer, Tailscale.

## ⚙️ Instalación Avanzada (ADB)

Para garantizar que el dispositivo servidor no apague la pantalla ni suspenda los procesos, se recomienda otorgar permisos de ajustes seguros vía ADB tras la instalación:

```bash
adb shell pm grant com.carlengosez.open_camera_server android.permission.WRITE_SECURE_SETTINGS
