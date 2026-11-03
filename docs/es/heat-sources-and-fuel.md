---
title: Fuentes Térmicas y Combustible
description: Guía de estaciones térmicas, estufas, combustión, curvas dinámicas de temperatura y disipación de calor.
sidebar:
  order: 11
---

# Fuentes Térmicas y Combustibles

Las estaciones de cocina como la olla y la sartén requieren energía térmica para preparar los alimentos. En lugar de interruptores binarios simples (encendido/apagado), Croppy modela curvas termodinámicas mediante **Fuentes de Calor**, destacando la Estufa Rústica.

---

## 1. La Estufa Rústica (`croppy:stove`)

La estufa actúa como el generador principal de calor en la cocina:

```text
+-----------------------------------------------------------+
|               CONFIGURACIÓN DE COCINA APILADA             |
+-----------------------------------------------------------+
|                                                           |
|       [OLLA / SARTÉN]      <-- Recibe el calor transferido|
|              |                  por convección            |
|              v                                            |
|       [ESTUFA RÚSTICA]     <-- Quema combustible sólido   |
|                                 y genera temperatura      |
+-----------------------------------------------------------+
```

### Especificación Térmica
* **Temperatura Ambiente Mínima**: `20.0°C` (temperatura base)
* **Pico Máximo Seguro**: `350.0°C`
* **Tasa de Calentamiento**: `+5.0°C` por tick mientras el combustible esté ardiendo
* **Tasa de Enfriamiento**: `-2.0°C` por tick al apagarse, disipando calor hacia el ambiente
* **Multiplicador de Combustión**: `1.0x` base

---

## 2. Tipos de Combustibles y Calor Aportado

La estufa admite combustibles vanilla o ítems personalizados de Croppy introducidos físicamente o mediante su menú (`croppy:stove_menu`):

| Combustible | Duración (Ticks) | Zona Térmica Alcanzada |
|---|---|---|
| **Tablas de Madera / Palos** | 300 - 600 ticks | **Tibia / Baja** (60°C - 100°C) |
| **Carbón / Carbón Vegetal** | 1,600 ticks | **Fuego Medio / Simmering** (100°C - 180°C) |
| **Bloque de Algas Secas** | 4,000 ticks | **Ebullición / Fuego Alto** (180°C - 240°C) |
| **Vara de Blaze** | 2,400 ticks | **Sellado Extremo** (240°C - 320°C) |

---

## 3. Manejo de la Estufa

1. **Colocar la Estufa**: Sitúa el bloque de Estufa Rústica en el suelo.
2. **Apilar la Estación de Cocina**: Coloca la Olla o la Sartén directamente sobre la estufa.
3. **Cargar Combustible**: Haz **Clic Derecho** en la estufa para abrir el menú del fogón o usa agacharse + clic derecho con el combustible en la mano.
4. **Encender**: Enciende el fogón con un mechero o botón de acción del menú.
5. **Monitorear**: Observa la temperatura en el display holográfico superior o utiliza el **Termómetro Culinario** (`/croppy item give tools thermometer 1`).
