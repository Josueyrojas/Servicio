# Diagrama de flujo del programa

El siguiente diagrama resume la navegación principal, la generación NONBPA y el análisis de alias. Los cálculos se ejecutan en segundo plano; los resultados y errores se muestran en la interfaz.

```mermaid
flowchart TD
    A([Iniciar NOBAITC]) --> B[Mostrar módulo NONBPA]
    B --> C{Elegir módulo}

    C -->|NONBPA| N1[Configurar niveles de factores]
    C -->|Alias Structure| X1[Elegir entrada de matriz]
    C -->|Others| O[Opción no disponible]
    O --> C

    N1 --> N2[Calcular y mostrar TR, LCM, GL y SF min]
    N2 --> N3{¿Diseño válido?}
    N3 -->|No| NE[Mostrar error de diseño]
    NE --> N1
    N3 -->|Sí| N4[Definir SF y modo de generación]
    N4 --> N5{¿Modo aleatorio?}
    N5 -->|Sí| N6[Leer cantidad solicitada]
    N5 -->|No| N7[Leer índices personalizados]
    N6 --> N8{¿Parámetros válidos?}
    N7 --> N8
    N8 -->|No| NV[Mostrar error de entrada]
    NV --> N4
    N8 -->|Sí| N9[Extraer fracciones y calcular GBM, J2 y VIF]
    N9 --> N10[Mostrar tabla y resumen]
    N10 --> NA{Elegir acción}
    NA -->|Ver detalles| ND[Abrir detalles y copiar matriz]
    NA -->|Copiar tabla| NC[Copiar resumen y matriz]
    NA -->|Exportar Excel| NE1[Guardar reporte Excel]
    NA -->|Exportar PDF| NP[Guardar reporte PDF]
    NA -->|Enviar a Alias| X2[Cargar matriz seleccionada en Alias]
    NA -->|Limpiar| NR[Restablecer formulario]
    ND --> NA
    NC --> NA
    NE1 --> NA
    NP --> NA
    NR --> N1
    X2 --> X3[Elegir Matrix o PlainText]

    X1 --> X3
    X3 -->|Matrix| X4[Definir dimensiones y completar celdas]
    X3 -->|PlainText| X5[Ingresar filas y columnas de la matriz]
    X4 --> X6[Validar matriz]
    X5 --> X6
    X6 -->|Inválida| XE[Mostrar error de formato o valores]
    XE --> X3
    X6 -->|Válida| X7{¿Cálculo grande?}
    X7 -->|Sí| X8{¿Continuar?}
    X8 -->|No| X3
    X8 -->|Sí| X9[Calcular estructura de alias en segundo plano]
    X7 -->|No| X9
    X9 --> X10{¿Cálculo correcto?}
    X10 -->|No| X11[Mostrar error]
    X10 -->|Sí| X12[Mostrar ortogonalidad y relaciones de alias]
    X11 --> X3
    X12 --> X13{¿Limpiar o cambiar módulo?}
    X13 -->|Limpiar| X3
    X13 -->|Cambiar módulo| C
```