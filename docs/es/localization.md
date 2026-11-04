---
title: Localización e i18n
description: Guía completa del sistema de paquetes de idioma BCP-47 de Croppy, resolución en cascada, formato MiniMessage e integración con Crowdin.
sidebar:
  order: 19
---

# Localización e Internacionalización de Croppy (i18n)

Este documento describe el diseño, la arquitectura, las convenciones de claves y el flujo de trabajo para el sistema de internacionalización (i18n) de Croppy y su integración con Crowdin.

---

## 1. Visión Arquitectónica

Croppy sigue una estricta **Arquitectura Hexagonal (Puertos y Adaptadores)** para desacoplar la lógica de dominio de las APIs de Minecraft/Paper y de las herramientas externas de traducción:

```text
+--------------------------------------------------------+
|                      croppy-api                        |
|   Contratos en Java 21 puro, cero dependencias         |
|   * MessageKey (registro semántico inmutable de clave) |
|   * LocaleRef (representación de etiqueta BCP-47)      |
|   * TranslationProvider (puerto SPI de consulta)       |
+--------------------------+-----------------------------+
                           | implementado por
+--------------------------v-----------------------------+
|                      croppy-core                       |
|   Motor de dominio y catálogo puro en memoria          |
|   * DefaultMessageCatalog (concurrente, atomic swap)   |
|   * YamlLocaleLoader (aplanador SnakeYAML)             |
|   * LocaleValidator & LocaleValidationReport          |
+--------------------------+-----------------------------+
                           | adaptado por
+--------------------------v-----------------------------+
|                     croppy-paper                       |
|   Adaptador de plataforma Paper (Paper 1.21.8+)        |
|   * PaperMessageService (MiniMessage & Adventure)      |
|   * Sanitización de argumentos (previene inyecciones)  |
|   * Resolvedor de reemplazos de contenido localizado   |
|   * Paquetes de recursos embebidos (en-US, es-ES)      |
+--------------------------------------------------------+
```

---

## 2. Resolución Determinista en Cascada (Fallback)

Al resolver un mensaje para una audiencia (como un `Player`), el catálogo evalúa la siguiente cadena de fallback de 5 niveles:

1. **Etiqueta Exacta del Cliente**: Coincide con el idioma del cliente Minecraft del jugador (ej. `es-MX`).
2. **Fallback al Idioma Base**: Si a `es-MX` le falta la clave, recurre al catálogo del idioma base (ej. `es-ES` o `es`).
3. **Idioma Predeterminado del Servidor**: Configurado mediante `messages.default-locale` en `config.yml` (por defecto `en-US`).
4. **Idioma Fuente Canónico Embebido**: Fallback canónico `en-US` integrado dentro del JAR del plugin.
5. **Token Seguro de Clave Faltante**: Si la clave falta en todos los catálogos, devuelve `<missing:namespace:value>` sin lanzar excepciones ni detener el tick del servidor. Las claves faltantes se registran con limitación de frecuencia para evitar saturación de logs.

---

## 3. Taxonomía y Estructura de Claves

Todas las claves de mensajes utilizan rutas jerárquicas en minúsculas separadas por puntos:

| Prefijo | Alcance | Ejemplos |
|---|---|---|
| `general.*` | Mensajes comunes del plugin y prefijo | `general.prefix`, `general.no-permission`, `general.reload-success` |
| `command.*` | Respuestas de comandos, argumentos y errores | `command.give.received`, `command.list.category-header` |
| `inspector.*` | Tarjeta de inspección de cultivos, estados y líneas | `inspector.title`, `inspector.species`, `inspector.progress` |
| `watering.*` | Regaderas y avisos de hidratación | `watering.refilled`, `watering.empty` |
| `fertilizer.*` | Fertilizantes y enriquecimiento del suelo | `fertilizer.applied`, `fertilizer.already-applied` |
| `station.*` | Estaciones, temperatura y barras de acción | `station.temperature`, `station.empty-feedback` |
| `prompt.*` | Prompts interactivos en chat (ej. temperatura) | `prompt.temperature-prompt`, `prompt.temperature-bounds` |
| `content.*` | Reemplazos opcionales de definiciones | `content.item.croppy.wheat_seeds.name` |

### Reemplazos de Localización de Contenido

Croppy permite traducir ítems, recetas o estaciones sin modificar ni duplicar los archivos YAML originales. Si un archivo de idioma define:

```yaml
content:
  item:
    croppy:
      wheat_seeds:
        name: "<yellow>Semillas de Trigo Doradas</yellow>"
        lore:
          - "<gray>Planta estas semillas en tierra arada.</gray>"
          - "<gold>Calidad superior garantizada.</gold>"
```

`PaperMessageService#resolveItemDisplayName` y `resolveItemLore` priorizarán automáticamente estos textos traducidos sobre los definidos por defecto en `items/seeds/wheat_seeds.yml`.

---

## 4. Parámetros y Seguridad contra Inyección de Etiquetas

Croppy utiliza **Kyori Adventure MiniMessage** para todo el formato de texto hacia el jugador:

```yaml
command:
  give:
    received: "<green>Has recibido <yellow><amount>x</yellow> <item>.</green>"
```

### Sanitización de Argumentos

Los valores dinámicos no confiables (nombres de jugadores, textos libres de entrada externa) DEBEN pasarse utilizando `PaperMessageService.arg(key, value)` o `PaperMessageService.args(...)`.

Estos métodos envuelven los valores en `Placeholder.unparsed(key, stringValue)`, impidiendo que un usuario malicioso inyecte etiquetas de formato o eventos de clic (ej. `<click:run_command:/op>`).

Cuando se requiera formato enriquecido intencional (como subcomponentes pre-formateados o gradientes), pase un `Component` de Adventure a `PaperMessageService.arg(key, component)`.

---

## 5. Añadir un Nuevo Idioma

### Paso 1: Crear el Archivo de Traducción

Crea un archivo nuevo en `croppy-paper/src/main/resources/locales/<etiqueta-locale>.yml` (ej. `fr-FR.yml` o `de-DE.yml`).

### Paso 2: Copiar Claves desde la Fuente Canónica

Copia todas las claves desde `croppy-paper/src/main/resources/locales/en-US.yml` y traduce sus valores utilizando sintaxis MiniMessage.

### Paso 3: Verificar la Integridad Localmente

Ejecuta el validador automático de idiomas:

```powershell
.\gradlew.bat test --tests "dev.darkblade.croppy.platform.paper.i18n.LocaleResourcesIntegrityTest"
```

El test comprueba:
- Sintaxis correcta del mapeo YAML.
- Valores no vacíos en todas las entradas.
- 100% de sincronización de claves entre el nuevo idioma y `en-US.yml`.

---

## 6. Integración con Crowdin

Croppy se integra con [Crowdin](https://crowdin.com) para permitir traducciones colaborativas.

### Configuración (`crowdin.yml`)

El archivo raíz `crowdin.yml` mapea la fuente en inglés hacia las traducciones de destino:

```yaml
project_id_env: CROWDIN_PROJECT_ID
api_token_env: CROWDIN_PERSONAL_TOKEN
base_path: "."
base_url: "https://api.crowdin.com"

preserve_hierarchy: true

files:
  - source: /croppy-paper/src/main/resources/locales/en-US.yml
    translation: /croppy-paper/src/main/resources/locales/%locale%.yml
    update_option: update_as_unapproved
```

### Sincronización CI/CD con GitHub Actions

- **`.github/workflows/build.yml`**: Se ejecuta en cada push y PR para verificar compilación, tests e integridad de idiomas.
- **`.github/workflows/crowdin-sync.yml`**: Sube automáticamente las claves actualizadas de `en-US.yml` a Crowdin y crea pull requests con las nuevas traducciones aportadas por la comunidad.
- **Secretos requeridos en el repositorio**:
  - `CROWDIN_PROJECT_ID`: ID numérico del proyecto en Crowdin.
  - `CROWDIN_PERSONAL_TOKEN`: Token de acceso personal con permisos sobre el proyecto.

### Independencia en Tiempo de Ejecución

Croppy es completamente independiente de Crowdin en el servidor. Crowdin opera exclusivamente en CI/CD; el plugin no realiza llamadas de red externas para funcionar.
