---
title: Introducción y Visión General
description: Resumen completo, mecánicas de simulación, arquitectura y características principales de Croppy.
sidebar:
  order: 1
---

# Centro de Documentación de Croppy

Bienvenido a la documentación oficial de **Croppy**, un motor extensible y modular de agricultura, ingredientes, estaciones de procesamiento y simulación culinaria diseñado para servidores modernos Paper (1.21.8+).

Croppy une la agricultura tradicional de Minecraft con una experiencia culinaria profunda e inmersiva. Transforma el cultivo en un ecosistema vivo: las semillas exigen luz solar y humedad específicas, los nutrientes del suelo se agotan y requieren fertilización, las estaciones físicas cocinan ingredientes sobre fuentes térmicas dinámicas y los chefs descubren recetas secretas en un compendio culinario interactivo.

---

## Aspectos Destacados de Jugabilidad

```text
+-----------------------------------------------------------------------------+
|                          CARACTERÍSTICAS DE CROPPY                          |
+-----------------------------------------------------------------------------+
|                                                                             |
|  [1. SIMULACIÓN AGRÍCOLA DINÁMICA]                                          |
|  Los cultivos evalúan niveles de luz, humedad del suelo, temperatura        |
|  ambiente e invernaderos en lugar de ticks aleatorios de Minecraft.         |
|                                                                             |
|  [2. DINÁMICA DE NUTRIENTES DEL SUELO (N-P-K)]                              |
|  La tierra arada almacena reservas de Nitrógeno, Fósforo y Potasio que se  |
|  agotan con el crecimiento y se reponen con fertilizantes orgánicos.        |
|                                                                             |
|  [3. RIEGO MANUAL Y PROGRESIVO]                                             |
|  Regaderas por niveles hidratan áreas multirango con capacidades custom,    |
|  propiedades de retención de humedad y tasas naturales de evaporación.      |
|                                                                             |
|  [4. ESTACIONES DE COCINA FÍSICAS]                                          |
|  Coloca sartenes y ollas directamente sobre fuentes de calor con displays   |
|  virtuales 3D, tiempos de cocción y mecánicas realistas de quemado.         |
|                                                                             |
|  [5. FUENTES TÉRMICAS VARIABLES Y COMBUSTIBLES]                             |
|  Estufas que consumen combustibles sólidos generando curvas de temperatura  |
|  que transfieren calor hacia las estaciones superiores.                     |
|                                                                             |
|  [6. DESCUBRIMIENTO DE RECETAS Y COMPENDIO CULINARIO]                       |
|  Explora un recetario categorizado, desbloquea recetas secretas mediante    |
|  la experimentación e inspecciona los ingredientes requeridos.              |
|                                                                             |
|  [7. SISTEMA MULTI-IDIOMA NATIVO (i18n)]                                    |
|  Mensajes, barras de acción, tooltips y nombres de ítems se muestran en     |
|  el idioma del cliente Minecraft del jugador mediante Crowdin.              |
|                                                                             |
|  [8. SISTEMA MODULAR DE ADDONS Y FUSIÓN DE RESOURCE PACKS]                  |
|  Expande mecánicas mediante addons de código Java o packs YAML declarativos  |
|  con ciclo de vida en caliente (/croppy addon) y resource packs automáticos.|
|                                                                             |
+-----------------------------------------------------------------------------+
```

---

## Principios Arquitectónicos

Croppy está desarrollado siguiendo una estricta **Arquitectura Hexagonal (Puertos y Adaptadores)**:

- **Cero Bukkit en el Núcleo**: El motor de dominio (`croppy-core`) y los contratos públicos (`croppy-api`) están completamente desacoplados de las APIs de Minecraft. Ejecutan lógica pura en Java 21, facilitando pruebas unitarias rápidas y máxima estabilidad.
- **Adaptador de Paper**: El módulo `croppy-paper` traduce los eventos del servidor, controla las entidades virtuales con PacketEvents, gestiona tareas asíncronas de base de datos e interactúa con el jugador.
- **Validación Rápida al Iniciar (Fail-Fast)**: Todos los archivos YAML, definiciones de especies, recetas y menús se validan estrictamente al arrancar para detectar inconsistencias antes de que los jugadores ingresen.

---

## Terminología Central

- **Especie**: Definición lógica de un cultivo (`croppy:wheat`, `croppy:carrot`), que determina sus fases de vida, tolerancias ambientales, apetito de nutrientes y botín de cosecha.
- **Instancia de Cultivo**: Cultivo activo en el mundo, vinculado a una coordenada de bloque con seguimiento de progreso, salud y edad.
- **Humedad de Tierra Arada**: Nivel discreto de hidratación (0 a 8) en la tierra arada, afectado por regaderas, lluvia y evaporación.
- **Nutrientes del Suelo**: Reserva de Nitrógeno (N), Fósforo (P) y Potasio (K) presente en los bloques de cultivo.
- **Estación de Trabajo**: Bloque físico (ej. olla, sartén) capaz de contener ingredientes y ejecutar procesos culinarios.
- **Fuente Térmica**: Componente de estación (como una estufa de leña) que consume combustible y transfiere calor.
- **Compendio Culinario**: Enciclopedia interactiva de recetas (`croppy:recipe_book`) que permite consultar preparaciones ordenadas por categorías.
- **Addon**: Módulo externo (JAR o directorio de pack declarativo) ubicado en `plugins/Croppy/addons/` que inyecta nuevos cultivos, ítems, estaciones y lógica personalizada en Croppy.
