Lema Vázquez, Christian

# Práctica 2 de Recuperación de Información

## Estructura

```text
src/main/java/es/udc/fi/irudc/pri/Cord19/
├── P2UtilsCord19.java              # Utilidades 
├── TopicSearcher.java              # Tarea 1: búsqueda sobre topics y generación de run TREC
├── EvalCord19.java                 # Tarea 2: evaluación de runs con métricas IR
├── BestValuesCord19.java           # Tarea 3: búsqueda de mejores valores de lambda
├── RankQueriesCord19.java          # Tarea 4: ranking de queries por dificultad
└── MetricCorrelationCord19.java    # Tarea 5: correlación Kendall tau-b entre métricas
```

`P2UtilsCord19.java` no se ejecuta directamente. Es una clase auxiliar usada por las demás clases.

---

## Archivos necesarios

Para ejecutar la práctica 2 se necesitan estos archivos:

```text
index-cord19/                         # Índice Lucene generado previamente
topics-rnd5.xml                       # Archivo XML con los 50 topics
qrels-covid_d5_j0.5-5.txt             # Archivo de juicios de relevancia
p2-results/                           # Carpeta para guardar resultados
```

El índice debe haberse creado con `StandardAnalyzer` y con `LMJelinekMercerSimilarity` en el `IndexWriterConfig`, ya que la práctica 2 usa ese modelo para realizar las búsquedas.

En los comandos se usa `index-cord19` como ruta de ejemplo del índice y `p2-results` como carpeta de salida. Se pueden sustituir por rutas absolutas.

---

## Campos de búsqueda

Las clases aceptan estos campos:

```text
title
abstract
full_text
full-text
```

Internamente, `full-text` se transforma a `full_text`, porque ese es el nombre usado normalmente en el índice Lucene.

---

# Instrucciones de ejecución

## Tarea 1: Search — `TopicSearcher.java`

### Uso general

```bash
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.TopicSearcher "-Dexec.args=<indexPath> <topicsFile> <runOutput.txt> <lambda> <field> <range>"
```

### Parámetros

```text
<indexPath>      Ruta al índice Lucene
<topicsFile>     Ruta al archivo topics-rnd5.xml
<runOutput.txt>  Archivo de salida en formato TREC
<lambda>         Valor de lambda para LMJelinekMercerSimilarity
<field>          Campo de búsqueda: title, abstract, full_text o full-text
<range>          Rango de topics: 1-50, 5-5, 10-29, etc.
```

### Ejemplos

```bash
# Buscar los 50 topics en el campo title con lambda 0.1
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.TopicSearcher "-Dexec.args=index-cord19 topics-rnd5.xml p2-results/run_title_l01.txt 0.1 title 1-50"

# Buscar los 50 topics en abstract con lambda 0.3
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.TopicSearcher "-Dexec.args=index-cord19 topics-rnd5.xml p2-results/run_abstract_l03.txt 0.3 abstract 1-50"

# Buscar los 50 topics en full_text con lambda 0.7
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.TopicSearcher "-Dexec.args=index-cord19 topics-rnd5.xml p2-results/run_fulltext_l07.txt 0.7 full_text 1-50"

# Buscar solo el topic 5
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.TopicSearcher "-Dexec.args=index-cord19 topics-rnd5.xml p2-results/run_topic5.txt 0.1 abstract 5-5"

# Buscar un rango concreto de topics
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.TopicSearcher "-Dexec.args=index-cord19 topics-rnd5.xml p2-results/run_10_29.txt 0.1 abstract 10-29"
```

---

## Tarea 2: Eval — `EvalCord19.java`

### Uso general

```bash
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.EvalCord19 "-Dexec.args=<resultsFile.txt> <qrelsFile.txt> <outputCsv.csv>"
```

### Parámetros

```text
<resultsFile.txt>  Archivo run en formato TREC generado por TopicSearcher
<qrelsFile.txt>    Archivo qrels-covid_d5_j0.5-5.txt
<outputCsv.csv>    Archivo CSV de salida con métricas por topic y promedio
```

### Ejemplos

```bash
# Evaluar title
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.EvalCord19 "-Dexec.args=p2-results/run_title_l01.txt qrels-covid_d5_j0.5-5.txt p2-results/eval_title_l01.csv"

# Evaluar abstract
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.EvalCord19 "-Dexec.args=p2-results/run_abstract_l03.txt qrels-covid_d5_j0.5-5.txt p2-results/eval_abstract_l03.csv"

# Evaluar full_text
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.EvalCord19 "-Dexec.args=p2-results/run_fulltext_l07.txt qrels-covid_d5_j0.5-5.txt p2-results/eval_fulltext_l07.csv"
```

---

## Tarea 3: BestValues — `BestValuesCord19.java`

Los valores probados son:

```text
0.01, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0
```

### Uso general

```bash
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.BestValuesCord19 "-Dexec.args=<indexPath> <topicsFile> <qrelsFile> <outputDir>"
```

### Parámetros

```text
<indexPath>   Ruta al índice Lucene
<topicsFile>  Ruta al archivo topics-rnd5.xml
<qrelsFile>   Ruta al archivo qrels-covid_d5_j0.5-5.txt
<outputDir>   Carpeta donde guardar lo generado y resumen final
```

### Ejemplo

```bash
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.BestValuesCord19 "-Dexec.args=index-cord19 topics-rnd5.xml qrels-covid_d5_j0.5-5.txt p2-results/best-values"
```

---

## Tarea 4: RankQueries — `RankQueriesCord19.java`

### Uso general

```bash
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.RankQueriesCord19 "-Dexec.args=<resultsFile.txt> <qrelsFile.txt> <range> <metric>"
```

### Parámetros

```text
<resultsFile.txt>  Archivo run generado por TopicSearcher
<qrelsFile.txt>    Archivo qrels-covid_d5_j0.5-5.txt
<range>            Rango de queries: 1-50, 5-5, 10-29, etc.
<metric>           Métrica: P@10, R@10, MAP@10, MAP@100, NDCG@10 o NDCG@100
```

### Ejemplos

```bash
# Ordenar las 50 queries por MAP@100
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.RankQueriesCord19 "-Dexec.args=p2-results/run_abstract_l03.txt qrels-covid_d5_j0.5-5.txt 1-50 MAP@100"

# Ordenar las 50 queries por NDCG@100
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.RankQueriesCord19 "-Dexec.args=p2-results/run_abstract_l03.txt qrels-covid_d5_j0.5-5.txt 1-50 NDCG@100"

# Ordenar solo las queries 10 a 29 por P@10
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.RankQueriesCord19 "-Dexec.args=p2-results/run_abstract_l03.txt qrels-covid_d5_j0.5-5.txt 10-29 P@10"
```

La salida se muestra por pantalla, ordenada de más difícil a más fácil.

---

## Tarea 5: MetricCorrelation — `MetricCorrelationCord19.java`

### Uso general

```bash
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.MetricCorrelationCord19 "-Dexec.args=<resultsFile.txt> <qrelsFile.txt> <range> <outputFile.txt>"
```

### Parámetros

```text
<resultsFile.txt>  Archivo run generado por TopicSearcher
<qrelsFile.txt>    Archivo qrels-covid_d5_j0.5-5.txt
<range>            Rango de queries: 1-50, 5-5, 10-29, etc.
<outputFile.txt>   Archivo donde guardar ranking de dificultad y matriz Kendall tau-b
```

### Ejemplos

```bash
# Correlación usando las 50 queries
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.MetricCorrelationCord19 "-Dexec.args=p2-results/run_abstract_l03.txt qrels-covid_d5_j0.5-5.txt 1-50 p2-results/metric_correlation_abstract.txt"

# Correlación usando solo queries 10-29
mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.MetricCorrelationCord19 "-Dexec.args=p2-results/run_abstract_l03.txt qrels-covid_d5_j0.5-5.txt 10-29 p2-results/metric_correlation_10_29.txt"
```


---


### Resultados T3
| Campo     | Mejor lambda MAP@100 |  MAP@100 | Mejor lambda NDCG@100 | NDCG@100 |
| --------- | -------------------: | -------: | --------------------: | -------: |
| title     |                 0.40 | 0.212857 |                  0.60 | 0.320349 |
| abstract  |                 0.60 | 0.271706 |                  0.30 | 0.382153 |
| full_text |                 0.70 | 0.249402 |                  0.80 | 0.357679 |

### Resultados T5
#### Matriz de correlación
| Métrica  |     P@10 |     R@10 |   MAP@10 |  MAP@100 |  NDCG@10 | NDCG@100 |
| -------- | -------: | -------: | -------: | -------: | -------: | -------: |
| P@10     | 1.000000 | 0.584077 | 0.892173 | 0.679022 | 0.822149 | 0.655107 |
| R@10     | 0.584077 | 1.000000 | 0.578477 | 0.362894 | 0.552599 | 0.362894 |
| MAP@10   | 0.892173 | 0.578477 | 1.000000 | 0.592540 | 0.836833 | 0.577768 |
| MAP@100  | 0.679022 | 0.362894 | 0.592540 | 1.000000 | 0.566640 | 0.882449 |
| NDCG@10  | 0.822149 | 0.552599 | 0.836833 | 0.566640 | 1.000000 | 0.609159 |
| NDCG@100 | 0.655107 | 0.362894 | 0.577768 | 0.882449 | 0.609159 | 1.000000 |
