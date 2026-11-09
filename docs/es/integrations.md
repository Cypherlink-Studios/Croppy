---
title: Integraciones de Terceros
description: Guía completa sobre las integraciones modulares de Croppy con WorldGuard, PlaceholderAPI y Vault.
sidebar:
  order: 20
---

# Integraciones de Terceros

Croppy cuenta con un subsistema modular y desacoplado de integraciones (`croppy-integrations`) diseñado bajo **Arquitectura Hexagonal (Puertos y Adaptadores)**. Las bibliotecas de terceros nunca contaminan los módulos del dominio (`croppy-core` o `croppy-api`), garantizando un 100% de estabilidad, cero dependencias obligatorias y un comportamiento seguro de degradación (fallback) cuando los plugins no están instalados o se desactivan.

---

## 1. Modelo Arquitectónico

```text
+-------------------------------------------------------------+
|                         croppy-api                          |
|         Puertos Puros Java 21 (Cero Dependencias)           |
|  * ProtectionPort y ProtectionAction                        |
|  * EconomyPort                                              |
+------------------------------+------------------------------+
                               | implementa
+------------------------------v------------------------------+
|                     croppy-integrations                     |
|  * common:      IntegrationManager, CroppyIntegration       |
|  * worldguard:  WorldGuardProtectionAdapter, WorldGuardFlags |
|  * papi:        CroppyPlaceholderExpansion                  |
|  * vault:       VaultEconomyAdapter                         |
+------------------------------+------------------------------+
                               | empaquetado y conectado
+------------------------------v------------------------------+
|                        croppy-paper                         |
|  * Event Listeners (CropPlantListener, WateringListener...) |
|  * Interceptores de Paquetes (Displays PacketEvents)        |
+-------------------------------------------------------------+
```

Cuando se detecta un plugin compatible en el servidor Paper, su módulo de integración se activa de forma automática. Si el plugin no está presente o se desactiva en `config.yml`, Croppy utiliza adaptadores transparentes `NoOp` sin generar excepciones en los registros ni interrumpir la simulación.

---

## 2. Configuración (`config.yml`)

El subsistema de integraciones se administra mediante la sección `integrations` en `plugins/Croppy/config.yml`:

```yaml
# ==============================================================================
# Third-Party Plugin Integrations
# ==============================================================================
integrations:
  worldguard:
    # Habilita la verificación de protecciones y regiones mediante WorldGuard
    enabled: true
    # Registra banderas personalizadas (croppy-plant, croppy-harvest, croppy-water, croppy-fertilize, croppy-cook)
    use-custom-flags: true

  papi:
    # Habilita la expansión de PlaceholderAPI para telemetría en tiempo real
    enabled: true
    # Distancia máxima de raycast en bloques para identificar cultivos y estaciones
    raycast-distance: 5

  vault:
    # Habilita operaciones monetarias y validaciones de transacciones mediante Vault
    enabled: true
```

---

## 3. Integración con WorldGuard (`croppy-integrations:worldguard`)

Croppy se integra profundamente con **WorldGuard 7.0+** para proteger terrenos y respetar los límites de regiones en todas las acciones agrícolas y culinarias.

### Banderas Personalizadas de WorldGuard

Cuando `use-custom-flags: true` está activo, Croppy registra 5 banderas de estado personalizadas durante el arranque del servidor (ciclo `onLoad`):

| Bandera | Descripción | Estado por Defecto |
|---|---|---|
| `croppy-plant` | Controla el permiso para plantar semillas en tierra arada o espalderas. | `ALLOW` |
| `croppy-harvest` | Controla el permiso para cosechar cultivos (tanto físicos como displays 3D virtuales). | `ALLOW` |
| `croppy-water` | Controla el permiso para usar regaderas sobre cultivos y tierra. | `ALLOW` |
| `croppy-fertilize` | Controla el permiso para aplicar fertilizantes orgánicos a la tierra. | `ALLOW` |
| `croppy-cook` | Controla el permiso para interactuar con estaciones, añadir ingredientes y recoger platillos. | `ALLOW` |

### Protección Secundaria y Bypass
* **Prioridad de Bandera Personalizada**: Si una región define una bandera específica de Croppy (por ejemplo `/rg flag spawn croppy-harvest deny`), ese valor tiene prioridad absoluta.
* **Fallback a Banderas Estándar**: Si las banderas personalizadas no están asignadas o están desactivadas, las acciones se validan contra las banderas estándar de WorldGuard:
  * Bandera `BUILD` para plantar semillas, aplicar fertilizante y colocar/desmantelar estaciones.
  * Bandera `INTERACT` para usar regaderas, cosechar cultivos e interactuar con estaciones.
* **Bypass de Administrador**: Los jugadores con permisos de bypass en WorldGuard (`worldguard.region.bypass.<world>`) o bypass total del servidor ignoran estas comprobaciones.

### Protección de Entidades Virtuales y Paquetes
Las estaciones y cultivos 3D de Croppy se renderizan mediante paquetes de cliente con **PacketEvents**. Si un jugador no autorizado hace clic en las hitboxes virtuales, sus paquetes de interacción son interceptados y bloqueados por `ProtectionPort`, evitando cualquier explotación por paquetes fuera de los eventos tradicionales de Bukkit.

---

## 4. Integración con PlaceholderAPI (`croppy-integrations:papi`)

Croppy registra una expansión nativa de PlaceholderAPI bajo el identificador `%croppy_*%`, proporcionando información en tiempo real para HUDs, barras de acción, marcadores y menús personalizados.

### Raycast Dinámico
Los marcadores con prefijo `%croppy_target_*%` calculan dinámicamente la línea de visión del jugador hasta una distancia de `raycast-distance` bloques (por defecto: `5`). El raycast examina directamente cultivos, estaciones y bloques de tierra arada (farmland) tanto al apuntarles directamente como debajo de cultivos activos.

### Marcadores Compatibles

#### Telemetría Global del Sistema
| Marcador | Descripción | Ejemplo de Salida |
|---|---|---|
| `%croppy_version%` | Cadena de versión activa del plugin Croppy. | `0.5.16` |
| `%croppy_total_active_crops%` | Cantidad total de instancias de cultivos cargadas actualmente en el servidor. | `128` |
| `%croppy_total_active_stations%` | Cantidad total de estaciones de cocina registradas actualmente en el servidor. | `16` |

#### Telemetría de Cultivos Apuntados
| Marcador | Descripción | Ejemplo de Salida |
|---|---|---|
| `%croppy_target_is_crop%` | Indica si el jugador está apuntando directamente a un cultivo válido de Croppy. | `true` |
| `%croppy_target_crop_species%` | Clave canónica con namespace de la especie del cultivo apuntado. | `croppy:grapes` |
| `%croppy_target_crop_species_name%` | Nombre legible formateado del cultivo para interfaces. | `Sweet Grapes` |
| `%croppy_target_crop_stage%` | Índice de la etapa de crecimiento actual (empezando en 0). | `3` |
| `%croppy_target_crop_max_stage%` | Índice de la etapa final de madurez (empezando en 0, `total_etapas - 1`). | `4` |
| `%croppy_target_crop_total_stages%` | Cantidad total de etapas de crecimiento de la especie. | `5` |
| `%croppy_target_crop_progress%` | Porcentaje de progreso de crecimiento hacia la siguiente etapa. | `74.5%` |
| `%croppy_target_crop_is_watered%` | Indica si el cultivo apuntado ha recibido riego durante el día actual. | `true` |
| `%croppy_target_crop_fertilizer%` | Clave con namespace del fertilizante aplicado al cultivo, o `none`. | `croppy:growth_fertilizer` |

#### Telemetría de Tierra y Sustrato
| Marcador | Descripción | Ejemplo de Salida |
|---|---|---|
| `%croppy_target_soil_is_farmland%` | Indica si el bloque apuntado o el bloque bajo el cultivo es tierra arada. | `true` |
| `%croppy_target_soil_water%` | Nivel numérico de humedad de vanilla de la tierra arada (`0` a `7`). | `6` |
| `%croppy_target_soil_moisture%` | Porcentaje de saturación de humedad en la tierra (`0%` a `100%`). | `86%` |

#### Telemetría de Estaciones de Cocina
| Marcador | Descripción | Ejemplo de Salida |
|---|---|---|
| `%croppy_target_is_station%` | Indica si el jugador está apuntando directamente a una estación de cocina. | `true` |
| `%croppy_target_station_name%` | Clave o tipo de la estación de cocina apuntada (alias de `_type`). | `croppy:skillet` |
| `%croppy_target_station_type%` | Clave canónica con namespace de la estación de cocina apuntada. | `croppy:skillet` |
| `%croppy_target_station_temperature%` | Temperatura actual de operación de la estación con un decimal. | `185.0°C` |
| `%croppy_target_station_is_cooking%` | Indica si la estación tiene un proceso de cocción o receta activo. | `true` |
| `%croppy_target_station_cooking_progress%` | Porcentaje de progreso del proceso de cocción multi-fase activo. | `62.0%` |

#### Compendio Culinario del Jugador
| Marcador | Descripción | Ejemplo de Salida |
|---|---|---|
| `%croppy_compendium_discovered%` | Número de recetas culinarias desbloqueadas por el jugador. | `14` |
| `%croppy_compendium_discovered_recipes%` | Alias descriptivo de `%croppy_compendium_discovered%`. | `14` |
| `%croppy_compendium_total%` | Total de recetas culinarias registradas en el servidor. | `42` |
| `%croppy_compendium_total_recipes%` | Alias descriptivo de `%croppy_compendium_total%`. | `42` |
| `%croppy_compendium_percentage%` | Porcentaje de dominio del compendio con un decimal. | `33.3%` |
| `%croppy_compendium_completion_percentage%` | Alias descriptivo de `%croppy_compendium_percentage%`. | `33.3%` |
| `%croppy_compendium_is_unlocked_<clave_receta>%` | Indica si el jugador ha descubierto la receta especificada. | `true` |

---

## 5. Integración con Vault (`croppy-integrations:vault`)

Croppy proporciona un puente financiero unificado compatible con **Vault 1.7+** a través del puerto desacoplado `EconomyPort`:

* **Seguridad Transaccional**: Las verificaciones de fondos, cobros y depósitos se ejecutan de manera atómica.
* **Formato Monetario Universal**: La cantidad de dinero formateada a través de `EconomyPort.format(amount)` produce representaciones estándar (ej. `$100.00`).
* **Degradación Segura**: Si Vault o un plugin de economía (EssentialsX, EconomyShopGUI, etc.) no están instalados, el adaptador `NoOpEconomyAdapter` rechaza operaciones de pago sin provocar errores de puntero nulo (`NullPointerException`) ni averiar el compendio o los procesos culinarios.
