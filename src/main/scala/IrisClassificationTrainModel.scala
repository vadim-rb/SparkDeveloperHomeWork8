import org.apache.spark.SparkContext
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.DecisionTreeClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.sql.SparkSession

object IrisClassificationTrainModel extends App {
  /*
    <label> <index1>:<value1> <index2>:<value2> <index3>:<value3> <index4>:<value4>
    0.0 1:5.1 2:3.5 3:1.4 4:0.2
    label
      Это сорт Ириса: 0 — Setosa, 1 — Versicolor, 2 — Virginica.
    index1
      sepal length in cm
    index2
      sepal width in cm
    index3
      petal length in cm
    index4
      petal width in cm
    Соответственно, наша задача — по имеющимся данным попробовать найти зависимости между размерами лепестков и сортами Ирисов
     */


  def train(spark: SparkSession, dataPath: String, modelPath: String)(implicit sc: SparkContext): Unit = {

    val irisData = spark.read
      .format("libsvm")
      .load(dataPath)

    //70% на обучающие данные, 30% на проверочные
    val Array(trainingData, testData) = irisData.randomSplit(Array(0.7, 0.3))
    //дерево решений
    val dt = new DecisionTreeClassifier()
      .setLabelCol("label")
      .setFeaturesCol("features")

    /*
    Конвейер (Pipeline), который связывает несколько преобразователей и оценщиков в единый рабочий процесс
    машинного обучения. Apache Spark предоставляет класс, который формируется путем объединения различных этапов
    конвейера, т.е. Estimator’ов и Transformer’ов, выполняемых последовательно. В классе конвейера есть метод fit(),
    который запускает весь рабочий процесс. Он возвращает модель PipelineModel, которая имеет точно такое же
    количество этапов, что и конвейер, за исключением того, что все этапы оценщика заменяются соответствующим
    преобразователем, полученным во время выполнения. Эта модель конвейера может быть сериализована для повторного
     использования без затрат на настройку или обучение. Во время выполнения каждый этап вызывается последовательно,
      в зависимости от его типа (преобразователь или оценщик) вызываются соответствующие методы fit() или transform().
     */
    val pipeline = new Pipeline()
      .setStages(Array(dt))
    //Запуск pipeline
    val model = pipeline.fit(trainingData)
    /*
    Преобразователь (Transformer) – алгоритм, который может преобразовывать один DataFrame в другой.
    Например, ML-модель – это трансформер, который преобразует DataFrame с предикторами (фичами) в DataFrame с
    прогнозами. Трансформер можно представить в виде абстракции, которая включает преобразователи фичей и обученные
    модели. Технически Transformer реализует метод transform(), который преобразует один DataFrame в другой
    путем добавления одного или нескольких столбцов
     */
    val predictions = model.transform(testData)
    //MulticlassClassificationEvaluator Класс для оценки модели
    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol("label")
      .setPredictionCol("prediction")
      .setMetricName("accuracy")
    // accuracy точность модели
    val accuracy = evaluator.evaluate(predictions)

    println(s"Test set accuracy= ${(accuracy)}")

    model.write.overwrite().save(modelPath)

    println(s"model saved at $modelPath")

  }

  val spark = SparkSession
    .builder()
    .appName("HomeWork7")
    .config("spark.master", "local")
    .config("spark.eventLog.enabled", "true")
    .config("spark.eventLog.dir", "file:///home/vadim/MyExp/spark-logs/event")
    .getOrCreate()
  spark.sparkContext.setLogLevel("ERROR")

  train(spark,dataPath = "./src/main/resources/iris_libsvm.txt",modelPath = "./src/main/resources/model/")(spark.sparkContext)



}
