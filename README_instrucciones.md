Lema Vázquez, Christian
# Práctica de Recuperación de Información

## Estructura

```
src/main/java/es/udc/fi/irudc/pri/Cord19/
├── IndexCord19.java           # Tarea 1: Indexación concurrente
├── SearchCord19.java          # Tarea 2: Búsqueda por campo único
├── SearchCord19MultiField.java # Tarea 3: Búsqueda multi-campo
├── StatsCord19.java           # Tareas 4 y 5: Estadísticas del índice
├── ReadCord19.java            # Clase auxiliar de lectura de metadatos y JSON
└── Cord19.java                # Modelo de datos (record)
```

### Rutas por Defecto

- **Colección CORD-19**: `%HOME%/Documents/GIF/3eiro/RI/resourcesCord19/2020-07-16`
- **Índice**: `./index-cord19` (en el directorio actual)
- **Hilos de indexación**: 4
- **Almacenar full-text**: No (para ahorrar espacio)

---

## Instrucciones de Ejecución

## Tarea 1: Indexación (IndexCord19.java)

### Uso

```bash
# Con valores por defecto (4 hilos, sin almacenar full-text)
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.IndexCord19

# Con 8 hilos
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.IndexCord19 "-Dexec.args=-threads 8"

# Con colección personalizada
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.IndexCord19 "-Dexec.args=-collection C:\ruta\a\cord19 -index C:\ruta\a\indice -threads 8"

# Almacenando full-text (aumenta tamaño del índice)
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.IndexCord19 "-Dexec.args=-threads 8 -storeFullText"

```
### Opciones
`-collection <ruta>` | Ruta a la colección CORD-19 \
`-index <ruta>` | Ruta donde crear el índice \
`-threads <n>` | Número de hilos para indexación \
`-storeFullText` | Almacenar el campo full-text 

---

## Tarea 2: Búsqueda por Campo Único (SearchCord19.java)

### Uso

```bash
# Búsqueda básica (10 resultados por defecto)
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.SearchCord19 "-Dexec.args=index-cord19 title coronavirus"

# Especificando número de resultados
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.SearchCord19 "-Dexec.args=index-cord19 title coronavirus 20"

# Búsqueda en abstract
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.SearchCord19 "-Dexec.args=index-cord19 abstract vaccine"

# Búsqueda en full_text
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.SearchCord19 "-Dexec.args=index-cord19 full_text COVID-19"

# Con índice personalizado
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.SearchCord19 "-Dexec.args=C:\ruta\a\indice title coronavirus 15"
```

---

## Tarea 3: Búsqueda Multi-Campo (SearchCord19MultiField.java)

### Uso

```bash
# Búsqueda en dos campos (10 resultados por defecto)
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.SearchCord19MultiField "-Dexec.args=index-cord19 coronavirus title abstract"

# Especificando número de resultados
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.SearchCord19MultiField "-Dexec.args=index-cord19 coronavirus title abstract 20"

# Búsqueda en mas campos
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.SearchCord19MultiField "-Dexec.args=index-cord19 vaccine title abstract authors journal"

# Con índice personalizado
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.SearchCord19MultiField "-Dexec.args=C:\ruta\a\indice COVID-19 title abstract 25"
```

---

## Tareas 4 y 5: Estadísticas del Índice (StatsCord19.java)

### Uso

#### Tarea 4: Estadísticas de Documento

```bash
# Análisis de términos en el título de un documento
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.StatsCord19 "-Dexec.args=index-cord19 0c1ud3y5 title"

# Análisis en el abstract
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.StatsCord19 "-Dexec.args=index-cord19 0c1ud3y5 abstract"

# Análisis en autores
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.StatsCord19 "-Dexec.args=index-cord19 0c1ud3y5 authors"

# Con índice personalizado
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.StatsCord19 "-Dexec.args=C:\ruta\a\indice 0c1ud3y5 title"
```

#### Tarea 5: Estadísticas de Colección

```bash
# Estadísticas generales del campo title
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.StatsCord19 "-Dexec.args=index-cord19 title"

# Estadísticas del abstract
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.StatsCord19 "-Dexec.args=index-cord19 abstract"

# Estadísticas del full_text
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.StatsCord19 "-Dexec.args=index-cord19 full_text"

# Con índice personalizado
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.StatsCord19 "-Dexec.args=C:\ruta\a\indice journal"
```



### ReadCord19.java

Clase auxiliar que lee y procesa la colección CORD-19 

```bash
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.ReadCord19 "-Dexec.args=C:\ruta\a\cord19"
```


## Análisis de Eficiencia

### Comparación de Indexación

Para analizar la eficiencia con diferentes números de hilos:

```bash
# 1 hilo
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.IndexCord19 "-Dexec.args=-threads 1"

# 4 hilos
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.IndexCord19 "-Dexec.args=-threads 4"

# 8 hilos
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.IndexCord19 "-Dexec.args=-threads 8"
```

### Comparación de Tamaño del Índice

```bash
# Sin almacenar full-text
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.IndexCord19

# Con almacenamiento de full-text
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.IndexCord19 "-Dexec.args=-storeFullText"
```

---

1. **Tiempo de indexación**:
    - Con 8 hilos: 176.932 segundos
    - Con 4 hilos: 293.741 segundos
    - Con 1 hilo: 576.321 segundos
2. **Espacio en Disco**:
    - Sin full-text: ~1-1.5 GB
    - Con full-text: ~2,2-3 GB

3. **Interrupción**: Se puede detener la indexación presionando Ctrl+C. El estado se puede recuperar ejecutando de nuevo.

---
