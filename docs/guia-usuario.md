# Guía de usuario de NOBAITC

NOBAITC es una aplicación de escritorio para generar fracciones factoriales mediante NONBPA y analizar la estructura de alias de una matriz experimental.

## Iniciar el programa

- **Versión distribuida:** ejecuta `dist-out/NOBAITC/NOBAITC.exe`. Conserva la carpeta `NOBAITC` completa; incluye el runtime de Java y los archivos necesarios.
- **Desde el código fuente:** con Java 17 y Maven instalados, ejecuta `mvn javafx:run` desde la carpeta del proyecto.

Al iniciar se abre el módulo **NONBPA**. La barra superior permite cambiar entre **NONBPA** y **Alias Structure**. La opción **Others** aún no está disponible.

## Generar fracciones NONBPA

1. En **Datos**, configura los niveles de cada factor. La pantalla inicia con dos factores; usa los botones `+` y `−` para agregar o quitar factores. Ingresa al menos 2 niveles por factor.
2. Revisa **Información del Diseño**. Un diseño válido debe tener entre 1 y 15 factores y cumplir `TR = LCM`, donde `TR` es el producto de los niveles y `LCM` su mínimo común múltiplo.
3. Define el **Tamaño de fracción (SF)**. Debe estar en el intervalo `SF min ≤ SF < TR`. La aplicación calcula `SF min = max(GL, nivel mayor)`, con `GL = número de factores + 2`.
4. Elige el tipo de fracción:
   - **Fracciones aleatorias:** indica cuántas fracciones generar. El inicio de cada una se elige aleatoriamente y sin repetir, entre `1` y `TR`.
   - **Fracciones personalizadas:** escribe uno o más índices de inicio, separados por comas o espacios; también se aceptan corchetes, por ejemplo `1, 3, 5` o `[1 3 5]`. Cada índice debe estar entre `1` y `TR`. En este modo, la lista determina cuántas fracciones se generan; el campo **Número de fracciones** no limita la lista.
5. Pulsa **Generar Fracciones**. El cálculo se ejecuta en segundo plano. Si hay errores de entrada o el diseño no es válido, la aplicación muestra un aviso.

La pestaña **Tablas** muestra las fracciones generadas. Sus columnas incluyen inicio, matriz de datos, métricas GBM y J2, GBM por factor, VIF y resumen de alias.

## Revisar, copiar y exportar resultados

- Haz doble clic en una fila para copiar la matriz de esa fracción al portapapeles y abrir su ventana de detalles.
- Selecciona una fila y pulsa **Copiar tabla** para copiar su resumen y matriz al portapapeles.
- Pulsa **EXCEL** o **PDF** para elegir dónde guardar el reporte.
- Para analizar una fracción, selecciona su fila y pulsa **Enviar a Alias**. La matriz se carga en el módulo de alias.
- **Limpiar datos** elimina resultados y restablece el formulario.

## Analizar una estructura de alias

En **Alias Structure** hay dos formas de introducir la matriz:

- **Matrix:** define `m` (corridas/filas) y `n` (factores/columnas), completa las celdas y pulsa **Calcular Alias**. La cuadrícula admite dimensiones de hasta 120 por lado. Las celdas vacías se interpretan como `0`; completa los datos explícitamente para evitar resultados accidentales.
- **PlainText:** pega una fila por corrida. Separa filas con punto y coma o saltos de línea, y valores con espacios o comas. Se permiten corchetes exteriores. Por ejemplo:

  ```text
  1 1 1
  1 2 1
  2 1 2
  2 2 2
  ```

  Todas las filas deben tener el mismo número de valores. Usa punto para decimales, por ejemplo `1.5`.

Pulsa **Calcular Alias**. El resultado indica las dimensiones, si el diseño es ortogonal o cuántos alias encontró, y presenta las relaciones entre efectos principales e interacciones. Para cálculos grandes puede aparecer una confirmación previa; puedes continuar o cancelar. **Limpiar** borra la entrada y el resultado.

## Diagrama

Consulta el [diagrama de flujo del programa](diagrama-flujo.md) para ver el recorrido desde el inicio hasta la generación, exportación y análisis de alias.

## Solución de problemas

- **Diseño no válido:** comprueba que todos los niveles sean al menos 2, que haya como máximo 15 factores y que `TR = LCM`.
- **Tamaño de fracción no válido:** utiliza un valor desde `SF min` hasta un valor menor que `TR`.
- **Índice personalizado fuera de rango:** cada inicio debe estar entre `1` y `TR`.
- **Matriz de alias inválida:** verifica que no haya filas vacías ni filas con diferente cantidad de valores; en la cuadrícula, comprueba las dimensiones y las celdas.
- **El ejecutable no inicia:** ejecuta el `.exe` desde la carpeta `NOBAITC` extraída completa, no de forma aislada.