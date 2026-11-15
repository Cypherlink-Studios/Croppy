---
title: Sistema de Addons y Extensibilidad
description: Guía completa de arquitectura y desarrollo del Sistema de Addons de Croppy, con soporte para packs declarativos, extensiones Java, grafo de ClassLoaders DAG y recarga en caliente segura.
sidebar:
  order: 21
---

# Sistema de Addons y Extensibilidad

Croppy incorpora un **Motor de Addons** oficial y desacoplado que permite tanto a administradores de servidores como a desarrolladores de Java expandir la simulación agrícola, cultivos, ítems, estaciones de cocina y recetas culinarias sin modificar el código base del plugin.

El sistema se rige por límites arquitectónicos estrictos:
- **Packs de Contenido Declarativo**: Definiciones puras en YAML y assets para resource packs sin necesidad de compilar código Java.
- **Addons de Código Java**: Archivos JAR modulares que implementan `CroppyAddon` en entornos aislados `AddonClassLoader`, con delegación DAG entre dependencias y hot-swapping seguro en 5 fases — consulta [Desarrollo de Addons en Java](./api/addon-development.md).
- **Variables y Contratos Extensibles**: Los addons pueden registrar tipos `DataType<T>` persistentes, namespaces `VariableProvider` y hooks `GrowthModifier` — consulta la [Referencia de API](./api/index.md).
- **Fusión Automática de Resource Packs**: Los assets ubicados en `assets/<namespace>/` dentro de los addons se combinan automáticamente en el resource pack del servidor.

---

## 1. Modelo Arquitectónico y Jerarquía de Capas

Croppy implementa un modelo de contenido en tres capas de prioridad:

```text
+-------------------------------------------------------------------------------+
|                       JERARQUÍA DE CONTENIDO Y PRIORIDAD                      |
+-------------------------------------------------------------------------------+
|                                                                               |
|  [ 1. OVERRIDES DEL SERVIDOR LOCAL ] (plugins/Croppy/crops/, items/, etc.)    |
|     * Prioridad Máxima: Sobrescribe cualquier definición de addon o del core. |
|     * Ajustes rápidos para administradores sin necesidad de reempaquetar.     |
|                                                                               |
|  [ 2. CAPA DE ADDONS ] (plugins/Croppy/addons/*.jar o carpetas)               |
|     * Unidades distribuibles con su propio namespace y manifiesto addon.yml.  |
|     * Pueden contener YAMLs declarativos, modelos 3D/texturas o código Java.  |
|                                                                               |
|  [ 3. NÚCLEO CROPPY DEFAULT ] (Contenido base de fábrica)                     |
|     * Cultivos y recetas estándar provistas por el plugin Croppy.             |
|                                                                               |
+-------------------------------------------------------------------------------+
```

---

## 2. El Manifiesto del Addon (`addon.yml`)

Todo addon debe declarar un archivo `addon.yml` en su raíz:

```yaml
# Identificador único del addon (minúsculas, números, guiones, puntos y guiones bajos)
id: arcane-farming

# Nombre legible para humanos
name: "Arcane Farming"

# Versión semántica
version: "1.0.0"

# Namespace principal de recursos perteneciente a este addon
namespace: arcane

# Clase principal opcional que implementa CroppyAddon.
# Si se omite o queda vacía, ¡Croppy lo trata como un Pack de Contenido Declarativo!
main: dev.darkblade.croppy.arcane.ArcaneFarmingAddon

# Metadatos opcionales de autores
authors:
  - DarkBladeDev
  - ContributorOne

# Descripción breve opcional
description: "Cultivos mágicos, fases lunares y mecánicas de infusión en altares."

# Dependencias requeridas (resueltas topológicamente en orden estricto)
dependencies:
  - croppy

# Dependencias blandas/opcionales (cargadas antes de este addon si están presentes)
softDependencies:
  - exotic-fruits
```

### Reglas de Validación
* `id` y `namespace`: Deben cumplir estrictamente con la expresión regular `^[a-z0-9_.-]+$`.
* `main`: Debe apuntar a una clase pública con constructor sin argumentos que implemente `dev.darkblade.croppy.api.addon.CroppyAddon`.
* Las dependencias circulares (`A -> B -> A`) son detectadas mediante el algoritmo de Kahn y rechazadas al inicio.

---

## 3. API de Desarrollo en Java

Los addons en código Java interactúan con Croppy exclusivamente a través de contratos de `croppy-api` (con adaptación opcional a Paper mediante `PaperAddonContext`). Toda la especificación técnica —grafo de ClassLoaders, protocolo de desmantelamiento en 5 fases, variables tipadas personalizadas, proveedores de expresiones y puertos SPI— se encuentra ahora en la subsección dedicada de la API:

- **[Resumen de la API y Contratos Base](./api/index.md)**: Arquitectura hexagonal, configuración Gradle, mapa de paquetes y tipos de valor `Key` / `BlockPosition`.
- **[Desarrollo de Addons en Java](./api/addon-development.md)**: `CroppyAddon`, `AddonContext`, `PaperAddonContext`, `AddonConfig`, delegación DAG en `AddonClassLoader`, hot-swapping en 5 fases, seguridad de cultivos latentes y hooks de crecimiento.
- **[Sistema de Variables Tipadas](./api/typed-variables.md)**: Esquemas `DataType<T>`, `DataTypeRegistry`, `TypedVariableStore`, `VariableOperation<T>` y `VariableStoreBinaryCodec`.
- **[Proveedores de Variables y Expresiones](./api/expressions-and-providers.md)**: Namespaces de `VariableProvider`, `EvaluationContext`, compilación AST Pratt y `ContextualTextFormatter`.
- **[Puertos, Logging y Registros](./api/ports-and-registries.md)**: `Registry<T>`, registros de dominio, `CroppyLogger`, `ProtectionPort`, `EconomyPort` y `TranslationProvider`.

---

## 4. Guía para Creadores de Contenido: Packs Declarativos

Un pack declarativo no requiere conocimientos de programación. Simplemente coloca tus archivos en `plugins/Croppy/addons/<tu-pack>/`:

```text
plugins/Croppy/addons/exotic-fruits/
├── addon.yml
├── crops/
│   └── dragonfruit.yml
├── items/
│   ├── seeds/
│   │   └── dragonfruit_seeds.yml
│   └── ingredients/
│       └── dragonfruit.yml
├── recipes/
│   └── dragonfruit_smoothie.yml
└── assets/
    └── exotic_fruits/
        ├── models/item/dragonfruit.json
        └── textures/item/dragonfruit.png
```

Croppy se encargará automáticamente de:
1. Parsear `addon.yml`.
2. Registrar cultivos, ítems y recetas en los registros del núcleo.
3. Fusionar `assets/exotic_fruits/` en el resource pack del servidor.

---

## 5. Referencia de Comandos

Todos los comandos de gestión de addons requieren el permiso `croppy.admin.addon`:

| Comando | Descripción |
|---|---|
| `/croppy addon list` | Muestra la lista de addons descubiertos, versión, tipo (Java / Pack) y estado (`ON_DISK`, `LOADED`, `ENABLED`). |
| `/croppy addon status <addonId>` | Muestra información detallada: metadatos, clases cargadas, especies/ítems registrados y listeners activos. |
| `/croppy addon load <addonId \| all>` | Carga el manifiesto y prepara el ClassLoader en memoria (estado `LOADED`). |
| `/croppy addon enable <addonId \| all>` | Activa el addon, ejecuta `onEnable` y registra su contenido en el juego (estado `ENABLED`). |
| `/croppy addon disable <addonId \| all>` | Ejecuta el protocolo de 5 fases, desregistrando listeners y registros del juego (estado `LOADED`). |
| `/croppy addon unload <addonId \| all>` | Cierra completamente el ClassLoader y libera referencias de memoria (estado `ON_DISK`). |
