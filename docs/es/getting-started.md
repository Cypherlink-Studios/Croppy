---
title: Primeros Pasos
description: Guía paso a paso de instalación de Croppy en Paper 1.21.8+, dependencias requeridas y tu primera cosecha.
sidebar:
  order: 2
---

# Primeros Pasos con Croppy

Esta guía acompaña a administradores y jugadores en la instalación de **Croppy** en servidores Paper 1.21.8+ y en el desarrollo de su primer ciclo agrícola.

---

## 1. Requisitos del Sistema

Antes de instalar Croppy, comprueba que tu entorno cumpla con los siguientes requisitos mínimos:

| Componente | Requisito Mínimo | Recomendado |
|---|---|---|
| **Plataforma del Servidor** | Paper 1.21.8 o Purpur 1.21.8 | Última compilación de Paper 1.21.8+ |
| **Java Runtime** | Java 21 (Temurin / GraalVM) | Java 21 LTS |
| **PacketEvents** | 2.14.0+ (Dependencia Obligatoria) | Última versión disponible |
| **Soporte Folia** | No soportado | No utilizar en Folia |

> **Importante**: **PacketEvents** es una dependencia obligatoria. Croppy lo utiliza para renderizar displays virtuales de ítems y texto en las estaciones de cocina a nivel de paquetes de red, eliminando entidades fantasma y garantizando un rendimiento óptimo en el cliente.

---

## 2. Pasos de Instalación

1. **Instalar PacketEvents**:
   Descarga el archivo `packetevents-spigot` desde los [Releases de PacketEvents en GitHub](https://github.com/retrooper/packetevents/releases) y colócalo en la carpeta `plugins/` de tu servidor.

2. **Instalar Croppy**:
   Descarga la versión más reciente de `Croppy-0.3.0.jar` desde [Croppy Releases](https://github.com/Cypherlink-Studios/Croppy/releases) y colócala en `plugins/`.

3. **Iniciar el Servidor**:
   Inicia tu servidor. Durante el arranque, Croppy creará sus directorios predeterminados, descomprimirá los archivos de configuración, inicializará el backend de persistencia y registrará las especies y recetas base.

4. **Verificar el Arranque**:
   En la consola del servidor, busca los mensajes de confirmación:
   ```text
   [Croppy] Registered 4 crop species definitions.
   [Croppy] Registered 3 station definitions.
   [Croppy] Registered recipe definitions into RecipeRegistry.
   [Croppy] Initialized SQLite database connection pool via HikariCP.
   [Croppy] Successfully enabled Croppy v0.3.0.
   ```

---

## 3. Tu Primer Cultivo: Guía Práctica

Sigue estos pasos dentro del juego para sembrar, cuidar y cosechar tu primer cultivo custom.

### Paso 1: Obtener Herramientas y Semillas
Con permisos de administración (`croppy.admin`), ejecuta:
```text
/croppy item give seeds wheat_seeds 5
/croppy item give watering_cans copper_can 1
/croppy item give tools inspector 1
```

### Paso 2: Preparar e Hidratar la Tierra Arada
1. Ara un bloque de tierra utilizando cualquier azada.
2. Toma la **Regadera de Cobre** y haz **Clic Derecho** sobre una fuente de agua o caldero para llenarla.
3. Haz **Clic Derecho** sobre la tierra arada con la regadera para depositar agua. El nivel de hidratación subirá hasta 8.

```text
+-----------------------------------------------------------+
|                   INDICADOR DE HIDRATACIÓN                |
|  [Tierra Arada] Humedad: 8/8  *** (Completamente Hidratado)|
+-----------------------------------------------------------+
```

### Paso 3: Sembrar las Semillas
Sostén las `Semillas de Trigo` en la mano y haz **Clic Derecho** en la tierra arada. Croppy registrará una nueva instancia de cultivo en esa coordenada.

### Paso 4: Inspeccionar el Progreso
Toma el **Inspector de Cultivos y Suelo** y haz **Clic Derecho** en la planta:

```text
-------- [ Inspección de Cultivo ] --------
Especie: Trigo (croppy:wheat)
Etapa de Crecimiento: 1 / 7 (Plántula)
Progreso: [======------------] 35%
Nivel de Luz: 14 / 9 (Óptimo)
Hidratación: 8 / 4 (Hidratado)
Nutrientes N-P-K: 100 / 100 / 100
-------------------------------------------
```

### Paso 5: Cosechar
Cuando el cultivo complete su última fase (100% de progreso), rómpelo o haz **Clic Derecho**. Recibirás el trigo cosechado y semillas frescas para volver a sembrar.
