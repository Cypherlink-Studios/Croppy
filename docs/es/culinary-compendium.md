---
title: Compendio Culinario
description: Guía de uso del recetario interactivo, navegación por categorías de platos, ítem físico y visualización de recetas.
sidebar:
  order: 12
---

# Compendio Culinario (Recetario)

El **Compendio Culinario** es la enciclopedia gastronómica interactiva de Croppy. Proporciona a los jugadores un libro ilustrado dentro del juego para seguir sus avances culinarios, verificar requerimientos de preparación y clasificar sus platos descubiertos.

---

## 1. Acceso al Compendio

Los jugadores cuentan con dos formas de abrir su libro de recetas:

1. **El Ítem Físico del Libro de Recetas (`croppy:recipe_book`)**:
   * Un libro encuadernado en cuero con adornos de latón.
   * Haz **Clic Derecho** con el libro en la mano en cualquier lugar del mundo para abrir el menú.
2. **Comando Directo**:
   * Escribe `/croppy recipes` en el chat.
   * Puedes abrir directamente una pestaña concreta: ej. `/croppy recipes bakery` o `/croppy recipes soups`.

---

## 2. Interfaz y Categorías Gastronómicas

El compendio se presenta en una matriz de 54 casillas:

```text
+-----------------------------------------------------------+
|                   COMPENDIO CULINARIO                     |
+-----------------------------------------------------------+
| [TODOS] [PANADERÍA] [SOPAS] [COCINA] [BEBIDAS] [CONSERVAS]|  <-- Pestañas
|-----------------------------------------------------------|
|                                                           |
|    [Plato 1]    [Plato 2]    [Plato 3]    [Plato 4]  ...  |  <-- Cuadrícula
|    [Plato 5]    [ ????? ]    [Plato 7]    [Plato 8]  ...  |
|                                                           |
|-----------------------------------------------------------|
|  [<< Anterior]           [Página 1 / 3]      [Siguiente >>|  <-- Navegación
+-----------------------------------------------------------+
```

### Categorías de Alimentos
* **Panadería (Bakery)**: Panes rústicos, bollos, pasteles, tartas y repostería.
* **Sopas y Guisos (Soups)**: Caldos nutritivos, sopas de verduras, cremas y estofados.
* **Cocina (Cooking)**: Cortes de carne sellados, salteados, pescados a la plancha y platos fuertes.
* **Bebidas (Beverages)**: Infusiones de té, jugos prensados, sidras y tónicos reconstituyentes.
* **Conservas (Preserves)**: Mermeladas, vegetales encurtidos, fermentos y frutas deshidratadas.

---

## 3. Ficha de Detalle de una Receta

Al hacer clic en cualquier receta desbloqueada, el tooltip muestra su ficha técnica:

* **Nombre y Calidad**: Título coloreado e indicador de rareza.
* **Estación Necesaria**: Aparato de cocina requerido (`Olla de Cocina` o `Sartén`).
* **Zona Térmica**: Rango de temperatura óptimo en grados Celsius (`120°C - 180°C`).
* **Duración de Cocción**: Segundos necesarios para alcanzar la textura ideal (ej. `12.5s`).
* **Ingredientes Requeridos**: Lista y cantidades exactas de productos de la granja.
* **Resultado Previsto**: Ítems obtenidos y posibles efectos gastronómicos.
